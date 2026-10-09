import { Outlet, createFileRoute, useRouter } from "@tanstack/react-router";
import { Link } from "@tanstack/react-router";
import { LogOut } from "lucide-react";
import { Button } from "@/components/lab";
import { logout } from "@/api/auth";
import { clearSession, getSession, getStatus } from "@/api/session";
import type { RoleUtilisateur } from "@/api/session";

export function cheminTableauDeBord(_role: RoleUtilisateur): "/app" {
  return "/app";
}

export const Route = createFileRoute("/app")({
  head: () => ({
    meta: [{ title: "Espace LabFlow — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    const status = getStatus();
    if (status === "anonyme") {
      throw Route.redirect({ to: "/login" });
    }
    if (status === "connecté") {
      const session = getSession();
      if (session?.user.mustChangePassword) {
        throw Route.redirect({ to: "/changer-mot-de-passe" });
      }
    }
  },
  component: AppLayout,
});

function AppLayout() {
  const router = useRouter();
  const session = getSession();
  const user = session?.user;
  const isSuperAdmin = user?.role === "SUPER_ADMINISTRATEUR";

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // Déconnexion locale même si l'API échoue
    } finally {
      clearSession();
      await router.navigate({ to: "/login" });
    }
  };

  if (!user) {
    void router.navigate({ to: "/login" });
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-brand-900/10 bg-card px-6 py-4">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
          <div className="flex items-center gap-6">
            <Link to="/app" className="flex items-center">
              <img src="/nom.png" alt="LabFlow" className="h-10" />
            </Link>
            {isSuperAdmin && (
              <nav className="hidden items-center gap-3 text-sm font-semibold sm:flex">
                <Link to="/app" className="text-ink-900/70 hover:text-ink-900">
                  Tableau de bord
                </Link>
                <Link
                  to="/app/demandes-integration"
                  className="text-ink-900/70 hover:text-ink-900"
                >
                  Demandes d'intégration
                </Link>
              </nav>
            )}
          </div>
          <Button
            variant="ghost"
            size="sm"
            icon={<LogOut className="size-4" aria-hidden="true" />}
            iconPosition="right"
            onClick={() => void handleLogout()}
            aria-label="Se déconnecter"
          >
            Se déconnecter
          </Button>
        </div>
      </header>
      <Outlet />
    </div>
  );
}
