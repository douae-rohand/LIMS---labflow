import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { Users, UserPlus } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { listerEmployes } from "@/api/employes";
import type { StatutEmploye, RoleEmploye, TriEmploye, DirectionTri } from "@/api/employes";
import { EmployesFiltres } from "@/components/employes/EmployesFiltres";
import { EmployesTable } from "@/components/employes/EmployesTable";
import { EmployesPagination } from "@/components/employes/EmployesPagination";
import { EmployePanneau } from "@/components/employes/EmployePanneau";
import { EmployeCreationModal } from "@/components/employes/EmployeCreationModal";
import type { ColTri } from "@/components/employes/EmployesTable";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface EmployesSearch {
  page: number;
  q?: string | undefined;
  statut?: StatutEmploye | undefined;
  role?: RoleEmploye | undefined;
  tri: TriEmploye;
  direction: DirectionTri;
  employe?: number | undefined;
}

// ---------------------------------------------------------------------------
// Helpers de validation défensive
// ---------------------------------------------------------------------------

const STATUTS_VALIDES = ["ACTIF", "EN_ATTENTE", "DESACTIVE"] as const;
const ROLES_VALIDES = ["RESPONSABLE", "TECHNICIEN", "ACCUEIL"] as const;
const TRI_VALIDES = ["nom", "prenom", "matricule", "dateCreation", "derniereConnexion"] as const;

/**
 * Tente de convertir un search param en entier valide.
 * Accepte chaîne numérique ou nombre; rejette NaN, non fini, hors bornes.
 */
function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

