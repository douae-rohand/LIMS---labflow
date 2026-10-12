import { useController, type UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { Button, Input, Select, Label } from "@/components/lab";
import { LIBELLE_ROLE } from "./dateUtils";
import { messageErreurApi } from "@/api/employes";
import type { EmployeDetailDto, ModifierEmployePayload, RoleEmploye } from "@/api/employes";

import { schemaEmployeForm, type EmployeFormValues } from "./employeSchemas";

interface EmployeFormProps {
  employe: EmployeDetailDto;
  form: UseFormReturn<EmployeFormValues>;
  onSubmitModif: (payload: ModifierEmployePayload) => void;
  isSubmitting: boolean;
  erreurModification: unknown;
  isMutatingGlobale: boolean;
}

export function EmployeForm({
  employe,
  form,
  onSubmitModif,
  isSubmitting,
  erreurModification,
  isMutatingGlobale,
}: EmployeFormProps) {
  const {
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty },
  } = form;

  const estEnAttente = employe.statut === "EN_ATTENTE";

  // Contrôleurs pour garantir une liaison bidirectionnelle réactive sans dépendance à forwardRef
  const prenomField = useController({ name: "prenom", control });
  const nomField = useController({ name: "nom", control });
  const emailField = useController({ name: "email", control });
  const telephoneField = useController({ name: "telephone", control });
  const roleField = useController({ name: "role", control });

  const handleFormSubmit = (data: EmployeFormValues) => {
    // Règle métier : le numéro ne peut pas être supprimé
    const initialTel = employe.telephone ? employe.telephone.trim() : "";
    const nouveauTel = data.telephone ? data.telephone.trim() : "";

    if (initialTel.length > 0 && nouveauTel.length === 0) {
      setError("telephone", {
        type: "manual",
        message: "Le numéro ne peut pas être supprimé.",
      });
      return;
    }

    // Construction exacte du payload (seulement les champs modifiés)
    const payload: ModifierEmployePayload = {};

    const initialPrenom = (employe.prenom ?? "").trim();
    const nouveauPrenom = data.prenom.trim();
    if (nouveauPrenom !== initialPrenom) {
      payload.prenom = nouveauPrenom;
    }

    const initialNom = (employe.nom ?? "").trim();
    const nouveauNom = data.nom.trim();
    if (nouveauNom !== initialNom) {
      payload.nom = nouveauNom;
    }

    if (nouveauTel !== initialTel) {
      if (nouveauTel.length > 0) {
        payload.telephone = nouveauTel;
      }
    }

    if (data.role !== employe.role) {
      payload.role = data.role as RoleEmploye;
    }

    // L'e-mail n'est envoyé que si statut = EN_ATTENTE et modifié
    if (estEnAttente) {
      const initialEmail = (employe.email ?? "").trim().toLowerCase();
      const nouveauEmail = data.email.trim().toLowerCase();
      if (nouveauEmail !== initialEmail) {
        payload.email = nouveauEmail;
      }
    }

    // Si aucun champ n'a réellement changé, rien à soumettre
    if (Object.keys(payload).length === 0) {
      return;
    }

    onSubmitModif(payload);
  };

  const handleAnnuler = () => {
    reset({
      prenom: employe.prenom ?? "",
      nom: employe.nom ?? "",
      email: employe.email ?? "",
      telephone: employe.telephone ?? "",
      role: employe.role,
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4" noValidate>
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          Modification des informations
        </h3>
        {isDirty && (
          <span className="text-[11px] font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
            Modifications non enregistrées
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* Prénom */}
        <div className="space-y-1">
          <Label htmlFor="employe-prenom">Prénom</Label>
          <Input
            id="employe-prenom"
            value={prenomField.field.value}
            onChange={prenomField.field.onChange}
            onBlur={prenomField.field.onBlur}
            disabled={isMutatingGlobale}
            placeholder="Ex : Karim"
            aria-invalid={!!errors.prenom}
            aria-describedby={errors.prenom ? "employe-prenom-error" : undefined}
          />
          {errors.prenom && (
            <p id="employe-prenom-error" role="alert" className="text-xs text-red-600">
              {errors.prenom.message}
            </p>
          )}
        </div>

        {/* Nom */}
        <div className="space-y-1">
          <Label htmlFor="employe-nom">Nom</Label>
          <Input
            id="employe-nom"
            value={nomField.field.value}
            onChange={nomField.field.onChange}
            onBlur={nomField.field.onBlur}
            disabled={isMutatingGlobale}
            placeholder="Ex : Benali"
            aria-invalid={!!errors.nom}
            aria-describedby={errors.nom ? "employe-nom-error" : undefined}
          />
          {errors.nom && (
            <p id="employe-nom-error" role="alert" className="text-xs text-red-600">
              {errors.nom.message}
            </p>
          )}
        </div>
      </div>

      {/* Fonction */}
      <div className="space-y-1">
        <Label htmlFor="employe-role">Fonction</Label>
        <Select
          id="employe-role"
          value={roleField.field.value}
          onChange={roleField.field.onChange}
          onBlur={roleField.field.onBlur}
          disabled={isMutatingGlobale}
          aria-invalid={!!errors.role}
          aria-describedby={errors.role ? "employe-role-error" : undefined}
        >
          <option value="RESPONSABLE">{LIBELLE_ROLE["RESPONSABLE"]}</option>
          <option value="TECHNICIEN">{LIBELLE_ROLE["TECHNICIEN"]}</option>
          <option value="ACCUEIL">{LIBELLE_ROLE["ACCUEIL"]}</option>
        </Select>
        {errors.role && (
          <p id="employe-role-error" role="alert" className="text-xs text-red-600">
            {errors.role.message}
          </p>
        )}
      </div>

      {/* Téléphone */}
      <div className="space-y-1">
        <Label htmlFor="employe-telephone">Téléphone</Label>
        <Input
          id="employe-telephone"
          type="tel"
          value={telephoneField.field.value}
          onChange={telephoneField.field.onChange}
          onBlur={telephoneField.field.onBlur}
          disabled={isMutatingGlobale}
          placeholder="+212600000000"
          aria-invalid={!!errors.telephone}
          aria-describedby={errors.telephone ? "employe-telephone-error" : undefined}
        />
        {errors.telephone ? (
          <p id="employe-telephone-error" role="alert" className="text-xs text-red-600">
            {errors.telephone.message}
          </p>
        ) : (
          <p className="text-[11px] text-muted-foreground">
            Format international recommandé (8 à 15 chiffres).
          </p>
        )}
      </div>

      {/* E-mail */}
      <div className="space-y-1">
        <Label htmlFor="employe-email">Adresse e-mail</Label>
        {estEnAttente ? (
          <>
            <Input
              id="employe-email"
              type="email"
              value={emailField.field.value}
              onChange={emailField.field.onChange}
              onBlur={emailField.field.onBlur}
              disabled={isMutatingGlobale}
              placeholder="karim.benali@labo.ma"
              aria-invalid={!!errors.email}
              aria-describedby={errors.email ? "employe-email-error" : "employe-email-aide"}
            />
            {errors.email ? (
              <p id="employe-email-error" role="alert" className="text-xs text-red-600">
                {errors.email.message}
              </p>
            ) : (
              <p id="employe-email-aide" className="text-[11px] text-brand-900/80">
                Un nouveau lien d'activation sera envoyé à la nouvelle adresse.
              </p>
            )}
          </>
        ) : (
          <div>
            <Input
              id="employe-email"
              type="email"
              value={employe.email}
              readOnly
              disabled
              className="bg-muted/40 cursor-not-allowed opacity-75"
            />
            <p className="mt-1 text-[11px] text-muted-foreground">
              Non modifiable une fois le compte activé.
            </p>
          </div>
        )}
      </div>

      {/* Erreur générale API de modification */}
      {erreurModification ? (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-700">
          {String(messageErreurApi(erreurModification))}
        </div>
      ) : null}

      {/* Boutons d'action formulaire */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={!isDirty || isMutatingGlobale}
          onClick={handleAnnuler}
        >
          Annuler
        </Button>
        <Button
          type="submit"
          variant="primary"
          size="sm"
          disabled={!isDirty || isMutatingGlobale}
          loading={isSubmitting}
        >
          Enregistrer
        </Button>
      </div>
    </form>
  );
}
