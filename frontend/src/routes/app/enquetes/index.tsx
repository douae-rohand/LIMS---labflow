import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo } from "react";
import { ClipboardCheck } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { BandeauLaboratoiresIndisponibles } from "@/components/client/BandeauLaboratoiresIndisponibles";
import { PaginationLocale } from "@/components/client/PaginationLocale";
import { EnquetesClientFiltres } from "@/components/enquetes/EnquetesClientFiltres";
import { EnquetesClientListe } from "@/components/enquetes/EnquetesClientListe";
import type { StatutEnqueteClient } from "@/components/enquetes/libelles";
import { useEnquetesClient } from "@/hooks/useEnquetesClient";
import { useLaboratoiresClient } from "@/hooks/useLaboratoiresClient";

const PAGE_SIZE = 10;
const MAX_PAGE = 10_000;

/** `page` reste optionnelle pour que les liens sans pagination restent valides. */
export interface MesEnquetesSearch {
  statut?: StatutEnqueteClient | undefined;
  laboratoireId?: number | undefined;
  recherche?: string | undefined;
  page?: number | undefined;
}

function parsePage(valeur: unknown): number | undefined {
  if (valeur === undefined || valeur === null || valeur === "") return undefined;
  const n = Number(valeur);
  if (!Number.isInteger(n) || n < 1 || n > MAX_PAGE) return undefined;
  return n;
}

function parseId(valeur: unknown): number | undefined {
  if (valeur === undefined || valeur === null || valeur === "") return undefined;
  const n = Number(valeur);
  return Number.isInteger(n) && n > 0 ? n : undefined;
}

/** Valeurs invalides ignorées : `statut` inconnu → aucun filtre. */
function parseStatut(valeur: unknown): StatutEnqueteClient | undefined {
  return valeur === "A_REPONDRE" || valeur === "REPONDUE"
    ? (valeur as StatutEnqueteClient)
    : undefined;
}

export const Route = createFileRoute("/app/enquetes/")({
  head: () => ({
    meta: [{ title: "Mes enquêtes — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): MesEnquetesSearch => ({
    statut: parseStatut(search["statut"]),
    laboratoireId: parseId(search["laboratoireId"]),
    recherche: typeof search["recherche"] === "string" ? search["recherche"] : undefined,
    page: parsePage(search["page"]) ?? 1,
  }),
  component: MesEnquetesPage,
});

function MesEnquetesPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const pageDemandee = search.page ?? 1;

  const enquetesQuery = useEnquetesClient();
  const enquetes = enquetesQuery.data?.elements ?? [];

  const labosQuery = useLaboratoiresClient();
  const laboratoires = useMemo(() => {
    return (labosQuery.data ?? []).map((lab) => ({ id: lab.id, nom: lab.raisonSociale }));
  }, [labosQuery.data]);

  const filtrees = useMemo(() => {
    let result = enquetes;

    if (search.statut !== undefined) {
      const attendue = search.statut === "REPONDUE";
      result = result.filter((enquete) => enquete.repondue === attendue);
    }

    if (search.laboratoireId !== undefined) {
      result = result.filter((e) => e.laboratoireId === search.laboratoireId);
    }

    if (search.recherche) {
      const query = search.recherche.toLowerCase();
      result = result.filter(
        (e) =>
          e.demandeReference?.toLowerCase().includes(query) ?? false
      );
    }

    return result;
  }, [enquetes, search.statut, search.laboratoireId, search.recherche]);

  const totalPages = Math.max(1, Math.ceil(filtrees.length / PAGE_SIZE));
  const page = Math.min(pageDemandee, totalPages);
  const visibles = filtrees.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => {
    if (pageDemandee > totalPages) {
      void navigate({
        to: "/app/enquetes",
        search: (prev: MesEnquetesSearch) => ({ ...prev, page: totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageDemandee, totalPages]);

  function aller(partiel: MesEnquetesSearch) {
    void navigate({
      to: "/app/enquetes",
      search: (prev: MesEnquetesSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  const indisponibles = enquetesQuery.data?.laboratoiresIndisponibles ?? 0;

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <ClipboardCheck className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mes enquêtes</h1>
          <p aria-live="polite" className="text-sm text-muted-foreground">
            {enquetesQuery.data !== undefined
              ? `${filtrees.length} enquête${filtrees.length !== 1 ? "s" : ""}`
              : "\u00A0"}
          </p>
        </div>
      </div>

      <div className="mb-4">
        <BandeauLaboratoiresIndisponibles
          nombre={indisponibles}
          onReessayer={() => void enquetesQuery.refetch()}
        />
      </div>

      <div className="mb-4">
        <EnquetesClientFiltres
          statut={search.statut}
          laboratoireId={search.laboratoireId}
          recherche={search.recherche}
          laboratoires={laboratoires}
          onStatutChange={(statut) => aller({ statut, page: 1 })}
          onLaboratoireChange={(laboratoireId) => aller({ laboratoireId, page: 1 })}
          onRechercheChange={(recherche) => aller({ recherche, page: 1 })}
        />
      </div>

      <EnquetesClientListe
        enquetes={enquetesQuery.data !== undefined ? visibles : undefined}
        isLoading={enquetesQuery.isLoading}
        isError={enquetesQuery.isError}
        error={enquetesQuery.error}
        onReessayer={() => void enquetesQuery.refetch()}
      />

      <PaginationLocale
        page={page}
        totalPages={totalPages}
        total={filtrees.length}
        size={PAGE_SIZE}
        nom="enquête"
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
