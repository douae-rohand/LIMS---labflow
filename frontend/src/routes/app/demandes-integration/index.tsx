import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/demandes-integration/")({
  beforeLoad: () => {
    const session = getSession();
    if (session?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw redirect({ to: "/app" });
    }
    throw redirect({
      to: "/app/gestion-laboratoires",
      search: { onglet: "demandes" },
    });
  },
});
