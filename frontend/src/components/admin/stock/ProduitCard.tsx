import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Trash2, Power, PowerOff, ArrowUpRight } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button } from "@/components/lab";
import * as stockAdmin from "@/api/stockAdmin";
import { messageErreurStockAdmin } from "@/api/stockAdmin";
import { CreerLotForm } from "./CreerLotForm";
import { ConfirmDialog } from "../catalogue/ConfirmDialog";
import type { ProduitDto } from "@/api/stockAdmin";

interface ProduitCardProps {
  produit: ProduitDto;
  onEdit: (id: number) => void;
  onDetails: (id: number) => void;
}

export function ProduitCard({ produit, onEdit, onDetails }: ProduitCardProps) {
  const queryClient = useQueryClient();
  const [lotFormOpen, setLotFormOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const lots = useQuery({
    queryKey: ["stock-admin-lots", produit.id],
    queryFn: () => stockAdmin.listerLots(produit.id),
  });

  const supprimerProduit = useMutation({
    mutationFn: () => stockAdmin.supprimerProduit(produit.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-alertes"] });
      setDeleteConfirm(null);
    },
    onError: (error) => {
      toast.error(messageErreurStockAdmin(error), { duration: 4000 });
    },
  });

  const creerLot = useMutation({
    mutationFn: stockAdmin.creerLot,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-lots", produit.id] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-alertes"] });
      setLotFormOpen(false);
    },
  });

  const basculerStatut = useMutation({
    mutationFn: () => stockAdmin.modifierProduit(produit.id, { actif: !produit.actif }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-alertes"] });
    },
  });

  return (
    <div
      className={`mb-3 rounded-2xl border border-brand-900/10 p-4 ${produit.actif ? "bg-card" : "bg-surface-card"}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-4">
        <button
          type="button"
          className="min-w-0 flex-1 text-left"
          onClick={() => onDetails(produit.id)}
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Référence : {produit.reference}
              </p>
              <p
                className={`font-semibold ${produit.actif ? "text-ink-900" : "text-muted-foreground"}`}
              >
                {produit.nom}
              </p>
            </div>
            <Badge variant={produit.actif ? "mint" : "soft"} size="sm">
              {produit.actif ? "Activé" : "Désactivé"}
            </Badge>
          </div>
          <dl className="mt-3 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Quantité en stock</dt>
              <dd className="font-medium text-ink-900">{produit.stockCourant} article(s)</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Quantité minimale</dt>
              <dd className="font-medium text-ink-900">{produit.seuilMinimal ?? "—"} article(s)</dd>
            </div>
          </dl>
          <p className="mt-1 inline-flex items-center gap-1 text-sm font-medium text-brand-900">
            {lots.isLoading ? "Chargement des lots..." : `${lots.data?.length ?? 0} lot(s)`} ·
            Détails
            <ArrowUpRight className="size-4" aria-hidden="true" />
          </p>
        </button>
        <div className="flex flex-wrap gap-2">
          <Button variant="ghost" size="sm" onClick={() => onEdit(produit.id)}>
            Modifier
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setLotFormOpen(true)}>
            + Lot
          </Button>
          <Button
            variant={produit.actif ? "ghost" : "primary"}
            size="sm"
            icon={
              produit.actif ? (
                <PowerOff className="size-4" aria-hidden="true" />
              ) : (
                <Power className="size-4" aria-hidden="true" />
              )
            }
            onClick={() => basculerStatut.mutate()}
            disabled={basculerStatut.isPending}
          >
            {produit.actif ? "Désactiver" : "Activer"}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            icon={<Trash2 className="size-4" aria-hidden="true" />}
            onClick={() => setDeleteConfirm(produit.id)}
            disabled={lots.isLoading || lots.isError}
          >
            Supprimer
          </Button>
        </div>
      </div>
      {(basculerStatut.isError || creerLot.isError || lots.isError) && (
        <p role="alert" className="mt-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {messageErreurStockAdmin(basculerStatut.error ?? creerLot.error ?? lots.error)}
        </p>
      )}
      {deleteConfirm === produit.id && (
        <ConfirmDialog
          open={true}
          onOpenChange={(open) => !open && setDeleteConfirm(null)}
          title="Supprimer le produit"
          description={
            lots.data && lots.data.length > 0
              ? "La suppression sera refusée si ce produit possède des lots."
              : "Voulez-vous vraiment supprimer ce produit ?"
          }
          onConfirm={() => supprimerProduit.mutate()}
          confirmLabel="Supprimer"
          isConfirming={supprimerProduit.isPending}
        />
      )}
      {lotFormOpen && (
        <CreerLotForm
          produitId={produit.id}
          onCancel={() => setLotFormOpen(false)}
          onSubmit={(data) => creerLot.mutate(data)}
          isSubmitting={creerLot.isPending}
        />
      )}
    </div>
  );
}
