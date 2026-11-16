import { Check, UserPlus } from "lucide-react";
import {
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface EmployeCreationStepperProps {
  etape: 1 | 2;
}

export function EmployeCreationStepper({ etape }: EmployeCreationStepperProps) {
  return (
    <DialogHeader className="space-y-3 text-left">
      <div className="flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900 shadow-sm">
          <UserPlus className="size-5" aria-hidden="true" />
        </div>
        <div>
          <DialogTitle className="text-xl font-bold tracking-tight text-ink-900">
            Ajouter un collaborateur
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
            Invitation par e-mail avec lien d'activation sécurisé.
          </DialogDescription>
        </div>
      </div>
      <div className="flex items-center gap-2 pt-1">
        <div className={`flex flex-1 items-center gap-2 rounded-xl p-2 ${
          etape === 1 ? "bg-brand-900/10 text-brand-900" : "bg-surface-card text-muted-foreground opacity-75"
        }`}>
          <span className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
            etape === 2 ? "bg-emerald-600 text-white" : "bg-brand-900 text-white"
          }`}>
            {etape === 2 ? <Check className="size-3.5" /> : "1"}
          </span>
          <div className="min-w-0">
            <p className="truncate text-xs font-bold">1. Coordonnées</p>
            <p className="hidden text-[10px] text-muted-foreground sm:block">Identité & contact</p>
          </div>
        </div>
        <div className="h-0.5 w-4 bg-brand-900/20" />
        <div className={`flex flex-1 items-center gap-2 rounded-xl p-2 ${
          etape === 2 ? "bg-brand-900/10 text-brand-900" : "bg-surface-card text-muted-foreground opacity-75"
        }`}>
          <span className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
            etape === 2 ? "bg-brand-900 text-white" : "bg-muted-foreground/20 text-muted-foreground"
          }`}>
            2
          </span>
          <div className="min-w-0">
            <p className="truncate text-xs font-bold">2. Fonction</p>
            <p className="hidden text-[10px] text-muted-foreground sm:block">Rôle & attribution</p>
          </div>
        </div>
      </div>
    </DialogHeader>
  );
}
