import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { Inbox } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { listerDemandesLaboratoire, indicateursDemandes } from "@/api/demandes";
import type { StatutDemande } from "@/api/demandes";
import { STATUTS_DEMANDE } from "@/api/demandes";
import { DemandesFiltres } from "@/components/demandes/DemandesFiltres";
import { DemandesTable } from "@/components/demandes/DemandesTable";
import { DemandesPagination } from "@/components/demandes/DemandesPagination";
import { IndicateursDemandesCartes } from "@/components/demandes/IndicateursDemandes";
import { DemandePanneau } from "@/components/demandes/DemandePanneau";

export interface DemandesSearch {
  page: number;
  q?: string | undefined;
  statut?: StatutDemande | undefined;
  demande?: number | undefined;
}

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

export const Route = createFileRoute("/app/demandes-clients/")({
  head: () => ({
    meta: [{ title: "Demandes clients — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): DemandesSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q =
      typeof search["q"] === "string" && search["q"].length <= 100 ? search["q"] : undefined;
    const statut = (STATUTS_DEMANDE as readonly string[]).includes(search["statut"] as string)
      ? (search["statut"] as StatutDemande)
      : undefined;
    const demande = parsePositiveInt(search["demande"], 1, Number.MAX_SAFE_INTEGER);
    return { page, q, statut, demande };
  },
  component: DemandesClientsPage,
});

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;
const Q_MIN_LEN = 2;

function DemandesClientsPage() {
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
      const qUrl = trimmed.length >= Q_MIN_LEN ? trimmed : undefined;
      if (qUrl !== search.q) {
        isDebouncing.current = true;
        void navigate({
          to: "/app/demandes-clients",
          search: (prev: DemandesSearch) => ({ ...prev, q: qUrl, page: 1 }),
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

  const page0 = search.page - 1;

  const liste = useQuery({
    queryKey: ["demandes-laboratoire", { q: search.q, statut: search.statut, page: page0 }],
    queryFn: () =>
      listerDemandesLaboratoire({
        q: search.q,
        statut: search.statut,
        page: page0,
        size: PAGE_SIZE,
      }),
    placeholderData: (prev) => prev,
    staleTime: 15_000,
    refetchOnWindowFocus: false,
  });

  const indicateurs = useQuery({
    queryKey: ["demandes-indicateurs"],
    queryFn: indicateursDemandes,
    staleTime: 15_000,
    refetchOnWindowFocus: false,
  });

  useEffect(() => {
    if (
      liste.data !== undefined &&
      !liste.isFetching &&
      liste.data.totalPages > 0 &&
      search.page > liste.data.totalPages
    ) {
      void navigate({
        to: "/app/demandes-clients",
        search: (prev: DemandesSearch) => ({ ...prev, page: liste.data!.totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [liste.data, liste.isFetching, search.page]);

  const filtresActifs = (search.q !== undefined && search.q.length > 0) || search.statut !== undefined;

  function aller(partiel: Partial<DemandesSearch>) {
    void navigate({
      to: "/app/demandes-clients",
      search: (prev: DemandesSearch) => ({ ...prev, ...partiel }),
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
          <h1 className="text-2xl font-bold text-ink-900">Demandes clients</h1>
          <p aria-live="polite" className="text-sm text-muted-foreground">
            {liste.data !== undefined
              ? `${liste.data.totalElements} demande${liste.data.totalElements !== 1 ? "s" : ""}`
              : "\u00A0"}
          </p>
        </div>
      </div>

      <div className="mb-6">
        <IndicateursDemandesCartes
          indicateurs={indicateurs.data}
          statut={search.statut}
          onStatut={(statut) => aller({ statut, page: 1 })}
        />
      </div>

      <div className="mb-4">
        <DemandesFiltres
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

      <DemandesTable
        demandes={liste.data?.content}
        isLoading={liste.isLoading}
        isError={liste.isError}
        error={liste.error}
        filtresActifs={filtresActifs}
        demandeSelectionneeId={search.demande}
        onSelect={(id) =>
          void navigate({
            to: "/app/demandes-clients",
            search: (prev: DemandesSearch) => ({ ...prev, demande: id }),
          })
        }
        onReinitialiser={reinitialiser}
        onReessayer={() => void liste.refetch()}
      />

      {liste.data !== undefined && liste.data.totalElements > 0 && (
        <DemandesPagination
          data={liste.data}
          onPrecedent={() => {
            if (search.page > 1) {
              void navigate({
                to: "/app/demandes-clients",
                search: (prev: DemandesSearch) => ({ ...prev, page: search.page - 1 }),
              });
            }
          }}
          onSuivant={() => {
            if (liste.data && search.page < liste.data.totalPages) {
              void navigate({
                to: "/app/demandes-clients",
                search: (prev: DemandesSearch) => ({ ...prev, page: search.page + 1 }),
              });
            }
          }}
        />
      )}

      <DemandePanneau
        demandeId={search.demande}
        onClose={() =>
          void navigate({
            to: "/app/demandes-clients",
            search: (prev: DemandesSearch) => ({ ...prev, demande: undefined }),
          })
        }
      />
    </Container>
  );
}
