import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { TableauDeBord } from "@/features/super-admin/TableauDeBord";

export const Route = createFileRoute("/app/tableau-de-bord")({
  beforeLoad: () => {
    if (getSession()?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  head: () => ({ meta: [{ title: "Tableau de bord — LabFlow" }] }),
  component: TableauDeBord,
});
