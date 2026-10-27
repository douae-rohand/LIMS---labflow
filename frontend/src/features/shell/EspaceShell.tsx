import { Link } from "@tanstack/react-router";
import { LogOut } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/lab";
import { Toaster } from "@/components/ui/sonner";
import type { SessionUser } from "@/api/session";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getInitiales(nomComplet: string): string {
  return nomComplet
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((partie) => partie[0]?.toUpperCase())
    .join("");
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

/** Chemins d'entrée de la sidebar (espace connecté `/app`). */
export type CheminEspace =
  | "/app"
  | "/app/demandes"
  | "/app/catalogue"
  | "/app/demandes-clients"
  | "/app/employes"
  | "/app/panier"
  | "/app/laboratoires"
  | "/app/carte";

export type EntreeNavigation = {
  to: CheminEspace;
  label: string;
  icon: LucideIcon;
  /** Mise en évidence stricte du chemin (utile pour `/app`, parent de tout). */
  exact?: boolean | undefined;
  /** Nombre affiché en pastille (ex. essais du panier). Masqué si 0 ou absent. */
  badge?: number | undefined;
};

// ---------------------------------------------------------------------------
// Classes (identiques à celles de l'historique AdminShell)
// ---------------------------------------------------------------------------

const CLASSES_LIEN = cn(
  "flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold",
  "text-ink-900/70 hover:bg-brand-900/8 hover:text-brand-900",
  "transition-colors duration-150",
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
);

const CLASSES_LIEN_ACTIF = cn(
  "flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold",
  "bg-brand-900 text-primary-foreground",
  "hover:bg-brand-800 hover:text-accent-lime",
  "transition-colors duration-150",
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
);

const CLASSES_LIEN_MOBILE = cn(
  "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold whitespace-nowrap",
  "text-ink-900/70 hover:bg-brand-900/8 hover:text-ink-900 transition-colors duration-150",
);

const CLASSES_LIEN_MOBILE_ACTIF = cn(
  "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold whitespace-nowrap",
  "bg-brand-900 text-primary-foreground hover:bg-brand-800 hover:text-accent-lime transition-colors duration-150",
);

function PastilleBadge({ valeur }: { valeur: number }) {
  return (
    <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-accent-lime px-1.5 text-xs font-bold text-ink-900">
      {valeur}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Composant
// ---------------------------------------------------------------------------

/**
 * Structure commune des espaces connectés : sidebar fixe sur desktop, en-tête et
 * navigation horizontale sur mobile, bloc identité + déconnexion.
 */
export function EspaceShell({
  children,
  utilisateur,
  libelleRole,
  accueil,
  titreSection,
  navigation,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  libelleRole: string;
  accueil: CheminEspace;
  titreSection: string;
  navigation: EntreeNavigation[];
  onLogout: () => void;
}) {
  const initiales = getInitiales(utilisateur.nomComplet) || "LF";

  return (
    <div className="min-h-screen bg-surface-alt flex flex-col">
      <Toaster />

      {/* Layout principal : sidebar + contenu */}
      <div className="flex flex-1 min-h-screen">
        {/* -------- Sidebar -------- */}
        <aside
          className="hidden md:flex flex-col w-64 shrink-0 bg-card border-r border-brand-900/10 sticky top-0 h-screen"
          aria-label="Navigation principale"
        >
          {/* Logo */}
          <div className="flex items-center px-5 py-5 border-b border-brand-900/10">
            <Link
              to={accueil}
              className="flex items-center focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 rounded-xl"
              aria-label="LabFlow LIMS — retour à l'accueil"
            >
              <img src="/nom.png" alt="LabFlow" className="h-8" />
            </Link>
          </div>

          {/* Section nav */}
          <div className="flex-1 overflow-y-auto py-4">
            <p className="px-5 pb-2 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">
              {titreSection}
            </p>
            <nav className="px-3 space-y-0.5">
              {navigation.map(({ to, label, icon: Icon, exact, badge }) => (
                <Link
                  key={to}
                  to={to}
                  className={CLASSES_LIEN}
                  activeOptions={{ exact: exact ?? false }}
                  activeProps={{ className: CLASSES_LIEN_ACTIF }}
                >
                  <Icon className="size-4 shrink-0" aria-hidden="true" />
                  {label}
                  {badge !== undefined && badge > 0 ? <PastilleBadge valeur={badge} /> : null}
                </Link>
              ))}
            </nav>
          </div>

          {/* Profil utilisateur + déconnexion */}
          <div className="border-t border-brand-900/10 p-3">
            <div className="flex items-center gap-3 rounded-2xl px-3 py-2.5">
              {/* Avatar initiales */}
              <span
                className="flex size-9 shrink-0 items-center justify-center rounded-full bg-brand-900 text-xs font-bold text-primary-foreground"
                aria-hidden="true"
              >
                {initiales}
              </span>
              {/* Nom + rôle */}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-ink-900">
                  {utilisateur.nomComplet}
                </p>
                <p className="truncate text-xs text-muted-foreground">{libelleRole}</p>
              </div>
              {/* Bouton déconnexion (icône seule) */}
              <Button
                variant="ghost"
                size="icon"
                onClick={onLogout}
                aria-label="Se déconnecter"
                className="shrink-0 text-muted-foreground hover:text-ink-900"
              >
                <LogOut className="size-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </aside>

        {/* -------- Topbar mobile -------- */}
        <div className="flex flex-1 flex-col min-w-0">
          <header className="md:hidden flex items-center justify-between gap-4 border-b border-brand-900/10 bg-card px-4 py-3">
            <Link to={accueil} className="flex items-center">
              <img src="/nom.png" alt="LabFlow" className="h-7" />
            </Link>

            <div className="flex items-center gap-2">
              {/* Avatar utilisateur mobile */}
              <span
                className="flex size-8 items-center justify-center rounded-full bg-brand-900 text-xs font-bold text-primary-foreground"
                aria-label={utilisateur.nomComplet}
              >
                {initiales}
              </span>
              <Button
                variant="ghost"
                size="sm"
                icon={<LogOut className="size-4" aria-hidden="true" />}
                iconPosition="left"
                onClick={onLogout}
                aria-label="Se déconnecter"
              >
                <span className="hidden sm:inline">Se déconnecter</span>
              </Button>
            </div>
          </header>

          {/* Nav mobile horizontale */}
          <nav
            className="md:hidden flex items-center gap-1 border-b border-brand-900/10 bg-card px-3 py-2 overflow-x-auto no-scrollbar"
            aria-label="Navigation"
          >
            {navigation.map(({ to, label, icon: Icon, exact, badge }) => (
              <Link
                key={to}
                to={to}
                className={CLASSES_LIEN_MOBILE}
                activeOptions={{ exact: exact ?? false }}
                activeProps={{ className: CLASSES_LIEN_MOBILE_ACTIF }}
              >
                <Icon className="size-4 shrink-0" aria-hidden="true" />
                {label}
                {badge !== undefined && badge > 0 ? <PastilleBadge valeur={badge} /> : null}
              </Link>
            ))}
          </nav>

          {/* Contenu de la page */}
          <main className="flex-1 min-w-0">{children}</main>
        </div>
      </div>
    </div>
  );
}
