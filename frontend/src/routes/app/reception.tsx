import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { exigerPermission } from "@/features/shell/garde";
import { PageReception } from "@/features/accueil/PageReception";

type RechercheReception = { demande?: number };

export const Route = createFileRoute("/app/reception")({
  head: () => ({
    meta: [{ title: "Réception des échantillons — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): RechercheReception => {
    const demande = Number(search["demande"]);
    return Number.isInteger(demande) && demande > 0 ? { demande } : {};
  },
  beforeLoad: () => {
    if (getSession()?.user.role !== "ACCUEIL") {
      throw redirect({ to: "/app" });
    }
    exigerPermission("ECHANTILLONS_GERER");
  },
  component: RouteReception,
});

function RouteReception() {
  const { demande } = Route.useSearch();
  const navigate = useNavigate({ from: "/app/reception" });
  return (
    <PageReception
      demandeInitiale={demande}
      onDemande={(id) => void navigate({ search: id ? { demande: id } : {}, replace: true })}
    />
  );
}
