import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/lab";
import * as catalogueAdmin from "@/api/catalogueAdmin";
import { messageErreurCatalogueAdmin } from "@/api/catalogueAdmin";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { ExigenceForm } from "./ExigenceForm";
import { ConfirmDialog } from "./ConfirmDialog";

interface ExigencesTabProps {
  essaiId: number;
}

export function ExigencesTab({ essaiId }: ExigencesTabProps) {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const exigences = useQuery({
    queryKey: ["catalogue-admin-exigences", essaiId],
    queryFn: () => catalogueAdmin.listerExigences(essaiId),
  });

  const creerExigence = useMutation({
    mutationFn: ({ essaiId: id, data }: { essaiId: number; data: catalogueAdmin.CreerExigenceRequest }) =>
      catalogueAdmin.creerExigence(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-exigences"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essai", essaiId] });
      setFormOpen(false);
    },
  });

  const modifierExigence = useMutation({
    mutationFn: ({ id, data }: { id: number; data: catalogueAdmin.ModifierExigenceRequest }) =>
      catalogueAdmin.modifierExigence(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-exigences", essaiId] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essai", essaiId] });
      setFormOpen(false);
      setSelectedId(null);
    },
  });

  const supprimerExigence = useMutation({
    mutationFn: catalogueAdmin.supprimerExigence,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-exigences"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essais"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-essai", essaiId] });
      setDeleteConfirm(null);
    },
  });
  const exigenceSelectionnee = exigences.data?.find((exigence) => exigence.id === detailId);

  const modifierDepuisDetail = (id: number) => {
    setDetailId(null);
    setSelectedId(id);
    setFormOpen(true);
  };

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-lg font-semibold">Exigences de l'essai #{essaiId}</h2>
        <Button variant="primary" size="sm" icon={<Plus className="size-4" />} onClick={() => setFormOpen(true)}>
          Nouvelle exigence
        </Button>
      </div>
      {exigences.isLoading && <p className="py-4 text-sm text-muted-foreground">Chargement des exigences...</p>}
      {exigences.isError && <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{messageErreurCatalogueAdmin(exigences.error)}</p>}
      {(creerExigence.isError || modifierExigence.isError || supprimerExigence.isError) && (
        <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurCatalogueAdmin(creerExigence.error ?? modifierExigence.error ?? supprimerExigence.error)}
        </p>
      )}
      {exigences.data?.map((exigence) => (
        <div key={exigence.id} className="border border-brand-900/10 rounded-lg p-4 mb-2 flex justify-between items-center">
          <button type="button" className="min-w-0 flex-1 text-left" onClick={() => setDetailId(exigence.id)}>
            <p className="font-semibold">{exigence.nature}</p>
            <p className="text-sm text-muted-foreground">Conservation : {exigence.etatConservation ?? "—"}</p>
            <p className="text-sm text-muted-foreground">Quantité minimale : {exigence.quantiteMinimale ?? "—"} {exigence.unite ?? ""}</p>
          </button>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" onClick={() => modifierDepuisDetail(exigence.id)}>
              Modifier
            </Button>
            <Button
              variant="ghost"
              size="sm"
              icon={<Trash2 className="size-4" />}
              onClick={() => setDeleteConfirm(exigence.id)}
            >
              Supprimer
            </Button>
          </div>
        </div>
      ))}

      {formOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="relative bg-white rounded-lg p-6 max-w-md w-full">
            <button type="button" onClick={() => { setFormOpen(false); setSelectedId(null); }} aria-label="Fermer" className="absolute right-4 top-4 rounded-full p-1 text-muted-foreground hover:bg-surface-card hover:text-ink-900">
              <X className="size-5" aria-hidden="true" />
            </button>
            <h3 className="text-lg font-semibold mb-4">{selectedId ? "Modifier l'exigence" : "Nouvelle exigence"}</h3>
            <ExigenceForm
              exigence={selectedId ? (() => {
                const e = exigences.data?.find(e => e.id === selectedId);
                if (!e) return undefined;
                return {
                  nature: e.nature,
                  etatConservation: e.etatConservation ?? "",
                  quantiteMinimale: e.quantiteMinimale ?? undefined,
                  unite: e.unite ?? undefined,
                };
              })() : undefined}
              onSubmit={(data) => {
                if (selectedId) {
                  modifierExigence.mutate({ id: selectedId, data });
                } else {
                  creerExigence.mutate({ essaiId, data: data as catalogueAdmin.CreerExigenceRequest });
                }
              }}
              onCancel={() => { setFormOpen(false); setSelectedId(null); }}
              isSubmitting={creerExigence.isPending || modifierExigence.isPending}
              error={creerExigence.isError ? messageErreurCatalogueAdmin(creerExigence.error) : modifierExigence.isError ? messageErreurCatalogueAdmin(modifierExigence.error) : undefined}
            />
          </div>
        </div>
      )}

      <Sheet open={detailId !== null} onOpenChange={(open) => { if (!open) setDetailId(null); }}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-xl">
          <SheetHeader className="pr-8 text-left">
            <SheetTitle>{exigenceSelectionnee?.nature ?? "Détail de l'exigence"}</SheetTitle>
            <SheetDescription>Exigence propre à l'essai sélectionné</SheetDescription>
          </SheetHeader>
          {exigenceSelectionnee && (
            <div className="mt-6 space-y-4">
              <section className="rounded-2xl border border-violet-200 bg-violet-50/70 p-4">
                <h3 className="mb-4 font-semibold text-violet-900">Détails de l'échantillon</h3>
                <dl className="space-y-4">
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Nature</dt>
                    <dd className="mt-1 text-sm text-ink-900">{exigenceSelectionnee.nature}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Conservation</dt>
                    <dd className="mt-1 text-sm text-ink-900">{exigenceSelectionnee.etatConservation ?? "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Quantité minimale</dt>
                    <dd className="mt-1 text-sm text-ink-900">{exigenceSelectionnee.quantiteMinimale ?? "—"} {exigenceSelectionnee.unite ?? ""}</dd>
                  </div>
                </dl>
              </section>
              <div className="flex justify-end">
                <Button type="button" variant="primary" onClick={() => modifierDepuisDetail(exigenceSelectionnee.id)}>Modifier l'exigence</Button>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>

      {deleteConfirm && (
        <ConfirmDialog
          open={!!deleteConfirm}
          onOpenChange={(open) => !open && setDeleteConfirm(null)}
          title="Confirmer la suppression"
          description="Êtes-vous sûr de vouloir supprimer cette exigence ?"
          onConfirm={() => supprimerExigence.mutate(deleteConfirm)}
          isConfirming={supprimerExigence.isPending}
        />
      )}
    </div>
  );
}
