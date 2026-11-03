import { createFileRoute, Outlet } from "@tanstack/react-router";
import { exigerPermission } from "@/features/shell/garde";

export const Route = createFileRoute("/app/catalogue")({
  beforeLoad: () => {
    exigerPermission("CATALOGUE_CONSULTER");
  },
  component: CatalogueLayout,
});

function CatalogueLayout() {
  return <Outlet />;
}
