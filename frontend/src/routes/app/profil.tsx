import { createFileRoute } from "@tanstack/react-router";
import { redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { ProfilClient } from "@/components/profil/ProfilClient";
import { ProfilEmploye } from "@/components/profil/ProfilEmploye";

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
  if (["ADMINISTRATEUR", "RESPONSABLE", "TECHNICIEN", "ACCUEIL"].includes(session?.user.role ?? "")) {
    return <ProfilEmploye />;
  }
  return null;
}
