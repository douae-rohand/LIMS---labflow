import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Users, UserPlus } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import type { StatutEmploye, RoleEmploye, TriEmploye, DirectionTri } from "@/api/employes";
import { EmployesPageContent } from "@/components/admin/employes/EmployesPageContent";
import { EmployeCreationModal } from "@/components/employes/EmployeCreationModal";
import { ExportButton } from "@/components/admin/exports/ExportButton";
import { getSession } from "@/api/session";

export interface EmployesSearch {
  page: number;
  q?: string | undefined;
  statut?: StatutEmploye | undefined;
  role?: RoleEmploye | undefined;
  tri: TriEmploye;
  direction: DirectionTri;
  employe?: number | undefined;
}

const STATUTS_VALIDES = ["ACTIF", "EN_ATTENTE", "DESACTIVE"] as const;
const ROLES_VALIDES = ["RESPONSABLE", "TECHNICIEN", "ACCUEIL"] as const;
const TRI_VALIDES = ["nom", "prenom", "matricule", "dateCreation", "derniereConnexion"] as const;

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

export const Route = createFileRoute("/app/employes/")({
  head: () => ({ meta: [{ title: "Employés — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (!session.user.permissions.includes("EMPLOYES_GERER")) throw redirect({ to: "/app" });
  },
  validateSearch: (search: Record<string, unknown>): EmployesSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q = typeof search["q"] === "string" && search["q"].length <= 100 ? search["q"] : undefined;
    const statut = (STATUTS_VALIDES as ReadonlyArray<string>).includes(search["statut"] as string) ? (search["statut"] as StatutEmploye) : undefined;
    const role = (ROLES_VALIDES as ReadonlyArray<string>).includes(search["role"] as string) ? (search["role"] as RoleEmploye) : undefined;
    const tri = (TRI_VALIDES as ReadonlyArray<string>).includes(search["tri"] as string) ? (search["tri"] as TriEmploye) : "nom";
    const direction = search["direction"] === "desc" ? ("desc" as const) : ("asc" as const);
    const employe = parsePositiveInt(search["employe"], 1, Number.MAX_SAFE_INTEGER);
    return { page, q, statut, role, tri, direction, employe };
  },
  component: EmployesPage,
});

function EmployesPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const [modalCreationOuverte, setModalCreationOuverte] = useState(false);

  const handleOuvrirCreation = () => setModalCreationOuverte(true);

  const handleSuccessCreated = (nouvelId: number | undefined) => {
    if (nouvelId) {
      void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, page: 1, employe: nouvelId }), replace: true });
    }
  };

  return (
    <Container className="py-6 sm:py-8">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
            <Users className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Gestion des employés</h1>
            <p className="text-sm text-muted-foreground">Liste du personnel</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <ExportButton jeu="employes" />
          <Button variant="primary" size="sm" icon={<UserPlus className="size-4" aria-hidden="true" />} iconPosition="left" onClick={handleOuvrirCreation}>
            Ajouter un employé
          </Button>
        </div>
      </div>

      <EmployesPageContent search={search} />

      <EmployeCreationModal open={modalCreationOuverte} onOpenChange={setModalCreationOuverte} onSuccessCreated={handleSuccessCreated} />
    </Container>
  );
}
