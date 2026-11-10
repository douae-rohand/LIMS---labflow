import { useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { Building2, CirclePause, FlaskConical, Inbox, RefreshCw, Users } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Button, Skeleton } from "@/components/lab";
import { obtenirTableauBord, type ActiviteLaboratoire } from "@/api/tableauBord";
import { Indicateurs, useStatistiquesPlateforme } from "./Indicateurs";
import { EnTetePage } from "./EnTetePage";
import { EtatVide } from "./EtatVide";

const PALETTE = [
  "var(--brand-900)",
  "var(--brand-600)",
  "var(--accent-lime)",
  "var(--brand-800)",
  "var(--mint-200)",
  "var(--ink-900)",
];

const ETAT: Record<string, string> = {
  OK: "Disponible",
  INDISPONIBLE: "Indisponible",
  NON_CONFIGURE: "Non configuré",
  DESACTIVE: "Désactivé",
};

export function TableauDeBord() {
  const queryClient = useQueryClient();
  const statistiques = useStatistiquesPlateforme();
  const tableau = useQuery({
    queryKey: ["tableau-de-bord"],
    queryFn: obtenirTableauBord,
    staleTime: 30_000,
  });

  const actualiser = () => {
    void queryClient.invalidateQueries({ queryKey: ["tableau-de-bord"] });
    void queryClient.invalidateQueries({ queryKey: ["statistiques-plateforme"] });
  };

  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <EnTetePage
        titre="Tableau de bord"
        description="Vue d'ensemble de la plateforme : comptes, laboratoires et volumes. Aucune donnée de patient n'est affichée."
        action={
          <Button variant="secondary" size="sm" icon={<RefreshCw className="size-4" aria-hidden="true" />} iconPosition="left" onClick={actualiser}>
            Actualiser
          </Button>
        }
      />

      {tableau.isLoading || statistiques.isLoading ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5" aria-busy="true" aria-label="Chargement des indicateurs">
          {Array.from({ length: 5 }, (_, index) => <Skeleton key={index} className="h-20 w-full" />)}
        </div>
      ) : tableau.isError || !tableau.data ? (
        <EtatVide titre="Le tableau de bord n'a pas pu être chargé." detail="Les sondes et les agrégats restent indisponibles tant que le rechargement n'a pas abouti." />
      ) : (
        <Contenu
          utilisateursActifs={tableau.data.utilisateursActifs}
          totalDemandes={tableau.data.totalDemandesAnalyse}
          exclus={tableau.data.laboratoiresExclus}
          actifs={statistiques.data?.laboratoiresActifs}
          demandes={statistiques.data?.demandesEnAttente}
          inscriptions={tableau.data.inscriptions}
          activites={tableau.data.activites}
          domaines={tableau.data.domaines}
          alertes={tableau.data.alertesIntegration}
          messageN8n={tableau.data.alertesN8n}
          services={tableau.data.services}
        />
      )}
    </div>
  );
}

