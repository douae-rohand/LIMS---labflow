import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMemo } from "react";
import { FileText } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { BandeauLaboratoiresIndisponibles } from "@/components/client/BandeauLaboratoiresIndisponibles";
import { PaginationLocale } from "@/components/client/PaginationLocale";
import { RapportsClientListe } from "@/components/rapports/RapportsClientListe";
import { RapportsClientFiltres } from "@/components/rapports/RapportsClientFiltres";
import { useRapportsClient } from "@/hooks/useRapportsClient";
import { useLaboratoiresClient } from "@/hooks/useLaboratoiresClient";

/** Pagination locale. */
const PAGE_SIZE = 10;
const MAX_PAGE = 10_000;

export interface MesRapportsSearch {
  type?: string | undefined;
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

export const Route = createFileRoute("/app/rapports/")({
  head: () => ({
    meta: [{ title: "Mes rapports — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): MesRapportsSearch => ({
    type: typeof search["type"] === "string" ? search["type"] : undefined,
    laboratoireId: parseId(search["laboratoireId"]),
    recherche: typeof search["recherche"] === "string" ? search["recherche"] : undefined,
    page: parsePage(search["page"]) ?? 1,
  }),
  component: MesRapportsPage,
});

function MesRapportsPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const pageDemandee = search.page ?? 1;

  const rapportsQuery = useRapportsClient();
  const rapports = rapportsQuery.data?.elements ?? [];

  const labosQuery = useLaboratoiresClient();
  const laboratoires = useMemo(() => {
    return (labosQuery.data ?? []).map((lab) => ({ id: lab.id, nom: lab.raisonSociale }));
  }, [labosQuery.data]);

  const filtrees = useMemo(() => {
    let result = rapports;

    if (search.type !== undefined) {
      result = result.filter((r) => r.type === search.type);
    }

    if (search.laboratoireId !== undefined) {
      result = result.filter((r) => r.laboratoireId === search.laboratoireId);
    }

    if (search.recherche) {
      const query = search.recherche.toLowerCase();
      result = result.filter(
        (r) =>
          (r.demandeReference?.toLowerCase().includes(query) ?? false) ||
          (r.demandeTitre?.toLowerCase().includes(query) ?? false)
      );
    }

    return result;
  }, [rapports, search.type, search.laboratoireId, search.recherche]);

  const totalPages = Math.max(1, Math.ceil(filtrees.length / PAGE_SIZE));
  const page = Math.min(pageDemandee, totalPages);
  const visibles = filtrees.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  function aller(partiel: MesRapportsSearch) {
    void navigate({
      to: "/app/rapports",
      search: (prev: MesRapportsSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  const indisponibles = rapportsQuery.data?.laboratoiresIndisponibles ?? 0;

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <FileText className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mes rapports</h1>
          <p aria-live="polite" className="text-sm text-muted-foreground">
            {rapportsQuery.data !== undefined
              ? `${filtrees.length} rapport${filtrees.length !== 1 ? "s" : ""}`
              : "\u00A0"}
          </p>
        </div>
      </div>

      <div className="mb-4">
        <BandeauLaboratoiresIndisponibles
          nombre={indisponibles}
          onReessayer={() => void rapportsQuery.refetch()}
        />
      </div>

      <div className="mb-4">
        <RapportsClientFiltres
          type={search.type}
          laboratoireId={search.laboratoireId}
          recherche={search.recherche}
          laboratoires={laboratoires}
          onTypeChange={(type) => aller({ type, page: 1 })}
          onLaboratoireChange={(laboratoireId) => aller({ laboratoireId, page: 1 })}
          onRechercheChange={(recherche) => aller({ recherche, page: 1 })}
        />
      </div>

      <RapportsClientListe
        rapports={rapportsQuery.data !== undefined ? visibles : undefined}
        isLoading={rapportsQuery.isLoading}
        isError={rapportsQuery.isError}
        error={rapportsQuery.error}
        onReessayer={() => void rapportsQuery.refetch()}
      />

      <PaginationLocale
        page={page}
        totalPages={totalPages}
        total={filtrees.length}
        size={PAGE_SIZE}
        nom="rapport"
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
