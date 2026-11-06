import { useState } from "react";
import { Button } from "@/components/lab";
import type { CreerLotRequest } from "@/api/stockAdmin";

interface CreerLotFormProps {
  produitId: number;
  onSubmit: (data: CreerLotRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export function CreerLotForm({ produitId, onSubmit, onCancel, isSubmitting }: CreerLotFormProps) {
  const [formData, setFormData] = useState<CreerLotRequest>({
    produitId,
    numero: "",
    quantite: 0,
    peremption: "",
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Numéro de lot *</label>
        <input
          type="text"
          value={formData.numero}
          onChange={(e) => setFormData({ ...formData, numero: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Quantité *</label>
        <input
          type="number"
          value={formData.quantite}
          onChange={(e) => setFormData({ ...formData, quantite: parseFloat(e.target.value) || 0 })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
          min="0"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Date de péremption *</label>
        <input
          type="date"
          value={formData.peremption}
          onChange={(e) => setFormData({ ...formData, peremption: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
        />
      </div>

      <div className="flex gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={onCancel} type="button">
          Annuler
        </Button>
        <Button variant="primary" size="sm" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Création..." : "Créer"}
        </Button>
      </div>
    </form>
  );
}
