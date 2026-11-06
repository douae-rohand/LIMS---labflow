import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";
import { getSession, getStatus } from "@/api/session";

/**
 * Garde du catalogue client :
 *  - visiteur anonyme → /login avec le chemin demandé en paramètre `redirect` ;
 *  - rôle différent de CLIENT → /app.
 */
export const Route = createFileRoute("/app/laboratoires")({
  head: () => ({
    meta: [{ title: "Laboratoires — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getStatus() === "anonyme") {
      throw redirect({ to: "/login" });
    }
    if (getSession()?.user.role !== "CLIENT") {
      throw redirect({ to: "/app" });
    }
  },
  component: LaboratoiresLayout,
});

function LaboratoiresLayout() {
  return <Outlet />;
}
