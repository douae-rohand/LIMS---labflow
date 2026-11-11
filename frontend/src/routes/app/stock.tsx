import { createFileRoute, redirect, Outlet } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/stock")({
  beforeLoad: () => {
    const session = getSession();
    if (!session) {
      throw redirect({ to: "/login" });
    }
    // L'Administrateur gère le stock ; le Responsable le consulte (permission STOCK_CONSULTER).
    if (session.user.role !== "ADMINISTRATEUR"
        && !(session.user.role === "RESPONSABLE" && session.user.permissions.includes("STOCK_CONSULTER"))) {
      throw redirect({ to: "/app" });
    }
  },
  head: () => ({ meta: [{ title: "Stock — LabFlow LIMS" }] }),
  component: () => <Outlet />,
});
