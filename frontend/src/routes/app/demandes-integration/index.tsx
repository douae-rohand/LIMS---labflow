import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Badge, Button, Card, Select } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { getSession } from "@/api/session";
import { listerDemandesIntegration, type StatutIntegration } from "@/api/plateforme";

export const Route = createFileRoute("/app/demandes-integration/")({
  beforeLoad: () => {
    const session = getSession();
    if (session?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  component: DemandesIntegrationPage,
});

const LIBELLES: Record<StatutIntegration, string> = {
  EN_ATTENTE: "En attente",
  EN_COURS_VALIDATION: "En cours",
  APPROUVEE: "Approuvée",
  REJETEE: "Refusée",
  SUSPENDUE: "Suspendue",
};

function badgeVariant(statut: StatutIntegration) {
  if (statut === "APPROUVEE") return "lime" as const;
  if (statut === "REJETEE") return "outline" as const;
  return "mint" as const;
}

function DemandesIntegrationPage() {
  const [statut, setStatut] = useState<StatutIntegration | "">("");
  const query = useQuery({
    queryKey: ["demandes-integration", statut],
    queryFn: () => listerDemandesIntegration(statut || undefined, 0, 50),
  });

  return (
    <Container className="py-10">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-ink-900">Demandes d'intégration</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Vérifiez le dossier, les documents, puis acceptez ou refusez la création du tenant.
          </p>
        </div>
        <div className="w-56">
          <Select
            value={statut}
            onChange={(event) => setStatut(event.target.value as StatutIntegration | "")}
          >
            <option value="">Tous les statuts</option>
            <option value="EN_ATTENTE">En attente</option>
            <option value="APPROUVEE">Approuvées</option>
            <option value="REJETEE">Refusées</option>
          </Select>
        </div>
      </div>

      {query.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
      {query.isError && (
        <p className="text-sm text-red-600">Impossible de charger les demandes.</p>
      )}

      <div className="flex flex-col gap-3">
        {query.data?.content.map((demande) => (
          <Card key={demande.id} variant="flat" padding="md">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-bold text-ink-900">{demande.nomLaboratoire}</p>
                <p className="text-sm text-muted-foreground">
                  {demande.numero} · {demande.adminEmail} · {demande.ville || "Ville non renseignée"}
                  {(demande.typesLaboratoire && demande.typesLaboratoire.length > 0
                    ? ` · ${demande.typesLaboratoire.join(", ")}`
                    : demande.typeLaboratoire
                      ? ` · ${demande.typeLaboratoire}`
                      : "")}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <Badge variant={badgeVariant(demande.statut)}>{LIBELLES[demande.statut]}</Badge>
                <Link to="/app/demandes-integration/$id" params={{ id: String(demande.id) }}>
                  <Button size="sm">Consulter</Button>
                </Link>
              </div>
            </div>
          </Card>
        ))}
        {query.data?.content.length === 0 && (
          <p className="text-sm text-muted-foreground">Aucune demande pour ce filtre.</p>
        )}
      </div>
    </Container>
  );
}
