import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession, getStatus } from "@/api/session";
import { PageCarte } from "@/components/laboratoires/PageCarte";

/**
 * Garde de la carte client :
 *  - visiteur anonyme → /login avec le chemin demandé en paramètre `redirect` ;
 *  - rôle différent de CLIENT → /app.
 */
export const Route = createFileRoute("/app/carte")({
  head: () => ({
    meta: [{ title: "Carte des laboratoires — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    if (getStatus() === "anonyme") {
      throw redirect({ to: "/login", search: { redirect: location.href } });
    }
    if (getSession()?.user.role !== "CLIENT") {
      throw redirect({ to: "/app" });
    }
  },
  component: CartePage,
});

function CartePage() {
  return <PageCarte base="/app/laboratoires" />;
}
