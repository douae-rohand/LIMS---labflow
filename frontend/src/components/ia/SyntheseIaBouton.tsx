import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { RefreshCw, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { lireSyntheseIa, messageErreurSynthese, regenererSyntheseIa } from "@/api/ia";

/**
 * Synthèse IA des résultats d'une demande (CDC M07), pour le personnel qui gère les rapports.
 * Elle est générée automatiquement quelques secondes après la signature ; le bouton « Régénérer »
 * sert si le fournisseur était indisponible à ce moment-là.
 */
export function SyntheseIaBouton({ demandeId, reference }: { demandeId: number; reference: string }) {
  const [ouvert, setOuvert] = useState(false);
  const queryClient = useQueryClient();
  const cle = ["synthese-ia", demandeId];

  const synthese = useQuery({ queryKey: cle, queryFn: () => lireSyntheseIa(demandeId), enabled: ouvert });
  const regeneration = useMutation({
    mutationFn: () => regenererSyntheseIa(demandeId),
    onSuccess: (resultat) => {
      queryClient.setQueryData(cle, resultat);
      toast.success("Synthèse régénérée.");
    },
    onError: (erreur) => toast.error(messageErreurSynthese(erreur)),
  });

  const donnees = synthese.data;
  return (
    <>
      <Button size="sm" variant="ghost" icon={<Sparkles className="size-4" aria-hidden="true" />} iconPosition="left"
        onClick={() => setOuvert(true)}>
        Synthèse IA
      </Button>
      <Dialogue
        ouvert={ouvert}
        titre={`Synthèse IA – ${reference}`}
        description="Résumé automatique des résultats, affiché au client à côté du rapport signé. Il ne remplace pas le rapport officiel."
        onFermer={() => setOuvert(false)}
      >
        <div className="flex flex-col gap-4">
          {synthese.isLoading ? (
            <p className="text-sm text-muted-foreground">Chargement…</p>
          ) : synthese.isError ? (
            <p className="text-sm text-red-700">{messageErreurSynthese(synthese.error)}</p>
          ) : donnees ? (
            <div className="rounded-xl bg-surface-alt px-4 py-3">
              <p className="whitespace-pre-line text-sm text-ink-900">{donnees.contenu}</p>
              <p className="mt-3 text-xs text-muted-foreground">
                {donnees.mention} · générée le {new Date(donnees.dateGeneration).toLocaleString("fr-FR")}
              </p>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              Pas encore de synthèse : elle est générée quelques secondes après la signature. Si elle n'apparaît pas,
              utilisez « Régénérer ».
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => setOuvert(false)}>Fermer</Button>
            <Button type="button" variant="secondary" loading={regeneration.isPending}
              icon={<RefreshCw className="size-4" aria-hidden="true" />} iconPosition="left"
              onClick={() => regeneration.mutate()}>
              Régénérer
            </Button>
          </div>
        </div>
      </Dialogue>
    </>
  );
}
