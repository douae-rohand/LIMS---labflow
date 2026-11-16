import { useRef, useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { ApiError } from "@/api/axios";
import type { PlanningSearch } from "@/routes/app/planning";
import { useTechnicienPlanning } from "@/hooks/useTechnicienEssais";
import {
  classeStatutEssai,
  libelleStatutEssai,
} from "@/components/technicien/libelles";
import type { LigneEssaiTechnicienDto } from "@/api/technicien/types";
import { LigneEssaiCard } from "@/components/technicien/essais/LigneEssaiCard";
import { cn } from "@/lib/utils";

const PLAGE_DEFAUT = 7;
const PLAGE_MAX = 62;

function versLocalDate(d: Date): string {
  const a = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const j = String(d.getDate()).padStart(2, "0");
  return `${a}-${m}-${j}`;
}

function depuisLocalDate(s: string): Date {
  const parts = s.split("-").map(Number);
  const a = parts[0] ?? 1970;
  const m = parts[1] ?? 1;
  const j = parts[2] ?? 1;
  return new Date(a, m - 1, j);
}

function ajouterJours(localDate: string, jours: number): string {
  const d = depuisLocalDate(localDate);
  d.setDate(d.getDate() + jours);
  return versLocalDate(d);
}

function estDateValide(s: string | undefined): s is string {
  if (!s) return false;
  return /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(depuisLocalDate(s).getTime());
}

function nbJours(debut: string, fin: string): number {
  const d = depuisLocalDate(debut).getTime();
  const f = depuisLocalDate(fin).getTime();
  return Math.round((f - d) / 86_400_000);
}

/** Formate une date YYYY-MM-DD en "lun. 09 nov." */
function formatJourCourt(dateStr: string): { jour: string; numero: string; mois: string } {
  const d = depuisLocalDate(dateStr);
  const jour = d.toLocaleDateString("fr-FR", { weekday: "short" });
  const numero = String(d.getDate());
  const mois = d.toLocaleDateString("fr-FR", { month: "short" });
  return { jour, numero, mois };
}

/** Formate une plage de dates en "09 nov. — 15 nov. 2026". */
function formatPlage(debut: string, fin: string): string {
  const d = depuisLocalDate(debut);
  const f = depuisLocalDate(fin);
  const opts: Intl.DateTimeFormatOptions = { day: "numeric", month: "short" };
  const annee = f.getFullYear();
  return `${d.toLocaleDateString("fr-FR", opts)} — ${f.toLocaleDateString("fr-FR", { ...opts, year: "numeric" })}`;
}

/** Vérifie si une date YYYY-MM-DD est aujourd'hui. */
function estAujourdhui(dateStr: string): boolean {
  return dateStr === versLocalDate(new Date());
}

export function PlanningTechnicien({
  debut,
  fin,
}: {
  debut: string | undefined;
  fin: string | undefined;
}) {
  const navigate = useNavigate();
  const dateInputRef = useRef<HTMLInputElement>(null);

  const aujourdhui = versLocalDate(new Date());
  const debutFinal = estDateValide(debut) ? debut : aujourdhui;
  const finInitiale = estDateValide(fin) ? fin : ajouterJours(aujourdhui, PLAGE_DEFAUT - 1);
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
  const handleAujourdhui = () =>
    mettreAJourPlage(aujourdhui, ajouterJours(aujourdhui, PLAGE_DEFAUT - 1));

  /** Sélection d'une date via input[type=date] : on centre la semaine autour de cette date. */
  const handleSelectDate = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    if (!estDateValide(val)) return;
    // Trouver le lundi de la semaine contenant la date choisie
    const d = depuisLocalDate(val);
    const jourSemaine = d.getDay(); // 0=dim, 1=lun…
    const lundiOffset = jourSemaine === 0 ? -6 : 1 - jourSemaine;
    const lundi = new Date(d);
    lundi.setDate(d.getDate() + lundiOffset);
    const debutSemaine = versLocalDate(lundi);
    mettreAJourPlage(debutSemaine, ajouterJours(debutSemaine, PLAGE_DEFAUT - 1));
  };

  const jours = nbJours(debutFinal, finFinal) + 1;

  return (
    <div className="space-y-5">
      {/* En-tête + navigation */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        {/* Titre */}
        <div className="flex items-center gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
            <CalendarDays className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Planning</h1>
            <p className="text-sm text-muted-foreground">{formatPlage(debutFinal, finFinal)}</p>
          </div>
        </div>

        {/* Navigation */}
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="icon"
            onClick={handlePrecedent}
            aria-label="Semaine précédente"
          >
            <ChevronLeft className="size-4" aria-hidden="true" />
          </Button>

          <Button variant="secondary" size="sm" onClick={handleAujourdhui}>
            Aujourd&apos;hui
          </Button>

          <Button
            variant="secondary"
            size="icon"
            onClick={handleSuivant}
            aria-label="Semaine suivante"
          >
            <ChevronRight className="size-4" aria-hidden="true" />
          </Button>

          {/* Sélecteur de date */}
          <div className="relative">
            <Button
              variant="secondary"
              size="icon"
              aria-label="Choisir une date"
              onClick={() => dateInputRef.current?.showPicker?.()}
            >
              <CalendarDays className="size-4" aria-hidden="true" />
            </Button>
            <input
              ref={dateInputRef}
              type="date"
              value={debutFinal}
              onChange={handleSelectDate}
              className="absolute inset-0 opacity-0 cursor-pointer"
              aria-label="Naviguer vers une date"
            />
          </div>
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

      {/* Corps */}
      {query.isLoading ? (
        <div className="grid grid-cols-7 gap-3">
          {Array.from({ length: 7 }).map((_, i) => (
            <Skeleton key={i} className="h-48 w-full rounded-2xl" />
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
      {/* Vue desktop */}
      <div className="hidden md:block overflow-x-auto">
        <div
          className="grid gap-3"
          style={{ gridTemplateColumns: `repeat(${planning.length}, minmax(140px, 1fr))` }}
        >
          {planning.map((jour) => {
            const { jour: jourLib, numero, mois } = formatJourCourt(jour.date);
            const actif = estAujourdhui(jour.date);
            const nbEssais = jour.essais.length;
            return (
              <div key={jour.date} className="flex flex-col gap-2">
                {/* En-tête colonne */}
                <div
                  className={cn(
                    "rounded-2xl px-3 py-2.5 text-center transition-colors",
                    actif
                      ? "bg-brand-900 text-primary-foreground"
                      : "bg-surface-alt",
                  )}
                >
                  <p className={cn(
                    "text-[11px] font-semibold uppercase tracking-widest",
                    actif ? "text-white/70" : "text-muted-foreground",
                  )}>
                    {jourLib}
                  </p>
                  <p className={cn(
                    "text-2xl font-bold leading-tight",
                    actif ? "text-white" : "text-ink-900",
                  )}>
                    {numero}
                  </p>
                  <p className={cn(
                    "text-[11px]",
                    actif ? "text-white/70" : "text-muted-foreground",
                  )}>
                    {mois}
                  </p>
                  <p className={cn(
                    "mt-1 text-[11px] font-medium",
                    actif ? "text-white/80" : "text-muted-foreground",
                  )}>
                    {nbEssais === 0 ? "Aucun essai" : `${nbEssais} essai${nbEssais > 1 ? "s" : ""}`}
                  </p>
                </div>

                {/* Cartes essais */}
                <ColonneJour essais={jour.essais} />
              </div>
            );
          })}
        </div>
      </div>

      {/* Vue mobile */}
      <div className="space-y-4 md:hidden">
        {planning.map((jour) => {
          const { jour: jourLib, numero, mois } = formatJourCourt(jour.date);
          const actif = estAujourdhui(jour.date);
          return (
            <div key={jour.date} className="space-y-2">
              <div className={cn(
                "flex items-center gap-2 rounded-2xl px-3 py-2",
                actif ? "bg-brand-900" : "bg-surface-alt",
              )}>
                <span className={cn("text-base font-bold", actif ? "text-white" : "text-ink-900")}>
                  {numero} {mois}
                </span>
                <span className={cn("text-xs capitalize", actif ? "text-white/70" : "text-muted-foreground")}>
                  {jourLib}
                </span>
              </div>
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
          );
        })}
      </div>
    </>
  );
}

function ColonneJour({ essais }: { essais: LigneEssaiTechnicienDto[] }) {
  if (essais.length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center rounded-2xl border border-dashed border-brand-900/10 px-2 py-6 text-center text-xs text-muted-foreground">
        —
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
          className="block rounded-2xl border border-brand-900/10 bg-card p-3 transition-colors hover:border-brand-900/30 hover:shadow-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
        >
          {/* Statut */}
          <div className="flex flex-wrap items-center gap-1.5 mb-1.5">
            <span className={cn(
              "inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold",
              classeStatutEssai(essai.statut),
            )}>
              {libelleStatutEssai(essai.statut)}
            </span>
            {essai.enRetard && (
              <span className="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-[10px] font-semibold text-red-700">
                En retard
              </span>
            )}
          </div>
          {/* Code + désignation */}
          <p className="truncate text-sm font-bold text-ink-900">{essai.essai.code}</p>
          <p className="truncate text-xs text-muted-foreground leading-snug">{essai.essai.designation}</p>
        </Link>
      ))}
    </div>
  );
}
