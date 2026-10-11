import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/employes")({
  beforeLoad: () => {
    const session = getSession();
    if (session?.user.role !== "ADMINISTRATEUR") {
      throw redirect({ to: "/app" });
    }
  },
  component: EmployesLayout,
});

function EmployesLayout() {
  return <Outlet />;
}
