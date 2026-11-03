import { Link } from "@tanstack/react-router";
import { Building2, LayoutDashboard, LogOut, ScrollText, Users } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/lab";
import { Toaster } from "@/components/ui/sonner";
import type { SessionUser } from "@/api/session";
import { cn } from "@/lib/utils";

const LIEN = cn(
  "flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold",
  "text-ink-900/70 hover:bg-brand-900/8 hover:text-brand-900",
  "transition-colors duration-150",
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
);
const ACTIF = cn(
  "flex items-center gap-3 rounded-2xl bg-brand-900 px-3 py-2.5 text-sm font-semibold text-primary-foreground",
  "hover:bg-brand-800 hover:text-accent-lime",
  "transition-colors duration-150",
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
);

export function SuperAdminShell({
  children,
  utilisateur,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  onLogout: () => void;
}) {
  const initiales = utilisateur.nomComplet
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((partie) => partie[0]?.toUpperCase())
    .join("") || "LF";

  return (
    <div className="flex min-h-screen flex-col bg-surface-alt">
      <Toaster />
      <div className="flex min-h-screen flex-1">
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-brand-900/10 bg-card md:flex" aria-label="Navigation principale">
          <div className="border-b border-brand-900/10 px-5 py-5">
            <Link to="/app/tableau-de-bord" className="rounded-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900" aria-label="LabFlow — tableau de bord">
              <img src="/nom.png" alt="LabFlow" className="h-8" />
              <p className="mt-1 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">Espace plateforme</p>
            </Link>
          </div>
          <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-4">
            <p className="px-2 pb-2 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase">Plateforme</p>
            <Lien to="/app/tableau-de-bord" icone={<LayoutDashboard className="size-4" aria-hidden="true" />} libelle="Tableau de bord" />
            <Lien to="/app/gestion-laboratoires" search={{ onglet: "laboratoires" }} icone={<Building2 className="size-4" aria-hidden="true" />} libelle="Laboratoires" />
            <Lien to="/app/utilisateurs" icone={<Users className="size-4" aria-hidden="true" />} libelle="Utilisateurs" />
            <Lien to="/app/journal-audit" icone={<ScrollText className="size-4" aria-hidden="true" />} libelle="Journal d'audit" />
          </nav>
          <div className="border-t border-brand-900/10 p-3">
            <div className="flex items-center gap-3 rounded-2xl px-3 py-2.5">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-brand-900 text-xs font-bold text-primary-foreground" aria-hidden="true">{initiales}</span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-ink-900">{utilisateur.nomComplet}</p>
                <p className="truncate text-xs text-muted-foreground">Super Administrateur</p>
              </div>
              <Button variant="ghost" size="icon" onClick={onLogout} aria-label="Se déconnecter">
                <LogOut className="size-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </aside>
        <div className="flex min-w-0 flex-1 flex-col">
          <header className="flex items-center justify-between gap-3 border-b border-brand-900/10 bg-card px-4 py-3 md:hidden">
            <Link to="/app/tableau-de-bord" aria-label="LabFlow — tableau de bord">
              <img src="/nom.png" alt="LabFlow" className="h-7" />
            </Link>
            <div className="flex items-center gap-2">
              <span className="flex size-8 items-center justify-center rounded-full bg-brand-900 text-xs font-bold text-primary-foreground" aria-label={utilisateur.nomComplet}>{initiales}</span>
              <Button variant="ghost" size="sm" onClick={onLogout} aria-label="Se déconnecter">
                <LogOut className="size-4" aria-hidden="true" />
              </Button>
            </div>
          </header>
          <nav className="flex gap-1 overflow-x-auto border-b border-brand-900/10 bg-card px-3 py-2 md:hidden" aria-label="Navigation">
            <Lien to="/app/tableau-de-bord" icone={<LayoutDashboard className="size-4" aria-hidden="true" />} libelle="Tableau de bord" />
            <Lien to="/app/gestion-laboratoires" search={{ onglet: "laboratoires" }} icone={<Building2 className="size-4" aria-hidden="true" />} libelle="Laboratoires" />
            <Lien to="/app/utilisateurs" icone={<Users className="size-4" aria-hidden="true" />} libelle="Utilisateurs" />
            <Lien to="/app/journal-audit" icone={<ScrollText className="size-4" aria-hidden="true" />} libelle="Journal" />
          </nav>
          <main className="min-w-0 flex-1">
            <div className="mx-auto w-full max-w-[1440px]">{children}</div>
          </main>
        </div>
      </div>
    </div>
  );
}

function Lien({
  to,
  search,
  icone,
  libelle,
}: {
  to: "/app/tableau-de-bord" | "/app/gestion-laboratoires" | "/app/utilisateurs" | "/app/journal-audit";
  search?: { onglet: "laboratoires" };
  icone: ReactNode;
  libelle: string;
}) {
  return (
    <Link to={to} search={search} className={LIEN} activeProps={{ className: ACTIF }}>
      {icone}
      {libelle}
    </Link>
  );
}
