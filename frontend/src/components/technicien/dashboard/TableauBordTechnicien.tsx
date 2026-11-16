import { Activity, AlertTriangle, ArrowRight, CalendarClock, CheckCircle2, Clock3, FlaskConical, RotateCcw, Timer, FilePenLine } from "lucide-react";
import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";
import { Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { getSession } from "@/api/session";
import { ApiError } from "@/api/axios";
import { useTechnicienSynthese } from "@/hooks/useTechnicienEssais";
import { CartesSynthese } from "@/components/technicien/dashboard/CartesSynthese";
import { ProchainsEssais } from "@/components/technicien/dashboard/ProchainsEssais";
import { ChargeSemaine } from "@/components/technicien/dashboard/ChargeSemaine";
import type { SyntheseTechnicienDto } from "@/api/technicien/types";

function Section({ titre, icone: Icon, children }: { titre: string; icone: typeof Activity; children: ReactNode }) {
  return <section><div className="mb-3 flex items-center gap-2"><Icon className="size-5 text-brand-900" aria-hidden="true"/><h2 className="text-lg font-bold text-ink-900">{titre}</h2></div>{children}</section>;
}

function LienEssais({ titre, nombre, recherche, couleur, icon: Icon }: { titre: string; nombre: number; recherche: { enRetard?: boolean; resultatASaisir?: boolean; retourneCorrection?: boolean; echeanceProche?: boolean }; couleur: string; icon: typeof Timer }) {
  return <Link to="/app/essais" search={{ ...recherche, page: 1 }} className="flex items-center gap-3 rounded-xl border border-ink-100 px-4 py-3 text-sm hover:border-brand-900/30 hover:bg-brand-900/5 focus-visible:outline-2 focus-visible:outline-brand-900">
    <Icon className={`size-4 shrink-0 ${couleur}`} aria-hidden="true"/><span className="min-w-0 flex-1">{titre}</span><span className={`font-bold ${couleur}`}>{nombre}</span>
  </Link>;
}

function MesPriorites({ synthese }: { synthese: SyntheseTechnicienDto }) {
  const categories = [
    { titre: "Essais en retard", nombre: synthese.enRetardCount, recherche: { enRetard: true }, couleur: "text-red-700", icon: AlertTriangle },
    { titre: "Échéances proches", nombre: synthese.essaisEcheanceProcheIds.length, recherche: { echeanceProche: true }, couleur: "text-amber-700", icon: Timer },
    { titre: "Résultats à saisir", nombre: synthese.essaisResultatASaisirIds.length, recherche: { resultatASaisir: true }, couleur: "text-sky-700", icon: FilePenLine },
    { titre: "Retournés pour correction", nombre: synthese.retournesCorrectionCount, recherche: { retourneCorrection: true }, couleur: "text-violet-700", icon: RotateCcw },
  ];
  return <Card variant="surface" padding="md"><div className="mb-3 flex items-center justify-between"><p className="text-sm text-muted-foreground">Interventions nécessaires</p><span className="rounded-full bg-brand-900/10 px-2.5 py-1 text-xs font-bold text-brand-900">{synthese.prioritesUniquesCount} essai(s) unique(s)</span></div><div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">{categories.map((categorie) => <LienEssais key={categorie.titre} {...categorie}/>)}</div></Card>;
}

function Repartition({ synthese }: { synthese: SyntheseTechnicienDto }) {
  const parts = [
    { label: "En cours", count: synthese.enCoursCount, color: "bg-sky-500" },
    { label: "En retard", count: synthese.enRetardCount, color: "bg-red-500" },
    { label: "Terminés", count: synthese.terminesCount, color: "bg-emerald-500" },
    { label: "À valider techniquement", count: synthese.validationsTechniquesAttenteCount, color: "bg-amber-500" },
  ];
  const total = parts.reduce((sum, part) => sum + part.count, 0);
  let position = 0;
  const segments = parts.filter((part) => part.count > 0).map((part) => {
    const debut = position; position += part.count / (total || 1) * 100;
    return `${part.color.replace("bg-sky-500", "#0ea5e9").replace("bg-red-500", "#ef4444").replace("bg-emerald-500", "#10b981").replace("bg-amber-500", "#f59e0b")} ${debut}% ${position}%`;
  });
  return <Card variant="surface" padding="md"><div className="flex items-center gap-5"><div className="grid size-28 shrink-0 place-items-center rounded-full" style={{ background: total ? `conic-gradient(${segments.join(", ")})` : "#e5e7eb" }} role="img" aria-label={`Répartition des essais, total ${total}`}><div className="grid size-20 place-items-center rounded-full bg-white text-center"><span><strong className="block text-xl">{total}</strong><small className="text-muted-foreground">essais</small></span></div></div><ul className="min-w-0 space-y-2">{parts.map((part) => <li key={part.label} className="flex items-center gap-2 text-xs"><span aria-hidden="true" className={`size-2.5 shrink-0 rounded-full ${part.color}`}/><span className="min-w-0 flex-1">{part.label}</span><strong>{part.count}</strong></li>)}</ul></div></Card>;
}

function ActiviteDisponible() {
  return <Card variant="flat" padding="md"><div className="flex items-start gap-3"><Activity className="mt-0.5 size-5 text-muted-foreground" aria-hidden="true"/><p className="text-sm text-muted-foreground">L’historique des événements liés aux essais n’est pas encore disponible dans le backend.</p></div></Card>;
}

function ActiviteMois({ synthese }: { synthese: SyntheseTechnicienDto }) {
  const realisesDelta = synthese.realisesMoisCount - synthese.realisesMoisPrecedentCount;
  const validesDelta = synthese.validesTechniquementMoisCount - synthese.validesTechniquementMoisPrecedentCount;
  const taux = synthese.essaisAvecEcheanceCount ? Math.round(synthese.realisesDansLesDelaisMoisCount / synthese.essaisAvecEcheanceCount * 100) : null;
  return <Card variant="flat" padding="md"><div className="grid gap-4 sm:grid-cols-3"><div><p className="text-xs text-muted-foreground">Essais terminés</p><p className="mt-1 text-2xl font-bold">{synthese.realisesMoisCount}</p><p className="text-xs text-muted-foreground">{realisesDelta >= 0 ? "+" : ""}{realisesDelta} vs mois précédent</p></div><div><p className="text-xs text-muted-foreground">Validés techniquement</p><p className="mt-1 text-2xl font-bold">{synthese.validesTechniquementMoisCount}</p><p className="text-xs text-muted-foreground">{validesDelta >= 0 ? "+" : ""}{validesDelta} vs mois précédent</p></div><div><p className="text-xs text-muted-foreground">Terminés dans les délais</p><p className="mt-1 text-2xl font-bold">{taux === null ? "—" : `${taux}%`}</p><p className="text-xs text-muted-foreground">Essais avec échéance calculable</p></div></div></Card>;
}

/** Salutation courte selon l'heure locale. */
function salutation(): string {
  const h = new Date().getHours();
  if (h < 12) return "Bonjour";
  if (h < 18) return "Bon après-midi";
  return "Bonsoir";
}

/** Bloc synthèse : cartes cliquables avec état d'erreur isolé. */
function BlocSynthese() {
  const query = useTechnicienSynthese();
  const { data, isLoading, isError, error } = query;

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-24 rounded-3xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 403) {
      return (
        <Card variant="flat" padding="lg" className="text-center">
          <p className="text-sm font-medium text-amber-700" role="alert">
            Vous n'avez pas la permission de consulter le tableau de bord.
          </p>
        </Card>
      );
    }
    return (
      <Card variant="flat" padding="lg" className="text-center">
        <p className="text-sm font-medium text-red-700" role="alert">
          Impossible de charger le tableau de bord.
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          Vérifiez votre connexion ou réessayez.
        </p>
      </Card>
    );
  }

  if (!data) return null;
  return <CartesSynthese synthese={data} />;
}

