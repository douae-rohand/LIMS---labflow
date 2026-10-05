import { createFileRoute, useRouter } from "@tanstack/react-router";
import { LogOut, User } from "lucide-react";
import { Button, Card, Badge } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { logout } from "@/api/auth";
import { clearSession, getSession, getStatus } from "@/api/session";
import type { RoleUtilisateur } from "@/api/session";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Retourne le chemin du tableau de bord selon le rôle.
 * À enrichir lorsque les modules métier auront leurs propres routes.
 */
export function cheminTableauDeBord(_role: RoleUtilisateur): "/app" {
  return "/app";
}

const LIBELLE_ROLE: Record<RoleUtilisateur, string> = {
  SUPER_ADMINISTRATEUR: "Super Administrateur",
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE_LABO: "Responsable",
  TECHNICIEN: "Technicien",
  CLIENT: "Client",
};

// ---------------------------------------------------------------------------
// Garde : exige une session connectée + bloque si mustChangePassword
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/app")({
  head: () => ({
    meta: [{ title: "Tableau de bord — LabFlow LIMS" }],
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
    // 'inconnu' : restoreSession() a déjà tourné → impossible d'arriver ici
  },
  component: AppPage,
});

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

function AppPage() {
  const router = useRouter();
  const session = getSession();
  const user = session?.user;

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // En cas d'échec réseau, on déconnecte quand même côté client
    } finally {
      clearSession();
      // Redirection vers /login même si logout a échoué
      await router.navigate({ to: "/login" });
    }
  };

  if (!user) {
    // Ne devrait pas arriver grâce à la garde, mais protège le rendu
    void router.navigate({ to: "/login" });
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* En-tête */}
      <header className="border-b border-brand-900/10 bg-card px-6 py-4">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <span className="text-lg font-extrabold text-ink-900">LabFlow LIMS</span>
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

      {/* Corps */}
      <Container className="py-12">
        <Card variant="flat" padding="lg" className="mx-auto max-w-lg">
          <div className="flex items-start gap-4">
            <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-brand-900/10">
              <User className="size-6 text-brand-900" aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
              <h1 className="truncate text-xl font-extrabold text-ink-900">
                {user.nomComplet}
              </h1>
              <p className="mt-0.5 truncate text-sm text-muted-foreground">{user.email}</p>
              <div className="mt-3">
                <Badge>{LIBELLE_ROLE[user.role] ?? user.role}</Badge>
              </div>
            </div>
          </div>

          {user.tenantId && (
            <p className="mt-6 text-xs text-muted-foreground">
              Laboratoire&nbsp;:{" "}
              <span className="font-medium text-ink-900">{user.tenantId}</span>
            </p>
          )}
        </Card>
      </Container>
    </div>
  );
}
