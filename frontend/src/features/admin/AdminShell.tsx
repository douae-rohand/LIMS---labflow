import { Link } from "@tanstack/react-router";
import { Inbox, LogOut, Users } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/lab";
import { Toaster } from "@/components/ui/sonner";
import type { RoleUtilisateur, SessionUser } from "@/api/session";
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
// Entrées de navigation de la sidebar admin
// ---------------------------------------------------------------------------

const NAV_ITEMS = [
  {
    to: "/app/employes" as const,
    label: "Employés",
    icon: Users,
    roles: ["ADMINISTRATEUR"] as const,
  },
  {
    to: "/app/demandes-clients" as const,
    label: "Demandes clients",
    icon: Inbox,
    roles: ["ADMINISTRATEUR", "RESPONSABLE"] as const,
  },
] as const;

const LIBELLE_ROLE: Partial<Record<RoleUtilisateur, string>> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
};

function entreesVisibles(role: RoleUtilisateur) {
  return NAV_ITEMS.filter((item) => (item.roles as readonly string[]).includes(role));
}

// ---------------------------------------------------------------------------
// Composant
// ---------------------------------------------------------------------------

export function AdminShell({
  children,
  utilisateur,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  onLogout: () => void;
}) {
  const initiales = getInitiales(utilisateur.nomComplet) || "LF";
  const navigation = entreesVisibles(utilisateur.role);
  const accueil = navigation[0]?.to ?? "/app";
  const libelleRole = LIBELLE_ROLE[utilisateur.role] ?? "Laboratoire";

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
              Laboratoire
            </p>
            <nav className="px-3 space-y-0.5">
              {navigation.map(({ to, label, icon: Icon }) => (
                <Link
                  key={to}
                  to={to}
                  className={cn(
                    "flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold",
                    "text-ink-900/70 hover:bg-brand-900/8 hover:text-brand-900",
                    "transition-colors duration-150",
                    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
                  )}
                  activeOptions={{ exact: false }}
                  activeProps={{
                    className: cn(
                      "flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold",
                      "bg-brand-900 text-primary-foreground",
                      "hover:bg-brand-800 hover:text-accent-lime",
                      "transition-colors duration-150",
                      "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
                    ),
                  }}
                >
                  <Icon className="size-4 shrink-0" aria-hidden="true" />
                  {label}
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
                <p className="truncate text-xs text-muted-foreground">
                  {libelleRole}
                </p>
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
            {navigation.map(({ to, label, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                className={cn(
                  "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold whitespace-nowrap",
                  "text-ink-900/70 hover:bg-brand-900/8 hover:text-ink-900 transition-colors duration-150",
                )}
                activeOptions={{ exact: false }}
                activeProps={{
                  className: cn(
                    "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold whitespace-nowrap",
                    "bg-brand-900 text-primary-foreground hover:bg-brand-800 hover:text-accent-lime transition-colors duration-150",
                  ),
                }}
              >
                <Icon className="size-4 shrink-0" aria-hidden="true" />
                {label}
              </Link>
            ))}
          </nav>

          {/* Contenu de la page */}
          <main className="flex-1 min-w-0">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}