/**
 * TableauBordTechnicien — Page d'accueil du technicien sur /app.
 * Salutation, cartes cliquables, prochains essais, charge de la semaine.
 */
export function TableauBordTechnicien() {
  const synthese = useTechnicienSynthese();
  const session = getSession();
  const prenom = session?.user.nomComplet.split(" ")[0] ?? "";

  return (
    <Container className="py-6 sm:py-8">
      {/* Salutation */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-ink-900">
          {salutation()}
          {prenom ? `, ${prenom}` : ""}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Voici vos essais et votre charge du moment.
        </p>
      </div>

      {/* Bloc cartes synthèse (erreur isolée) */}
      <BlocSynthese />

      {/* Ligne 2 : actions prioritaires et alertes */}
      <div className="mt-6"><Section titre="Mes priorités" icone={CheckCircle2}><BlocDonnees data={synthese.data} isLoading={synthese.isLoading} isError={synthese.isError}>{(data) => <MesPriorites synthese={data}/>}</BlocDonnees></Section></div>

      {/* Ligne 3 : planning et répartition de la charge */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Section titre="Mon planning" icone={CalendarClock}><ChargeSemaine charge={synthese.data?.chargeSemaine} isLoading={synthese.isLoading} isError={synthese.isError} error={synthese.error}/></Section>
        <Section titre="Ma charge de travail" icone={Clock3}><BlocDonnees data={synthese.data} isLoading={synthese.isLoading} isError={synthese.isError}>{(data) => <Repartition synthese={data}/>}</BlocDonnees></Section>
      </div>

      {/* Ligne 4 : prochains essais et activité récente */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="titre-prochains">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2"><FlaskConical className="size-5 text-brand-900" aria-hidden="true"/><h2 id="titre-prochains" className="text-lg font-bold text-ink-900">Mes prochains essais</h2></div>
            <Link to="/app/essais" search={{ page: 1 }} className="group flex shrink-0 items-center gap-1 text-sm font-semibold text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-brand-900">
              Voir tous <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true"/>
            </Link>
          </div>
          <ProchainsEssais/>
        </section>
        <Section titre="Activité récente" icone={Activity}><ActiviteDisponible/></Section>
      </div>

      {/* Ligne 5 : statistiques mensuelles secondaires */}
      <div className="mt-6"><Section titre="Mon activité ce mois-ci" icone={RotateCcw}><BlocDonnees data={synthese.data} isLoading={synthese.isLoading} isError={synthese.isError}>{(data) => <ActiviteMois synthese={data}/>}</BlocDonnees></Section></div>
    </Container>
  );
}

function BlocDonnees({ data, isLoading, isError, children }: { data: SyntheseTechnicienDto | undefined; isLoading: boolean; isError: boolean; children: (data: SyntheseTechnicienDto) => ReactNode }) {
  if (isLoading) return <Skeleton className="h-40 rounded-3xl"/>;
  if (isError || !data) return <Card variant="flat" padding="md"><p role="alert" className="text-sm text-red-700">Impossible de charger ces indicateurs.</p></Card>;
  return <>{children(data)}</>;
}
