import { z } from "zod";

// ---------------------------------------------------------------------------
// Briques de validation partagées entre création et modification d'un employé
// ---------------------------------------------------------------------------

export const champPrenom = z
  .string()
  .trim()
  .min(1, "Le prénom est obligatoire.")
  .max(100, "100 caractères maximum.");

export const champNom = z
  .string()
  .trim()
  .min(1, "Le nom est obligatoire.")
  .max(100, "100 caractères maximum.");

export const champEmail = z
  .string()
  .trim()
  .min(1, "L'e-mail est obligatoire.")
  .max(180, "180 caractères maximum.")
  .email("Format d'e-mail invalide.")
  .toLowerCase();

export const champTelephone = z
  .string()
  .trim()
  .regex(/^[+]?[0-9]{8,15}$/, "Numéro de téléphone invalide.")
  .optional()
  .or(z.literal(""));

export const ROLES_EMPLOYES = ["RESPONSABLE", "TECHNICIEN", "ACCUEIL"] as const;

// ---------------------------------------------------------------------------
// Schéma pour la modification d'un employé dans le panneau
// ---------------------------------------------------------------------------
export const schemaEmployeForm = z.object({
  prenom: champPrenom,
  nom: champNom,
  email: champEmail,
  telephone: champTelephone,
  role: z.enum(ROLES_EMPLOYES, {
    required_error: "La fonction est obligatoire.",
    invalid_type_error: "La fonction est obligatoire.",
  }),
});

export type EmployeFormValues = z.infer<typeof schemaEmployeForm>;

// ---------------------------------------------------------------------------
// Schéma pour la création d'un employé (/app/employes/nouveau)
// Valeur initiale attendue dans le select : "" (placeholder « Choisir une fonction »)
// z.enum avec invalid_type_error / required_error traite la chaîne vide ""
// comme une valeur invalide et affiche « La fonction est obligatoire. »
// ---------------------------------------------------------------------------
export const schemaCreerEmploye = z.object({
  prenom: champPrenom,
  nom: champNom,
  email: champEmail,
  telephone: champTelephone,
  role: z.enum(ROLES_EMPLOYES, {
    errorMap: (issue, ctx) => {
      if (
        issue.code === z.ZodIssueCode.invalid_enum_value ||
        issue.code === z.ZodIssueCode.invalid_type
      ) {
        return { message: "La fonction est obligatoire." };
      }
      return { message: ctx.defaultError };
    },
  }),
});

export type CreerEmployeFormValues = z.infer<typeof schemaCreerEmploye>;
