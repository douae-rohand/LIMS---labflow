import { createFileRoute, Link } from "@tanstack/react-router";
import { User } from "lucide-react";
import { Badge, Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { getSession } from "@/api/session";
import type { RoleUtilisateur } from "@/api/session";

export const Route = createFileRoute("/app/")({
  component: AppHomePage,
});

const LIBELLE_ROLE: Record<RoleUtilisateur, string> = {
  SUPER_ADMINISTRATEUR: "Super Administrateur",
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE_LABO: "Responsable",
  TECHNICIEN: "Technicien",
  CLIENT: "Client",
};

function AppHomePage() {
  const session = getSession();
  const user = session?.user;
  if (!user) return null;

  return (
    <Container className="py-12">
      <Card variant="flat" padding="lg" className="mx-auto max-w-lg">
        <div className="flex items-start gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-brand-900/10">
            <User className="size-6 text-brand-900" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-xl font-extrabold text-ink-900">{user.nomComplet}</h1>
            <p className="mt-0.5 truncate text-sm text-muted-foreground">{user.email}</p>
            <div className="mt-3">
              <Badge>{LIBELLE_ROLE[user.role] ?? user.role}</Badge>
            </div>
          </div>
        </div>
        {user.tenantId && (
          <p className="mt-6 text-xs text-muted-foreground">
            Laboratoire&nbsp;: <span className="font-medium text-ink-900">{user.tenantId}</span>
          </p>
        )}
        {user.role === "SUPER_ADMINISTRATEUR" && (
          <div className="mt-8">
            <Link to="/app/demandes-integration">
              <Button>Traiter les demandes d'intégration</Button>
            </Link>
          </div>
        )}
      </Card>
    </Container>
  );
}
