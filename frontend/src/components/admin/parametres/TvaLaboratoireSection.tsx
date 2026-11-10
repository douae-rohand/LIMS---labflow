import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Percent } from "lucide-react";
import { toast } from "sonner";
import { ApiError } from "@/api/axios";
import { enregistrerTvaLaboratoire, formaterDh, tvaComprise, useTvaLaboratoire } from "@/api/tvaLaboratoire";
import { Button } from "@/components/lab";

const CHAMP = "h-10 w-full rounded-xl border border-brand-900/15 bg-white px-3 text-sm text-ink-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20";

/** Taux de TVA du laboratoire : taux de la plateforme ou taux propre. Section autonome (enregistrement séparé). */
export function TvaLaboratoireSection() {
  const queryClient = useQueryClient();
  const tva = useTvaLaboratoire();
  const [propre, setPropre] = useState(false);
  const [taux, setTaux] = useState("");

  useEffect(() => {
    if (!tva.data) return;
    setPropre(tva.data.tauxLaboratoire !== null);
    setTaux(String(tva.data.tauxLaboratoire ?? tva.data.tauxPlateforme));
  }, [tva.data]);

  const enregistrement = useMutation({
    mutationFn: (valeur: number | null) => enregistrerTvaLaboratoire(valeur),
    onSuccess: (donnees) => {
      queryClient.setQueryData(["tva-laboratoire"], donnees);
      toast.success("Taux de TVA enregistré.");
    },
    onError: (erreur) => toast.error(erreur instanceof ApiError ? erreur.message : "L'enregistrement a échoué."),
  });

  if (!tva.data) {
    return tva.isError ? (
      <p className="rounded-2xl border border-destructive/20 bg-white p-5 text-sm text-destructive" role="alert">
        Le taux de TVA n'a pas pu être chargé.
      </p>
    ) : null;
  }

  const valeur = Number(taux.replace(",", "."));
  const valide = !propre || (taux.trim() !== "" && Number.isFinite(valeur) && valeur >= 0 && valeur <= 100);
  const cible = propre ? Math.round(valeur * 100) / 100 : null;
  const modifie = valide && cible !== tva.data.tauxLaboratoire;
  const tauxApercu = propre && valide ? valeur : tva.data.tauxPlateforme;

  return (
    <section className="max-w-4xl rounded-2xl border border-brand-900/10 bg-white p-5 shadow-sm sm:p-6">
      <div className="mb-5 flex items-center gap-3">
        <span className="flex size-9 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
          <Percent className="size-4" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-semibold text-ink-900">TVA</h2>
          <p className="text-sm text-muted-foreground">
            Vos prix sont saisis TTC. Ce taux calcule la TVA comprise dans le catalogue et les factures ; il n'est visible que par l'administrateur.
          </p>
        </div>
      </div>
      <form
        className="grid gap-4 sm:grid-cols-2"
        onSubmit={(event) => {
          event.preventDefault();
          if (modifie) enregistrement.mutate(cible);
        }}
      >
        <fieldset className="flex flex-col gap-2 text-sm">
          <legend className="sr-only">Origine du taux</legend>
          <label className="flex items-center gap-3 rounded-xl border border-brand-900/10 px-4 py-3 font-medium text-ink-900">
            <input type="radio" name="origine-tva" className="accent-brand-900" checked={!propre} onChange={() => setPropre(false)} />
            Taux de la plateforme ({tva.data.tauxPlateforme} %)
          </label>
          <label className="flex items-center gap-3 rounded-xl border border-brand-900/10 px-4 py-3 font-medium text-ink-900">
            <input type="radio" name="origine-tva" className="accent-brand-900" checked={propre} onChange={() => setPropre(true)} />
            Taux propre au laboratoire
          </label>
          {propre && (
            <div className="mt-1">
              <label htmlFor="taux-tva-labo" className="mb-1 block text-xs font-semibold text-muted-foreground">Taux (%)</label>
              <input
                id="taux-tva-labo"
                type="number"
                inputMode="decimal"
                min={0}
                max={100}
                step="0.01"
                value={taux}
                onChange={(event) => setTaux(event.target.value)}
                className={CHAMP}
              />
            </div>
          )}
        </fieldset>
        <div className="flex flex-col justify-between gap-3">
          <div className="rounded-xl bg-surface-alt p-4 text-sm text-ink-900">
            <p>Exemple : essai à <strong>{formaterDh(100)}</strong> TTC</p>
            <p>TVA comprise ({tauxApercu} %) : <strong>{formaterDh(tvaComprise(100, tauxApercu))}</strong></p>
            <p>Hors taxes : <strong>{formaterDh(100 - tvaComprise(100, tauxApercu))}</strong></p>
          </div>
          <div className="flex justify-end">
            <Button type="submit" variant="primary" disabled={!modifie || enregistrement.isPending}>
              {enregistrement.isPending ? "Enregistrement…" : "Enregistrer le taux"}
            </Button>
          </div>
        </div>
      </form>
      <p className="mt-3 text-xs text-muted-foreground">
        Taux appliqué actuellement : {tva.data.tauxEffectif} %. Une facture déjà émise conserve le taux de son émission.
      </p>
    </section>
  );
}
