import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { Link } from "@tanstack/react-router";
import { LocateFixed, Minus, Plus, Satellite, Search, X } from "lucide-react";
import type { LaboratoirePublic } from "@/api/landing";
import { Badge, Button, Card, Input } from "@/components/lab";
import {
  geocodePlace,
  labToMapPoint,
  matchesLab,
  type MapPoint,
} from "@/lib/map/geocode";
import { fetchDrivingRoute, type RouteSummary } from "@/lib/map/routing";
import { cn } from "@/lib/utils";
import { LabMapDirections, type DirectionSuggestion } from "./LabMapDirections";

const MOROCCO_CENTER: L.LatLngExpression = [31.8, -7.0];
const DEFAULT_ZOOM = 6;
const FLY_ZOOM = 14;

const STREET_TILES = {
  url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  attribution: "&copy; OpenStreetMap",
  maxZoom: 19,
};

const SATELLITE_TILES = {
  url: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
  attribution: "&copy; Esri",
  maxZoom: 19,
};

type PanelMode = "search" | "directions";

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function createLabIcon(nom: string, highlighted = false): L.DivIcon {
  const safeNom = escapeHtml(nom);
  return L.divIcon({
    className: "labflow-map-marker",
    html: `
      <button type="button" class="labflow-map-marker-btn${highlighted ? " is-active" : ""}" aria-label="${safeNom}">
        <span class="labflow-map-marker-dot"></span>
        <span class="labflow-map-marker-label">${safeNom}</span>
      </button>
    `,
    iconSize: [168, 48],
    iconAnchor: [84, 24],
  });
}

