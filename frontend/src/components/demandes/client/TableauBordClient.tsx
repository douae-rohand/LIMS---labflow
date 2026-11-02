import { Link } from "@tanstack/react-router";
import {
  Activity,
  ArrowRight,
  Bell,
  Calendar,
  CheckCircle2,
  Clock,
  FileText,
  ListFilter,
  Receipt,
  ShoppingCart,
  User,
  type LucideIcon,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Button, Card, IconBox } from "@/components/lab";
import { StatutDemandeBadge } from "@/components/demandes/StatutDemandeBadge";
import { Container } from "@/components/layout/Container";
import { BandeauLaboratoiresIndisponibles } from "@/components/client/BandeauLaboratoiresIndisponibles";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { formatMontant } from "@/components/panier/format";
import { useDemandesClient } from "@/hooks/useDemandesClient";
import { useSyntheseClient } from "@/hooks/useSyntheseClient";
import { useProfilClient } from "@/hooks/useProfilClient";
import { useRapportsClient } from "@/hooks/useRapportsClient";
import { useFacturesClient } from "@/hooks/useFacturesClient";
import { useEnquetesClient } from "@/hooks/useEnquetesClient";
import { formatDateFr, formatDateLocaleFr } from "@/lib/dates";
import type { StatutDemande } from "@/api/demandes";
import type { DemandeResumeClient } from "@/api/demandesClient";
import type { RapportResumeClient } from "@/api/rapportsClient";
import type { FactureResumeClient } from "@/api/facturesClient";
import { cn } from "@/lib/utils";

/** Statuts regroupés sous « en cours » dans le tableau de bord. */
const STATUTS_EN_COURS: StatutDemande[] = ["ACCEPTEE", "EN_COURS", "EN_VALIDATION"];

