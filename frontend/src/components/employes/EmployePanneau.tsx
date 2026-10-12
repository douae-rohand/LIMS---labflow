import { useQuery } from "@tanstack/react-query";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/lab";
import { StatutEmployeBadge } from "./StatutEmployeBadge";
import { LIBELLE_ROLE, formatDate, formatDateTime } from "./dateUtils";
import { trouverEmploye, messageErreurApi } from "@/api/employes";
import { ApiError } from "@/api/axios";
import { Mail, Clock, AlertCircle, RefreshCw, X } from "lucide-react";

interface EmployePanneauProps {
  employeId: number | undefined;
  onClose: () => void;
}

export function EmployePanneau({ employeId, onClose }: EmployePanneauProps) {
  const estOuvert = employeId !== undefined;

  const query = useQuery({
    queryKey: ["employe", employeId],
    queryFn: () => {
      if (employeId === undefined) {
        throw new Error("Identifiant d'employé manquant");
      }
      return trouverEmploye(employeId);
    },
    enabled: estOuvert,
    staleTime: 30_000,
    retry: (failureCount, error) => {
      // Aucun réessai si 404 (employé introuvable)
      if (error instanceof ApiError && error.status === 404) {
        return false;
      }
      return failureCount < 1;
    },
    refetchOnWindowFocus: false,
  });

  const emp = query.data;
  const is404 = query.error instanceof ApiError && query.error.status === 404;

  // Restauration explicite du focus vers le bouton de la ligne ouverte à la fermeture du Sheet
  function handleOpenChange(open: boolean) {
    if (!open) {
      if (employeId !== undefined) {
        const idCible = `btn-employe-${employeId}`;
        setTimeout(() => {
          const el = document.getElementById(idCible);
          if (el) {
            el.focus();
          }
        }, 0);
      }
      onClose();
    }
  }

  return (
    <Sheet open={estOuvert} onOpenChange={handleOpenChange}>
      <SheetContent
        side="right"
        className="w-full sm:max-w-lg overflow-y-auto p-6 focus:outline-none"
      >
        {/* État de chargement initial */}
        {query.isLoading && (
          <div aria-live="polite" className="space-y-6">
            <SheetHeader className="text-left space-y-2">
              <Skeleton className="h-7 w-3/4" />
              <Skeleton className="h-4 w-1/3" />
            </SheetHeader>
            <div className="space-y-4 pt-4">
              <Skeleton className="h-6 w-24" />
              <div className="grid grid-cols-2 gap-4">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
              <Skeleton className="h-20 w-full rounded-xl" />
            </div>
          </div>
        )}

        {/* État d'erreur */}
        {query.isError && (
          <div aria-live="assertive" className="flex flex-col items-center py-12 text-center">
            <div className="mb-4 flex size-12 items-center justify-center rounded-full bg-red-100 text-red-600">
              <AlertCircle className="size-6" aria-hidden="true" />
            </div>
            <SheetHeader className="text-center">
              <SheetTitle className="text-lg font-bold text-ink-900">
                {is404 ? "Employé introuvable" : "Erreur de chargement"}
              </SheetTitle>
              <SheetDescription className="text-sm text-muted-foreground mt-1">
                {messageErreurApi(query.error)}
              </SheetDescription>
            </SheetHeader>
            <div className="mt-6 flex items-center gap-3">
              {is404 ? (
                <button
                  type="button"
                  onClick={onClose}
                  className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                >
                  <X className="size-4" aria-hidden="true" />
                  Fermer
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => void query.refetch()}
                    className="inline-flex items-center gap-2 rounded-full bg-brand-900 px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-brand-900/90"
                  >
                    <RefreshCw className="size-4" aria-hidden="true" />
                    Réessayer
                  </button>
                  <button
                    type="button"
                    onClick={onClose}
                    className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                  >
                    Fermer
                  </button>
                </>
              )}
            </div>
          </div>
        )}

        {/* Données de l'employé */}
        {!query.isLoading && !query.isError && emp && (
          <div className="space-y-6">
            <SheetHeader className="text-left space-y-1 pr-6">
              <SheetTitle className="text-xl font-bold text-ink-900">
                {emp.nomComplet}
              </SheetTitle>
              <SheetDescription className="font-mono text-xs text-muted-foreground">
                Matricule : {emp.matricule}
              </SheetDescription>
            </SheetHeader>

            {/* Statut & Fonction */}
            <div className="flex flex-wrap items-center gap-3 border-b border-brand-900/10 pb-4">
              <StatutEmployeBadge statut={emp.statut} />
              <span className="text-xs font-medium text-muted-foreground bg-surface-card px-2.5 py-1 rounded-full border border-brand-900/10">
                {LIBELLE_ROLE[emp.role] ?? emp.role}
              </span>
            </div>

            {/* Coordonnées et détails */}
            <div className="space-y-4">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Informations générales
              </h3>
              <div className="grid grid-cols-1 gap-3 rounded-2xl border border-brand-900/10 bg-surface-card/40 p-4 text-sm sm:grid-cols-2">
                <div>
                  <span className="block text-xs text-muted-foreground">E-mail</span>
                  <span className="font-medium text-ink-900 break-all">{emp.email}</span>
                </div>
                <div>
                  <span className="block text-xs text-muted-foreground">Téléphone</span>
                  <span className="font-medium text-ink-900">
                    {emp.telephone && emp.telephone.trim().length > 0
                      ? emp.telephone
                      : "Non renseigné"}
                  </span>
                </div>
                <div>
                  <span className="block text-xs text-muted-foreground">Dernière connexion</span>
                  <span className="font-medium text-ink-900">
                    {formatDate(emp.derniereConnexion)}
                  </span>
                </div>
                <div>
                  <span className="block text-xs text-muted-foreground">Date de création</span>
                  <span className="font-medium text-ink-900">
                    {formatDate(emp.dateCreation)}
                  </span>
                </div>
              </div>
            </div>

            {/* Bloc Invitation (Affiché UNIQUEMENT si statut === EN_ATTENTE) */}
            {emp.statut === "EN_ATTENTE" && (
              <div className="space-y-2">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  État de l'invitation
                </h3>
                {emp.invitationExpiree ? (
                  <div
                    role="status"
                    className="flex items-start gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-900"
                  >
                    <Clock className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
                    <div>
                      <p className="font-semibold text-amber-950">
                        Le lien d'invitation a expiré
                      </p>
                      <p className="mt-0.5 text-xs text-amber-900/80">
                        L'employé ne peut plus activer son compte avec l'invitation précédente.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div
                    role="status"
                    className="flex items-start gap-3 rounded-2xl border border-brand-900/15 bg-brand-900/5 p-4 text-sm text-brand-900"
                  >
                    <Mail className="mt-0.5 size-5 shrink-0 text-brand-900" aria-hidden="true" />
                    <div>
                      <p className="font-semibold text-ink-900">
                        Invitation valide jusqu'au {formatDateTime(emp.invitationExpireLe)}
                      </p>
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        En attente de la première connexion et de l'activation du mot de passe.
                      </p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
