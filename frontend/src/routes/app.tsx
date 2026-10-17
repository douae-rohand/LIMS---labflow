import { Link, Outlet, createFileRoute, redirect, useRouter } from "@tanstack/react-router";
import { logout } from "@/api/auth";
import { clearSession, getSession, getStatus } from "@/api/session";
import type { RoleUtilisateur } from "@/api/session";
import { SuperAdminShell } from "@/features/super-admin/SuperAdminShell";
import { AdminShell } from "@/features/admin/AdminShell";
import { ClipboardList, LogOut, ShoppingCart, User } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { nombreTotalEssais, usePaniers } from "@/hooks/usePanier";

export function cheminTableauDeBord(_role: RoleUtilisateur): "/app" {
  return "/app";
}

export const Route = createFileRoute("/app")({
  head: () => ({
    meta: [{ title: "Espace LabFlow — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    const status = getStatus();
    if (status === "anonyme") {
      throw redirect({ to: "/login", search: { redirect: location.href } });
    }
    if (status === "connecté") {
      const session = getSession();
      if (session?.user.mustChangePassword) {
        throw redirect({ to: "/changer-mot-de-passe" });
      }
    }
  },
  component: AppLayout,
});

function AppLayout() {
  const router = useRouter();
  const session = getSession();
  const user = session?.user;
  const paniersQuery = usePaniers();
  const nombrePanier = nombreTotalEssais(paniersQuery.data);
  const isSuperAdmin = user?.role === "SUPER_ADMINISTRATEUR";
  const espaceLaboratoire = user?.role === "ADMINISTRATEUR" || user?.role === "RESPONSABLE";

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

  if (isSuperAdmin) {
    return (
      <SuperAdminShell utilisateur={user} onLogout={() => void handleLogout()}>
        <Outlet />
      </SuperAdminShell>
    );
  }

  if (espaceLaboratoire) {
    return (
      <AdminShell utilisateur={user} onLogout={() => void handleLogout()}>
        <Outlet />
      </AdminShell>
    );
  }

  // Autres rôles (RESPONSABLE, TECHNICIEN, ACCUEIL, CLIENT) — layout minimal avec logout
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-background/50 backdrop-blur-md">
        <Container className="flex items-center justify-between py-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-brand-900/10">
              <User className="size-5 text-brand-900" />
            </div>
            <div>
              <p className="text-sm font-semibold text-ink-900">{user.nomComplet}</p>
              <p className="text-xs text-muted-foreground">{user.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {user.role === "CLIENT" ? (
              <Link
                to="/app/demandes"
                className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-brand-900 transition-colors hover:border-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
              >
                <ClipboardList className="size-4" aria-hidden="true" />
                Mes demandes
              </Link>
            ) : null}
            {user.role === "CLIENT" ? (
              <Link
                to="/app/panier"
                className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-brand-900 transition-colors hover:border-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
              >
                <ShoppingCart className="size-4" aria-hidden="true" />
                Mon panier
                {nombrePanier > 0 ? (
                  <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-brand-900 px-1.5 text-xs font-bold text-primary-foreground">
                    {nombrePanier}
                  </span>
                ) : null}
              </Link>
            ) : null}
            <Button
              variant="secondary"
              size="sm"
              onClick={() => void handleLogout()}
              icon={<LogOut className="size-4" />}
              iconPosition="left"
            >
              Déconnexion
            </Button>
          </div>
        </Container>
      </header>
      <Outlet />
    </div>
  );
}