export function TableauBordClient() {
  const profilQuery = useProfilClient();
  const profil = profilQuery.data;

  const demandesQuery = useDemandesClient();
  const demandes = demandesQuery.data ?? [];

  const syntheseQuery = useSyntheseClient();
  const synthese = syntheseQuery.data;

  const rapportsQuery = useRapportsClient();
  const rapports = rapportsQuery.data?.elements ?? [];

  const facturesQuery = useFacturesClient();
  const factures = facturesQuery.data?.elements ?? [];

  const enquetesQuery = useEnquetesClient();
  const enquetes = enquetesQuery.data ?? [];

  const prenom = profil?.prenom ?? "";
  const dateAujourdhui = new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "full",
  }).format(new Date());

  // Calcul des statistiques
  const enAttente = demandes.filter((d) => d.statut === "NOUVEAU").length;
  const enCours = demandes.filter((d) => STATUTS_EN_COURS.includes(d.statut)).length;
  const terminees = demandes.filter((d) => d.statut === "TERMINEE").length;

  // Données pour le graphique d'activité (6 derniers mois)
  const chartData = compilerDonneesGraphique(demandes);

  return (
    <Container className="space-y-8 py-10">
      {/* Header et bienvenue */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-ink-900">
            Bonjour, {prenom || "Client"}
          </h1>
          <p className="mt-1 text-muted-foreground">
            Bienvenue sur votre espace LabFlow. Voici un aperçu de vos activités.
          </p>
        </div>
        <div className="flex items-center gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-2 text-sm font-medium text-ink-900 shadow-sm">
          <Calendar className="size-4 text-brand-900" />
          {dateAujourdhui}
        </div>
      </div>

      {/* Widgets KPI modernes */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Demandes en cours"
          value={enCours}
          icon={ListFilter}
          variant="info"
          to="/app/demandes"
        />
        <KpiCard
          label="Demandes en attente"
          value={enAttente}
          icon={Clock}
          variant="warning"
          to="/app/demandes"
        />
        <KpiCard
          label="Rapports disponibles"
          value={synthese?.rapportsDisponibles ?? 0}
          icon={FileText}
          variant="brand"
          to="/app/rapports"
        />
        <KpiCard
          label="Factures impayées"
          value={synthese?.facturesImpayees ?? 0}
          valueSuffix={
            synthese?.facturesImpayees !== undefined
              ? synthese.facturesImpayees > 0
                ? ` facture${synthese.facturesImpayees > 1 ? "s" : ""}`
                : "Aucune facture impayée"
              : ""
          }
          subValue={
            synthese?.montantRestantDu
              ? `${formatMontant(synthese.montantRestantDu)} restant dû`
              : "0,00 MAD"
          }
          icon={Receipt}
          variant="error"
          to="/app/factures"
          search={{ statut: "IMPAYEE" }}
          hideValue={synthese?.facturesImpayees === 0}
        />
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
        {/* Graphique d'activité */}
        <Card variant="surface" className="lg:col-span-2">
          <div className="mb-6 flex items-center justify-between">
            <h2 className="text-lg font-bold text-ink-900">Activité des demandes</h2>
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <span className="size-2 rounded-full bg-brand-900" />
              Nombre de demandes
            </div>
          </div>
          <div className="h-[260px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis
                  dataKey="name"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#64748b", fontSize: 12 }}
                  dy={10}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#64748b", fontSize: 12 }}
                />
                <Tooltip
                  cursor={{ fill: "#f1f5f9" }}
                  contentStyle={{
                    borderRadius: "12px",
                    border: "none",
                    boxShadow: "0 10px 15px -3px rgb(0 0 0 / 0.1)",
                  }}
                />
                <Bar
                  dataKey="total"
                  fill="var(--color-brand-900)"
                  radius={[4, 4, 0, 0]}
                  barSize={32}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Actions à effectuer */}
        <ActionsCard
          enquetesCount={synthese?.enquetesEnAttente ?? 0}
          facturesImpayees={synthese?.facturesImpayees ?? 0}
        />
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        {/* Derniers rapports */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-ink-900">Derniers rapports</h2>
            <Link
              to="/app/rapports"
              className="text-sm font-semibold text-brand-900 hover:underline"
            >
              Voir tous les rapports
            </Link>
          </div>
          <LandingState
            loading={rapportsQuery.isPending}
            error={rapportsQuery.error as Error | null}
            onRetry={() => void rapportsQuery.refetch()}
            empty={rapports.length === 0}
            emptyMessage="Aucun rapport disponible."
            skeleton={<CardsSkeleton count={2} />}
          >
            <div className="grid gap-4">
              {rapports.slice(0, 2).map((rapport) => (
                <LigneRapport key={rapport.rapportId} rapport={rapport} />
              ))}
            </div>
          </LandingState>
        </section>

        {/* Dernières commandes */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-ink-900">Dernières commandes</h2>
            <Link
              to="/app/demandes"
              className="text-sm font-semibold text-brand-900 hover:underline"
            >
              Voir toutes mes commandes
            </Link>
          </div>
          <LandingState
            loading={demandesQuery.isPending}
            error={demandesQuery.error}
            onRetry={() => void demandesQuery.refetch()}
            empty={demandes.length === 0}
            emptyMessage="Aucune demande récente."
            skeleton={<CardsSkeleton count={2} />}
          >
            <div className="grid gap-4">
              {demandes.slice(0, 2).map((demande) => (
                <LigneDemande key={demande.id} demande={demande} />
              ))}
            </div>
          </LandingState>
        </section>
      </div>

      {synthese?.laboratoiresIndisponibles ? (
        <BandeauLaboratoiresIndisponibles
          nombre={synthese.laboratoiresIndisponibles}
          onReessayer={() => void syntheseQuery.refetch()}
        />
      ) : null}
    </Container>
  );
}

// --- Composants internes ---

function KpiCard({
  label,
  value,
  valueSuffix,
  subValue,
  icon: Icon,
  variant,
  to,
  search,
  hideValue,
}: {
  label: string;
  value: number;
  valueSuffix?: string;
  subValue?: string;
  icon: LucideIcon;
  variant: "brand" | "success" | "warning" | "error" | "info";
  to: string;
  search?: any;
  hideValue?: boolean;
}) {
  const variants = {
    brand: "bg-brand-900/10 text-brand-900",
    success: "bg-emerald-500/10 text-emerald-600",
    warning: "bg-amber-500/10 text-amber-600",
    error: "bg-rose-500/10 text-rose-600",
    info: "bg-sky-500/10 text-sky-600",
  };

  return (
    <Link to={to as any} search={search} className="group block h-full">
      <Card
        variant="surface"
        className="h-full transition-all duration-200 hover:-translate-y-1 hover:shadow-lg"
        padding="md"
      >
        <div className="flex h-full flex-col justify-between gap-4">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <p className="text-sm font-medium text-muted-foreground">{label}</p>
              <div className="mt-2 flex flex-wrap items-baseline gap-1">
                {!hideValue && <span className="text-3xl font-extrabold text-ink-900">{value}</span>}
                {valueSuffix && (
                  <span
                    className={cn(
                      "text-sm font-bold",
                      hideValue ? "text-ink-900" : "text-muted-foreground",
                    )}
                  >
                    {valueSuffix}
                  </span>
                )}
              </div>
            </div>
            <div
              className={cn("shrink-0 rounded-xl p-2.5 transition-colors", variants[variant])}
            >
              <Icon className="size-5" />
            </div>
          </div>
          {subValue && (
            <p className="text-sm font-semibold text-ink-900">
              {subValue}
            </p>
          )}
        </div>
      </Card>
    </Link>
  );
}

function LigneDemande({ demande }: { demande: DemandeResumeClient }) {
  return (
    <Link
      to="/app/demandes/$laboratoireId/$demandeId"
      params={{
        laboratoireId: String(demande.laboratoireId),
        demandeId: String(demande.id),
      }}
      className="group flex items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card p-4 transition-all hover:bg-surface-card hover:shadow-md"
    >
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs font-medium text-muted-foreground">
            {demande.reference}
          </span>
          <StatutDemandeBadge statut={demande.statut} />
        </div>
        <p className="mt-1 truncate font-bold text-ink-900 group-hover:text-brand-900">
          {demande.objet ?? "Demande d'analyses"}
        </p>
        <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
          <span>{demande.laboratoireNom}</span>
          <span>•</span>
          <span>{formatDateFr(demande.dateSoumission)}</span>
        </div>
      </div>
      <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-brand-900" />
    </Link>
  );
}

function LigneRapport({ rapport }: { rapport: RapportResumeClient }) {
  return (
    <Link
      to="/app/rapports/$laboratoireId/$rapportId"
      params={{
        laboratoireId: String(rapport.laboratoireId),
        rapportId: String(rapport.rapportId),
      }}
      className="group flex items-center gap-4 rounded-2xl border border-brand-900/10 bg-card p-4 transition-all hover:bg-surface-card hover:shadow-md"
    >
      <div className="rounded-xl bg-brand-900/10 p-2 text-brand-900">
        <FileText className="size-5" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-bold text-ink-900 group-hover:text-brand-900">
          {rapport.demandeTitre || "Rapport d'analyse"}
        </p>
        <p className="text-xs text-muted-foreground">
          Version {rapport.version} • {formatDateFr(rapport.dateEmission)}
        </p>
      </div>
      <ArrowRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-brand-900" />
    </Link>
  );
}

function ActionsCard({
  enquetesCount,
  facturesImpayees,
}: {
  enquetesCount: number;
  facturesImpayees: number;
}) {
  const aDesActions = enquetesCount > 0 || facturesImpayees > 0;

  return (
    <Card variant="surface" className="flex flex-col gap-4">
      <h2 className="text-lg font-bold text-ink-900">Actions à effectuer</h2>
      {!aDesActions ? (
        <p className="text-sm text-muted-foreground italic">Aucune action requise pour le moment.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {enquetesCount > 0 && (
            <Link
              to="/app/enquetes"
              search={{ statut: "A_REPONDRE" }}
              className="group flex items-center gap-3 rounded-2xl bg-amber-50 p-3 transition-all hover:bg-amber-100 hover:shadow-sm"
            >
              <div className="rounded-xl bg-amber-500/10 p-2 text-amber-600 group-hover:bg-amber-500/20">
                <ShoppingCart className="size-5" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-bold text-ink-900">Enquêtes à compléter</p>
                <p className="text-xs text-muted-foreground">
                  {enquetesCount} questionnaire{enquetesCount > 1 ? "s" : ""} en attente
                </p>
              </div>
              <ArrowRight className="size-4 text-amber-600 transition-transform group-hover:translate-x-1" />
            </Link>
          )}
          {facturesImpayees > 0 && (
            <Link
              to="/app/factures"
              search={{ statut: "IMPAYEE" }}
              className="group flex items-center gap-3 rounded-2xl bg-rose-50 p-3 transition-all hover:bg-rose-100 hover:shadow-sm"
            >
              <div className="rounded-xl bg-rose-500/10 p-2 text-rose-600 group-hover:bg-rose-500/20">
                <Receipt className="size-5" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-bold text-ink-900">Factures à régler</p>
                <p className="text-xs text-muted-foreground">
                  {facturesImpayees} facture{facturesImpayees > 1 ? "s" : ""} impayée{facturesImpayees > 1 ? "s" : ""}
                </p>
              </div>
              <ArrowRight className="size-4 text-rose-600 transition-transform group-hover:translate-x-1" />
            </Link>
          )}
        </div>
      )}
    </Card>
  );
}

function compilerDonneesGraphique(demandes: DemandeResumeClient[]) {
  const moisNoms = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sept", "Oct", "Nov", "Déc"];
  const aujourdhui = new Date();
  const donnees = [];

  for (let i = 5; i >= 0; i--) {
    const d = new Date(aujourdhui.getFullYear(), aujourdhui.getMonth() - i, 1);
    const mois = d.getMonth();
    const annee = d.getFullYear();
    const nom = `${moisNoms[mois]} ${annee}`;

    const total = demandes.filter((demande) => {
      if (!demande.dateSoumission) return false;
      const dateDemande = new Date(demande.dateSoumission);
      return dateDemande.getMonth() === mois && dateDemande.getFullYear() === annee;
    }).length;

    donnees.push({ name: nom, total });
  }

  return donnees;
}
