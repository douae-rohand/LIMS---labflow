import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/catalogue")({
  beforeLoad: () => {
    const role = getSession()?.user.role;
    if (role !== "ADMINISTRATEUR" && role !== "RESPONSABLE") {
      throw redirect({ to: "/app" });
    }
  },
  component: CatalogueLayout,
});

function CatalogueLayout() {
  return <Outlet />;
}
