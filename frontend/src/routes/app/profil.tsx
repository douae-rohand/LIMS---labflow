import { createFileRoute } from "@tanstack/react-router";
import { redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { ProfilClient } from "@/components/profil/ProfilClient";
import { ProfilEmploye } from "@/components/profil/ProfilEmploye";
import { PageMonCompte } from "@/features/super-admin/compte/PageMonCompte";
import { EnTetePage } from "@/features/super-admin/EnTetePage";

export const Route = createFileRoute("/app/profil")({
  head: () => ({
    meta: [{ title: "Mon profil — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
  },
  component: ProfilPage,
});

function ProfilPage() {
  const session = getSession();
  if (session?.user.role === "CLIENT") return <ProfilClient />;
  if (session?.user.role === "SUPER_ADMINISTRATEUR") {
    return (
      <div className="px-4 py-6 sm:px-6 lg:px-8">
        <EnTetePage
          titre="Mon compte"
          description="Vos informations personnelles, votre mot de passe et les appareils connectés à votre compte. Chaque modification est tracée dans le journal d'audit."
        />
        <PageMonCompte />
      </div>
    );
  }
  if (["ADMINISTRATEUR", "RESPONSABLE", "TECHNICIEN", "ACCUEIL"].includes(session?.user.role ?? "")) {
    return <ProfilEmploye />;
  }
  return null;
}
