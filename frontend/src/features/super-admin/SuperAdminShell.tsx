import { Link } from "@tanstack/react-router";
import { Building2, LogOut } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/lab";
import { Toaster } from "@/components/ui/sonner";
import type { SessionUser } from "@/api/session";

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
    .join("");

  return (
    <div className="min-h-screen bg-surface-alt">
      <Toaster />
      <header className="flex items-center justify-between gap-4 border-b border-brand-900/10 bg-card px-4 py-3 sm:px-6">
        <Link
          to="/app/gestion-laboratoires"
          search={{ onglet: "laboratoires" }}
          className="flex items-center rounded-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
        >
          <img src="/nom.png" alt="LabFlow" className="h-8 sm:h-9" />
        </Link>
        <div className="flex items-center gap-3">
          <div className="hidden text-right sm:block">
            <p className="text-sm font-semibold text-ink-900">{utilisateur.nomComplet}</p>
            <p className="text-xs text-muted-foreground">Super Administrateur</p>
          </div>
          <span
            className="flex size-9 items-center justify-center rounded-full bg-brand-900 text-xs font-bold text-primary-foreground"
            aria-hidden="true"
          >
            {initiales || "SA"}
          </span>
          <Button
            variant="secondary"
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
      <div className="flex min-h-[calc(100vh-61px)] flex-col md:flex-row">
        <aside className="border-b border-brand-900/10 bg-card md:w-64 md:shrink-0 md:border-r md:border-b-0">
          <p className="hidden px-5 pt-5 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase md:block">
            Administration
          </p>
          <nav className="p-3 md:px-3 md:py-3" aria-label="Espace super administrateur">
            <Link
              to="/app/gestion-laboratoires"
              search={{ onglet: "laboratoires" }}
              className="flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold text-ink-900/70 hover:bg-surface-alt focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
              activeOptions={{ includeSearch: false }}
              activeProps={{
                className:
                  "flex items-center gap-3 rounded-2xl bg-brand-900 px-3 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-brand-900",
              }}
            >
              <Building2 className="size-4 shrink-0" aria-hidden="true" />
              Gestion des laboratoires
            </Link>
          </nav>
        </aside>
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}
