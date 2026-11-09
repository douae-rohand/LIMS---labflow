import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { Container } from "@/components/layout/Container";
import { DetailEssaiTechnicien } from "@/components/technicien/detail/DetailEssaiTechnicien";

export const Route = createFileRoute("/app/essais/$ligneEssaiId")({
  head: () => ({ meta: [{ title: "Détail de l'essai — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "TECHNICIEN") throw redirect({ to: "/app" });
  },
  component: DetailEssaiPage,
});

function DetailEssaiPage() {
  const { ligneEssaiId } = Route.useParams();
  const id = Number(ligneEssaiId);
  return (
    <Container className="py-6 sm:py-8">
      <DetailEssaiTechnicien id={id} />
    </Container>
  );
}
