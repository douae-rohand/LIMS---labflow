import { createFileRoute, Outlet } from "@tanstack/react-router";
import { exigerPermission } from "@/features/shell/garde";

export const Route = createFileRoute("/app/employes")({
  beforeLoad: () => {
    exigerPermission("EMPLOYES_CONSULTER");
  },
  component: EmployesLayout,
});

function EmployesLayout() {
  return <Outlet />;
}
