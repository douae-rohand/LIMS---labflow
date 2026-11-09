import { useNavigate } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { FlaskConical } from "lucide-react";
import { Card } from "@/components/lab";
import type { EssaisSearch } from "@/routes/app/essais/index";
import type { ListerEssaisParams } from "@/api/technicien/types";
import { useTechnicienEssais, useTechnicienSynthese } from "@/hooks/useTechnicienEssais";
import { messageErreurTechnicien } from "@/api/technicien";
import { FiltresEssais } from "@/components/technicien/essais/FiltresEssais";
import { LigneEssaiCard } from "@/components/technicien/essais/LigneEssaiCard";
import { TableauEssais } from "@/components/technicien/essais/TableauEssais";
import { PaginationEssais } from "@/components/technicien/essais/PaginationEssais";
import { ApiError } from "@/api/axios";

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;
const Q_MIN_LEN = 2;

/**
 * Page « Mes essais » : liste paginée du technicien.
 * Filtres dans l'URL (statut, recherche, en retard).
 * Vue cartes sur mobile, tableau natif sur desktop.
 */
export function ListeEssaisTechnicien({ search }: { search: EssaisSearch }) {
  const navigate = useNavigate();
  const [inputValue, setInputValue] = useState(search.q ?? "");
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Debounce de la recherche → met à jour l'URL
  useEffect(() => {
    if (debounceTimer.current !== null) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      debounceTimer.current = null;
      const trimmed = inputValue.trim();
      const qUrl = trimmed.length >= Q_MIN_LEN ? trimmed : undefined;
      if (qUrl !== search.q) {
        void navigate({
          to: "/app/essais",
          search: (prev: EssaisSearch) => ({ ...prev, q: qUrl, page: 1 }),
          replace: true,
        });
      }
    }, DEBOUNCE_MS);
    return () => {
      if (debounceTimer.current !== null) clearTimeout(debounceTimer.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputValue]);

  // Sync de l'input quand l'URL change (ex. réinitialisation)
  useEffect(() => {
    setInputValue(search.q ?? "");
  }, [search.q]);

  // Compteur « en retard » depuis la synthèse (pour la puce)
  const syntheseQuery = useTechnicienSynthese();
  const compteurEnRetard = syntheseQuery.data?.enRetardCount;

  // Construction des paramètres API (page 0-indexée côté backend)
  const page0 = search.page - 1;
  const params: ListerEssaisParams = {
    page: page0,
    size: PAGE_SIZE,
    tri: search.tri ?? "dateAttribution",
    direction: search.direction ?? "asc",
    statut: search.statut as ListerEssaisParams["statut"],
    domaineId: search.domaineId,
    debut: search.debut,
    fin: search.fin,
    enRetard: search.enRetard,
    q: search.q,
  };

  const query = useTechnicienEssais(params);

  // Re-corrige la page si on dépasse le total (ex. après suppression)
  useEffect(() => {
    if (query.data !== undefined && !query.isFetching && query.data.totalPages > 0 && search.page > query.data.totalPages) {
      void navigate({
        to: "/app/essais",
        search: (prev: EssaisSearch) => ({ ...prev, page: query.data!.totalPages }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.data, query.isFetching, search.page]);

  const handleStatut = (v: string | undefined) =>
    void navigate({
      to: "/app/essais",
      search: (prev: EssaisSearch) => ({ ...prev, statut: v, page: 1 }),
      replace: true,
    });
  const handleEnRetard = (v: boolean | undefined) =>
    void navigate({
      to: "/app/essais",
      search: (prev: EssaisSearch) => ({ ...prev, enRetard: v, page: 1 }),
      replace: true,
    });
  const handleEffacerRecherche = () => {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    if (search.q !== undefined) {
      void navigate({
        to: "/app/essais",
        search: (prev: EssaisSearch) => ({ ...prev, q: undefined, page: 1 }),
        replace: true,
      });
    }
  };
  const handleReinitialiser = () => {
    setInputValue("");
    if (debounceTimer.current !== null) {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    }
    void navigate({
      to: "/app/essais",
      search: { page: 1 },
      replace: true,
    });
  };
  const handlePrecedent = () => {
    if (search.page > 1) {
      void navigate({
        to: "/app/essais",
        search: (prev: EssaisSearch) => ({ ...prev, page: search.page - 1 }),
      });
    }
  };
  const handleSuivant = () => {
    if (query.data && search.page < query.data.totalPages) {
      void navigate({
        to: "/app/essais",
        search: (prev: EssaisSearch) => ({ ...prev, page: search.page + 1 }),
      });
    }
  };

  const essais = query.data?.content;
  const isLoading = query.isLoading;
  const isError = query.isError;
  const error = query.error;

  // État vide (chargement terminé, pas d'erreur, liste vide)
  const estVide = !isLoading && !isError && (essais === undefined || essais.length === 0);

  return (
    <div className="space-y-5">
      {/* En-tête */}
      <div className="flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
          <FlaskConical className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mes essais</h1>
          <p className="text-sm text-muted-foreground">
            Essais qui vous sont attribués
          </p>
        </div>
      </div>

      <FiltresEssais
        inputValue={inputValue}
        search={search}
        compteurEnRetard={compteurEnRetard}
        onInputChange={setInputValue}
        onStatut={handleStatut}
        onEnRetard={handleEnRetard}
        onEffacerRecherche={handleEffacerRecherche}
        onReinitialiser={handleReinitialiser}
      />

      {/* Vue desktop : tableau natif */}
      <div className="hidden md:block">
        <TableauEssais
          essais={essais}
          isLoading={isLoading}
          isError={isError}
          error={error}
          onReessayer={() => void query.refetch()}
        />
      </div>

      {/* Vue mobile : cartes */}
      <div className="space-y-3 md:hidden">
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Chargement…</p>
        ) : isError ? (
          <Card variant="flat" padding="lg" className="text-center">
            <p className="text-sm font-medium text-red-700" role="alert">
              {error instanceof ApiError
                ? messageErreurTechnicien(error)
                : "Impossible de charger la liste."}
            </p>
            <button
              type="button"
              onClick={() => void query.refetch()}
              className="mt-3 rounded-full border border-brand-900/10 px-4 py-2 text-sm font-semibold hover:bg-brand-900/5"
            >
              Réessayer
            </button>
          </Card>
        ) : (
          essais?.map((essai) => <LigneEssaiCard key={essai.ligneEssaiId} essai={essai} />)
        )}
      </div>

      {/* État vide */}
      {estVide && (
        <Card variant="flat" padding="lg" className="text-center">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-brand-900/10">
            <FlaskConical className="size-6 text-brand-900" aria-hidden="true" />
          </div>
          <p className="mt-3 text-sm font-medium text-ink-900">
            Aucun essai ne correspond à votre recherche
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Essayez de modifier les filtres ou réinitialisez la recherche.
          </p>
        </Card>
      )}

      {/* Pagination */}
      {query.data !== undefined && query.data.totalElements > 0 && (
        <PaginationEssais
          data={query.data}
          page={search.page}
          onPrecedent={handlePrecedent}
          onSuivant={handleSuivant}
        />
      )}
    </div>
  );
}
