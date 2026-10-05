import { ArrowLeftRight, Car, Footprints, MapPin, Target } from "lucide-react";
import type { LaboratoirePublic } from "@/api/landing";
import { Button, Input } from "@/components/lab";
import type { MapPoint } from "@/lib/map/geocode";
import { formatRouteDuration, type RouteSummary } from "@/lib/map/routing";

export type DirectionSuggestion =
  | { kind: "lab"; lab: LaboratoirePublic }
  | { kind: "place"; place: MapPoint };

type LabMapDirectionsProps = {
  originQuery: string;
  destinationQuery: string;
  suggestions: DirectionSuggestion[];
  suggestionsOpen: boolean;
  activeField: "origin" | "destination";
  routeLoading: boolean;
  routeError: string | null;
  routeSummary: RouteSummary | null;
  onOriginQueryChange: (value: string) => void;
  onDestinationQueryChange: (value: string) => void;
  onFocusField: (field: "origin" | "destination") => void;
  onBlurSuggestions: () => void;
  onSelectSuggestion: (field: "origin" | "destination", suggestion: DirectionSuggestion) => void;
  onSwap: () => void;
  onUseMyPosition: () => void;
};

export function LabMapDirections({
  originQuery,
  destinationQuery,
  suggestions,
  suggestionsOpen,
  activeField,
  routeLoading,
  routeError,
  routeSummary,
  onOriginQueryChange,
  onDestinationQueryChange,
  onFocusField,
  onBlurSuggestions,
  onSelectSuggestion,
  onSwap,
  onUseMyPosition,
}: LabMapDirectionsProps) {
  return (
    <div className="min-w-0">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <span className="size-2.5 shrink-0 rounded-full border-2 border-brand-600 bg-card" aria-hidden="true" />
          <Input
            type="search"
            value={originQuery}
            onChange={(event) => onOriginQueryChange(event.target.value)}
            onFocus={() => onFocusField("origin")}
            onBlur={onBlurSuggestions}
            placeholder="Point de départ"
            aria-label="Point de départ"
            autoComplete="off"
            className="h-11 py-2"
          />
        </div>
        <Button
          type="button"
          variant="secondary"
          size="icon"
          className="self-end sm:self-auto"
          aria-label="Inverser départ et destination"
          onClick={onSwap}
        >
          <ArrowLeftRight className="size-4" aria-hidden="true" />
        </Button>
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <MapPin className="size-4 shrink-0 text-brand-600" aria-hidden="true" />
          <Input
            type="search"
            value={destinationQuery}
            onChange={(event) => onDestinationQueryChange(event.target.value)}
            onFocus={() => onFocusField("destination")}
            onBlur={onBlurSuggestions}
            placeholder="Laboratoire ou lieu"
            aria-label="Destination"
            autoComplete="off"
            className="h-11 py-2"
          />
        </div>
      </div>

      {suggestionsOpen && suggestions.length > 0 ? (
        <ul className="mt-2 max-h-44 overflow-y-auto rounded-2xl border border-brand-900/10 bg-card py-1 shadow-soft">
          {suggestions.map((item) =>
            item.kind === "lab" ? (
              <li key={`dir-lab-${item.lab.id}`}>
                <button
                  type="button"
                  className="w-full px-4 py-2.5 text-left hover:bg-surface-card"
                  onMouseDown={() => onSelectSuggestion(activeField, item)}
                >
                  <span className="text-sm font-semibold text-ink-900">{item.lab.raisonSociale}</span>
                  <span className="mt-0.5 block text-xs text-muted-foreground">
                    {item.lab.ville?.trim() ? `${item.lab.ville} · ` : ""}Laboratoire
                  </span>
                </button>
              </li>
            ) : (
              <li key={`dir-place-${item.place.label}-${item.place.lat}`}>
                <button
                  type="button"
                  className="w-full px-4 py-2.5 text-left hover:bg-surface-card"
                  onMouseDown={() => onSelectSuggestion(activeField, item)}
                >
                  <span className="text-sm font-semibold text-ink-900">{item.place.label}</span>
                  <span className="mt-0.5 block text-xs text-muted-foreground">Lieu</span>
                </button>
              </li>
            ),
          )}
        </ul>
      ) : null}

      <button
        type="button"
        onClick={onUseMyPosition}
        className="mt-2 flex w-full items-center gap-2 rounded-2xl px-1 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
      >
        <span className="flex size-8 items-center justify-center rounded-full bg-mint-200 text-brand-900">
          <Target className="size-4" aria-hidden="true" />
        </span>
        Votre position
      </button>

      {routeLoading || routeSummary || routeError ? (
        <div className="mt-2 rounded-2xl border border-brand-900/10 bg-mint-200/40 px-4 py-3 text-sm">
          {routeLoading ? (
            <span className="text-muted-foreground">Calcul de l'itinéraire…</span>
          ) : routeError ? (
            <span className="font-semibold text-brand-900">{routeError}</span>
          ) : routeSummary ? (
            <span className="flex flex-wrap items-center gap-x-3 gap-y-1 text-ink-900">
              <span className="font-bold">{routeSummary.distanceKm.toFixed(1)} km</span>
              <span className="inline-flex items-center gap-1">
                <Car className="size-4 text-brand-600" aria-hidden="true" />
                {formatRouteDuration(routeSummary.durationSec)}
              </span>
              <span className="inline-flex items-center gap-1">
                <Footprints className="size-4 text-brand-600" aria-hidden="true" />
                {formatRouteDuration(routeSummary.walkingDurationSec)} à pied
              </span>
            </span>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
