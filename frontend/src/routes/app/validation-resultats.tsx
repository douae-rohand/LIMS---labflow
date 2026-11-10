import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageValidation } from "@/features/responsable/PageValidation";

export const Route = createFileRoute("/app/validation-resultats")({
  head: () => ({
    meta: [{ title: "Validation des résultats — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("VALIDATION_GERER");
  },
  component: PageValidation,
});
