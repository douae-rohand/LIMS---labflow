import { Mail, Clock } from "lucide-react";
import { formatDateTime } from "./dateUtils";
import type { EmployeDetailDto } from "@/api/employes";

interface EmployeInvitationProps {
  employe: EmployeDetailDto;
}

export function EmployeInvitation({ employe }: EmployeInvitationProps) {
  if (employe.statut !== "EN_ATTENTE") {
    return null;
  }

  return (
    <div className="space-y-2">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        État de l'invitation
      </h3>
      {employe.invitationExpiree ? (
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
              Invitation valide jusqu'au {formatDateTime(employe.invitationExpireLe)}
            </p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              En attente de la première connexion et de l'activation du mot de passe.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
