import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { Container } from "@/components/layout/Container";
import { ListeEssaisTechnicien } from "@/components/technicien/essais/ListeEssaisTechnicien";

export interface EssaisSearch {
  page: number;
  q?: string | undefined;
  statut?: string | undefined;
  domaineId?: number | undefined;
  enRetard?: boolean | undefined;
  tri?: string | undefined;
  direction?: "asc" | "desc" | undefined;
  debut?: string | undefined;
  fin?: string | undefined;
}

const STATUTS_VALIDES = [
  "ATTENTE_ECHANTILLON",
  "EN_COURS",
  "TERMINE",
  "NON_REALISABLE",
] as const;

function parsePositiveInt(v: unknown, min: number, max: number): number | undefined {
  if (v === undefined || v === null || v === "") return undefined;
  const n = Number(v);
  if (!isFinite(n) || !Number.isInteger(n) || n < min || n > max) return undefined;
  return n;
}

export const Route = createFileRoute("/app/essais/")({
  head: () => ({ meta: [{ title: "Mes essais — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "TECHNICIEN") throw redirect({ to: "/app" });
  },
  validateSearch: (search: Record<string, unknown>): EssaisSearch => {
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q =
      typeof search["q"] === "string" && search["q"].length <= 100
        ? search["q"]
        : undefined;
    const statut = (STATUTS_VALIDES as ReadonlyArray<string>).includes(
      search["statut"] as string,
    )
      ? (search["statut"] as string)
      : undefined;
    const domaineId = parsePositiveInt(search["domaineId"], 1, Number.MAX_SAFE_INTEGER);
    const enRetard = search["enRetard"] === true || search["enRetard"] === "true";
    const tri = typeof search["tri"] === "string" ? search["tri"] : undefined;
    const direction =
      search["direction"] === "desc" ? ("desc" as const) : search["direction"] === "asc" ? ("asc" as const) : undefined;
    const debut = typeof search["debut"] === "string" ? search["debut"] : undefined;
    const fin = typeof search["fin"] === "string" ? search["fin"] : undefined;
    return { page, q, statut, domaineId, enRetard, tri, direction, debut, fin };
  },
  component: EssaisPage,
});

function EssaisPage() {
  const search = Route.useSearch();
  return (
    <Container className="py-6 sm:py-8">
      <ListeEssaisTechnicien search={search} />
    </Container>
  );
}