// ---------------------------------------------------------------------------
// Route
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/app/employes/")({
  head: () => ({
    meta: [{ title: "Employés — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): EmployesSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;

    const q =
      typeof search["q"] === "string" && search["q"].length <= 100
        ? search["q"]
        : undefined;

    const statut = (STATUTS_VALIDES as ReadonlyArray<string>).includes(
      search["statut"] as string,
    )
      ? (search["statut"] as StatutEmploye)
      : undefined;

    const role = (ROLES_VALIDES as ReadonlyArray<string>).includes(
      search["role"] as string,
    )
      ? (search["role"] as RoleEmploye)
      : undefined;

    const tri = (TRI_VALIDES as ReadonlyArray<string>).includes(
      search["tri"] as string,
    )
      ? (search["tri"] as TriEmploye)
      : "nom";

    const direction =
      search["direction"] === "desc" ? ("desc" as const) : ("asc" as const);

    const employe = parsePositiveInt(search["employe"], 1, Number.MAX_SAFE_INTEGER);

    return { page, q, statut, role, tri, direction, employe };
  },
  component: EmployesPage,
});

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;
const Q_MIN_LEN = 2;

function EmployesPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();

  // ---------- Modale d'ajout d'employé (centralisée avec backdrop blur) ----
  const [modalCreationOuverte, setModalCreationOuverte] = useState(false);

  const handleOuvrirCreation = () => {
    setModalCreationOuverte(true);
  };

  const handleSuccessCreated = (nouvelId: number | undefined) => {
    if (nouvelId) {
      void navigate({
        to: "/app/employes",
        search: (prev: EmployesSearch) => ({
          ...prev,
          page: 1,
          employe: nouvelId,
        }),
        replace: true,
      });
    }
  };

  // ---------- État local du champ de recherche ----------------------------
  // Initialisé depuis l'URL au montage. Séparé de search.q pour le debounce.
  const [inputValue, setInputValue] = useState(search.q ?? "");

  // Référence vers le timer debounce pour pouvoir l'annuler.
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Flag pour distinguer un changement venant du debounce (interne)
  // d'un changement venant de la navigation (retour arrière, externe).
  // Anti-boucle : le second useEffect observe search.q mais ne navigue jamais ;
  // isDebouncing.current empêche la re-sync du champ quand c'est le debounce
  // lui-même qui vient de mettre à jour l'URL.
  const isDebouncing = useRef(false);

  // ---------- Debounce : champ → URL ------------------------------------
  // Déclenché chaque fois que inputValue change (frappe utilisateur).
  // Ne navigue que si la valeur normalisée diffère de search.q.
  useEffect(() => {
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
    }

    debounceTimer.current = setTimeout(() => {
      debounceTimer.current = null;
      const trimmed = inputValue.trim();
      // q < 2 caractères → traité comme vide → retiré de l'URL
      const qUrl = trimmed.length >= Q_MIN_LEN ? trimmed : undefined;

      // Navigation seulement si la valeur change réellement
      if (qUrl !== search.q) {
        isDebouncing.current = true;
        void navigate({
          to: "/app/employes",
          search: (prev: EmployesSearch) => ({ ...prev, q: qUrl, page: 1 }),
          replace: true,
        });
      }
    }, DEBOUNCE_MS);

    return () => {
      if (debounceTimer.current !== null) {
        clearTimeout(debounceTimer.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputValue]);

  // ---------- Sync URL → champ (retour arrière navigateur) ---------------
  // Détecte un changement de search.q venant de l'extérieur (navigation).
  // Ce useEffect ne déclenche JAMAIS de navigate() — il ne fait que setInputValue.
  // isDebouncing.current à true = c'est notre propre debounce qui a mis à jour l'URL :
  // on ne re-sync pas le champ (il est déjà à jour) pour éviter la boucle.
  useEffect(() => {
    if (isDebouncing.current) {
      isDebouncing.current = false;
      return;
    }
    setInputValue(search.q ?? "");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search.q]);

  // ---------- Query -------------------------------------------------------
  const page0 = search.page - 1; // 0-indexed pour l'API

  const query = useQuery({
    queryKey: [
      "employes",
      {
        q: search.q,
        statut: search.statut,
        role: search.role,
        page: page0,
        tri: search.tri,
        direction: search.direction,
      },
    ],
    queryFn: () =>
      listerEmployes({
        q: search.q,
        statut: search.statut,
        role: search.role,
        page: page0,
        size: PAGE_SIZE,
        tri: search.tri,
        direction: search.direction,
      }),
    placeholderData: (prev) => prev,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  // ---------- Correction de page hors limites ----------------------------
  // Uniquement quand les données réelles arrivent (isFetching = false, pas le placeholder).
  // La garde "query.data !== undefined && !query.isFetching" empêche la boucle :
  // tant qu'on affiche des données précédentes (placeholder), on ne redirige pas.
  useEffect(() => {
    if (
      query.data !== undefined &&
      !query.isFetching &&
      query.data.totalPages > 0 &&
      search.page > query.data.totalPages
    ) {
      void navigate({
        to: "/app/employes",
        search: (prev: EmployesSearch) => ({ ...prev, page: query.data!.totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.data, query.isFetching, search.page]);

  // ---------- Handlers de navigation ------------------------------------

  const filtresActifs =
    (search.q !== undefined && search.q.length > 0) ||
    search.statut !== undefined ||
    search.role !== undefined;

  function handleStatut(value: StatutEmploye | undefined) {
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({ ...prev, statut: value, page: 1 }),
      replace: true,
    });
  }

  function handleRole(value: RoleEmploye | undefined) {
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({ ...prev, role: value, page: 1 }),
      replace: true,
    });
  }

  function handleReinitialiser() {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({
        ...prev,
        q: undefined,
        statut: undefined,
        role: undefined,
        page: 1,
      }),
      replace: true,
    });
  }

  function handleInputClear() {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    if (search.q !== undefined) {
      void navigate({
        to: "/app/employes",
        search: (prev: EmployesSearch) => ({ ...prev, q: undefined, page: 1 }),
        replace: true,
      });
    }
  }

  function handleTri(col: ColTri) {
    const nouvelleDirection: DirectionTri =
      search.tri === col && search.direction === "asc" ? "desc" : "asc";
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({
        ...prev,
        tri: col,
        direction: nouvelleDirection,
        page: 1,
      }),
      replace: true,
    });
  }

  function handlePrecedent() {
    if (search.page > 1) {
      void navigate({
        to: "/app/employes",
        search: (prev: EmployesSearch) => ({ ...prev, page: search.page - 1 }),
        replace: false,
      });
    }
  }

  function handleSuivant() {
    if (query.data && search.page < query.data.totalPages) {
      void navigate({
        to: "/app/employes",
        search: (prev: EmployesSearch) => ({ ...prev, page: search.page + 1 }),
        replace: false,
      });
    }
  }

  function handleSelectEmploye(id: number) {
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({ ...prev, employe: id }),
      replace: false,
    });
  }

  function handleFermerPanneau() {
    void navigate({
      to: "/app/employes",
      search: (prev: EmployesSearch) => ({ ...prev, employe: undefined }),
      replace: false,
    });
  }

  // ---------- Rendu -------------------------------------------------------
  return (
    <Container className="py-8">
      {/* En-tête */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
            <Users className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Gestion des employés</h1>
            <p
              id="employes-compteur"
              aria-live="polite"
              aria-atomic="true"
              className="text-sm text-muted-foreground"
            >
              {query.data !== undefined
                ? `${query.data.totalElements} employé${query.data.totalElements !== 1 ? "s" : ""} au total`
                : "\u00A0"}
            </p>
          </div>
        </div>

        <Button
          variant="primary"
          icon={<UserPlus className="size-4" aria-hidden="true" />}
          iconPosition="left"
          onClick={handleOuvrirCreation}
        >
          Ajouter un employé
        </Button>
      </div>

      {/* Filtres */}
      <div className="mb-4">
        <EmployesFiltres
          inputValue={inputValue}
          statut={search.statut}
          role={search.role}
          filtresActifs={filtresActifs}
          onInputChange={setInputValue}
          onInputClear={handleInputClear}
          onStatutChange={handleStatut}
          onRoleChange={handleRole}
          onReinitialiser={handleReinitialiser}
        />
      </div>

      {/* Tableau */}
      <EmployesTable
        employes={query.data?.content}
        isLoading={query.isLoading}
        isError={query.isError}
        error={query.error}
        tri={search.tri}
        direction={search.direction}
        filtresActifs={filtresActifs}
        employeSelectionneId={search.employe}
        onSelectEmploye={handleSelectEmploye}
        onTriChange={handleTri}
        onReinitialiser={handleReinitialiser}
        onReessayer={() => void query.refetch()}
      />

      {/* Pagination */}
      {query.data !== undefined && query.data.totalElements > 0 && (
        <EmployesPagination
          data={query.data}
          onPrecedent={handlePrecedent}
          onSuivant={handleSuivant}
        />
      )}

      {/* Panneau latéral de détail (Lecture seule - Lot 3a) */}
      <EmployePanneau
        employeId={search.employe}
        onClose={handleFermerPanneau}
      />

      {/* Modale d'ajout d'employé centralisée avec backdrop flouté (blur) */}
      <EmployeCreationModal
        open={modalCreationOuverte}
        onOpenChange={setModalCreationOuverte}
        onSuccessCreated={handleSuccessCreated}
      />
    </Container>
  );
}
