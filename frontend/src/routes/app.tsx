import { Outlet, createFileRoute, useRouter } from "@tanstack/react-router";
import { logout } from "@/api/auth";
import { clearSession, getSession, getStatus } from "@/api/session";
import type { RoleUtilisateur } from "@/api/session";
import { SuperAdminShell } from "@/features/super-admin/SuperAdminShell";
import { AdminShell } from "@/features/admin/AdminShell";

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
  const isAdmin = user?.role === "ADMINISTRATEUR";

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

  if (isAdmin) {
    return (
      <AdminShell utilisateur={user} onLogout={() => void handleLogout()}>
        <Outlet />
      </AdminShell>
    );
  }

  // Autres rôles (RESPONSABLE, TECHNICIEN, ACCUEIL, CLIENT) — layout minimal
  return (
    <div className="min-h-screen bg-background">
      <Outlet />
    </div>
  );
}
