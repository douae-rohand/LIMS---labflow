import { createFileRoute } from "@tanstack/react-router";
import { Container } from "@/components/layout/Container";
import { Card } from "@/components/lab";
import { Users } from "lucide-react";

export const Route = createFileRoute("/app/employes/")({
  head: () => ({
    meta: [{ title: "Employés — LabFlow LIMS" }],
  }),
  component: EmployesPage,
});

function EmployesPage() {
  return (
    <Container className="py-8">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <Users className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Gestion des employés</h1>
          <p className="text-sm text-muted-foreground">
            Consultez, filtrez et gérez les employés de votre laboratoire.
          </p>
        </div>
      </div>

      <Card variant="flat" padding="lg" className="mt-6">
        <p className="text-sm text-muted-foreground">
          Squelette de la page employés initialisé (Lot 1). La liste, les filtres et le panneau latéral seront intégrés aux lots suivants.
        </p>
      </Card>
    </Container>
  );
}
