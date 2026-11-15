import { useController, type UseFormReturn } from "react-hook-form";
import { ArrowRight, Mail, Phone } from "lucide-react";
import { Button } from "@/components/lab";
import type { CreerEmployeFormValues } from "./employeSchemas";

interface EmployeCreationCoordonneesProps {
  form: UseFormReturn<CreerEmployeFormValues>;
  disabled: boolean;
  onCancel: () => void;
  onContinue: () => void;
}

export function EmployeCreationCoordonnees({
  form,
  disabled,
  onCancel,
  onContinue,
}: EmployeCreationCoordonneesProps) {
  const prenom = useController({ name: "prenom", control: form.control });
  const nom = useController({ name: "nom", control: form.control });
  const email = useController({ name: "email", control: form.control });
  const telephone = useController({ name: "telephone", control: form.control });
  const errors = form.formState.errors;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="modal-prenom" className="text-xs font-semibold text-ink-900">
            Prénom <span className="text-rose-500">*</span>
          </label>
          <input
            id="modal-prenom"
            type="text"
            placeholder="Ex : Karim"
            value={prenom.field.value || ""}
            onChange={(event) => prenom.field.onChange(event.target.value)}
            onBlur={prenom.field.onBlur}
            disabled={disabled}
            className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
          />
          {errors.prenom && <p role="alert" className="text-xs font-medium text-rose-600">{errors.prenom.message}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="modal-nom" className="text-xs font-semibold text-ink-900">
            Nom <span className="text-rose-500">*</span>
          </label>
          <input
            id="modal-nom"
            type="text"
            placeholder="Ex : Benali"
            value={nom.field.value || ""}
            onChange={(event) => nom.field.onChange(event.target.value)}
            onBlur={nom.field.onBlur}
            disabled={disabled}
            className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
          />
          {errors.nom && <p role="alert" className="text-xs font-medium text-rose-600">{errors.nom.message}</p>}
        </div>
      </div>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="modal-email" className="text-xs font-semibold text-ink-900">
          <span className="flex items-center gap-1.5">
            <Mail className="size-3.5 text-muted-foreground" aria-hidden="true" />
            Adresse email professionnelle <span className="text-rose-500">*</span>
          </span>
        </label>
        <input
          id="modal-email"
          type="email"
          placeholder="karim.benali@labo.ma"
          value={email.field.value || ""}
          onChange={(event) => email.field.onChange(event.target.value)}
          onBlur={email.field.onBlur}
          disabled={disabled}
          className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
        />
        {errors.email ? (
          <p role="alert" className="text-xs font-medium text-rose-600">{errors.email.message}</p>
        ) : (
          <p className="text-[11px] text-muted-foreground">
            Le lien d'activation sécurisé sera transmis à cette adresse.
          </p>
        )}
      </div>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="modal-telephone" className="text-xs font-semibold text-ink-900">
          <span className="flex items-center gap-1.5">
            <Phone className="size-3.5 text-muted-foreground" aria-hidden="true" />
            Téléphone portable <span className="text-xs font-normal text-muted-foreground">(optionnel)</span>
          </span>
        </label>
        <input
          id="modal-telephone"
          type="tel"
          placeholder="0612345678 ou +212612345678"
          value={telephone.field.value || ""}
          onChange={(event) => telephone.field.onChange(event.target.value)}
          onBlur={telephone.field.onBlur}
          disabled={disabled}
          className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
        />
        {errors.telephone && <p role="alert" className="text-xs font-medium text-rose-600">{errors.telephone.message}</p>}
      </div>
      <div className="flex items-center justify-end gap-3 border-t border-brand-900/10 pt-4">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={disabled}>
          Annuler
        </Button>
        <Button
          type="button"
          variant="primary"
          onClick={onContinue}
          disabled={disabled}
          icon={<ArrowRight className="size-4" />}
          iconPosition="right"
        >
          Continuer (Rôle)
        </Button>
      </div>
    </div>
  );
}
