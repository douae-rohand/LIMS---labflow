import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";
import { Inbox } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { STATUTS_DEMANDE, type StatutDemande } from "@/api/demandes";
import type { DemandeResumeClient } from "@/api/demandesClient";
import { DemandesClientFiltres } from "@/components/demandes/client/DemandesClientFiltres";
import { DemandesClientListe } from "@/components/demandes/client/DemandesClientListe";
import { DemandesClientPagination } from "@/components/demandes/client/DemandesClientPagination";
import { useDemandesClient } from "@/hooks/useDemandesClient";

export interface MesDemandesSearch {
  page: number;
  q?: string | undefined;
  statut?: StatutDemande | undefined;
}

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

function normaliser(valeur: string | null): string {
  if (valeur === null) return "";
  return valeur
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function correspond(demande: DemandeResumeClient, recherche: string): boolean {
  if (recherche === "") return true;
  const cible = normaliser(
    [demande.reference, demande.objet, demande.laboratoireNom, demande.laboratoireCode]
      .filter((valeur): valeur is string => valeur !== null)
      .join(" "),
  );
  return cible.includes(recherche);
}

export const Route = createFileRoute("/app/demandes/")({
  head: () => ({
    meta: [{ title: "Mes demandes — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): MesDemandesSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q =
      typeof search["q"] === "string" && search["q"].length <= 100 ? search["q"] : undefined;
    const statut = (STATUTS_DEMANDE as readonly string[]).includes(search["statut"] as string)
      ? (search["statut"] as StatutDemande)
      : undefined;
    return { page, q, statut };
  },
  component: MesDemandesPage,
});

function MesDemandesPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const [inputValue, setInputValue] = useState(search.q ?? "");
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isDebouncing = useRef(false);

  useEffect(() => {
    if (debounceTimer.current !== null) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      debounceTimer.current = null;
      const trimmed = inputValue.trim();
      const qUrl = trimmed.length > 0 ? trimmed : undefined;
      if (qUrl !== search.q) {
        isDebouncing.current = true;
        void navigate({
          to: "/app/demandes",
          search: (prev: MesDemandesSearch) => ({ ...prev, q: qUrl, page: 1 }),
          replace: true,
        });
      }
    }, DEBOUNCE_MS);
    return () => {
      if (debounceTimer.current !== null) clearTimeout(debounceTimer.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputValue]);

  useEffect(() => {
    if (isDebouncing.current) {
      isDebouncing.current = false;
      return;
    }
    setInputValue(search.q ?? "");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search.q]);

  const demandesQuery = useDemandesClient();
  const demandes = demandesQuery.data ?? [];

  const filtrees = useMemo(() => {
    const recherche = normaliser(search.q ?? null).trim();
    return demandes.filter(
      (demande) =>
        correspond(demande, recherche) &&
        (search.statut === undefined || demande.statut === search.statut),
    );
  }, [demandes, search.q, search.statut]);

  const totalPages = Math.max(1, Math.ceil(filtrees.length / PAGE_SIZE));
  const page = Math.min(search.page, totalPages);
  const visibles = filtrees.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => {
    if (search.page > totalPages) {
      void navigate({
        to: "/app/demandes",
        search: (prev: MesDemandesSearch) => ({ ...prev, page: totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search.page, totalPages]);

  const filtresActifs =
    search.q !== undefined && search.q.length > 0 ? true : search.statut !== undefined;

  function aller(partiel: Partial<MesDemandesSearch>) {
    void navigate({
      to: "/app/demandes",
      search: (prev: MesDemandesSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  function reinitialiser() {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    aller({ q: undefined, statut: undefined, page: 1 });
  }

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <Inbox className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mes demandes</h1>
          <p aria-live="polite" className="text-sm text-muted-foreground">
            {demandesQuery.data !== undefined
              ? `${filtrees.length} demande${filtrees.length !== 1 ? "s" : ""}`
              : "\u00A0"}
          </p>
        </div>
      </div>

      <div className="mb-4">
        <DemandesClientFiltres
          inputValue={inputValue}
          statut={search.statut}
          filtresActifs={filtresActifs}
          onInputChange={setInputValue}
          onInputClear={() => {
            setInputValue("");
            if (search.q !== undefined) aller({ q: undefined, page: 1 });
          }}
          onStatutChange={(statut) => aller({ statut, page: 1 })}
          onReinitialiser={reinitialiser}
        />
      </div>

      <DemandesClientListe
        demandes={demandesQuery.data !== undefined ? visibles : undefined}
        isLoading={demandesQuery.isLoading}
        isError={demandesQuery.isError}
        error={demandesQuery.error}
        filtresActifs={filtresActifs}
        onReinitialiser={reinitialiser}
        onReessayer={() => void demandesQuery.refetch()}
      />

      <DemandesClientPagination
        page={page}
        totalPages={totalPages}
        total={filtrees.length}
        size={PAGE_SIZE}
        onPrecedent={() => {
          if (page > 1) aller({ page: page - 1 });
        }}
        onSuivant={() => {
          if (page < totalPages) aller({ page: page + 1 });
        }}
      />
    </Container>
  );
}
