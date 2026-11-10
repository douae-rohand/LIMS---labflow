import { useEffect, useState } from "react";
import { libelleTva, useTvaLaboratoire } from "@/api/tvaLaboratoire";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Plus, Power, PowerOff, Trash2, X } from "lucide-react";
import { Button, Select } from "@/components/lab";
import * as catalogueAdmin from "@/api/catalogueAdmin";
import { messageErreurCatalogueAdmin } from "@/api/catalogueAdmin";
import type { CreerEssaiRequest, ModifierEssaiRequest } from "@/api/catalogueAdmin";
import { EssaiForm, type ExigenceEssaiFormValue } from "./EssaiForm";
import { EssaiPanneau } from "./EssaiPanneau";
import { ConfirmDialog } from "./ConfirmDialog";

const PAGE_SIZE = 4;

interface EnregistrerEssaiPayload {
  id: number | null;
  data: CreerEssaiRequest | ModifierEssaiRequest;
  exigences: ExigenceEssaiFormValue[];
  exigencesSupprimees: number[];
  etape?: number;
  fermerApresSauvegarde?: boolean;
}

export function EssaisTab() {
  const tva = useTvaLaboratoire();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [actif, setActif] = useState<boolean | undefined>(undefined);
  const [domaineId, setDomaineId] = useState<number | undefined>(undefined);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const essais = useQuery({
    queryKey: ["catalogue-admin-essais", page, PAGE_SIZE, actif, domaineId],
    queryFn: () => catalogueAdmin.listerEssais({ page, size: PAGE_SIZE, actif, domaineId }),
  });
  const domaines = useQuery({
    queryKey: ["catalogue-admin-domaines"],
    queryFn: catalogueAdmin.listerDomaines,
  });

  const enregistrerEssai = useMutation({
    mutationFn: async ({ id, data, exigences, exigencesSupprimees, etape }: EnregistrerEssaiPayload) => {
      if (id === null) {
        return catalogueAdmin.creerEssai({
          ...(data as CreerEssaiRequest),
          code: (data as CreerEssaiRequest).code.trim().toUpperCase(),
          exigences: exigences.map(({ nature, etatConservation, quantiteMinimale, unite }) => ({
            nature: nature.trim(),
            etatConservation: etatConservation?.trim(),
            quantiteMinimale,
            unite: unite?.trim(),
          })),
        });
      }

      const champsEtape: ModifierEssaiRequest = etape === 0
        ? {
          code: data.code?.trim().toUpperCase(),
          designation: data.designation,
          tarif: data.tarif,
          domaineId: data.domaineId,
        }
        : etape === 1
          ? { description: data.description, methode: data.methode }
          : etape === 2
            ? {
              dureeEstimee: data.dureeEstimee,
              unite: data.unite,
              limiteMin: data.limiteMin,
              limiteMax: data.limiteMax,
              seuilCritiqueMin: data.seuilCritiqueMin,
              seuilCritiqueMax: data.seuilCritiqueMax,
            }
            : {};

      const essai = await catalogueAdmin.modifierEssai(id, champsEtape);

      if (etape === 3) {
        for (const exigence of exigences) {
          const { id: exigenceId, ...champs } = exigence;
          if (exigenceId === undefined) {
            await catalogueAdmin.creerExigence(essai.id, champs);
          } else {
            await catalogueAdmin.modifierExigence(exigenceId, champs);
          }
        }
        for (const exigenceId of exigencesSupprimees) {
          await catalogueAdmin.supprimerExigence(exigenceId);
        }
      }
      return essai;
    },
    onSuccess: (essai, payload) => {
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] });
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais-select"] });
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essai", essai.id] });
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-exigences", essai.id] });
      if (payload.fermerApresSauvegarde ?? payload.id === null) {
        setFormOpen(false);
        setSelectedId(null);
      }
    },
  });

  const supprimerEssai = useMutation({
    mutationFn: catalogueAdmin.supprimerEssai,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] });
      void queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais-select"] });
      setDeleteConfirm(null);
    },
  });
  const activerEssai = useMutation({
    mutationFn: catalogueAdmin.activerEssai,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] }),
  });
  const desactiverEssai = useMutation({
    mutationFn: catalogueAdmin.desactiverEssai,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] }),
  });

  useEffect(() => {
    if (essais.data && page >= Math.max(essais.data.totalPages, 1)) {
      setPage(Math.max(essais.data.totalPages - 1, 0));
    }
  }, [essais.data, page]);

  const essaiSelectionne = essais.data?.content.find((essai) => essai.id === selectedId);
  const ouvrirModification = (id: number) => {
    setDetailId(null);
    setSelectedId(id);
    setFormOpen(true);
  };

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <h2 className="text-lg font-semibold">Essais</h2>
        <div className="flex flex-wrap items-center gap-2">
          <Select
            aria-label="Filtrer les essais par état"
            className="w-44"
            value={actif === undefined ? "" : String(actif)}
            onChange={(event) => {
              const value = event.target.value;
              setActif(value === "" ? undefined : value === "true");
              setPage(0);
            }}
          >
            <option value="">Tous les essais</option>
            <option value="true">Activés</option>
            <option value="false">Désactivés</option>
          </Select>
          <Select
            aria-label="Filtrer les essais par domaine"
            className="w-44"
            value={domaineId === undefined ? "" : String(domaineId)}
            onChange={(event) => {
              const value = event.target.value;
              setDomaineId(value === "" ? undefined : Number(value));
              setPage(0);
            }}
          >
            <option value="">Tous les domaines</option>
            {domaines.data?.map((domaine) => (
              <option key={domaine.id} value={domaine.id}>{domaine.libelle}</option>
            ))}
          </Select>
          <Button variant="primary" size="sm" icon={<Plus className="size-4" aria-hidden="true" />} onClick={() => { setSelectedId(null); setFormOpen(true); }}>
            Nouvel essai
          </Button>
        </div>
      </div>

      {essais.isLoading && <p className="py-6 text-sm text-muted-foreground">Chargement des essais...</p>}
      {essais.isError && <p role="alert" className="py-4 text-sm text-rose-700">{messageErreurCatalogueAdmin(essais.error)}</p>}
      {enregistrerEssai.isError && <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{messageErreurCatalogueAdmin(enregistrerEssai.error)}</p>}
      {(activerEssai.isError || desactiverEssai.isError || supprimerEssai.isError) && (
        <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurCatalogueAdmin(activerEssai.error ?? desactiverEssai.error ?? supprimerEssai.error)}
        </p>
      )}
      {essais.data?.content.map((essai) => (
        <div key={essai.id} className={`mb-2 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 p-4 transition-colors ${essai.actif ? "hover:bg-surface-card/70" : "bg-slate-100"}`}>
          <button
            type="button"
            aria-label={`Afficher les détails de ${essai.designation}`}
            onClick={() => setDetailId(essai.id)}
            className="min-w-0 flex-1 rounded-lg text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900"
          >
            <p className={`font-semibold ${essai.actif ? "text-ink-900" : "text-muted-foreground"}`}>{essai.code} - {essai.designation}{!essai.actif && <span className="ml-2 text-xs font-medium uppercase">Inactif</span>}</p>
            <p className="text-sm text-muted-foreground">{essai.domaineLibelle} - {essai.tarif} MAD TTC{tva.data ? ` (${libelleTva(essai.tarif, tva.data.tauxEffectif)})` : ""}</p>
          </button>
          <div className="flex flex-wrap gap-1">
            <Button variant="ghost" size="sm" onClick={() => ouvrirModification(essai.id)}>Modifier</Button>
            <Button
              variant={essai.actif ? "ghost" : "primary"}
              size="sm"
              icon={essai.actif ? <PowerOff className="size-4" aria-hidden="true" /> : <Power className="size-4" aria-hidden="true" />}
              onClick={() => essai.actif ? desactiverEssai.mutate(essai.id) : activerEssai.mutate(essai.id)}
            >
              {essai.actif ? "Désactiver" : "Activer"}
            </Button>
            <Button variant="ghost" size="sm" icon={<Trash2 className="size-4" aria-hidden="true" />} onClick={() => setDeleteConfirm(essai.id)}>
              Supprimer
            </Button>
          </div>
        </div>
      ))}

      {essais.data && essais.data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">Page {page + 1} sur {essais.data.totalPages}</p>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
              <ChevronLeft className="size-4" aria-hidden="true" /> Précédent
            </Button>
            <Button variant="ghost" size="sm" disabled={page + 1 >= essais.data.totalPages} onClick={() => setPage((current) => current + 1)}>
              Suivant <ChevronRight className="size-4" aria-hidden="true" />
            </Button>
          </div>
        </div>
      )}

      <EssaiPanneau
        essaiId={detailId}
        open={detailId !== null}
        onOpenChange={(open) => { if (!open) setDetailId(null); }}
        onEdit={ouvrirModification}
      />

      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="form-scrollbar relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6">
            <button type="button" onClick={() => { setFormOpen(false); setSelectedId(null); enregistrerEssai.reset(); }} aria-label="Fermer" className="absolute right-4 top-4 rounded-full p-1 text-muted-foreground hover:bg-surface-card hover:text-ink-900">
              <X className="size-5" aria-hidden="true" />
            </button>
            <h3 className="mb-5 pr-8 text-lg font-semibold">{selectedId ? "Modifier l'essai" : "Nouvel essai"}</h3>
            {!domaines.isLoading && domaines.data && (
              <EssaiForm
                key={selectedId ?? "nouvel-essai"}
                isEditing={selectedId !== null}
                essai={selectedId ? (() => {
                  const essai = essais.data?.content.find((item) => item.id === selectedId);
                  if (!essai) return undefined;
                  return {
                    code: essai.code, designation: essai.designation,
                    description: essai.description ?? undefined, methode: essai.methode ?? undefined,
                    tarif: essai.tarif, dureeEstimee: essai.dureeEstimee ?? undefined,
                    unite: essai.unite ?? undefined, limiteMin: essai.limiteMin ?? undefined,
                    limiteMax: essai.limiteMax ?? undefined,
                    seuilCritiqueMin: essai.seuilCritiqueMin ?? undefined,
                    seuilCritiqueMax: essai.seuilCritiqueMax ?? undefined,
                    domaineId: essai.domaineId ?? undefined,
                  };
                })() : undefined}
                codeEssai={essaiSelectionne?.code}
                exigencesInitiales={essaiSelectionne?.exigences}
                domaines={domaines.data}
                onSubmit={(data, exigences, exigencesSupprimees) => {
                  enregistrerEssai.mutate({ id: selectedId, data, exigences, exigencesSupprimees });
                }}
                onSaveStep={(etape, data, exigences, exigencesSupprimees, fermerApresSauvegarde) => {
                  if (selectedId !== null) {
                    enregistrerEssai.mutate({
                      id: selectedId,
                      data,
                      exigences,
                      exigencesSupprimees,
                      etape,
                      fermerApresSauvegarde,
                    });
                  }
                }}
                onCancel={() => { setFormOpen(false); setSelectedId(null); enregistrerEssai.reset(); }}
                isSubmitting={enregistrerEssai.isPending}
                error={enregistrerEssai.isError ? messageErreurCatalogueAdmin(enregistrerEssai.error) : undefined}
              />
            )}
          </div>
        </div>
      )}

      {deleteConfirm !== null && (
        <ConfirmDialog
          open
          onOpenChange={(open) => { if (!open) setDeleteConfirm(null); }}
          title="Confirmer la suppression"
          description="Êtes-vous sûr de vouloir supprimer cet essai ?"
          onConfirm={() => supprimerEssai.mutate(deleteConfirm)}
          isConfirming={supprimerEssai.isPending}
        />
      )}
    </div>
  );
}
