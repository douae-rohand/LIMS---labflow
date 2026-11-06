import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { FileText, Plus } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { ApiError } from "@/api/axios";
import {
  DEMANDES_FACTURABLES_QUERY_KEY,
  listerDemandesFacturables,
  listerFactures,
  messageErreurFacturesAdmin,
  type EmettreFactureRequest,
} from "@/api/facturesAdmin";
import { useFacturesAdmin } from "@/hooks/useFacturesAdmin";
import { EmettreFactureForm } from "@/components/admin/facturation/EmettreFactureForm";
import { FacturesAdminFiltres } from "@/components/admin/facturation/FacturesAdminFiltres";
import type { StatutFactureAdmin } from "@/components/admin/facturation/FacturesAdminFiltres";
import { FacturesAdminTable } from "@/components/admin/facturation/FacturesAdminTable";
import { FacturePanneau } from "@/components/admin/facturation/FacturePanneau";
import { ExportButton } from "@/components/admin/exports/ExportButton";

export interface FacturationSearch {
  page: number;
  tri: "numero" | "date";
  direction: "asc" | "desc";
  statut?: StatutFactureAdmin | undefined;
  debut?: string | undefined;
  fin?: string | undefined;
  q?: string | undefined;
}

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

export const Route = createFileRoute("/app/facturation/")({
  head: () => ({ meta: [{ title: "Facturation — LabFlow LIMS" }] }),
  validateSearch: (search: Record<string, unknown>): FacturationSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const statutsFacture: StatutFactureAdmin[] = ["EMISE", "PAYEE", "EN_RETARD", "IMPAYEE"];
    const statut = typeof search["statut"] === "string" && statutsFacture.includes(search["statut"] as StatutFactureAdmin)
      ? search["statut"] as StatutFactureAdmin
      : undefined;
    const debut = typeof search["debut"] === "string" ? search["debut"] : undefined;
    const fin = typeof search["fin"] === "string" ? search["fin"] : undefined;
    const q = typeof search["q"] === "string" && search["q"].length <= 100 ? search["q"] : undefined;
    const tris = ["numero", "date"] as const;
    const tri = tris.includes(search["tri"] as (typeof tris)[number])
      ? (search["tri"] as (typeof tris)[number])
      : "date";
    const direction = search["direction"] === "asc" ? "asc" : "desc";
    return { page, statut, debut, fin, q, tri, direction };
  },
  component: FacturationListe,
});

const PAGE_SIZE = 10;