function createRoutePin(kind: "origin" | "destination"): L.DivIcon {
  return L.divIcon({
    className: "labflow-map-pin",
    html: `<span class="labflow-map-pin-dot labflow-map-pin-dot--${kind}"></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}

type LabMapProps = {
  laboratoires: LaboratoirePublic[];
  variant?: "full" | "embed";
  className?: string;
};

export function LabMap({
  laboratoires,
  variant = "full",
  className,
}: LabMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const streetLayerRef = useRef<L.TileLayer | null>(null);
  const satelliteLayerRef = useRef<L.TileLayer | null>(null);
  const markersRef = useRef<Map<number, L.Marker>>(new Map());
  const userMarkerRef = useRef<L.CircleMarker | null>(null);
  const searchMarkerRef = useRef<L.Marker | null>(null);
  const routeLayerRef = useRef<L.Polyline | null>(null);
  const routeOriginMarkerRef = useRef<L.Marker | null>(null);
  const routeDestinationMarkerRef = useRef<L.Marker | null>(null);

  const [panelMode, setPanelMode] = useState<PanelMode>("search");
  const [satelliteView, setSatelliteView] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const [geocodeResults, setGeocodeResults] = useState<MapPoint[]>([]);
  const [highlightedLabId, setHighlightedLabId] = useState<number | null>(null);
  const [selectedLab, setSelectedLab] = useState<LaboratoirePublic | null>(null);

  const [origin, setOrigin] = useState<MapPoint | null>(null);
  const [destination, setDestination] = useState<MapPoint | null>(null);
  const [originQuery, setOriginQuery] = useState("");
  const [destinationQuery, setDestinationQuery] = useState("");
  const [directionGeocodeResults, setDirectionGeocodeResults] = useState<MapPoint[]>([]);
  const [activeDirectionField, setActiveDirectionField] = useState<"origin" | "destination">("origin");
  const [directionSuggestionsOpen, setDirectionSuggestionsOpen] = useState(false);
  const [routeLoading, setRouteLoading] = useState(false);
  const [routeError, setRouteError] = useState<string | null>(null);
  const [routeSummary, setRouteSummary] = useState<RouteSummary | null>(null);

  const positionedLabs = useMemo(
    () => laboratoires.filter((lab) => lab.latitude != null && lab.longitude != null),
    [laboratoires],
  );

  const labMatches = useMemo(() => {
    if (searchQuery.trim().length < 2) return [];
    return positionedLabs.filter((lab) => matchesLab(lab, searchQuery));
  }, [positionedLabs, searchQuery]);

  const directionSuggestions = useMemo<DirectionSuggestion[]>(() => {
    const query = activeDirectionField === "origin" ? originQuery : destinationQuery;
    const labs = query.trim().length >= 2
      ? positionedLabs.filter((lab) => matchesLab(lab, query)).map((lab) => ({ kind: "lab" as const, lab }))
      : [];
    const places = directionGeocodeResults.map((place) => ({ kind: "place" as const, place }));
    return [...labs, ...places];
  }, [activeDirectionField, originQuery, destinationQuery, positionedLabs, directionGeocodeResults]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || mapRef.current) return;

    const map = L.map(container, {
      center: MOROCCO_CENTER,
      zoom: DEFAULT_ZOOM,
      zoomControl: false,
    });
    streetLayerRef.current = L.tileLayer(STREET_TILES.url, STREET_TILES).addTo(map);
    satelliteLayerRef.current = L.tileLayer(SATELLITE_TILES.url, SATELLITE_TILES);
    mapRef.current = map;
    const invalidate = window.setTimeout(() => map.invalidateSize(), 80);

    return () => {
      window.clearTimeout(invalidate);
      map.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    if (satelliteView) {
      streetLayerRef.current?.remove();
      satelliteLayerRef.current?.addTo(map);
    } else {
      satelliteLayerRef.current?.remove();
      streetLayerRef.current?.addTo(map);
    }
  }, [satelliteView]);

  const flyToLab = useCallback((lab: LaboratoirePublic) => {
    const point = labToMapPoint(lab);
    const map = mapRef.current;
    if (!point || !map) return;
    map.flyTo([point.lat, point.lng], FLY_ZOOM, { duration: 0.8 });
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    markersRef.current.forEach((marker) => marker.remove());
    markersRef.current.clear();

    positionedLabs.forEach((lab) => {
      const point = labToMapPoint(lab);
      if (!point) return;
      const marker = L.marker([point.lat, point.lng], {
        icon: createLabIcon(lab.raisonSociale, highlightedLabId === lab.id),
      }).addTo(map);

      marker.on("click", () => {
        setHighlightedLabId(lab.id);
        setSelectedLab(lab);
        if (panelMode === "directions") {
          setDestination(point);
          setDestinationQuery(point.label);
          setDirectionSuggestionsOpen(false);
          return;
        }
        flyToLab(lab);
      });
      markersRef.current.set(lab.id, marker);
    });

    if (
      positionedLabs.length > 0 &&
      highlightedLabId == null &&
      origin == null &&
      destination == null
    ) {
      const bounds = L.latLngBounds(
        positionedLabs
          .map((lab) => labToMapPoint(lab))
          .filter((point): point is MapPoint => point != null)
          .map((point) => [point.lat, point.lng] as L.LatLngTuple),
      );
      if (bounds.isValid()) {
        map.fitBounds(bounds, { padding: [48, 48], maxZoom: 12 });
      }
    }
  }, [positionedLabs, highlightedLabId, panelMode, flyToLab, origin, destination]);

  useEffect(() => {
    const controller = new AbortController();
    if (searchQuery.trim().length < 3) {
      setGeocodeResults([]);
      return;
    }
    const timer = window.setTimeout(() => {
      void geocodePlace(searchQuery, controller.signal).then(setGeocodeResults);
    }, 450);
    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [searchQuery]);

  useEffect(() => {
    const controller = new AbortController();
    const query = activeDirectionField === "origin" ? originQuery : destinationQuery;
    if (query.trim().length < 3) {
      setDirectionGeocodeResults([]);
      return;
    }
    const timer = window.setTimeout(() => {
      void geocodePlace(query, controller.signal).then(setDirectionGeocodeResults);
    }, 450);
    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [activeDirectionField, originQuery, destinationQuery]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !origin || !destination) {
      routeLayerRef.current?.remove();
      routeOriginMarkerRef.current?.remove();
      routeDestinationMarkerRef.current?.remove();
      setRouteSummary(null);
      return;
    }

    const controller = new AbortController();
    setRouteLoading(true);
    setRouteError(null);
    void fetchDrivingRoute(origin, destination, controller.signal)
      .then((summary) => {
        setRouteSummary(summary);
        routeLayerRef.current?.remove();
        routeOriginMarkerRef.current?.remove();
        routeDestinationMarkerRef.current?.remove();
        routeLayerRef.current = L.polyline(summary.coordinates, {
          color: "#1f6b6a",
          weight: 5,
          opacity: 0.85,
        }).addTo(map);
        routeOriginMarkerRef.current = L.marker([origin.lat, origin.lng], {
          icon: createRoutePin("origin"),
        }).addTo(map);
        routeDestinationMarkerRef.current = L.marker([destination.lat, destination.lng], {
          icon: createRoutePin("destination"),
        }).addTo(map);
        map.fitBounds(routeLayerRef.current.getBounds(), { padding: [40, 40] });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setRouteSummary(null);
        setRouteError(error instanceof Error ? error.message : "Itinéraire indisponible.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setRouteLoading(false);
      });

    return () => controller.abort();
  }, [origin, destination]);

  const locateUser = useCallback((asOrigin = false) => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((position) => {
      const map = mapRef.current;
      const latlng: L.LatLngExpression = [position.coords.latitude, position.coords.longitude];
      if (!map) return;
      userMarkerRef.current?.remove();
      userMarkerRef.current = L.circleMarker(latlng, {
        radius: 8,
        color: "#1f6b6a",
        fillColor: "#c8e6e3",
        fillOpacity: 0.9,
        weight: 2,
      }).addTo(map);
      map.flyTo(latlng, FLY_ZOOM, { duration: 0.7 });
      if (asOrigin) {
        setOrigin({ label: "Votre position", lat: position.coords.latitude, lng: position.coords.longitude });
        setOriginQuery("Votre position");
      }
    });
  }, []);

  const zoomBy = (delta: number) => {
    mapRef.current?.setZoom((mapRef.current.getZoom() ?? DEFAULT_ZOOM) + delta);
  };

  const showPanel = variant === "full";

  return (
    <Card variant="glossy" padding="none" className={cn("mx-auto w-full max-w-6xl overflow-hidden", className)}>
      {showPanel ? (
        <div className="border-b border-brand-900/10 bg-card/80 p-4 sm:p-5">
          <div className="mb-3 flex gap-2">
            <Button
              type="button"
              size="sm"
              variant={panelMode === "search" ? "primary" : "secondary"}
              onClick={() => setPanelMode("search")}
            >
              Rechercher
            </Button>
            <Button
              type="button"
              size="sm"
              variant={panelMode === "directions" ? "primary" : "secondary"}
              onClick={() => setPanelMode("directions")}
            >
              Itinéraire
            </Button>
          </div>

          {panelMode === "search" ? (
            <div className="relative">
              <Search className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-brand-600" aria-hidden="true" />
              <Input
                type="search"
                value={searchQuery}
                onChange={(event) => {
                  setSearchQuery(event.target.value);
                  setSearchOpen(true);
                }}
                onFocus={() => setSearchOpen(true)}
                onBlur={() => window.setTimeout(() => setSearchOpen(false), 150)}
                placeholder="Nom de laboratoire, ville ou lieu au Maroc"
                className="pl-11"
                autoComplete="off"
              />
              {searchOpen && (labMatches.length > 0 || geocodeResults.length > 0) ? (
                <ul className="absolute z-20 mt-2 max-h-56 w-full overflow-y-auto rounded-2xl border border-brand-900/10 bg-card py-1 shadow-soft">
                  {labMatches.map((lab) => (
                    <li key={lab.id}>
                      <button
                        type="button"
                        className="w-full px-4 py-2.5 text-left hover:bg-surface-card"
                        onMouseDown={() => {
                          setHighlightedLabId(lab.id);
                          setSelectedLab(lab);
                          flyToLab(lab);
                          setSearchQuery(lab.raisonSociale);
                          setSearchOpen(false);
                        }}
                      >
                        <span className="text-sm font-semibold text-ink-900">{lab.raisonSociale}</span>
                        <span className="mt-0.5 block text-xs text-muted-foreground">
                          {lab.ville?.trim() ? `${lab.ville} · ` : ""}Laboratoire
                        </span>
                      </button>
                    </li>
                  ))}
                  {geocodeResults.map((place) => (
                    <li key={`${place.label}-${place.lat}`}>
                      <button
                        type="button"
                        className="w-full px-4 py-2.5 text-left hover:bg-surface-card"
                        onMouseDown={() => {
                          const map = mapRef.current;
                          if (!map) return;
                          searchMarkerRef.current?.remove();
                          searchMarkerRef.current = L.marker([place.lat, place.lng], {
                            icon: createRoutePin("origin"),
                          }).addTo(map);
                          map.flyTo([place.lat, place.lng], FLY_ZOOM, { duration: 0.7 });
                          setSearchQuery(place.label);
                          setSearchOpen(false);
                        }}
                      >
                        <span className="text-sm font-semibold text-ink-900">{place.label}</span>
                        <span className="mt-0.5 block text-xs text-muted-foreground">Lieu</span>
                      </button>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : (
            <LabMapDirections
              originQuery={originQuery}
              destinationQuery={destinationQuery}
              suggestions={directionSuggestions}
              suggestionsOpen={directionSuggestionsOpen}
              activeField={activeDirectionField}
              routeLoading={routeLoading}
              routeError={routeError}
              routeSummary={routeSummary}
              onOriginQueryChange={(value) => {
                setOriginQuery(value);
                setDirectionSuggestionsOpen(true);
              }}
              onDestinationQueryChange={(value) => {
                setDestinationQuery(value);
                setDirectionSuggestionsOpen(true);
              }}
              onFocusField={(field) => {
                setActiveDirectionField(field);
                setDirectionSuggestionsOpen(true);
              }}
              onBlurSuggestions={() => window.setTimeout(() => setDirectionSuggestionsOpen(false), 150)}
              onSelectSuggestion={(field, suggestion) => {
                const point = suggestion.kind === "lab" ? labToMapPoint(suggestion.lab) : suggestion.place;
                if (!point) return;
                if (suggestion.kind === "lab") {
                  setHighlightedLabId(suggestion.lab.id);
                  setSelectedLab(suggestion.lab);
                }
                if (field === "origin") {
                  setOrigin(point);
                  setOriginQuery(point.label);
                } else {
                  setDestination(point);
                  setDestinationQuery(point.label);
                }
                setDirectionSuggestionsOpen(false);
              }}
              onSwap={() => {
                setOrigin(destination);
                setDestination(origin);
                setOriginQuery(destinationQuery);
                setDestinationQuery(originQuery);
              }}
              onUseMyPosition={() => locateUser(true)}
            />
          )}
        </div>
      ) : null}

      <div className="relative">
        <div
          ref={containerRef}
          className={cn(
            "labflow-map-canvas w-full",
            variant === "embed" ? "h-[18rem] sm:h-[24rem]" : "h-[24rem] sm:h-[30rem] lg:h-[34rem]",
          )}
        />
        <div className="absolute top-4 right-4 z-[400] flex flex-col gap-2">
          <Button type="button" variant="secondary" size="icon" aria-label="Zoom avant" onClick={() => zoomBy(1)}>
            <Plus className="size-4" aria-hidden="true" />
          </Button>
          <Button type="button" variant="secondary" size="icon" aria-label="Zoom arrière" onClick={() => zoomBy(-1)}>
            <Minus className="size-4" aria-hidden="true" />
          </Button>
          <Button
            type="button"
            variant={satelliteView ? "lime" : "secondary"}
            size="icon"
            aria-label={satelliteView ? "Vue plan" : "Vue satellite"}
            onClick={() => setSatelliteView((value) => !value)}
          >
            <Satellite className="size-4" aria-hidden="true" />
          </Button>
          <Button type="button" variant="secondary" size="icon" aria-label="Ma position" onClick={() => locateUser(false)}>
            <LocateFixed className="size-4" aria-hidden="true" />
          </Button>
        </div>

        {selectedLab ? (
          <div className="absolute inset-x-4 bottom-4 z-[400] sm:inset-x-auto sm:left-4 sm:max-w-sm">
            <Card variant="glass" padding="md">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-extrabold text-ink-900">{selectedLab.raisonSociale}</p>
                  <p className="mt-1 text-xs font-semibold tracking-wide text-brand-600 uppercase">
                    {selectedLab.code}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge size="sm" variant="mint">{selectedLab.statut}</Badge>
                  <button
                    type="button"
                    className="rounded-full p-1 text-ink-900/60 hover:bg-surface-card hover:text-ink-900"
                    aria-label="Fermer la fiche laboratoire"
                    onClick={() => {
                      setSelectedLab(null);
                      setHighlightedLabId(null);
                    }}
                  >
                    <X className="size-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
              <p className="mt-2 text-sm text-muted-foreground">
                {selectedLab.ville?.trim() || "Ville non renseignée"}
                {selectedLab.adresse?.trim() ? ` — ${selectedLab.adresse}` : ""}
              </p>
              <Link
                to="/laboratoires/$id"
                params={{ id: String(selectedLab.id) }}
                className="mt-3 inline-flex text-sm font-semibold text-brand-900 hover:underline"
              >
                Voir la fiche
              </Link>
            </Card>
          </div>
        ) : null}
      </div>
    </Card>
  );
}
