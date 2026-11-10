import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { AlarmClock, ClipboardCheck, FileSignature, Gauge, Inbox, ListTodo, RefreshCw, Users } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { lireSuivi } from "@/api/responsable";
import { BadgeStatutAnalyse, EnTeteResponsable, EtatChargement, formaterDate } from "./commun";

export function PageSuivi() {
  const suivi = useQuery({ queryKey: ["responsable-suivi"], queryFn: lireSuivi, refetchInterval: 60_000 });
  const donnees = suivi.data;

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={Gauge}
        titre="Tableau de suivi"
        description="Charge de l'équipe, dossiers en retard et travail en attente de votre décision."
        action={
          <Button variant="secondary" size="sm" icon={<RefreshCw className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => void suivi.refetch()}>
            Actualiser
          </Button>
        }
      />

      <EtatChargement chargement={suivi.isLoading} erreur={suivi.isError} vide={false} messageVide="" onReessayer={() => void suivi.refetch()} />

      {donnees && (
        <div className="flex flex-col gap-6">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <Indicateur icone={Inbox} libelle="Demandes à décider" valeur={donnees.indicateurs.demandesADecider} lien="/app/demandes-clients" />
            <Indicateur icone={ListTodo} libelle="Analyses à affecter" valeur={donnees.indicateurs.analysesAAffecter} lien="/app/planification" />
            <Indicateur icone={Users} libelle="Analyses en cours" valeur={donnees.indicateurs.analysesEnCours} lien="/app/planification" />
            <Indicateur icone={AlarmClock} libelle="Analyses en retard" valeur={donnees.indicateurs.analysesEnRetard} lien="/app/planification" alerte />
            <Indicateur icone={ClipboardCheck} libelle="Résultats à valider" valeur={donnees.indicateurs.resultatsAValider} lien="/app/validation-resultats" />
            <Indicateur icone={FileSignature} libelle="Rapports à préparer ou signer" valeur={donnees.indicateurs.rapportsAPreparer} lien="/app/rapports-laboratoire" />
          </div>

          <section>
            <h2 className="mb-3 text-lg font-bold text-ink-900">Charge des techniciens</h2>
            {donnees.techniciens.length === 0 ? (
              <EtatChargement chargement={false} erreur={false} vide messageVide="Aucun technicien actif dans le laboratoire." />
            ) : (
              <div className="overflow-x-auto rounded-2xl border border-brand-900/10 bg-card">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead>
                    <tr className="bg-surface-alt text-xs font-semibold tracking-wide text-brand-900 uppercase">
                      <th className="px-4 py-3">Technicien</th>
                      <th className="px-4 py-3">Domaines</th>
                      <th className="px-4 py-3 text-right">À faire</th>
                      <th className="px-4 py-3 text-right">En cours</th>
                      <th className="px-4 py-3 text-right">En retard</th>
                      <th className="px-4 py-3">Charge</th>
                    </tr>
                  </thead>
                  <tbody>
                    {donnees.techniciens.map((technicien) => {
                      const total = technicien.aFaire + technicien.enCours;
                      const max = Math.max(...donnees.techniciens.map((t) => t.aFaire + t.enCours), 1);
                      return (
                        <tr key={technicien.id} className="border-t border-brand-900/10">
                          <td className="px-4 py-3">
                            <p className="font-semibold text-ink-900">{technicien.nomComplet}</p>
                            <p className="text-xs text-muted-foreground">{technicien.email}</p>
                          </td>
                          <td className="px-4 py-3 text-xs text-muted-foreground">{technicien.domaines.join(", ") || "Tous"}</td>
                          <td className="px-4 py-3 text-right">{technicien.aFaire}</td>
                          <td className="px-4 py-3 text-right">{technicien.enCours}</td>
                          <td className={`px-4 py-3 text-right ${technicien.enRetard > 0 ? "font-bold text-red-700" : ""}`}>{technicien.enRetard}</td>
                          <td className="px-4 py-3">
                            <div className="h-2 w-32 overflow-hidden rounded-full bg-surface-alt" role="img" aria-label={`${total} analyse(s) en charge`}>
                              <div className="h-full rounded-full bg-brand-900" style={{ width: `${(total / max) * 100}%` }} />
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section>
            <h2 className="mb-3 text-lg font-bold text-ink-900">Dossiers en retard</h2>
            {donnees.retards.length === 0 ? (
              <EtatChargement chargement={false} erreur={false} vide messageVide="Aucune analyse en retard." />
            ) : (
              <ul className="flex flex-col gap-2">
                {donnees.retards.map((analyse) => (
                  <li key={analyse.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-ink-900">{analyse.essaiLibelle ?? analyse.code}</p>
                      <p className="text-xs text-muted-foreground">
                        {analyse.demandeReference} · {analyse.technicienNom ?? "Non affectée"} · échéance {formaterDate(analyse.echeance)}
                      </p>
                    </div>
                    <BadgeStatutAnalyse analyse={analyse} />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </Container>
  );
}

function Indicateur({ icone: Icone, libelle, valeur, lien, alerte }: {
  icone: LucideIcon;
  libelle: string;
  valeur: number;
  lien: "/app/demandes-clients" | "/app/planification" | "/app/validation-resultats" | "/app/rapports-laboratoire";
  alerte?: boolean;
}) {
  return (
    <Link to={lien} className="rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900">
      <Card className={`flex items-center gap-4 p-4 transition-colors hover:bg-surface-alt ${alerte && valeur > 0 ? "border-red-200" : ""}`}>
        <span className={`flex size-10 items-center justify-center rounded-xl ${alerte && valeur > 0 ? "bg-red-50 text-red-700" : "bg-brand-900/10 text-brand-900"}`}>
          <Icone className="size-5" aria-hidden="true" />
        </span>
        <div>
          <p className="text-2xl font-extrabold text-ink-900">{valeur}</p>
          <p className="text-sm text-muted-foreground">{libelle}</p>
        </div>
      </Card>
    </Link>
  );
}
