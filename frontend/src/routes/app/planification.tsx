import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PagePlanification } from "@/features/responsable/PagePlanification";

export const Route = createFileRoute("/app/planification")({
  head: () => ({
    meta: [{ title: "Planification — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("PLANIFICATION_GERER");
  },
  component: PagePlanification,
});
