import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";
import { getSession, getStatus } from "@/api/session";

/**
 * Garde du panier client :
 *  - visiteur anonyme → /login avec le chemin demandé en paramètre `redirect` ;
 *  - rôle différent de CLIENT → /app.
 */
export const Route = createFileRoute("/app/panier")({
  head: () => ({
    meta: [{ title: "Mon panier — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    if (getStatus() === "anonyme") {
      throw redirect({ to: "/login", search: { redirect: location.href } });
    }
    if (getSession()?.user.role !== "CLIENT") {
      throw redirect({ to: "/app" });
    }
  },
  component: PanierLayout,
});

function PanierLayout() {
  return <Outlet />;
}
