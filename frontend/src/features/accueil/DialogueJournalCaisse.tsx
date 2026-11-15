import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FileDown } from "lucide-react";
import { toast } from "sonner";
import { Button, Input, Label } from "@/components/lab";
import { lireJournalCaisse, ouvrirJournalCaisse } from "@/api/accueil";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { messageErreur } from "@/features/responsable/commun";
import { formaterMontant, LIBELLES_MODE } from "./commun";

/** Date du jour au format AAAA-MM-JJ (heure locale du poste). */
function jourLocal(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const heure = (iso: string) => new Date(iso).toLocaleTimeString("fr-MA", { hour: "2-digit", minute: "2-digit" });

/** Journal de caisse d'un jour : écritures, totaux par mode et clôture PDF. */
export function DialogueJournalCaisse({ ouvert, onFermer }: { ouvert: boolean; onFermer: () => void }) {
  const [jour, setJour] = useState(jourLocal);
  const journal = useQuery({
    queryKey: ["accueil-caisse", "journal", jour],
    queryFn: () => lireJournalCaisse(jour),
    enabled: ouvert && /^\d{4}-\d{2}-\d{2}$/.test(jour),
  });
  const donnees = journal.data;

  return (
    <Dialogue ouvert={ouvert} titre="Journal de caisse" description="Encaissements et annulations du jour, totaux par mode de paiement."
      onFermer={onFermer} large>
      <div className="flex flex-col gap-4 text-sm">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="jour-journal">Jour</Label>
            <Input id="jour-journal" type="date" value={jour} max={jourLocal()} onChange={(event) => setJour(event.target.value)} />
          </div>
          <Button variant="secondary" icon={<FileDown className="size-4" aria-hidden="true" />} iconPosition="left"
            disabled={!donnees}
            onClick={() => void ouvrirJournalCaisse(jour)
              .catch((erreur: unknown) => toast.error(messageErreur(erreur, "Le journal n'a pas pu être généré.")))}>
            Imprimer la clôture (PDF)
          </Button>
        </div>

        {journal.isLoading && <p className="text-muted-foreground">Chargement…</p>}
        {journal.isError && <p className="text-red-700">Le journal n'a pas pu être chargé.</p>}
        {donnees && (
          <>
            <div className="grid gap-2 sm:grid-cols-[repeat(auto-fit,minmax(9rem,1fr))]">
              {donnees.totaux.map((t) => (
                <div key={t.mode} className="rounded-2xl bg-surface-alt px-3 py-2">
                  <p className="text-xs text-muted-foreground">{LIBELLES_MODE[t.mode] ?? t.mode} · {t.operations} op.</p>
                  <p className="font-bold text-ink-900">{formaterMontant(t.montant)}</p>
                </div>
              ))}
              <div className="rounded-2xl bg-brand-900 px-3 py-2 text-primary-foreground">
                <p className="text-xs opacity-80">Total encaissé</p>
                <p className="font-extrabold">{formaterMontant(donnees.total)}</p>
              </div>
            </div>
            {donnees.annulations > 0 && (
              <p className="text-xs text-muted-foreground">{donnees.annulations} annulation(s) déduite(s) des totaux.</p>
            )}
            {donnees.lignes.length === 0 ? (
              <p className="text-muted-foreground">Aucun encaissement ce jour.</p>
            ) : (
              <div className="max-h-80 overflow-auto rounded-2xl border border-brand-900/10">
                <table className="w-full text-left text-xs">
                  <thead className="sticky top-0 bg-surface-alt text-muted-foreground">
                    <tr>
                      <th className="px-3 py-2">Heure</th><th className="px-3 py-2">Reçu</th><th className="px-3 py-2">Facture</th>
                      <th className="px-3 py-2">Client</th><th className="px-3 py-2">Mode</th>
                      <th className="px-3 py-2 text-right">Montant</th><th className="px-3 py-2">Par</th>
                    </tr>
                  </thead>
                  <tbody>
                    {donnees.lignes.map((l) => (
                      <tr key={l.paiementId} className={`border-t border-brand-900/5 ${l.contrePassation ? "text-red-700" : "text-ink-900"}`}
                        title={l.contrePassation ? l.reference ?? undefined : undefined}>
                        <td className="px-3 py-2">{heure(l.date)}</td>
                        <td className="px-3 py-2 font-semibold">{l.code}{l.contrePassation ? " (annulation)" : ""}</td>
                        <td className="px-3 py-2">{l.facture ?? "—"}</td>
                        <td className="px-3 py-2">{l.client ?? "—"}</td>
                        <td className="px-3 py-2">{LIBELLES_MODE[l.mode] ?? l.mode}</td>
                        <td className="px-3 py-2 text-right font-semibold">{formaterMontant(l.montant)}</td>
                        <td className="px-3 py-2">{l.encaisseur ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </Dialogue>
  );
}
