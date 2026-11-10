import { Link, useNavigate } from "@tanstack/react-router";
import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { ApiError } from "@/api/axios";
import type { PlanningSearch } from "@/routes/app/planning";
import { useTechnicienPlanning } from "@/hooks/useTechnicienEssais";
import {
  classeStatutEssai,
  formatDateLocale,
  libelleStatutEssai,
} from "@/components/technicien/libelles";
import type { PlanningJourDto, LigneEssaiTechnicienDto } from "@/api/technicien/types";
import { LigneEssaiCard } from "@/components/technicien/essais/LigneEssaiCard";
import { cn } from "@/lib/utils";

const PLAGE_DEFAUT = 7;
const PLAGE_MAX = 62;

/** Construit une date YYYY-MM-DD à partir d'un objet Date local. */
function versLocalDate(d: Date): string {
  const a = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const j = String(d.getDate()).padStart(2, "0");
  return `${a}-${m}-${j}`;
}

/** Parse une date YYYY-MM-DD en objet Date local (sans décalage UTC). */
function depuisLocalDate(s: string): Date {
  const parts = s.split("-").map(Number);
  const a = parts[0] ?? 1970;
  const m = parts[1] ?? 1;
  const j = parts[2] ?? 1;
  return new Date(a, m - 1, j);
}

/** Ajoute des jours à une date locale (retourne YYYY-MM-DD). */
function ajouterJours(localDate: string, jours: number): string {
  const d = depuisLocalDate(localDate);
  d.setDate(d.getDate() + jours);
  return versLocalDate(d);
}

/** Vérifie qu'une chaîne est une date YYYY-MM-DD valide. */
function estDateValide(s: string | undefined): s is string {
  if (!s) return false;
  return /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(depuisLocalDate(s).getTime());
}

/** Nombre de jours entre deux dates locales. */
function nbJours(debut: string, fin: string): number {
  const d = depuisLocalDate(debut).getTime();
  const f = depuisLocalDate(fin).getTime();
  return Math.round((f - d) / 86_400_000);
}

export function PlanningTechnicien({
  debut,
  fin,
}: {
  debut: string | undefined;
  fin: string | undefined;
}) {
  const navigate = useNavigate();

  // Résolution des dates : défaut = aujourd'hui + 6 jours
  const aujourdhui = versLocalDate(new Date());
  const debutFinal = estDateValide(debut) ? debut : aujourdhui;
  const finInitiale = estDateValide(fin) ? fin : ajouterJours(aujourdhui, PLAGE_DEFAUT - 1);

  // Si fin avant début, on corrige
  const finFinal = nbJours(debutFinal, finInitiale) >= 0 ? finInitiale : debutFinal;

  const query = useTechnicienPlanning(debutFinal, finFinal);

  const mettreAJourPlage = (nouveauDebut: string, nouvelleFin: string) => {
    void navigate({
      to: "/app/planning",
      search: (prev: PlanningSearch) => ({ ...prev, debut: nouveauDebut, fin: nouvelleFin }),
      replace: true,
    });
  };

  const handlePrecedent = () =>
    mettreAJourPlage(ajouterJours(debutFinal, -PLAGE_DEFAUT), ajouterJours(finFinal, -PLAGE_DEFAUT));
  const handleSuivant = () =>
    mettreAJourPlage(ajouterJours(debutFinal, PLAGE_DEFAUT), ajouterJours(finFinal, PLAGE_DEFAUT));
  const handleAujourdhui = () => mettreAJourPlage(aujourdhui, ajouterJours(aujourdhui, PLAGE_DEFAUT - 1));

  const jours = nbJours(debutFinal, finFinal) + 1;

  return (
    <div className="space-y-5">
      {/* En-tête + navigation */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
            <CalendarDays className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Planning</h1>
            <p className="text-sm text-muted-foreground">
              Du {formatDateLocale(debutFinal)} au {formatDateLocale(finFinal)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="icon" onClick={handlePrecedent} aria-label="Semaine précédente">
            <ChevronLeft className="size-4" aria-hidden="true" />
          </Button>
          <Button variant="secondary" size="sm" onClick={handleAujourdhui}>
            Aujourd&apos;hui
          </Button>
          <Button variant="secondary" size="icon" onClick={handleSuivant} aria-label="Semaine suivante">
            <ChevronRight className="size-4" aria-hidden="true" />
          </Button>
        </div>
      </div>

      {/* Alerte plage trop grande */}
      {jours > PLAGE_MAX && (
        <Card variant="flat" padding="md" className="border-amber-200 bg-amber-50">
          <p className="text-sm font-medium text-amber-800" role="alert">
            La plage demandée dépasse {PLAGE_MAX} jours. Réduisez la période pour afficher le planning.
          </p>
        </Card>
      )}

      {/* Corps du planning */}
      {query.isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full rounded-3xl" />
          ))}
        </div>
      ) : query.isError ? (
        <Card variant="flat" padding="lg" className="text-center">
          <p className="text-sm font-medium text-red-700" role="alert">
            {query.error instanceof ApiError && query.error.status === 403
              ? "Vous n'avez pas la permission de consulter le planning."
              : "Impossible de charger le planning."}
          </p>
          <Button variant="secondary" size="sm" className="mt-3" onClick={() => void query.refetch()}>
            Réessayer
          </Button>
        </Card>
      ) : (
        <ContenuPlanning planning={query.data} />
      )}
    </div>
  );
}

