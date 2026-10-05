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