function FacturationListe() {
  const navigate = useNavigate();
  const search = Route.useSearch();
  const { emettre } = useFacturesAdmin();
  const [emissionOpen, setEmissionOpen] = useState(false);
  const [recherche, setRecherche] = useState(search.q ?? "");
  const [erreurEmission, setErreurEmission] = useState<string>();
  const [factureSelectionneeId, setFactureSelectionneeId] = useState<number | null>(null);

  const page0 = search.page - 1;

  const data = useQuery({
    queryKey: ["factures-admin", search],
    queryFn: () => listerFactures({ ...search, page: page0, size: PAGE_SIZE }),
    placeholderData: (prev) => prev,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const demandes = useQuery({
    queryKey: DEMANDES_FACTURABLES_QUERY_KEY,
    queryFn: listerDemandesFacturables,
    enabled: emissionOpen,
    staleTime: 30_000,
  });

  useEffect(() => {
    setRecherche(search.q ?? "");
  }, [search.q]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (recherche.trim() !== (search.q ?? "")) {
        void navigate({
          to: "/app/facturation",
          search: (prev: FacturationSearch) => ({
            ...prev,
            q: recherche.trim() || undefined,
            page: 1,
          }),
          replace: true,
        });
      }
    }, 350);
    return () => window.clearTimeout(timer);
  }, [recherche, search.q, navigate]);

  const handleEmettre = (request: EmettreFactureRequest) => {
    setErreurEmission(undefined);
    emettre.mutate(request, {
      onSuccess: () => {
        setEmissionOpen(false);
      },
      onError: (error) => {
        if (error instanceof ApiError && error.code === "FACTURE_DEJA_EMISE") {
          setErreurEmission("Une facture existe déjà pour cette demande. La liste a été actualisée.");
        } else if (error instanceof ApiError && error.code === "DEMANDE_NON_FACTURABLE") {
          setErreurEmission("Cette demande n'est plus facturable. La liste a été actualisée.");
        } else if (error instanceof ApiError && error.code === "DEMANDE_SANS_MONTANT") {
          setErreurEmission("Cette demande ne contient aucun montant facturable.");
        } else {
          setErreurEmission(messageErreurFacturesAdmin(error));
        }
      },
    });
  };

  useEffect(() => {
    if (data.data && data.data.totalPages > 0 && search.page > data.data.totalPages) {
      void navigate({
        to: "/app/facturation",
        search: (prev: FacturationSearch) => ({ ...prev, page: data.data!.totalPages }),
        replace: true,
      });
    }
  }, [data.data, search.page, navigate]);

  function aller(partiel: Partial<FacturationSearch>) {
    void navigate({
      to: "/app/facturation",
      search: (prev: FacturationSearch) => ({ ...prev, ...partiel }),
      replace: true,
    });
  }

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
            <FileText className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Facturation</h1>
            <p className="text-sm text-muted-foreground">
              {data.data ? `${data.data.totalElements} facture${data.data.totalElements !== 1 ? "s" : ""}` : "Chargement..."}
            </p>
          </div>
        </div>
      </div>

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <p className="max-w-lg text-sm text-muted-foreground">
          Les factures sont créées automatiquement lorsqu&apos;une demande est acceptée. Utilisez cette option uniquement si une demande acceptée n&apos;a pas de facture.
        </p>
        <Button type="button" icon={<Plus className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => { setErreurEmission(undefined); setEmissionOpen(true); }}>
          Émettre manuellement
        </Button>
      </div>

      <FacturesAdminFiltres
        recherche={recherche}
        statut={search.statut}
        onRechercheChange={setRecherche}
        onRechercheClear={() => {
          setRecherche("");
          if (search.q !== undefined) aller({ q: undefined, page: 1 });
        }}
        onStatutChange={(statut) => aller({ statut, page: 1 })}
        onReinitialiser={() => {
          setRecherche("");
          aller({ q: undefined, statut: undefined, page: 1 });
        }}
        actions={<ExportButton jeu="factures" disabled={data.isLoading} />}
      />
      <FacturesAdminTable
        factures={data.data?.content}
        isLoading={data.isLoading}
        error={data.error}
        page={search.page}
        totalPages={data.data?.totalPages ?? 1}
        hasFilters={Boolean(search.q || search.statut !== undefined)}
        tri={search.tri}
        direction={search.direction}
        onPrevious={() => aller({ page: search.page - 1 })}
        onNext={() => aller({ page: search.page + 1 })}
        onSortChange={(tri) =>
          aller({
            tri,
            direction: search.tri === tri && search.direction === "asc" ? "desc" : "asc",
            page: 1,
          })
        }
        onDetails={setFactureSelectionneeId}
      />

      <FacturePanneau
        factureId={factureSelectionneeId}
        open={factureSelectionneeId !== null}
        onOpenChange={(open) => {
          if (!open) setFactureSelectionneeId(null);
        }}
      />

      {emissionOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="mb-4 text-lg font-semibold">Émettre manuellement</h3>
            <EmettreFactureForm
              demandes={demandes.data ?? []}
              onSubmit={handleEmettre}
              onCancel={() => setEmissionOpen(false)}
              isSubmitting={emettre.isPending}
              isLoading={demandes.isLoading}
              error={erreurEmission ?? (demandes.isError ? messageErreurFacturesAdmin(demandes.error) : undefined)}
              onRetry={demandes.isError ? () => { void demandes.refetch(); } : undefined}
            />
          </div>
        </div>
      )}
    </Container>
  );
}