/** Affiche le contenu du planning : colonnes desktop + liste mobile. */
function ContenuPlanning({
  planning,
}: {
  planning: ReturnType<typeof useTechnicienPlanning>["data"];
}) {
  if (!planning || planning.length === 0) {
    return (
      <Card variant="flat" padding="lg" className="text-center">
        <p className="text-sm text-muted-foreground">Aucun essai planifié sur cette période.</p>
      </Card>
    );
  }

  return (
    <>
      {/* Vue desktop : colonnes par jour */}
      <div className="hidden md:grid md:auto-cols-fr md:grid-flow-col md:gap-4 md:overflow-x-auto">
        {planning.map((jour) => (
          <div key={jour.date} className="min-w-48 space-y-2">
            <div className="rounded-2xl bg-surface-alt px-3 py-2">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {formatDateLocale(jour.date)}
              </p>
              <p className="text-xs text-muted-foreground">
                {jour.essais.length} essai{jour.essais.length > 1 ? "s" : ""}
              </p>
            </div>
            <JourColonnes essais={jour.essais} />
          </div>
        ))}
      </div>

      {/* Vue mobile : liste verticale groupée par jour */}
      <div className="space-y-4 md:hidden">
        {planning.map((jour) => (
          <div key={jour.date} className="space-y-2">
            <p className="text-sm font-bold text-ink-900">{formatDateLocale(jour.date)}</p>
            {jour.essais.length === 0 ? (
              <p className="rounded-2xl border border-dashed border-brand-900/10 px-3 py-2 text-xs text-muted-foreground">
                Aucun essai
              </p>
            ) : (
              <div className="space-y-2">
                {jour.essais.map((essai) => (
                  <LigneEssaiCard key={essai.ligneEssaiId} essai={essai} />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </>
  );
}

/** Mini-carte d'essai pour la colonne desktop. */
function JourColonnes({ essais }: { essais: LigneEssaiTechnicienDto[] }) {
  if (essais.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-brand-900/10 px-3 py-4 text-center text-xs text-muted-foreground">
        Aucun essai
      </div>
    );
  }
  return (
    <div className="space-y-2">
      {essais.map((essai) => (
        <Link
          key={essai.ligneEssaiId}
          to="/app/essais/$ligneEssaiId"
          params={{ ligneEssaiId: String(essai.ligneEssaiId) }}
          className="block rounded-2xl border border-brand-900/10 bg-card p-3 transition-colors hover:border-brand-900/20 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
        >
          <div className="flex items-center gap-2">
            <span
              className={cn(
                "inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold",
                classeStatutEssai(essai.statut),
              )}
            >
              {libelleStatutEssai(essai.statut)}
            </span>
            {essai.enRetard && (
              <span className="text-[11px] font-semibold text-red-700">En retard</span>
            )}
          </div>
          <p className="mt-1 truncate text-sm font-semibold text-ink-900">
            {essai.essai.code}
          </p>
          <p className="truncate text-xs text-muted-foreground">{essai.essai.designation}</p>
        </Link>
      ))}
    </div>
  );
}
