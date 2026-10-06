import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { LocateFixed, MapPin } from "lucide-react";
import { Button, Label } from "@/components/lab";
import { geocodePlace, reverseGeocode } from "@/lib/map/geocode";
import { cn } from "@/lib/utils";

const MOROCCO_CENTER: L.LatLngExpression = [31.8, -7.0];

type LocationValue = {
  latitude: number | null;
  longitude: number | null;
};

type AddressPatch = {
  adresse?: string;
  ville?: string;
  region?: string;
  pays?: string;
  codePostal?: string;
};

type LocationPickerProps = {
  value: LocationValue;
  onChange: (next: { latitude: number; longitude: number }) => void;
  onAddressResolved?: (patch: AddressPatch) => void;
  label?: string;
  addressQuery?: string;
  className?: string;
  readOnly?: boolean;
};

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function locationIcon(label: string): L.DivIcon {
  const safe = escapeHtml(label || "Position du laboratoire");
  return L.divIcon({
    className: "labflow-location-marker",
    html: `
      <div class="labflow-location-marker-wrap">
        <span class="labflow-location-marker-pin" aria-hidden="true"></span>
        <span class="labflow-location-marker-label">${safe}</span>
      </div>
    `,
    iconSize: [220, 56],
    iconAnchor: [18, 48],
  });
}

export function LocationPicker({
  value,
  onChange,
  onAddressResolved,
  label = "Position du laboratoire",
  addressQuery,
  className,
  readOnly = false,
}: LocationPickerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markerRef = useRef<L.Marker | null>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const [searchError, setSearchError] = useState<string | null>(null);

  const onChangeRef = useRef(onChange);
  const onAddressRef = useRef(onAddressResolved);
  onChangeRef.current = onChange;
  onAddressRef.current = onAddressResolved;

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const map = L.map(containerRef.current, {
      center: MOROCCO_CENTER,
      zoom: 6,
      zoomControl: true,
    });
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
      maxZoom: 19,
    }).addTo(map);

    if (!readOnly) {
      map.on("click", (event: L.LeafletMouseEvent) => {
        onChangeRef.current({ latitude: event.latlng.lat, longitude: event.latlng.lng });
        void reverseGeocode(event.latlng.lat, event.latlng.lng).then((resolved) => {
          if (resolved) onAddressRef.current?.(resolved);
        });
      });
    }

    mapRef.current = map;
    window.setTimeout(() => map.invalidateSize(), 80);
    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, [readOnly]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || value.latitude == null || value.longitude == null) return;
    const latLng: L.LatLngExpression = [value.latitude, value.longitude];
    if (!markerRef.current) {
      markerRef.current = L.marker(latLng, {
        icon: locationIcon(label),
        draggable: !readOnly,
      }).addTo(map);
      if (!readOnly) {
        markerRef.current.on("dragend", () => {
          const pos = markerRef.current?.getLatLng();
          if (!pos) return;
          onChangeRef.current({ latitude: pos.lat, longitude: pos.lng });
          void reverseGeocode(pos.lat, pos.lng).then((resolved) => {
            if (resolved) onAddressRef.current?.(resolved);
          });
        });
      }
    } else {
      markerRef.current.setLatLng(latLng);
      markerRef.current.setIcon(locationIcon(label));
    }
    map.setView(latLng, Math.max(map.getZoom(), 14));
    window.setTimeout(() => map.invalidateSize(), 80);
  }, [label, readOnly, value.latitude, value.longitude]);

  const appliquerPoint = async (lat: number, lng: number, reverse = true) => {
    onChange({ latitude: lat, longitude: lng });
    if (!reverse) return;
    const resolved = await reverseGeocode(lat, lng);
    if (resolved) onAddressResolved?.(resolved);
  };

  const handleSearch = async () => {
    const query = searchRef.current?.value?.trim() || addressQuery?.trim();
    if (!query) {
      setSearchError("Saisissez une adresse pour l'afficher sur la carte.");
      return;
    }
    const results = await geocodePlace(query);
    const first = results[0];
    if (!first) {
      setSearchError("Cette adresse n'a pas été trouvée. Cliquez sur la carte pour placer l'icône.");
      return;
    }
    setSearchError(null);
    await appliquerPoint(first.lat, first.lng, !addressQuery?.trim());
  };

  const handleLocate = () => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((position) => {
      void appliquerPoint(position.coords.latitude, position.coords.longitude);
    });
  };

  return (
    <div className={cn("flex flex-col gap-3", className)}>
      {!readOnly && (
        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="min-w-0 flex-1">
            <Label htmlFor="map-search">Rechercher une adresse</Label>
            <input
              id="map-search"
              ref={searchRef}
              placeholder="Ville, quartier, laboratoire…"
              className="w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm text-ink-900 placeholder:text-muted-foreground"
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void handleSearch();
                }
              }}
            />
          </div>
          <div className="flex items-end gap-2">
            <Button type="button" variant="secondary" onClick={() => void handleSearch()}>
              Afficher sur la carte
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label="Utiliser ma position"
              onClick={handleLocate}
            >
              <LocateFixed className="size-4" />
            </Button>
          </div>
        </div>
      )}
      <div
        ref={containerRef}
        className="labflow-map-canvas h-80 overflow-hidden rounded-3xl border border-brand-900/10"
      />
      {searchError && <p className="text-sm text-red-600">{searchError}</p>}
      <p className="flex items-center gap-2 text-xs text-muted-foreground">
        <MapPin className="size-3.5 shrink-0 text-brand-900" />
        {value.latitude != null && value.longitude != null
          ? `Position affichée : ${value.latitude.toFixed(5)}, ${value.longitude.toFixed(5)}`
          : readOnly
            ? "Aucune coordonnée enregistrée."
            : "Saisissez l'adresse puis cliquez sur « Afficher sur la carte », ou cliquez directement sur la carte."}
      </p>
    </div>
  );
}
