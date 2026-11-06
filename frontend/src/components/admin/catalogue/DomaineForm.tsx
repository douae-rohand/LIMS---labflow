import { useState } from "react";
import { Button } from "@/components/lab";
import type { CreerDomaineRequest, ModifierDomaineRequest } from "@/api/catalogueAdmin";

interface DomaineFormProps {
  domaine?: CreerDomaineRequest | ModifierDomaineRequest | undefined;
  onSubmit: (data: CreerDomaineRequest | ModifierDomaineRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  error?: string | undefined;
}

export function DomaineForm({ domaine, onSubmit, onCancel, isSubmitting, error }: DomaineFormProps) {
  const [formData, setFormData] = useState<CreerDomaineRequest | ModifierDomaineRequest>(
    domaine || { code: "", libelle: "" }
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Code *</label>
        <input
          type="text"
          value={"code" in formData ? formData.code : ""}
          onChange={(e) => setFormData({ ...formData, code: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          disabled={!("code" in formData)}
          required
          maxLength={50}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Libellé *</label>
        <input
          type="text"
          value={formData.libelle}
          onChange={(e) => setFormData({ ...formData, libelle: e.target.value })}
          className="w-full px-3 py-2 rounded-md border border-brand-900/20"
          required
          maxLength={100}
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
