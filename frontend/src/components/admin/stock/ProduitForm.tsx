import { useState } from "react";
import { Button } from "@/components/lab";
import type { CreerProduitRequest, ModifierProduitRequest } from "@/api/stockAdmin";

interface ProduitFormProps {
  produit?: CreerProduitRequest | ModifierProduitRequest | undefined;
  referenceProduit?: string | undefined;
  onSubmit: (data: CreerProduitRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  isEditing?: boolean;
  error?: string | undefined;
}

export function ProduitForm({
  produit,
  referenceProduit,
  onSubmit,
  onCancel,
  isSubmitting,
  isEditing = false,
  error,
}: ProduitFormProps) {
  const [formData, setFormData] = useState<CreerProduitRequest>(() => ({
    reference: produit && "reference" in produit ? produit.reference : (referenceProduit ?? ""),
    nom: produit?.nom ?? "",
    unite: produit?.unite,
    seuilMinimal: produit?.seuilMinimal,
    conditionsStockage: produit?.conditionsStockage,
  }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Référence *</label>
        <input
          type="text"
          value={formData.reference}
          onChange={(e) => setFormData({ ...formData, reference: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          disabled={isEditing}
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Nom *</label>
        <input
          type="text"
          value={formData.nom}
          onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Unité</label>
        <input
          type="text"
          value={formData.unite || ""}
          onChange={(e) => setFormData({ ...formData, unite: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Seuil minimal</label>
        <input
          type="number"
          value={formData.seuilMinimal || ""}
          onChange={(e) =>
            setFormData({
              ...formData,
              seuilMinimal: e.target.value ? parseFloat(e.target.value) : undefined,
            })
          }
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Conditions de stockage</label>
        <input
          type="text"
          value={formData.conditionsStockage || ""}
          onChange={(e) => setFormData({ ...formData, conditionsStockage: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
        />
      </div>

      <div className="flex gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={onCancel} type="button">
          Annuler
        </Button>
        <Button variant="primary" size="sm" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Enregistrement..." : "Enregistrer"}
        </Button>
      </div>
      {error && (
        <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
          {error}
        </p>
      )}
    </form>
  );
}
