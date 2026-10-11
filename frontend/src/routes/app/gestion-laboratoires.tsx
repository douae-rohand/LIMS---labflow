import { createFileRoute } from "@tanstack/react-router";
import { getSession } from "@/api/session";
import { ListeDemandes } from "@/features/super-admin/demandes/ListeDemandes";
import { Indicateurs, useStatistiquesPlateforme } from "@/features/super-admin/Indicateurs";
import { TableauLaboratoires } from "@/features/super-admin/laboratoires/TableauLaboratoires";

type RechercheGestion = {
  onglet: "laboratoires" | "demandes";
  demande?: number;
};

export const Route = createFileRoute("/app/gestion-laboratoires")({
  validateSearch: (search: Record<string, unknown>): RechercheGestion => {
    const demande = Number(search["demande"]);
    return {
      onglet: search["onglet"] === "demandes" ? "demandes" : "laboratoires",
      ...(Number.isInteger(demande) && demande > 0 ? { demande } : {}),
    };
  },
  beforeLoad: () => {
    const session = getSession();
    if (session?.user.role !== "SUPER_ADMINISTRATEUR") {
      throw Route.redirect({ to: "/app" });
    }
  },
  head: () => ({
    meta: [{ title: "Gestion des laboratoires — LabFlow" }],
  }),
  component: GestionLaboratoiresPage,
});

function GestionLaboratoiresPage() {
  const { onglet, demande } = Route.useSearch();
  const navigate = Route.useNavigate();
  const statistiques = useStatistiquesPlateforme();
  const totaux = statistiques.data;

  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
            Gestion des laboratoires
          </h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Suivez les demandes d'intégration et administrez les laboratoires déjà ouverts sur la plateforme.
          </p>
        </div>
        {totaux && (
          <p className="text-sm text-muted-foreground">
            <span className="font-semibold text-ink-900">{totaux.totalLaboratoires}</span> laboratoire
            {totaux.totalLaboratoires > 1 ? "s" : ""} enregistré
            {totaux.totalLaboratoires > 1 ? "s" : ""}
          </p>
        )}
      </header>

      <Indicateurs statistiques={totaux} />

      <div
        className="mt-5 mb-4 inline-flex w-full gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto"
        role="tablist"
        aria-label="Sections"
      >
        <BoutonOnglet
          actif={onglet === "laboratoires"}
          compteur={totaux?.totalLaboratoires}
          onClick={() => void navigate({ search: { onglet: "laboratoires" } })}
        >
          Laboratoires
        </BoutonOnglet>
        <BoutonOnglet
          actif={onglet === "demandes"}
          compteur={totaux?.totalDemandes}
          onClick={() => void navigate({ search: { onglet: "demandes", ...(demande ? { demande } : {}) } })}
        >
          Demandes d'intégration
        </BoutonOnglet>
      </div>

      {onglet === "laboratoires" ? (
        <TableauLaboratoires />
      ) : (
        <ListeDemandes demandeInitiale={demande} />
      )}
    </div>
  );
}

function BoutonOnglet({
  actif,
  compteur,
  onClick,
  children,
}: {
  actif: boolean;
  compteur?: number | undefined;
  onClick: () => void;
  children: string;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={actif}
      onClick={onClick}
      className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 sm:flex-none ${
        actif ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"
      }`}
    >
      {children}
      {compteur != null && (
        <span
          className={`rounded-full px-2 py-0.5 text-[11px] ${
            actif ? "bg-white/15 text-primary-foreground" : "bg-surface-alt text-brand-900"
          }`}
        >
          {compteur}
        </span>
      )}
    </button>
  );
}
