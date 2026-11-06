import { createFileRoute, redirect, Outlet } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/facturation")({
  beforeLoad: () => {
    const session = getSession();
    if (!session) {
      throw redirect({ to: "/login" });
    }
    if (session.user.role !== "ADMINISTRATEUR") {
      throw redirect({ to: "/app" });
    }
  },
  head: () => ({ meta: [{ title: "Facturation — LabFlow LIMS" }] }),
  component: () => <Outlet />,
});
