import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Eye, RefreshCw, Search, X } from "lucide-react";
import { Badge, Button, Input, Select, Tooltip } from "@/components/lab";
import {
  listerDemandesIntegration,
  type DemandeIntegration,
  type StatutIntegration,
} from "@/api/plateforme";
import { formaterDate } from "../texte";
import { PanneauDemande } from "./PanneauDemande";

const LIBELLES: Record<StatutIntegration, string> = {
  EN_ATTENTE: "En attente",
  EN_COURS_VALIDATION: "En cours de validation",
  APPROUVEE: "Acceptée",
  REJETEE: "Refusée",
  SUSPENDUE: "Suspendue",
};

function variante(statut: StatutIntegration) {
  if (statut === "APPROUVEE") return "lime" as const;
  if (statut === "REJETEE") return "outline" as const;
  return "mint" as const;
}

export function ListeDemandes({ demandeInitiale }: { demandeInitiale?: number | undefined }) {
  const [statut, setStatut] = useState<StatutIntegration | "">("");
  const [recherche, setRecherche] = useState("");
  const [rechercheDifferee, setRechercheDifferee] = useState("");
  const [page, setPage] = useState(0);
  const [demandeId, setDemandeId] = useState<number | null>(demandeInitiale ?? null);

  useEffect(() => {
    const timer = window.setTimeout(() => setRechercheDifferee(recherche.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [recherche]);

  useEffect(() => {
    setPage(0);
  }, [statut, rechercheDifferee]);

  useEffect(() => {
    if (demandeInitiale) setDemandeId(demandeInitiale);
  }, [demandeInitiale]);

  const demandes = useQuery({
    queryKey: ["demandes-integration", statut, rechercheDifferee, page],
    queryFn: () => listerDemandesIntegration(statut || undefined, page, 10, rechercheDifferee || undefined),
  });

  const total = demandes.data?.totalElements ?? 0;
  const debut = total === 0 ? 0 : page * 10 + 1;
  const fin = Math.min(total, (page + 1) * 10);
  const filtresActifs = Boolean(rechercheDifferee || statut);
  const aucune = !demandes.isLoading && !demandes.isError && total === 0;

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-2xl border border-brand-900/10 bg-card p-3 sm:p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              value={recherche}
              onChange={(event) => setRecherche(event.target.value)}
              placeholder="Rechercher par nom, numéro, ville ou e-mail"
              aria-label="Rechercher une demande"
              className="pl-10"
            />
          </div>
          <Select
            value={statut}
            aria-label="Filtrer par statut de la demande"
            className="lg:w-64"
            onChange={(event) => setStatut(event.target.value as StatutIntegration | "")}
          >
            <option value="">Tous les statuts</option>
            <option value="EN_ATTENTE">En attente</option>
            <option value="EN_COURS_VALIDATION">En cours de validation</option>
            <option value="APPROUVEE">Acceptées</option>
            <option value="REJETEE">Refusées</option>
            <option value="SUSPENDUE">Suspendues</option>
          </Select>
          <div className="flex gap-2">
            {filtresActifs && (
              <Button variant="ghost" size="sm" onClick={() => { setRecherche(""); setStatut(""); }}>
                Réinitialiser
              </Button>
            )}
            <Button
              variant="ghost"
              size="sm"
              icon={<RefreshCw className="size-4" aria-hidden="true" />}
              iconPosition="left"
              onClick={() => void demandes.refetch()}
            >
              Actualiser
            </Button>
          </div>
        </div>
        {filtresActifs && (
          <div className="mt-3 flex flex-wrap gap-2">
            {rechercheDifferee && (
              <button type="button" className="inline-flex items-center gap-1 rounded-full bg-surface-alt px-3 py-1 text-xs font-semibold text-brand-900" onClick={() => setRecherche("")}>
                Recherche : {rechercheDifferee}
                <X className="size-3" aria-hidden="true" />
              </button>
            )}
            {statut && (
              <button type="button" className="inline-flex items-center gap-1 rounded-full bg-surface-alt px-3 py-1 text-xs font-semibold text-brand-900" onClick={() => setStatut("")}>
                {LIBELLES[statut]}
                <X className="size-3" aria-hidden="true" />
              </button>
            )}
          </div>
        )}
      </div>

      <div className="overflow-x-auto rounded-2xl border border-brand-900/10 bg-card">
        <table className="w-full min-w-[980px] border-separate border-spacing-0 text-left text-sm">
          <thead>
            <tr className="bg-surface-alt text-brand-900">
              {["Laboratoire", "Date de soumission", "Demandeur", "Statut", "Documents"].map((colonne) => (
                <th key={colonne} scope="col" className="px-4 py-3 text-xs font-semibold tracking-wide whitespace-nowrap">
                  {colonne}
                </th>
              ))}
              <th
                scope="col"
                className="sticky right-0 z-30 bg-surface-alt px-3 py-3 text-right text-xs font-semibold tracking-wide shadow-[-8px_0_12px_-12px_rgba(36,42,46,0.45)]"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {demandes.isLoading && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-sm text-muted-foreground">
                  Chargement des demandes…
                </td>
              </tr>
            )}
            {demandes.isError && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">Les demandes n'ont pas pu être chargées.</p>
                  <Button className="mt-3" size="sm" variant="secondary" onClick={() => void demandes.refetch()}>
                    Réessayer
                  </Button>
                </td>
              </tr>
            )}
            {aucune && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">
                    {filtresActifs ? "Aucune demande ne correspond à ces critères." : "Aucune demande d'intégration."}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {statut === "EN_ATTENTE" || (!filtresActifs && total === 0)
                      ? "Les nouvelles demandes apparaîtront ici dès leur envoi."
                      : "Modifiez la recherche ou retirez un filtre."}
                  </p>
                </td>
              </tr>
            )}
            {demandes.data?.content.map((demande) => (
              <LigneDemande key={demande.id} demande={demande} onOuvrir={() => setDemandeId(demande.id)} />
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
        <p>
          {debut}–{fin} sur {total}
        </p>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Précédent
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={!demandes.data || page + 1 >= demandes.data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Suivant
          </Button>
        </div>
      </div>

      <PanneauDemande demandeId={demandeId} onFermer={() => setDemandeId(null)} />
    </div>
  );
}

function LigneDemande({ demande, onOuvrir }: { demande: DemandeIntegration; onOuvrir: () => void }) {
  const demandeur = [demande.adminPrenom, demande.adminNom].filter(Boolean).join(" ") || "—";
  return (
    <tr className="group">
      <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
        <p className="max-w-[240px] truncate font-semibold text-ink-900" title={demande.nomLaboratoire}>
          {demande.nomLaboratoire}
        </p>
        <p className="text-xs text-muted-foreground">{demande.numero}{demande.ville ? ` · ${demande.ville}` : ""}</p>
      </td>
      <td className="border-t border-brand-900/10 bg-card px-4 py-3 whitespace-nowrap text-ink-900 group-hover:bg-surface-alt">
        {formaterDate(demande.dateSoumission)}
      </td>
      <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
        <p className="font-medium text-ink-900">{demandeur}</p>
        <p className="max-w-[200px] truncate text-xs text-muted-foreground" title={demande.adminEmail}>
          {demande.adminEmail}
        </p>
      </td>
      <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
        <Badge variant={variante(demande.statut)} size="sm">
          {LIBELLES[demande.statut]}
        </Badge>
      </td>
      <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
        <p className="font-medium text-ink-900">{demande.nombreDocuments ?? 0} / 6</p>
        <p className="text-xs text-muted-foreground">{demande.dossierComplet ? "Dossier complet" : "Dossier incomplet"}</p>
      </td>
      <td className="sticky right-0 z-10 border-t border-brand-900/10 bg-card px-2 py-3 text-right shadow-[-8px_0_12px_-12px_rgba(36,42,46,0.35)] group-hover:bg-surface-alt">
        <Tooltip content="Consulter">
          <Button
            variant="ghost"
            size="sm"
            className="size-9 px-0"
            aria-label={`Consulter la demande ${demande.numero}`}
            icon={<Eye className="size-4" aria-hidden="true" />}
            iconPosition="left"
            onClick={onOuvrir}
          />
        </Tooltip>
      </td>
    </tr>
  );
}
