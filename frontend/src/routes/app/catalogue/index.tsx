import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { FlaskConical, Plus } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { getSession } from "@/api/session";
import { listerCatalogue, listerDomainesCatalogue, type AnalyseCatalogue, type StatutCatalogue } from "@/api/catalogue";
import { CatalogueFiltres } from "@/components/catalogue/CatalogueFiltres";
import { CatalogueTable } from "@/components/catalogue/CatalogueTable";
import { CataloguePagination } from "@/components/catalogue/CataloguePagination";
import { CatalogueFormulaire } from "@/components/catalogue/CatalogueFormulaire";

export interface CatalogueSearch {
  page: number;
  q?: string | undefined;
  statut: StatutCatalogue;
}

const STATUTS: StatutCatalogue[] = ["TOUS", "ACTIF", "INACTIF"];

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

export const Route = createFileRoute("/app/catalogue/")({
  head: () => ({
    meta: [{ title: "Catalogue des analyses — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): CatalogueSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q =
      typeof search["q"] === "string" && search["q"].length <= 100 ? search["q"] : undefined;
    const statut = STATUTS.includes(search["statut"] as StatutCatalogue)
      ? (search["statut"] as StatutCatalogue)
      : "TOUS";
    return { page, q, statut };
  },
  component: CataloguePage,
});

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;
const Q_MIN_LEN = 2;

function CataloguePage() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const laboratoire = getSession()?.user.tenantId;
  const [inputValue, setInputValue] = useState(search.q ?? "");
  const [edition, setEdition] = useState<AnalyseCatalogue | null | undefined>(undefined);
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
          to: "/app/catalogue",
          search: (prev: CatalogueSearch) => ({ ...prev, q: qUrl, page: 1 }),
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
    queryKey: ["catalogue", { q: search.q, statut: search.statut, page: page0 }],
    queryFn: () =>
      listerCatalogue({
        q: search.q,
        statut: search.statut,
        page: page0,
        size: PAGE_SIZE,
      }),
    placeholderData: (prev) => prev,
    staleTime: 15_000,
    refetchOnWindowFocus: false,
  });

  const domaines = useQuery({
    queryKey: ["catalogue-domaines"],
    queryFn: listerDomainesCatalogue,
    staleTime: 60_000,
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
        to: "/app/catalogue",
        search: (prev: CatalogueSearch) => ({ ...prev, page: liste.data!.totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [liste.data, liste.isFetching, search.page]);

  const filtresActifs = (search.q !== undefined && search.q.length > 0) || search.statut !== "TOUS";

  function aller(partiel: Partial<CatalogueSearch>) {
    void navigate({
      to: "/app/catalogue",
      search: (prev: CatalogueSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  function reinitialiser() {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    aller({ q: undefined, statut: "TOUS", page: 1 });
  }

  return (
    <Container className="py-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
            <FlaskConical className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Catalogue des analyses</h1>
            <p className="text-sm text-muted-foreground">
              {laboratoire
                ? `Analyses de votre laboratoire (${laboratoire}) uniquement.`
                : "Analyses de votre laboratoire uniquement."}
              {liste.data !== undefined
                ? ` ${liste.data.totalElements} analyse${liste.data.totalElements !== 1 ? "s" : ""}.`
                : ""}
            </p>
          </div>
        </div>
        <Button
          type="button"
          icon={<Plus className="size-4" aria-hidden="true" />}
          iconPosition="left"
          onClick={() => setEdition(null)}
        >
          Nouvelle analyse
        </Button>
      </div>

      <div className="mb-4">
        <CatalogueFiltres
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

      <CatalogueTable
        analyses={liste.data?.content}
        isLoading={liste.isLoading}
        isError={liste.isError}
        error={liste.error}
        filtresActifs={filtresActifs}
        onModifier={(analyse) => setEdition(analyse)}
        onReinitialiser={reinitialiser}
        onReessayer={() => void liste.refetch()}
      />

      {liste.data !== undefined && liste.data.totalElements > 0 && (
        <CataloguePagination
          data={liste.data}
          onPrecedent={() => {
            if (search.page > 1) aller({ page: search.page - 1 });
          }}
          onSuivant={() => {
            if (liste.data && search.page < liste.data.totalPages) aller({ page: search.page + 1 });
          }}
        />
      )}

      <CatalogueFormulaire
        open={edition !== undefined}
        analyse={edition ?? null}
        domaines={domaines.data ?? []}
        onOpenChange={(open) => {
          if (!open) setEdition(undefined);
        }}
      />
    </Container>
  );
}
