import { useState } from "react";
import { Button } from "@/components/lab";
import type { CreerExigenceRequest, ModifierExigenceRequest } from "@/api/catalogueAdmin";

interface ExigenceFormProps {
  exigence?: CreerExigenceRequest | ModifierExigenceRequest | undefined;
  onSubmit: (data: CreerExigenceRequest | ModifierExigenceRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  error?: string | undefined;
}

export function ExigenceForm({ exigence, onSubmit, onCancel, isSubmitting, error }: ExigenceFormProps) {
  const [formData, setFormData] = useState<CreerExigenceRequest | ModifierExigenceRequest>(
    exigence || { nature: "", etatConservation: "" }
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Nature *</label>
        <input
          type="text"
          value={formData.nature ?? ""}
          onChange={(e) => setFormData({ ...formData, nature: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
          maxLength={100}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">État de conservation</label>
        <input
          type="text"
          value={formData.etatConservation || ""}
          onChange={(e) => setFormData({ ...formData, etatConservation: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
          maxLength={255}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Quantité minimale</label>
        <input
          type="number"
          min="0"
          value={formData.quantiteMinimale ?? ""}
          onChange={(e) => setFormData({ ...formData, quantiteMinimale: e.target.value ? parseFloat(e.target.value) : undefined })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
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

      <div className="flex gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={onCancel} type="button">
          Annuler
        </Button>
        <Button variant="primary" size="sm" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Enregistrement..." : "Enregistrer"}
        </Button>
      </div>
      {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    </form>
  );
}
