import { useState } from "react";
import { Button } from "@/components/lab";
import type { EmettreFactureRequest } from "@/api/facturesAdmin";
import type { DemandeFacturableDto } from "@/api/facturesAdmin";

interface EmettreFactureFormProps {
  demandes: DemandeFacturableDto[];
  onSubmit: (data: EmettreFactureRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  isLoading?: boolean;
  error?: string | undefined;
  onRetry?: (() => void) | undefined;
}

function formatMontant(total: number): string {
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(total);
}

export function EmettreFactureForm({
  demandes,
  onSubmit,
  onCancel,
  isSubmitting,
  isLoading,
  error,
  onRetry,
}: EmettreFactureFormProps) {
  const [selectedDemandeId, setSelectedDemandeId] = useState<number | null>(null);
  const [dateEcheance, setDateEcheance] = useState("");
  const demandeSelectionnee = demandes.find((demande) => demande.id === selectedDemandeId);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedDemandeId === null) return;
    onSubmit({
      demandeId: selectedDemandeId,
      dateEcheance: dateEcheance || undefined,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Chargement des demandes facturables…</p>
      ) : error ? (
        <div className="space-y-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800" role="alert">
          <p>{error}</p>
          {onRetry && <button type="button" className="font-semibold underline" onClick={onRetry}>Réessayer</button>}
        </div>
      ) : demandes.length === 0 ? (
        <p className="rounded-xl bg-surface-card p-3 text-sm text-muted-foreground">
          Toutes les demandes acceptées ont déjà une facture.
        </p>
      ) : (
        <>
          <div>
            <label htmlFor="demande-facturable" className="mb-1 block text-sm font-medium">
              Demande à facturer *
            </label>
            <select
              id="demande-facturable"
              value={selectedDemandeId ?? ""}
              onChange={(e) => setSelectedDemandeId(e.target.value ? Number(e.target.value) : null)}
              className="w-full rounded-md border border-brand-900/20 px-3 py-2"
              required
            >
              <option value="">Sélectionner une demande</option>
              {demandes.map((demande) => (
                <option key={demande.id} value={demande.id}>
                  {demande.numero} — {demande.titre} · {demande.clientNom ?? "Client non renseigné"} · {formatMontant(demande.total)}
                </option>
              ))}
            </select>
          </div>

          {demandeSelectionnee && (
            <dl className="grid gap-3 rounded-xl border border-brand-900/10 bg-surface-card p-4 text-sm sm:grid-cols-2">
              <div><dt className="text-muted-foreground">Numéro</dt><dd className="font-semibold">{demandeSelectionnee.numero}</dd></div>
              <div><dt className="text-muted-foreground">Client</dt><dd className="font-semibold">{demandeSelectionnee.clientNom ?? "—"}</dd></div>
              <div><dt className="text-muted-foreground">Intitulé</dt><dd className="font-semibold">{demandeSelectionnee.titre}</dd></div>
              <div><dt className="text-muted-foreground">Total</dt><dd className="font-semibold">{formatMontant(demandeSelectionnee.total)}</dd></div>
            </dl>
          )}

          <div>
            <label htmlFor="date-echeance" className="mb-1 block text-sm font-medium">
              Date d&apos;échéance (optionnelle)
            </label>
            <input
              id="date-echeance"
              type="date"
              value={dateEcheance}
              onChange={(e) => setDateEcheance(e.target.value)}
              className="w-full rounded-md border border-brand-900/20 px-3 py-2"
            />
          </div>
        </>
      )}

      {error && !isLoading && !onRetry && (
        <p className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800" role="alert">{error}</p>
      )}

      <div className="flex gap-2 justify-end">
        <Button variant="ghost" size="sm" onClick={onCancel} type="button">
          Annuler
        </Button>
        <Button variant="primary" size="sm" type="submit" disabled={isSubmitting || selectedDemandeId === null || isLoading || demandes.length === 0}>
          {isSubmitting ? "Émission..." : "Émettre manuellement"}
        </Button>
      </div>
    </form>
  );
}
