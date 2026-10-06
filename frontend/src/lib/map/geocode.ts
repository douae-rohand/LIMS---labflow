import type { LaboratoirePublic } from "@/api/landing";

export type MapPoint = {
  label: string;
  lat: number;
  lng: number;
};

export function normalizeSearch(text: string): string {
  return text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

export function matchesLab(lab: LaboratoirePublic, query: string): boolean {
  const needle = normalizeSearch(query);
  if (!needle) return false;
  const haystack = normalizeSearch(
    [lab.raisonSociale, lab.ville, lab.code, lab.adresse].filter(Boolean).join(" "),
  );
  return haystack.includes(needle);
}

export function labToMapPoint(lab: LaboratoirePublic): MapPoint | null {
  if (lab.latitude == null || lab.longitude == null) return null;
  return {
    label: lab.ville ? `${lab.raisonSociale} · ${lab.ville}` : lab.raisonSociale,
    lat: lab.latitude,
    lng: lab.longitude,
  };
}

export async function geocodePlace(
  query: string,
  signal?: AbortSignal,
): Promise<MapPoint[]> {
  const params = new URLSearchParams({
    format: "json",
    q: query,
    countrycodes: "ma",
    limit: "4",
  });
  const response = await fetch(
    `https://nominatim.openstreetmap.org/search?${params.toString()}`,
    {
      ...(signal ? { signal } : {}),
      headers: {
        Accept: "application/json",
        "Accept-Language": "fr",
      },
    },
  );
  if (!response.ok) return [];

  const data = (await response.json()) as Array<{
    display_name: string;
    lat: string;
    lon: string;
  }>;

  return data.map((item) => ({
    label: item.display_name.split(",").slice(0, 3).join(",").trim(),
    lat: Number(item.lat),
    lng: Number(item.lon),
  }));
}

export type ReverseGeocodeResult = {
  adresse: string;
  ville: string;
  region: string;
  pays: string;
  codePostal: string;
};

export async function reverseGeocode(
  lat: number,
  lng: number,
  signal?: AbortSignal,
): Promise<ReverseGeocodeResult | null> {
  const params = new URLSearchParams({
    format: "json",
    lat: String(lat),
    lon: String(lng),
    addressdetails: "1",
  });
  const response = await fetch(
    `https://nominatim.openstreetmap.org/reverse?${params.toString()}`,
    {
      ...(signal ? { signal } : {}),
      headers: {
        Accept: "application/json",
        "Accept-Language": "fr",
      },
    },
  );
  if (!response.ok) return null;
  const data = (await response.json()) as {
    display_name?: string;
    address?: {
      road?: string;
      house_number?: string;
      suburb?: string;
      city?: string;
      town?: string;
      village?: string;
      state?: string;
      country?: string;
      postcode?: string;
    };
  };
  const address = data.address ?? {};
  const rue = [address.house_number, address.road, address.suburb].filter(Boolean).join(" ");
  return {
    adresse: rue || data.display_name?.split(",").slice(0, 2).join(", ").trim() || "",
    ville: address.city || address.town || address.village || "",
    region: address.state || "",
    pays: address.country || "Maroc",
    codePostal: address.postcode || "",
  };
}
