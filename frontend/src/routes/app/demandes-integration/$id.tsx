import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/demandes-integration/$id")({
  beforeLoad: ({ params }) => {
    const session = getSession();
    if (session?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw redirect({ to: "/app" });
    }
    const demande = Number(params.id);
    throw redirect({
      to: "/app/gestion-laboratoires",
      search: {
        onglet: "demandes",
        ...(Number.isInteger(demande) && demande > 0 ? { demande } : {}),
      },
    });
  },
});
