import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import type { FiltreAnalyses } from "@/api/responsable";
import { exigerPermission } from "@/features/shell/garde";
import { PagePlanification } from "@/features/responsable/PagePlanification";

const FILTRES: FiltreAnalyses[] = ["A_AFFECTER", "AFFECTEES", "EN_RETARD"];

export const Route = createFileRoute("/app/planification")({
  validateSearch: (search: Record<string, unknown>): { filtre?: FiltreAnalyses } =>
    FILTRES.includes(search["filtre"] as FiltreAnalyses) ? { filtre: search["filtre"] as FiltreAnalyses } : {},
  head: () => ({
    meta: [{ title: "Planification — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("PLANIFICATION_GERER");
  },
  component: Planification,
});

function Planification() {
  const { filtre } = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <PagePlanification
      filtre={filtre ?? "A_AFFECTER"}
      onFiltre={(suivant) => void navigate({ search: { filtre: suivant }, replace: true })}
    />
  );
}
