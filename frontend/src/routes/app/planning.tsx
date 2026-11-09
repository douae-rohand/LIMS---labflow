import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { Container } from "@/components/layout/Container";
import { PlanningTechnicien } from "@/components/technicien/planning/PlanningTechnicien";

export interface PlanningSearch {
  debut?: string | undefined;
  fin?: string | undefined;
}

/** Valide qu'une chaîne est une date YYYY-MM-DD. */
function estDateLocale(v: unknown): v is string {
  if (typeof v !== "string") return false;
  return /^\d{4}-\d{2}-\d{2}$/.test(v);
}

export const Route = createFileRoute("/app/planning")({
  head: () => ({ meta: [{ title: "Planning — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "TECHNICIEN") throw redirect({ to: "/app" });
  },
  validateSearch: (search: Record<string, unknown>): PlanningSearch => {
    const debut = estDateLocale(search["debut"]) ? search["debut"] : undefined;
    const fin = estDateLocale(search["fin"]) ? search["fin"] : undefined;
    return { debut, fin };
  },
  component: PlanningPage,
});

function PlanningPage() {
  const search = Route.useSearch();
  return (
    <Container className="py-6 sm:py-8">
      <PlanningTechnicien debut={search.debut} fin={search.fin} />
    </Container>
  );
}