function Contenu({
  utilisateursActifs, totalDemandes, exclus, actifs, demandes, inscriptions, activites, domaines, alertes, messageN8n, services,
}: {
  utilisateursActifs: number;
  totalDemandes: number;
  exclus: number;
  actifs?: number | undefined;
  demandes?: number | undefined;
  inscriptions: { cle: string; nombre: number }[];
  activites: ActiviteLaboratoire[];
  domaines: { cle: string; nombre: number }[];
  alertes: { numero: string; nom?: string | null; jours: number }[];
  messageN8n: string;
  services: { nom: string; etat: string }[];
}) {
  const top = [...activites]
    .filter((ligne) => ligne.nombre != null)
    .sort((a, b) => (b.nombre ?? 0) - (a.nombre ?? 0))
    .slice(0, 10)
    .map((ligne) => ({ nom: ligne.nom, cle: ligne.nom, nombre: ligne.nombre ?? 0 }));
  const mois = new Map<string, number>();
  for (const ligne of activites) {
    for (const point of ligne.demandesParMois ?? []) {
      mois.set(point.cle, (mois.get(point.cle) ?? 0) + point.nombre);
    }
  }
  const tendance = [...mois.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([cle, nombre]) => ({ cle, nombre }));

  return (
    <div className="flex flex-col gap-5">
      <Indicateurs
        cartes={[
          { libelle: "Laboratoires actifs", valeur: actifs, icone: Building2 },
          { libelle: "Suspendus ou désactivés", valeur: exclus, icone: CirclePause },
          { libelle: "Demandes d'intégration en attente", valeur: demandes, icone: Inbox },
          { libelle: "Utilisateurs actifs", valeur: utilisateursActifs, icone: Users },
          { libelle: "Demandes d'analyse", valeur: totalDemandes, icone: FlaskConical },
        ]}
      />

      <section className="grid gap-3 xl:grid-cols-2" aria-label="Diagrammes">
        <Graphique titre="Inscriptions de laboratoires" sousTitre="Douze derniers mois" lignes={inscriptions}>
          {inscriptions.length === 0 ? <EtatVide titre="Aucune inscription sur la période." /> : (
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={inscriptions}>
                <CartesianGrid stroke="var(--border)" vertical={false} />
                <XAxis dataKey="cle" tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                <Tooltip />
                <Line type="monotone" dataKey="nombre" name="Laboratoires" stroke="var(--brand-900)" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Graphique>
        <Graphique titre="Répartition par domaine" sousTitre="Demandes rattachées à un essai" lignes={domaines}>
          {domaines.length === 0 ? <EtatVide titre="Aucun domaine à répartir." /> : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={domaines} dataKey="nombre" nameKey="cle" outerRadius={84}>
                  {domaines.map((entree, index) => (
                    <Cell key={entree.cle} fill={PALETTE[index % PALETTE.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Graphique>
        <Graphique titre="Activité par laboratoire" sousTitre="Dix premiers, demandes d'analyse" lignes={top}>
          {top.length === 0 ? <EtatVide titre="Aucune activité agrégée." /> : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={top}>
                <CartesianGrid stroke="var(--border)" vertical={false} />
                <XAxis dataKey="nom" tick={{ fontSize: 11, fill: "var(--muted-foreground)" }} interval={0} hide={top.length > 6} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                <Tooltip />
                <Bar dataKey="nombre" name="Demandes" fill="var(--brand-600)" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Graphique>
        <Graphique titre="Tendance mensuelle des demandes" sousTitre="Somme des laboratoires interrogés" lignes={tendance}>
          {tendance.length === 0 ? <EtatVide titre="Pas encore de tendance mensuelle." /> : (
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={tendance}>
                <CartesianGrid stroke="var(--border)" vertical={false} />
                <XAxis dataKey="cle" tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                <Tooltip />
                <Line type="monotone" dataKey="nombre" name="Demandes" stroke="var(--brand-800)" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Graphique>
      </section>

      <section className="overflow-hidden rounded-2xl border border-brand-900/10 bg-card" aria-label="Supervision des laboratoires">
        <div className="border-b border-brand-900/10 px-4 py-4">
          <h2 className="text-sm font-extrabold text-ink-900">Supervision comparative</h2>
          <p className="mt-1 text-xs text-muted-foreground">Chaque laboratoire reste sur sa ligne. Suspendu ou désactivé : non interrogé. Actif mais injoignable : inaccessible.</p>
        </div>
        {activites.length === 0 ? <EtatVide titre="Aucun laboratoire à comparer." /> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead>
                <tr className="bg-surface-alt text-xs font-semibold tracking-wide text-brand-900 uppercase">
                  <th className="px-4 py-3">Laboratoire</th>
                  <th className="px-4 py-3">Statut</th>
                  <th className="px-4 py-3">Couverture</th>
                  <th className="px-4 py-3">Demandes</th>
                  <th className="px-4 py-3">Utilisateurs</th>
                  <th className="px-4 py-3">Clôture</th>
                </tr>
              </thead>
              <tbody>
                {activites.map((ligne) => (
                  <tr key={ligne.laboratoireId} className="border-t border-brand-900/10 hover:bg-surface-alt/60">
                    <td className="px-4 py-3 font-semibold text-ink-900">{ligne.nom}</td>
                    <td className="px-4 py-3">{statutLabo(ligne.statut)}</td>
                    <td className="px-4 py-3 text-muted-foreground">{couverture(ligne.couverture)}</td>
                    <td className="px-4 py-3 tabular-nums">{ligne.nombre == null ? "—" : ligne.nombre}</td>
                    <td className="px-4 py-3 tabular-nums">{ligne.utilisateurs ?? 0}</td>
                    <td className="px-4 py-3 tabular-nums">{ligne.tauxCloture == null ? "—" : `${ligne.tauxCloture} %`}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="grid gap-3 lg:grid-cols-2">
        <article className="rounded-2xl border border-brand-900/10 bg-card p-4">
          <h2 className="text-sm font-extrabold text-ink-900">Demandes d'intégration anciennes</h2>
          <p className="mt-1 text-xs text-muted-foreground">En attente depuis plus de 7 jours.</p>
          {alertes.length === 0 ? (
            <p className="mt-4 text-sm text-muted-foreground">Aucune demande en attente au-delà de ce délai.</p>
          ) : (
            <ul className="mt-3 flex flex-col gap-2">
              {alertes.map((alerte) => (
                <li key={alerte.numero} className="flex items-center justify-between gap-3 text-sm">
                  <span className="min-w-0 truncate text-ink-900">{alerte.numero} · {alerte.nom || "Laboratoire non nommé"}</span>
                  <span className="shrink-0 text-muted-foreground">{alerte.jours} j</span>
                </li>
              ))}
            </ul>
          )}
          <p className="mt-4 text-xs text-muted-foreground">{messageN8n}</p>
        </article>
        <article className="rounded-2xl border border-brand-900/10 bg-card p-4">
          <h2 className="text-sm font-extrabold text-ink-900">Services</h2>
          <p className="mt-1 text-xs text-muted-foreground">Sondes centrales, sans adresse ni clé.</p>
          <ul className="mt-3 flex flex-col gap-2">
            {services.map((service) => (
              <li key={service.nom} className="flex items-center justify-between text-sm">
                <span className="font-medium text-ink-900">{service.nom}</span>
                <span className="rounded-full bg-surface-alt px-2.5 py-1 text-xs font-semibold text-ink-900">{ETAT[service.etat] ?? service.etat}</span>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </div>
  );
}

function statutLabo(valeur?: string | null) {
  if (valeur === "ACTIF") return "Actif";
  if (valeur === "SUSPENDU") return "Suspendu";
  if (valeur === "DESACTIVE") return "Désactivé";
  return valeur ?? "—";
}

function couverture(valeur?: string | null) {
  if (valeur === "EXCLU") return "Non interrogé : suspendu ou désactivé";
  if (valeur === "INACCESSIBLE") return "Inaccessible : agrégats indisponibles";
  return "Inclus";
}

function Graphique({
  titre,
  sousTitre,
  lignes = [],
  children,
}: {
  titre: string;
  sousTitre: string;
  lignes?: { cle: string; nombre: number }[];
  children: ReactNode;
}) {
  return (
    <article className="rounded-2xl border border-brand-900/10 bg-card p-4">
      <h2 className="text-sm font-extrabold text-ink-900">{titre}</h2>
      <p className="mb-3 text-xs text-muted-foreground">{sousTitre}</p>
      {children}
      {lignes.length > 0 && (
        <details className="mt-3 border-t border-brand-900/10 pt-3">
          <summary className="cursor-pointer text-xs font-semibold text-brand-900">Valeurs du diagramme</summary>
          <ul className="mt-2 max-h-28 overflow-y-auto text-xs text-muted-foreground" aria-label={`Valeurs : ${titre}`}>
            {lignes.map((ligne) => (
              <li key={ligne.cle} className="flex justify-between gap-3 py-0.5">
                <span>{ligne.cle}</span>
                <span className="tabular-nums text-ink-900">{ligne.nombre}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </article>
  );
}
