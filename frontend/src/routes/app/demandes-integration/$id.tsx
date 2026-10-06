import { createFileRoute, Link, useRouter } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Badge, Button, Card, Label, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { LocationPicker } from "@/components/integration/LocationPicker";
import { LIBELLES_DOCUMENTS } from "@/components/integration/constants";
import { getSession } from "@/api/session";
import { ApiError } from "@/api/axios";
import {
  ouvrirDocumentIntegration,
  renvoyerInvitation,
  trouverDemandeIntegration,
  traiterDemandeIntegration,
} from "@/api/plateforme";

export const Route = createFileRoute("/app/demandes-integration/$id")({
  beforeLoad: () => {
    const session = getSession();
    if (session?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  component: DemandeIntegrationDetailPage,
});

function DemandeIntegrationDetailPage() {
  const { id } = Route.useParams();
  const demandeId = Number(id);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [motif, setMotif] = useState("");
  const [error, setError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["demande-integration", demandeId],
    queryFn: () => trouverDemandeIntegration(demandeId),
    enabled: Number.isFinite(demandeId),
  });

  const mutation = useMutation({
    mutationFn: (decision: "APPROUVEE" | "REJETEE") =>
      traiterDemandeIntegration(demandeId, decision, decision === "REJETEE" ? motif : undefined),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["demande-integration", demandeId] });
      await queryClient.invalidateQueries({ queryKey: ["demandes-integration"] });
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.message : "Le traitement a échoué.");
    },
  });

  const inviteMutation = useMutation({
    mutationFn: () => renvoyerInvitation(demandeId),
  });

  const demande = query.data;
  const enAttente = demande?.statut === "EN_ATTENTE" || demande?.statut === "EN_COURS_VALIDATION";

  return (
    <Container className="py-10">
      <Link to="/app/demandes-integration" className="text-sm font-semibold text-brand-900">
        ← Retour aux demandes
      </Link>
      {query.isLoading && <p className="mt-6 text-sm text-muted-foreground">Chargement…</p>}
      {query.isError && (
        <p className="mt-6 text-sm text-red-600">Demande introuvable.</p>
      )}
      {demande && (
        <div className="mt-6 flex flex-col gap-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-3xl font-extrabold text-ink-900">{demande.nomLaboratoire}</h1>
              <p className="text-sm text-muted-foreground">
                {demande.numero} · {demande.dateSoumission?.slice(0, 10)}
              </p>
            </div>
            <Badge>{demande.statut.replaceAll("_", " ")}</Badge>
          </div>

          <Card padding="lg">
            <h2 className="text-lg font-bold text-ink-900">Laboratoire</h2>
            <dl className="mt-3 grid gap-2 text-sm sm:grid-cols-2">
              <div>Raison sociale : {demande.raisonSociale}</div>
              <div>
                Type :{" "}
                {(demande.typesLaboratoire && demande.typesLaboratoire.length > 0
                  ? demande.typesLaboratoire
                  : demande.typeLaboratoire
                    ? [demande.typeLaboratoire]
                    : []
                ).join(", ") || "—"}
              </div>
              <div>ICE : {demande.ice || "—"}</div>
              <div>Téléphone : {demande.telephoneLaboratoire || "—"}</div>
              <div>E-mail : {demande.emailLaboratoire || "—"}</div>
              <div>Site web : {demande.siteWeb || "—"}</div>
              <div className="sm:col-span-2">
                Adresse : {demande.adresse}, {demande.ville} {demande.codePostal} — {demande.region}{" "}
                {demande.pays}
              </div>
              {demande.informationsComplementaires && (
                <div className="sm:col-span-2">Infos : {demande.informationsComplementaires}</div>
              )}
              {demande.nomSchema && (
                <div className="sm:col-span-2">Schéma tenant : {demande.nomSchema}</div>
              )}
            </dl>
            {demande.latitude != null && demande.longitude != null && (
              <div className="mt-4">
                <LocationPicker
                  readOnly
                  label={demande.nomLaboratoire}
                  value={{ latitude: demande.latitude, longitude: demande.longitude }}
                  onChange={() => undefined}
                />
              </div>
            )}
          </Card>

          <Card padding="lg">
            <h2 className="text-lg font-bold text-ink-900">Administrateur laboratoire</h2>
            <p className="mt-3 text-sm">
              {demande.adminPrenom} {demande.adminNom} · {demande.adminFonction || "—"}
            </p>
            <p className="text-sm text-muted-foreground">
              {demande.adminEmail} · {demande.adminTelephone || "—"} · CIN {demande.adminCin || "—"}
            </p>
          </Card>

          <Card padding="lg">
            <h2 className="text-lg font-bold text-ink-900">Documents</h2>
            <div className="mt-4 flex flex-col gap-3">
              {(demande.documents ?? []).map((doc) => (
                <div
                  key={doc.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 p-3"
                >
                  <div>
                    <p className="font-semibold text-ink-900">
                      {doc.libelle || LIBELLES_DOCUMENTS[doc.typeDocument]?.libelle}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {doc.nomFichier} · {Math.round(doc.taille / 1024)} Ko
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => void ouvrirDocumentIntegration(demande.id, doc.typeDocument)}
                    >
                      Consulter
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() =>
                        void ouvrirDocumentIntegration(demande.id, doc.typeDocument, true)
                      }
                    >
                      Télécharger
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {demande.statut === "REJETEE" && demande.motifRefus && (
            <Card padding="md">
              <p className="text-sm">
                Motif du refus : <strong>{demande.motifRefus}</strong>
              </p>
            </Card>
          )}

          {enAttente && (
            <Card padding="lg" className="flex flex-col gap-4">
              <div>
                <Label htmlFor="motif">Motif du refus (obligatoire en cas de rejet)</Label>
                <Textarea
                  id="motif"
                  value={motif}
                  onChange={(event) => setMotif(event.target.value)}
                />
              </div>
              {error && <p className="text-sm text-red-600">{error}</p>}
              <div className="flex flex-wrap gap-3">
                <Button
                  loading={mutation.isPending}
                  onClick={() => {
                    setError(null);
                    mutation.mutate("APPROUVEE");
                  }}
                >
                  Accepter et créer le tenant
                </Button>
                <Button
                  variant="secondary"
                  loading={mutation.isPending}
                  onClick={() => {
                    if (!motif.trim()) {
                      setError("Le motif du refus est obligatoire.");
                      return;
                    }
                    setError(null);
                    mutation.mutate("REJETEE");
                  }}
                >
                  Refuser
                </Button>
              </div>
            </Card>
          )}

          {demande.statut === "APPROUVEE" && (
            <div className="flex flex-wrap gap-3">
              <Button
                variant="secondary"
                loading={inviteMutation.isPending}
                onClick={() => inviteMutation.mutate()}
              >
                Régénérer le lien d'activation
              </Button>
              <Button variant="ghost" onClick={() => void router.navigate({ to: "/app" })}>
                Retour au tableau de bord
              </Button>
            </div>
          )}
        </div>
      )}
    </Container>
  );
}
