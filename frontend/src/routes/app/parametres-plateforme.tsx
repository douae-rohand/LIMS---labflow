import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { EnTetePage } from "@/features/super-admin/EnTetePage";
import { PageParametres, type OngletParametres } from "@/features/super-admin/parametres/PageParametres";

const ONGLETS: OngletParametres[] = ["securite", "documents", "types", "domaines"];

export const Route = createFileRoute("/app/parametres-plateforme")({
  validateSearch: (search: Record<string, unknown>): { onglet?: OngletParametres } =>
    ONGLETS.includes(search["onglet"] as OngletParametres)
      ? { onglet: search["onglet"] as OngletParametres }
      : {},
  beforeLoad: () => {
    if (getSession()?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  head: () => ({
    meta: [{ title: "Paramètres de la plateforme — LabFlow" }],
  }),
  component: PageParametresPlateforme,
});

function PageParametresPlateforme() {
  const { onglet } = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <EnTetePage
        titre="Paramètres de la plateforme"
        description="Règles de sécurité, documents exigés pour l'intégration et référentiels communs à tous les laboratoires. Chaque modification est tracée dans le journal d'audit."
      />
      <PageParametres onglet={onglet ?? "securite"} onChangerOnglet={(suivant) => void navigate({ search: { onglet: suivant } })} />
    </div>
  );
}
