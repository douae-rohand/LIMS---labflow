import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageCaisse } from "@/features/accueil/PageCaisse";

export const Route = createFileRoute("/app/caisse")({
  head: () => ({
    meta: [{ title: "Caisse — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "ACCUEIL") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("FACTURATION_PAYER");
  },
  component: PageCaisse,
});
