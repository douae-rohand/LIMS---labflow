import { useController, type UseFormReturn } from "react-hook-form";
import { AlertCircle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/lab";
import { messageErreurApi, type RoleEmploye } from "@/api/employes";
import { useDomainesLaboratoire } from "@/hooks/useDomainesLaboratoire";
import { DomainesTechnicienSelection } from "./DomainesTechnicienSelection";
import { LIBELLE_ROLE } from "./dateUtils";
import type { CreerEmployeFormValues } from "./employeSchemas";

const ROLES = [
  {
    value: "RESPONSABLE" as const,
    description: "Supervise les analyses, valide les résultats et coordonne l'équipe technique.",
    badge: "Supervision & Validation",
    badgeColor: "bg-brand-900/10 text-brand-900 border-brand-900/20",
  },
  {
    value: "TECHNICIEN" as const,
    description: "Prend en charge les échantillons, réalise les tests et saisit les résultats bruts.",
    badge: "Analyses & Paillasse",
    badgeColor: "bg-accent-lime/40 text-ink-900 border-accent-lime",
  },
  {
    value: "ACCUEIL" as const,
    description: "Accueille les patients, réceptionne les prélèvements et enregistre les dossiers.",
    badge: "Accueil & Enregistrement",
    badgeColor: "bg-mint-200/60 text-brand-900 border-mint-200",
  },
];

interface EmployeCreationRoleProps {
  form: UseFormReturn<CreerEmployeFormValues>;
  domainesSelectionnes: number[];
  disabled: boolean;
  erreurCreation: unknown;
  onDomainesChange: (selection: number[]) => void;
  onPrevious: () => void;
}

export function EmployeCreationRole({
  form,
  domainesSelectionnes,
  disabled,
  erreurCreation,
  onDomainesChange,
  onPrevious,
}: EmployeCreationRoleProps) {
  const role = useController({ name: "role", control: form.control });
  const domainesQuery = useDomainesLaboratoire();
  const roleSelectionne = ROLES.find((item) => item.value === role.field.value);

  const choisirRole = (value: RoleEmploye) => {
    role.field.onChange(value);
    if (value !== "TECHNICIEN") {
      onDomainesChange([]);
    }
    if (form.formState.errors.role) {
      void form.trigger("role");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between rounded-xl border border-brand-900/10 bg-brand-900/5 px-3.5 py-2 text-xs">
        <div className="flex items-center gap-2 text-ink-900 font-medium">
          <span>{form.getValues("prenom")} {form.getValues("nom")}</span>
          <span className="text-muted-foreground">({form.getValues("email")})</span>
        </div>
        <button type="button" onClick={onPrevious} className="text-xs font-semibold text-brand-900 hover:underline">
          Modifier
        </button>
      </div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-xs font-semibold text-ink-900">
            Attribuer une fonction au laboratoire <span className="text-rose-500">*</span>
          </label>
          {roleSelectionne && (
            <span className="text-[11px] font-semibold text-brand-900">
              Sélectionné : {LIBELLE_ROLE[roleSelectionne.value]}
            </span>
          )}
        </div>
        <div className="grid grid-cols-1 gap-2.5">
          {ROLES.map((item) => {
            const selected = role.field.value === item.value;
            return (
              <button
                type="button"
                key={item.value}
                aria-pressed={selected}
                onClick={() => choisirRole(item.value)}
                disabled={disabled}
                className={`group relative flex cursor-pointer items-start gap-3 rounded-2xl border-2 p-3 text-left transition-all duration-200 ${
                  selected ? "border-brand-900 bg-brand-900/5 shadow-sm" : "border-brand-900/10 bg-card hover:border-brand-900/30 hover:bg-surface-card/60"
                }`}
              >
                <div className="mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border border-brand-900/40 bg-card">
                  {selected && <div className="size-2 rounded-full bg-brand-900" />}
                </div>
                <div className="flex-1 space-y-0.5">
                  <div className="flex flex-wrap items-center justify-between gap-1">
                    <span className="text-sm font-bold text-ink-900">{LIBELLE_ROLE[item.value]}</span>
                    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${item.badgeColor}`}>
                      {item.badge}
                    </span>
                  </div>
                  <p className="text-xs leading-relaxed text-muted-foreground">{item.description}</p>
                </div>
              </button>
            );
          })}
        </div>
        {form.formState.errors.role && (
          <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
            {form.formState.errors.role.message}
          </p>
        )}
      </div>
      {role.field.value === "TECHNICIEN" && (
        <div className="space-y-2">
          {domainesQuery.isLoading ? (
            <p className="text-xs text-muted-foreground">Chargement des domaines…</p>
          ) : domainesQuery.isError ? (
            <p role="alert" className="text-xs text-rose-700">{messageErreurApi(domainesQuery.error)}</p>
          ) : (
            <DomainesTechnicienSelection
              domaines={domainesQuery.data ?? []}
              selection={domainesSelectionnes}
              messageVide="Aucun domaine sélectionné."
              disabled={disabled}
              onChange={onDomainesChange}
            />
          )}
        </div>
      )}
      {erreurCreation !== undefined && erreurCreation !== null && (
        <div role="alert" className="flex items-center gap-2 rounded-xl border border-rose-500/20 bg-rose-500/10 p-3 text-xs text-rose-900">
          <AlertCircle className="size-4 shrink-0 text-rose-700" aria-hidden="true" />
          <span>Une erreur est survenue lors de l'enregistrement de l'employé.</span>
        </div>
      )}
      <div className="flex items-center justify-between border-t border-brand-900/10 pt-4">
        <Button type="button" variant="secondary" onClick={onPrevious} disabled={disabled}
          icon={<ArrowLeft className="size-4" />} iconPosition="left">
          Retour
        </Button>
        <Button type="submit" variant="primary" disabled={disabled} loading={disabled}>
          Envoyer l'invitation
        </Button>
      </div>
    </div>
  );
}
