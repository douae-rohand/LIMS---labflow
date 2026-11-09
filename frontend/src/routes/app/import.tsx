import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { Container } from "@/components/layout/Container";
import { ImportCsvTechnicien } from "@/components/technicien/import/ImportCsvTechnicien";

export const Route = createFileRoute("/app/import")({
  head: () => ({ meta: [{ title: "Import CSV — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "TECHNICIEN") throw redirect({ to: "/app" });
    // Permission IMPORT_CSV requise pour accéder à la page
    if (!session.user.permissions.includes("ESSAIS_IMPORT_CSV")) {
      throw redirect({ to: "/app" });
    }
  },
  component: ImportPage,
});

function ImportPage() {
  return (
    <Container className="py-6 sm:py-8">
      <ImportCsvTechnicien />
    </Container>
  );
}
