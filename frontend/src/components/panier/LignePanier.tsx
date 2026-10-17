import { Clock, Trash2 } from "lucide-react";
import type { LignePanier as LignePanierDto } from "@/api/paniers";
import { Badge, Button } from "@/components/lab";
import { formatDuree, formatMontant } from "./format";

/**
 * Une ligne de panier. Une ligne indisponible reste affichée et retirable,
 * mais n'entre pas dans le total (calculé côté serveur).
 */
export function LignePanier({
  ligne,
  enRetrait = false,
  onRetirer,
}: {
  ligne: LignePanierDto;
  enRetrait?: boolean;
  onRetirer: () => void;
}) {
  const boutonRetirer = (
    <Button
      size="sm"
      variant="secondary"
      onClick={onRetirer}
      loading={enRetrait}
      disabled={enRetrait}
      icon={<Trash2 className="size-4" aria-hidden="true" />}
      iconPosition="left"
    >
      Retirer
    </Button>
  );

  if (!ligne.disponible) {
    return (
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-amber-200 bg-amber-50/60 px-4 py-3">
        <div>
          <p className="text-sm font-semibold text-amber-900">Essai indisponible</p>
          <p className="text-xs text-amber-800">
            Cet essai n'est plus proposé et n'est pas compté dans le total.
          </p>
        </div>
        {boutonRetirer}
      </div>
    );
  }

  const duree = formatDuree(ligne.dureeEstimee);

  return (
    <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-bold text-ink-900">{ligne.designation ?? "Essai"}</p>
          {ligne.code ? (
            <span className="text-xs font-semibold tracking-wide text-brand-600 uppercase">
              {ligne.code}
            </span>
          ) : null}
          {ligne.domaineLibelle?.trim() ? (
            <Badge size="sm" variant="mint">
              {ligne.domaineLibelle}
            </Badge>
          ) : null}
        </div>
        {duree ? (
          <p className="mt-1 inline-flex items-center gap-1 text-xs text-muted-foreground">
            <Clock className="size-3.5" aria-hidden="true" />
            {duree}
          </p>
        ) : null}
      </div>
      <div className="flex items-center gap-4">
        <span className="text-sm font-bold text-ink-900">{formatMontant(ligne.tarif)}</span>
        {boutonRetirer}
      </div>
    </div>
  );
}
