import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

/**
 * Layout parent /app/essais — garde TECHNICIEN.
 * Anonyme → /login ; autre rôle → /app.
 */
export const Route = createFileRoute("/app/essais")({
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "TECHNICIEN") throw redirect({ to: "/app" });
  },
  component: EssaisLayout,
});

function EssaisLayout() {
  return <Outlet />;
}
