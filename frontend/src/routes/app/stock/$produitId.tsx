import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { peut } from "@/api/session";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft, Activity } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Badge, Button } from "@/components/lab";
import { AjusterStockForm } from "@/components/admin/stock/AjusterStockForm";
import { getProduit, listerLots, ajusterStock, messageErreurStockAdmin } from "@/api/stockAdmin";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export const Route = createFileRoute("/app/stock/$produitId")({
  head: () => ({ meta: [{ title: "Détail produit — LabFlow LIMS" }] }),
  component: StockDetail,
});

function StockDetail() {
  const { produitId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedLotId, setSelectedLotId] = useState<number | null>(null);

  const {
    data: produit,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["stock-admin-produit", Number(produitId)],
    queryFn: () => getProduit(Number(produitId)),
  });

  const {
    data: lots,
    isLoading: lotsLoading,
    isError: lotsError,
  } = useQuery({
    queryKey: ["stock-admin-lots", Number(produitId)],
    queryFn: () => listerLots(Number(produitId)),
    enabled: !!produit,
  });

  const ajusterLot = useMutation({
    mutationFn: ({ lotId, data }: { lotId: number; data: Parameters<typeof ajusterStock>[1] }) =>
      ajusterStock(lotId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-lots", Number(produitId)] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-alertes"] });
      setSelectedLotId(null);
    },
  });

  if (isLoading)
    return (
      <Container className="py-8">
        <p>Chargement...</p>
      </Container>
    );
  if (isError)
    return (
      <Container className="py-8">
        <p role="alert" className="text-rose-700">
          Impossible de charger ce produit.
        </p>
      </Container>
    );
  if (!produit)
    return (
      <Container className="py-8">
        <p>Produit introuvable</p>
      </Container>
    );

  return (
    <Container className="py-8">
      <div className="mb-6">
        <Button
          variant="ghost"
          size="sm"
          icon={<ArrowLeft className="size-4" />}
          iconPosition="left"
          onClick={() => navigate({ to: "/app/stock" })}
        >
          Retour
        </Button>
        <div className="mt-4 flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
            <Activity className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">{produit.nom}</h1>
            <p className="text-sm text-muted-foreground">{produit.reference}</p>
          </div>
          <Badge variant={produit.actif ? "mint" : "soft"}>
            {produit.actif ? "Activé" : "Désactivé"}
          </Badge>
        </div>
      </div>

      <div className="space-y-6">
        <div className="rounded-lg border border-brand-900/10 p-6">
          <h2 className="text-lg font-semibold mb-4">Informations</h2>
          <dl className="grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Unité</dt>
              <dd>{produit.unite ?? "—"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Quantité en stock</dt>
              <dd>{produit.stockCourant} article(s)</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Quantité minimale</dt>
              <dd>{produit.seuilMinimal ?? "—"} article(s)</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Conditions de stockage</dt>
              <dd>{produit.conditionsStockage ?? "—"}</dd>
            </div>
          </dl>
        </div>

        <div className="rounded-lg border border-brand-900/10 p-6">
          <h2 className="text-lg font-semibold mb-4">Lots ({lots?.length ?? 0})</h2>
          {lotsLoading && <p className="text-sm text-muted-foreground">Chargement des lots...</p>}
          {lotsError && (
            <p role="alert" className="text-sm text-rose-700">
              Impossible de charger les lots de ce produit.
            </p>
          )}
          {lots && lots.length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun lot pour ce produit.</p>
          )}
          {lots && lots.length > 0 && (
            <table className="w-full border-collapse">
              <thead>
                <tr>
                  <th scope="col" className="px-4 py-2 text-left text-sm font-semibold">
                    Numéro
                  </th>
                  <th scope="col" className="px-4 py-2 text-right text-sm font-semibold">
                    Quantité
                  </th>
                  <th scope="col" className="px-4 py-2 text-left text-sm font-semibold">
                    Péremption
                  </th>
                  <th scope="col" className="px-4 py-2 text-left text-sm font-semibold">
                    Statut
                  </th>
                  <th scope="col" className="px-4 py-2 text-right text-sm font-semibold">
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {lots.map((lot) => (
                  <tr key={lot.id} className="border-t border-brand-900/10">
                    <td className="px-4 py-2 text-sm">{lot.numero}</td>
                    <td className="px-4 py-2 text-sm text-right">{lot.quantite}</td>
                    <td className="px-4 py-2 text-sm">{lot.peremption}</td>
                    <td className="px-4 py-2 text-sm">{lot.statut}</td>
                    <td className="px-4 py-2 text-right">
                      {peut("STOCK_GERER") ? (
                        <Button variant="ghost" size="sm" onClick={() => setSelectedLotId(lot.id)}>
                          Ajuster
                        </Button>
                      ) : (
                        <span className="text-xs text-muted-foreground">Consultation</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Dialog
            open={selectedLotId !== null}
            onOpenChange={(open) => {
              if (!open) {
                setSelectedLotId(null);
                ajusterLot.reset();
              }
            }}
          >
            <DialogContent className="max-h-[90vh] overflow-y-auto rounded-3xl border-brand-900/10 bg-card sm:max-w-xl">
              <DialogHeader className="pr-8">
                <DialogTitle>
                  Ajuster le lot {lots?.find((lot) => lot.id === selectedLotId)?.numero}
                </DialogTitle>
                <DialogDescription>
                  Choisissez le type d’ajustement, la quantité et indiquez le motif.
                </DialogDescription>
              </DialogHeader>
              {selectedLotId && lots && (
                <AjusterStockForm
                  key={selectedLotId}
                  lotId={selectedLotId}
                  stockCourant={lots.find((lot) => lot.id === selectedLotId)?.quantite ?? 0}
                  onCancel={() => {
                    setSelectedLotId(null);
                    ajusterLot.reset();
                  }}
                  onSubmit={(data) => ajusterLot.mutate({ lotId: selectedLotId, data })}
                  isSubmitting={ajusterLot.isPending}
                />
              )}
              {ajusterLot.isError && (
                <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
                  {messageErreurStockAdmin(ajusterLot.error)}
                </p>
              )}
            </DialogContent>
          </Dialog>
        </div>
      </div>
    </Container>
  );
}
