import type { MapPoint } from "./geocode";

export type RouteSummary = {
  distanceKm: number;
  durationSec: number;
  walkingDurationSec: number;
  coordinates: [number, number][];
};

const WALKING_SPEED_KMH = 5;

export function estimateWalkingDurationSec(distanceKm: number): number {
  return Math.round((distanceKm / WALKING_SPEED_KMH) * 3600);
}

export function formatRouteDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) return `${hours} h ${minutes} min`;
  return `${minutes} min`;
}

export async function fetchDrivingRoute(
  origin: MapPoint,
  destination: MapPoint,
  signal?: AbortSignal,
): Promise<RouteSummary> {
  const drivingUrl = `https://router.project-osrm.org/route/v1/driving/${origin.lng},${origin.lat};${destination.lng},${destination.lat}?overview=full&geometries=geojson`;
  const drivingResponse = await fetch(drivingUrl, {
    ...(signal ? { signal } : {}),
    headers: { Accept: "application/json" },
  });

  if (!drivingResponse.ok) {
    throw new Error("Impossible de calculer l'itinéraire.");
  }

  const data = (await drivingResponse.json()) as {
    code: string;
    routes?: Array<{
      distance: number;
      duration: number;
      geometry: { coordinates: [number, number][] };
    }>;
  };

  if (data.code !== "Ok" || data.routes?.[0] == null) {
    throw new Error("Aucun itinéraire trouvé entre ces deux points.");
  }

  const route = data.routes[0];
  const distanceKm = route.distance / 1000;

  return {
    distanceKm,
    durationSec: Math.round(route.duration),
    walkingDurationSec: estimateWalkingDurationSec(distanceKm),
    coordinates: route.geometry.coordinates.map(([lng, lat]) => [lat, lng]),
  };
}
