import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageRapports } from "@/features/responsable/PageRapports";

export const Route = createFileRoute("/app/rapports-laboratoire")({
  head: () => ({
    meta: [{ title: "Rapports — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("RAPPORTS_GERER");
  },
  component: PageRapports,
});
