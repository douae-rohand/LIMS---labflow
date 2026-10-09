import type { ReactNode } from "react";
import {
  AlertTriangle,
  Check,
  CornerUpLeft,
  FileSignature,
  MessageSquare,
  Sparkles,
  X,
} from "lucide-react";
import { Badge, Skeleton } from "@/components/lab";
import type { LaboratoirePublic } from "@/api/landing";
import { cn } from "@/lib/utils";

function Frame({
  title,
  children,
  className,
}: {
  title: string;
  children: ReactNode;
  className?: string | undefined;
}) {
  return (
    <div
      className={cn(
        "glass-card glossy-top overflow-hidden rounded-3xl",
        className,
      )}
    >
      <div className="flex items-center gap-2 border-b border-brand-900/10 px-4 py-3">
        <span className="size-2.5 rounded-full bg-brand-900/20" />
        <span className="size-2.5 rounded-full bg-brand-900/20" />
        <span className="size-2.5 rounded-full bg-accent-lime" />
        <span className="ml-2 text-xs font-semibold text-brand-900">{title}</span>
      </div>
      <div className="p-4">{children}</div>
    </div>
  );
}

type DashboardMockupProps = {
  className?: string | undefined;
  loading?: boolean | undefined;
  nombreLaboratoires?: number | undefined;
  nombreRoles?: number | undefined;
  laboratoires?: LaboratoirePublic[] | undefined;
  statutsDemande?: string[] | undefined;
};

export function DashboardMockup({
  className,
  loading = false,
  nombreLaboratoires,
  nombreRoles,
  laboratoires = [],
  statutsDemande = [],
}: DashboardMockupProps) {
  return (
    <Frame title="Données plateforme" className={className}>
      {loading ? (
        <div className="flex flex-col gap-3" aria-busy="true">
          <div className="grid grid-cols-2 gap-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
          <Skeleton className="h-24 w-full" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-surface-card p-3">
              <p className="text-lg font-extrabold text-brand-900">
                {nombreLaboratoires ?? "—"}
              </p>
              <p className="text-[11px] text-muted-foreground">Laboratoires actifs</p>
            </div>
            <div className="rounded-2xl bg-surface-card p-3">
              <p className="text-lg font-extrabold text-brand-900">
                {nombreRoles ?? "—"}
              </p>
              <p className="text-[11px] text-muted-foreground">Rôles</p>
            </div>
          </div>
          <div className="mt-3 rounded-2xl bg-surface-alt p-3">
            <p className="text-[11px] font-semibold text-brand-900">
              Laboratoires
            </p>
            {laboratoires.length === 0 ? (
              <p className="mt-2 text-xs text-muted-foreground">
                Aucun laboratoire actif.
              </p>
            ) : (
              <ul className="mt-2 flex flex-col gap-2">
                {laboratoires.slice(0, 3).map((lab) => (
                  <li
                    key={lab.id}
                    className="flex items-center justify-between rounded-xl bg-card px-3 py-2 text-xs"
                  >
                    <span className="font-semibold text-ink-900">{lab.raisonSociale}</span>
                    <Badge size="sm" variant="mint">
                      {lab.ville?.trim() ? lab.ville : lab.code}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </div>
          {statutsDemande.length > 0 ? (
            <div className="mt-3 flex flex-wrap gap-1.5">
              {statutsDemande.map((statut) => (
                <Badge key={statut} size="sm" variant="soft">
                  {statut}
                </Badge>
              ))}
            </div>
          ) : null}
        </>
      )}
    </Frame>
  );
}

export function ValidationMockup({ className }: { className?: string }) {
  return (
    <Frame title="Aperçu — écran de validation" className={className}>
      <div className="rounded-2xl bg-surface-card p-3 text-xs">
        {[
          ["Résultat dans les seuils", "Conforme"],
          ["Valeur hors seuil", "À contrôler"],
          ["Pièce jointe", "Jointe"],
        ].map(([n, v]) => (
          <div
            key={n}
            className="flex items-center justify-between border-b border-brand-900/10 py-2 last:border-0"
          >
            <span className="text-ink-900">{n}</span>
            <span className="font-semibold text-ink-900">{v}</span>
          </div>
        ))}
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2 text-[11px] font-semibold">
        <span className="flex items-center justify-center gap-1 rounded-full bg-brand-900 py-2 text-primary-foreground">
          <Check className="size-3" aria-hidden="true" /> Valider
        </span>
        <span className="flex items-center justify-center gap-1 rounded-full bg-mint-200 py-2 text-brand-900">
          <CornerUpLeft className="size-3" aria-hidden="true" /> Retourner
        </span>
        <span className="flex items-center justify-center gap-1 rounded-full border border-brand-900/20 py-2 text-brand-900">
          <X className="size-3" aria-hidden="true" /> Rejeter
        </span>
      </div>
      <div className="mt-3 flex items-center gap-2 rounded-2xl bg-accent-lime/60 p-3 text-xs text-ink-900">
        <FileSignature className="size-4" aria-hidden="true" />
        Signature électronique du responsable
      </div>
    </Frame>
  );
}

export function AiMockup({ className }: { className?: string }) {
  return (
    <Frame title="Aperçu — assistant" className={className}>
      <div className="flex flex-col gap-2 text-xs">
        <div className="ml-auto max-w-[80%] rounded-2xl rounded-br-md bg-brand-900 px-3 py-2 text-primary-foreground">
          Quelle est la procédure de cette méthode ?
        </div>
        <div className="max-w-[85%] rounded-2xl rounded-bl-md bg-surface-card px-3 py-2 text-ink-900">
          <MessageSquare className="mr-1 inline size-3 text-brand-600" aria-hidden="true" />
          Réponse sourcée dans les modes opératoires du laboratoire, soumise à
          validation humaine.
        </div>
      </div>
      <div className="mt-3 rounded-2xl bg-surface-alt p-3">
        <div className="flex items-center justify-between text-[11px]">
          <span className="font-semibold text-brand-900">Détection d'anomalie</span>
          <AlertTriangle className="size-3 text-brand-600" aria-hidden="true" />
        </div>
        <p className="mt-2 flex items-center gap-1 text-[10px] text-muted-foreground">
          <Sparkles className="size-3" aria-hidden="true" /> Suggestion soumise à
          validation humaine
        </p>
      </div>
    </Frame>
  );
}
