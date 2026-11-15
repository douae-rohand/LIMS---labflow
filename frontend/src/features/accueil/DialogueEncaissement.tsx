import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { FileText, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { Button, Input, Label, Select } from "@/components/lab";
import { annulerEncaissement, encaisser, ouvrirRecu, type FactureCaisse, type ModePaiement } from "@/api/accueil";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { formaterDate, messageErreur } from "@/features/responsable/commun";
import { formaterMontant, LIBELLES_MODE } from "./commun";

const MODES: ModePaiement[] = ["ESPECES", "CARTE", "CHEQUE", "VIREMENT"];
const AVEC_REFERENCE: ModePaiement[] = ["CHEQUE", "VIREMENT"];

export function DialogueEncaissement({ facture, onFermer }: { facture: FactureCaisse | null; onFermer: () => void }) {
  const queryClient = useQueryClient();
  const [montant, setMontant] = useState("");
  const [mode, setMode] = useState<ModePaiement>("ESPECES");
  const [reference, setReference] = useState("");
  /** Paiement en cours d'annulation et motif saisi. */
  const [aAnnuler, setAAnnuler] = useState<number | null>(null);
  const [motif, setMotif] = useState("");

  const factureId = facture?.id;
  useEffect(() => {
    if (facture) {
      setMontant(facture.reste > 0 ? facture.reste.toFixed(2) : "");
      setMode("ESPECES");
      setReference("");
      setAAnnuler(null);
      setMotif("");
    }
    // Réinitialisé à l'ouverture d'une autre facture uniquement.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [factureId]);

  const ouvrir = (paiementId: number) =>
    void ouvrirRecu(paiementId).catch((erreur: unknown) => toast.error(messageErreur(erreur, "Le reçu n'a pas pu être ouvert.")));

  const envoi = useMutation({
    mutationFn: () => encaisser(facture?.id as number, {
      montant: Number(montant.replace(",", ".")),
      mode,
      reference: reference.trim() || undefined,
    }),
    onSuccess: async (resultat) => {
      toast.success(resultat.facture.reste === 0
        ? "Paiement enregistré : la facture est réglée."
        : `Paiement enregistré. Reste à payer : ${formaterMontant(resultat.facture.reste)}.`);
      if (resultat.paiement) ouvrir(resultat.paiement.id);
      setMontant(resultat.facture.reste > 0 ? resultat.facture.reste.toFixed(2) : "");
      setReference("");
      await queryClient.invalidateQueries({ queryKey: ["accueil-caisse"] });
      await queryClient.invalidateQueries({ queryKey: ["accueil-tableau"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le paiement n'a pas pu être enregistré.")),
  });

  const annulation = useMutation({
    mutationFn: () => annulerEncaissement(aAnnuler as number, motif.trim()),
    onSuccess: async (resultat) => {
      toast.success(`Encaissement annulé. Reste à payer : ${formaterMontant(resultat.facture.reste)}.`);
      setAAnnuler(null);
      setMotif("");
      setMontant(resultat.facture.reste > 0 ? resultat.facture.reste.toFixed(2) : "");
      await queryClient.invalidateQueries({ queryKey: ["accueil-caisse"] });
      await queryClient.invalidateQueries({ queryKey: ["accueil-tableau"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'encaissement n'a pas pu être annulé.")),
  });

  const valeur = Number(montant.replace(",", "."));
  const erreur = !facture || facture.reste <= 0 ? null
    : !montant.trim() || Number.isNaN(valeur) || valeur <= 0 ? "Indiquez un montant positif."
      : valeur > facture.reste + 0.001 ? `Le montant dépasse le reste à payer (${formaterMontant(facture.reste)}).`
        : AVEC_REFERENCE.includes(mode) && !reference.trim()
          ? `Indiquez le numéro du ${mode === "CHEQUE" ? "chèque" : "virement"}.` : null;

  return (
    <Dialogue
      ouvert={facture != null}
      titre={`Facture ${facture?.numero ?? ""}`}
      description={facture ? `${facture.client ?? "Client"}${facture.demandeReference ? ` · demande ${facture.demandeReference}` : ""}` : ""}
      onFermer={onFermer}
    >
      {facture && (
        <div className="flex flex-col gap-4 text-sm">
          <dl className="grid grid-cols-3 gap-2 rounded-2xl bg-surface-alt p-3 text-center">
            <div><dt className="text-xs text-muted-foreground">Montant</dt><dd className="font-bold text-ink-900">{formaterMontant(facture.montant)}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Déjà réglé</dt><dd className="font-bold text-ink-900">{formaterMontant(facture.paye)}</dd></div>
            <div><dt className="text-xs text-muted-foreground">Reste</dt><dd className="font-bold text-brand-900">{formaterMontant(facture.reste)}</dd></div>
          </dl>

          {facture.reste > 0 && (
            <form
              className="flex flex-col gap-3"
              onSubmit={(event) => {
                event.preventDefault();
                if (!erreur) envoi.mutate();
              }}
            >
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="montant-encaisse">Montant reçu (MAD)</Label>
                  <Input id="montant-encaisse" inputMode="decimal" value={montant}
                    onChange={(event) => setMontant(event.target.value)} />
                  <p className="text-xs text-muted-foreground">Un montant inférieur au reste enregistre un règlement partiel.</p>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="mode-paiement">Mode de paiement</Label>
                  <Select id="mode-paiement" value={mode} onChange={(event) => setMode(event.target.value as ModePaiement)}>
                    {MODES.map((m) => <option key={m} value={m}>{LIBELLES_MODE[m]}</option>)}
                  </Select>
                </div>
              </div>
              {(AVEC_REFERENCE.includes(mode) || mode === "CARTE") && (
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="reference-paiement">
                    {mode === "CHEQUE" ? "Numéro du chèque" : mode === "VIREMENT" ? "Référence du virement" : "Référence de transaction (facultatif)"}
                  </Label>
                  <Input id="reference-paiement" value={reference} maxLength={255}
                    onChange={(event) => setReference(event.target.value)} />
                </div>
              )}
              {erreur && <p className="text-xs text-red-700">{erreur}</p>}
              <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={onFermer}>Fermer</Button>
                <Button type="submit" loading={envoi.isPending} disabled={erreur != null}>Encaisser et imprimer le reçu</Button>
              </div>
            </form>
          )}

          <section>
            <h3 className="mb-2 font-bold text-ink-900">Paiements</h3>
            {facture.paiements.length === 0 ? (
              <p className="text-xs text-muted-foreground">Aucun paiement enregistré.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {facture.paiements.map((p) => (
                  <li key={p.id} className={`flex flex-wrap items-center justify-between gap-2 rounded-xl border px-3 py-2 ${
                    p.contrePassation ? "border-red-200 bg-red-50/60" : "border-brand-900/10"}`}>
                    <div>
                      <p className={`font-semibold ${p.annule ? "text-muted-foreground line-through" : p.contrePassation ? "text-red-800" : "text-ink-900"}`}>
                        {formaterMontant(p.montant)} · {LIBELLES_MODE[p.mode] ?? p.mode}
                        {p.contrePassation && <span className="ml-2 text-xs font-bold no-underline">Annulation</span>}
                      </p>
                      {p.annule && <p className="text-xs font-semibold text-red-700">Encaissement annulé</p>}
                      <p className="text-xs text-muted-foreground">
                        {p.code} · {formaterDate(p.date, true)}{p.encaisseur ? ` · ${p.encaisseur}` : ""}
                        {p.reference ? ` · réf. ${p.reference}` : ""}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      {p.annulable && aAnnuler !== p.id && (
                        <Button size="sm" variant="ghost" icon={<Undo2 className="size-4" aria-hidden="true" />}
                          iconPosition="left" onClick={() => { setAAnnuler(p.id); setMotif(""); }}>
                          Annuler
                        </Button>
                      )}
                      <Button size="sm" variant="secondary" icon={<FileText className="size-4" aria-hidden="true" />}
                        iconPosition="left" onClick={() => ouvrir(p.id)}>
                        Reçu
                      </Button>
                    </div>
                    {aAnnuler === p.id && (
                      <form
                        className="flex w-full flex-col gap-2 border-t border-brand-900/10 pt-2"
                        onSubmit={(event) => {
                          event.preventDefault();
                          if (motif.trim().length >= 5) annulation.mutate();
                        }}
                      >
                        <Label htmlFor={`motif-annulation-${p.id}`}>Motif de l'annulation</Label>
                        <Input id={`motif-annulation-${p.id}`} value={motif} maxLength={200} autoFocus
                          placeholder="Ex. erreur de montant, mauvais mode de paiement…"
                          onChange={(event) => setMotif(event.target.value)} />
                        <p className="text-xs text-muted-foreground">
                          Une écriture négative compense ce reçu (il n'est jamais supprimé). Possible uniquement le jour même.
                        </p>
                        <div className="flex justify-end gap-2">
                          <Button type="button" size="sm" variant="ghost" onClick={() => setAAnnuler(null)}>Retour</Button>
                          <Button type="submit" size="sm" variant="secondary" className="text-red-700" loading={annulation.isPending}
                            disabled={motif.trim().length < 5}>
                            Confirmer l'annulation
                          </Button>
                        </div>
                      </form>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </Dialogue>
  );
}
