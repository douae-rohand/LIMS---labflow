import { useEffect, useState } from "react";
import { peut } from "@/api/session";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { Button } from "@/components/lab";
import * as stockAdmin from "@/api/stockAdmin";
import { messageErreurStockAdmin } from "@/api/stockAdmin";
import { ExportButton } from "@/components/admin/exports/ExportButton";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ProduitForm } from "./ProduitForm";
import { ProduitCard } from "./ProduitCard";

const PAGE_SIZE = 2;

export function ProduitsTab() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);

  const produits = useQuery({
    queryKey: ["stock-admin-produits", page, PAGE_SIZE],
    queryFn: () => stockAdmin.listerProduits(page, PAGE_SIZE),
  });

  const creerProduit = useMutation({
    mutationFn: stockAdmin.creerProduit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      setPage(0);
      setFormOpen(false);
      setSelectedId(null);
    },
  });

  const modifierProduit = useMutation({
    mutationFn: ({ id, data }: { id: number; data: stockAdmin.ModifierProduitRequest }) =>
      stockAdmin.modifierProduit(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      setFormOpen(false);
      setSelectedId(null);
    },
  });

  const handleEdit = (id: number) => {
    setSelectedId(id);
    setFormOpen(true);
  };

  useEffect(() => {
    if (produits.data && page >= Math.max(produits.data.totalPages, 1)) {
      setPage(Math.max(produits.data.totalPages - 1, 0));
    }
  }, [produits.data, page]);

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Produits</h2>
        <div className="flex flex-wrap items-center gap-2">
          <ExportButton jeu="produits" />
          {peut("STOCK_GERER") && (
            <Button
              variant="primary"
              size="sm"
              icon={<Plus className="size-4" />}
              onClick={() => {
                setSelectedId(null);
                setFormOpen(true);
              }}
            >
              Nouveau produit
            </Button>
          )}
        </div>
      </div>
      {produits.isLoading && (
        <p className="py-6 text-sm text-muted-foreground">Chargement des produits...</p>
      )}
      {produits.isError && (
        <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurStockAdmin(produits.error)}
        </p>
      )}
      {(creerProduit.isError || modifierProduit.isError) && (
        <p role="alert" className="mb-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurStockAdmin(creerProduit.error ?? modifierProduit.error)}
        </p>
      )}
      {produits.data?.content.length === 0 && (
        <p className="py-6 text-sm text-muted-foreground">Aucun produit enregistré.</p>
      )}
      {produits.data?.content.map((produit) => (
        <ProduitCard
          key={produit.id}
          produit={produit}
          onEdit={handleEdit}
          onDetails={(id) =>
            navigate({ to: "/app/stock/$produitId", params: { produitId: String(id) } })
          }
        />
      ))}
      {produits.data && produits.data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} sur {produits.data.totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="ghost"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
              <ChevronLeft className="size-4" aria-hidden="true" /> Précédent
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={page + 1 >= produits.data.totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Suivant <ChevronRight className="size-4" aria-hidden="true" />
            </Button>
          </div>
        </div>
      )}
      <Dialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) {
            setSelectedId(null);
            creerProduit.reset();
            modifierProduit.reset();
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto rounded-3xl border-brand-900/10 bg-card sm:max-w-xl">
          <DialogHeader className="pr-8">
            <DialogTitle>{selectedId ? "Modifier le produit" : "Nouveau produit"}</DialogTitle>
            <DialogDescription>Renseignez les informations du produit de stock.</DialogDescription>
          </DialogHeader>
          <ProduitForm
            key={selectedId ?? "nouveau-produit"}
            produit={
              selectedId
                ? (() => {
                    const p = produits.data?.content.find((p) => p.id === selectedId);
                    if (!p) return undefined;
                    return {
                      nom: p.nom,
                      unite: p.unite || undefined,
                      seuilMinimal: p.seuilMinimal ?? undefined,
                      conditionsStockage: p.conditionsStockage || undefined,
                    };
                  })()
                : undefined
            }
            referenceProduit={
              selectedId
                ? produits.data?.content.find((p) => p.id === selectedId)?.reference
                : undefined
            }
            isEditing={selectedId !== null}
            onCancel={() => {
              setFormOpen(false);
              setSelectedId(null);
            }}
            onSubmit={(data) => {
              if (selectedId) {
                modifierProduit.mutate({
                  id: selectedId,
                  data: {
                    nom: data.nom,
                    unite: data.unite,
                    seuilMinimal: data.seuilMinimal,
                    conditionsStockage: data.conditionsStockage,
                  },
                });
              } else {
                creerProduit.mutate(data);
              }
            }}
            isSubmitting={creerProduit.isPending || modifierProduit.isPending}
            error={
              creerProduit.isError
                ? messageErreurStockAdmin(creerProduit.error)
                : modifierProduit.isError
                  ? messageErreurStockAdmin(modifierProduit.error)
                  : undefined
            }
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
