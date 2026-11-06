import { useState } from "react";
import { Button } from "@/components/lab";
import type { AjusterStockRequest } from "@/api/stockAdmin";

interface AjusterStockFormProps {
  lotId: number;
  stockCourant: number;
  onSubmit: (data: AjusterStockRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export function AjusterStockForm({ lotId, stockCourant, onSubmit, onCancel, isSubmitting }: AjusterStockFormProps) {
  const [formData, setFormData] = useState<AjusterStockRequest>({
    type: "SORTIE",
    quantite: 0,
    motif: "",
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Type d'ajustement *</label>
        <select
          value={formData.type}
          onChange={(e) => setFormData({ ...formData, type: e.target.value as any })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
        >
          <option value="SORTIE">Sortie</option>
          <option value="PERTE">Perte</option>
          <option value="AJUSTEMENT">Ajustement</option>
        </select>
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
        <p className="text-xs text-muted-foreground mt-1">Stock actuel : {stockCourant}</p>
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Motif *</label>
        <textarea
          value={formData.motif}
          onChange={(e) => setFormData({ ...formData, motif: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          rows={3}
          required
        />
      </div>

      <div className="flex gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={onCancel} type="button">
          Annuler
        </Button>
        <Button variant="primary" size="sm" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Ajustement..." : "Ajuster"}
        </Button>
      </div>
    </form>
  );
}
