import { Link } from "@tanstack/react-router";
import { CalendarCheck, Clock, FlaskConical, Hourglass, TriangleAlert } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Card } from "@/components/lab";
import { cn } from "@/lib/utils";
import type { SyntheseTechnicienDto } from "@/api/technicien/types";
import type { EssaisSearch } from "@/routes/app/essais/index";

type CarteDef = {
  key: string;
  label: string;
  valeur: number;
  icon: LucideIcon;
  recherche: Partial<EssaisSearch>;
  /** Accent visuel supplémentaire (ex. bordure rouge pour le retard). */
  accent?: "retard" | undefined;
};

/** Construit une date ISO « début de journée » en local (sans décalage UTC). */
function debutJourLocal(date: Date): string {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0);
  return d.toISOString();
}

/** Construit une date ISO « fin de journée » en local. */
function finJourLocal(date: Date): string {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59);
  return d.toISOString();
}

/** Carte synthèse cliquable : mène vers la liste pré-filtrée. */
function CarteSynthese({ carte }: { carte: CarteDef }) {
  const { icon: Icon, label, valeur, accent } = carte;
  const classeAccent =
    accent === "retard"
      ? "border-red-200 bg-red-50/50 hover:border-red-300"
      : "hover:border-brand-900/20";
  return (
    <Link
      to="/app/essais"
      search={carte.recherche}
      className="block focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 rounded-3xl"
    >
      <Card
        variant="flat"
        padding="md"
        className={cn("h-full transition-colors duration-150 cursor-pointer", classeAccent)}
      >
        <div className="flex items-center gap-3">
          <span
            className={cn(
              "flex size-11 shrink-0 items-center justify-center rounded-2xl",
              accent === "retard" ? "bg-red-100 text-red-700" : "bg-brand-900/10 text-brand-900",
            )}
          >
            <Icon className="size-5" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <p className="text-3xl font-extrabold text-ink-900 leading-none">
              {valeur}
            </p>
            <p className="mt-1 text-sm font-medium text-muted-foreground">{label}</p>
          </div>
        </div>
      </Card>
    </Link>
  );
}

/**
 * Grille des cartes synthèse cliquables.
 * Chaque carte mène vers /app/essais avec un filtre adapté.
 */
export function CartesSynthese({ synthese }: { synthese: SyntheseTechnicienDto }) {
  const aujourd = new Date();
  const cartes: CarteDef[] = [
    {
      key: "aujourdhui",
      label: "Aujourd'hui",
      valeur: synthese.essaisAujourdhuiCount,
      icon: CalendarCheck,
      recherche: { page: 1, debut: debutJourLocal(aujourd), fin: finJourLocal(aujourd) },
    },
    {
      key: "enCours",
      label: "En cours",
      valeur: synthese.enCoursCount,
      icon: Clock,
      recherche: { page: 1, statut: "EN_COURS" },
    },
    {
      key: "enRetard",
      label: "En retard",
      valeur: synthese.enRetardCount,
      icon: TriangleAlert,
      recherche: { page: 1, enRetard: true },
      accent: "retard",
    },
    {
      key: "enAttente",
      label: "En attente d'échantillon",
      valeur: synthese.enAttenteEchantillonCount,
      icon: Hourglass,
      recherche: { page: 1, statut: "ATTENTE_ECHANTILLON" },
    },
    {
      key: "aValider",
      label: "À valider techniquement",
      valeur: synthese.validationsTechniquesAttenteCount,
      icon: FlaskConical,
      recherche: { page: 1, statut: "EN_COURS" },
    },
    {
      key: "termines",
      label: "Terminés (7 jours)",
      valeur: synthese.terminesSemaineCount,
      icon: CalendarCheck,
      recherche: { page: 1, statut: "TERMINE" },
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3">
      {cartes.map((carte) => (
        <CarteSynthese key={carte.key} carte={carte} />
      ))}
    </div>
  );
}
