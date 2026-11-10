import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageSuivi } from "@/features/responsable/PageSuivi";

export const Route = createFileRoute("/app/suivi")({
  head: () => ({
    meta: [{ title: "Tableau de suivi — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("PLANIFICATION_GERER");
  },
  component: PageSuivi,
});
