import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/lab";
import * as catalogueAdmin from "@/api/catalogueAdmin";
import { messageErreurCatalogueAdmin } from "@/api/catalogueAdmin";
import { DomaineForm } from "./DomaineForm";
import { ConfirmDialog } from "./ConfirmDialog";

export function DomainesTab() {
  const PAGE_SIZE = 5;
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const domaines = useQuery({
    queryKey: ["catalogue-admin-domaines"],
    queryFn: catalogueAdmin.listerDomaines,
  });

  const creerDomaine = useMutation({
    mutationFn: catalogueAdmin.creerDomaine,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
      setFormOpen(false);
      setSelectedId(null);
    },
  });
  const modifierDomaine = useMutation({
    mutationFn: ({ id, data }: { id: number; data: catalogueAdmin.ModifierDomaineRequest }) =>
      catalogueAdmin.modifierDomaine(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
      setFormOpen(false);
      setSelectedId(null);
    },
  });

  const supprimerDomaine = useMutation({
    mutationFn: catalogueAdmin.supprimerDomaine,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
      if (page > 0 && (domaines.data?.length ?? 0) - 1 <= page * PAGE_SIZE) {
        setPage((current) => current - 1);
      }
      setDeleteConfirm(null);
    },
    onError: (err) => {
      const msg = messageErreurCatalogueAdmin(err);
      if (msg.includes("DOMAINE_NON_VIDE")) {
        alert("Ce domaine contient des essais. Supprimez d'abord les essais.");
      }
    },
  });

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-lg font-semibold">Domaines</h2>
        <Button variant="primary" size="sm" icon={<Plus className="size-4" />} onClick={() => setFormOpen(true)}>
          Nouveau domaine
        </Button>
      </div>
      {domaines.data?.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map((domaine) => (
        <div key={domaine.id} className="border border-brand-900/10 rounded-lg p-4 mb-2 flex justify-between items-center">
          <div>
            <p className="font-semibold">{domaine.code} - {domaine.libelle}</p>
          </div>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" onClick={() => { setSelectedId(domaine.id); setFormOpen(true); }}>
              Modifier
            </Button>
            <Button
              variant="ghost"
              size="sm"
              icon={<Trash2 className="size-4" />}
              onClick={() => setDeleteConfirm(domaine.id)}
            >
              Supprimer
            </Button>
          </div>
        </div>
      ))}
      {(creerDomaine.isError || modifierDomaine.isError || supprimerDomaine.isError) && (
        <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurCatalogueAdmin(creerDomaine.error ?? modifierDomaine.error ?? supprimerDomaine.error)}
        </p>
      )}

      {domaines.data && domaines.data.length > PAGE_SIZE && (
        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} sur {Math.ceil(domaines.data.length / PAGE_SIZE)}
          </p>
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
              <ChevronLeft className="size-4" aria-hidden="true" /> Précédent
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={(page + 1) * PAGE_SIZE >= domaines.data.length}
              onClick={() => setPage((current) => current + 1)}
            >
              Suivant <ChevronRight className="size-4" aria-hidden="true" />
            </Button>
          </div>
        </div>
      )}

      {formOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="relative bg-white rounded-lg p-6 max-w-md w-full">
            <button type="button" onClick={() => { setFormOpen(false); setSelectedId(null); }} aria-label="Fermer" className="absolute right-4 top-4 rounded-full p-1 text-muted-foreground hover:bg-surface-card hover:text-ink-900">
              <X className="size-5" aria-hidden="true" />
            </button>
            <h3 className="text-lg font-semibold mb-4">{selectedId ? "Modifier le domaine" : "Nouveau domaine"}</h3>
            <DomaineForm
              domaine={selectedId ? domaines.data?.find(d => d.id === selectedId) : undefined}
              onSubmit={(data) => {
                if (selectedId) {
                  modifierDomaine.mutate({ id: selectedId, data: { libelle: data.libelle } });
                } else {
                  creerDomaine.mutate(data as catalogueAdmin.CreerDomaineRequest);
                }
              }}
              onCancel={() => { setFormOpen(false); setSelectedId(null); }}
              isSubmitting={creerDomaine.isPending || modifierDomaine.isPending}
              error={creerDomaine.isError ? messageErreurCatalogueAdmin(creerDomaine.error) : modifierDomaine.isError ? messageErreurCatalogueAdmin(modifierDomaine.error) : undefined}
            />
          </div>
        </div>
      )}

      {deleteConfirm && (
        <ConfirmDialog
          open={!!deleteConfirm}
          onOpenChange={(open) => !open && setDeleteConfirm(null)}
          title="Confirmer la suppression"
          description="Êtes-vous sûr de vouloir supprimer ce domaine ?"
          onConfirm={() => supprimerDomaine.mutate(deleteConfirm)}
          isConfirming={supprimerDomaine.isPending}
        />
      )}
    </div>
  );
}
