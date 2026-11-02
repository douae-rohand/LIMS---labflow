import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo } from "react";
import { Receipt } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { BandeauLaboratoiresIndisponibles } from "@/components/client/BandeauLaboratoiresIndisponibles";
import { PaginationLocale } from "@/components/client/PaginationLocale";
import { FacturesClientFiltres } from "@/components/factures/FacturesClientFiltres";
import { FacturesClientListe } from "@/components/factures/FacturesClientListe";
import type { StatutFactureClient } from "@/api/facturesClient";
import { useFacturesClient } from "@/hooks/useFacturesClient";
import { useLaboratoiresClient } from "@/hooks/useLaboratoiresClient";
import { echeanceDepassee } from "@/lib/dates";

const PAGE_SIZE = 10;
const MAX_PAGE = 10_000;

/** `page` reste optionnelle pour que les liens sans pagination restent valides. */
export interface MesFacturesSearch {
  statut?: StatutFactureClient | "RETARD" | undefined;
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
function parseStatut(valeur: unknown): StatutFactureClient | "RETARD" | undefined {
  return valeur === "IMPAYEE" || valeur === "PAYEE" || valeur === "RETARD"
    ? (valeur as StatutFactureClient | "RETARD")
    : undefined;
}

export const Route = createFileRoute("/app/factures/")({
  head: () => ({
    meta: [{ title: "Mes factures — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): MesFacturesSearch => ({
    statut: parseStatut(search["statut"]),
    laboratoireId: parseId(search["laboratoireId"]),
    recherche: typeof search["recherche"] === "string" ? search["recherche"] : undefined,
    page: parsePage(search["page"]) ?? 1,
  }),
  component: MesFacturesPage,
});

function MesFacturesPage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const pageDemandee = search.page ?? 1;

  const facturesQuery = useFacturesClient();
  const factures = facturesQuery.data?.elements ?? [];

  const labosQuery = useLaboratoiresClient();
  const laboratoires = useMemo(() => {
    return (labosQuery.data ?? []).map((lab) => ({ id: lab.id, nom: lab.raisonSociale }));
  }, [labosQuery.data]);

  const filtrees = useMemo(() => {
    let result = factures;

    if (search.statut === "RETARD") {
      result = result.filter(
        (f) => f.statut === "IMPAYEE" && echeanceDepassee(f.dateEcheance)
      );
    } else if (search.statut !== undefined) {
      result = result.filter((f) => f.statut === search.statut);
    }

    if (search.laboratoireId !== undefined) {
      result = result.filter((f) => f.laboratoireId === search.laboratoireId);
    }

    if (search.recherche) {
      const query = search.recherche.toLowerCase();
      result = result.filter(
        (f) =>
          f.numero.toLowerCase().includes(query) ||
          (f.demandeReference?.toLowerCase().includes(query) ?? false)
      );
    }

    return result;
  }, [factures, search.statut, search.laboratoireId, search.recherche]);

  const totalPages = Math.max(1, Math.ceil(filtrees.length / PAGE_SIZE));
  const page = Math.min(pageDemandee, totalPages);
  const visibles = filtrees.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => {
    if (pageDemandee > totalPages) {
      void navigate({
        to: "/app/factures",
        search: (prev: MesFacturesSearch) => ({ ...prev, page: totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageDemandee, totalPages]);

  function aller(partiel: MesFacturesSearch) {
    void navigate({
      to: "/app/factures",
      search: (prev: MesFacturesSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  const indisponibles = facturesQuery.data?.laboratoiresIndisponibles ?? 0;

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <Receipt className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mes factures</h1>
          <p aria-live="polite" className="text-sm text-muted-foreground">
            {facturesQuery.data !== undefined
              ? `${filtrees.length} facture${filtrees.length !== 1 ? "s" : ""}`
              : "\u00A0"}
          </p>
        </div>
      </div>

      <div className="mb-4">
        <BandeauLaboratoiresIndisponibles
          nombre={indisponibles}
          onReessayer={() => void facturesQuery.refetch()}
        />
      </div>

      <div className="mb-4">
        <FacturesClientFiltres
          statut={search.statut}
          laboratoireId={search.laboratoireId}
          recherche={search.recherche}
          laboratoires={laboratoires}
          onStatutChange={(statut) => aller({ statut, page: 1 })}
          onLaboratoireChange={(laboratoireId) => aller({ laboratoireId, page: 1 })}
          onRechercheChange={(recherche) => aller({ recherche, page: 1 })}
        />
      </div>

      <FacturesClientListe
        factures={facturesQuery.data !== undefined ? visibles : undefined}
        isLoading={facturesQuery.isLoading}
        isError={facturesQuery.isError}
        error={facturesQuery.error}
        onReessayer={() => void facturesQuery.refetch()}
      />

      <PaginationLocale
        page={page}
        totalPages={totalPages}
        total={filtrees.length}
        size={PAGE_SIZE}
        nom="facture"
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
