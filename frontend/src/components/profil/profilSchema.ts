import { z } from "zod";
import type { ModifierProfilClientBody, ProfilClientDto } from "@/api/profilClient";

/**
 * ICE : exactement 15 chiffres. Même motif que l'inscription publique (la
 * regex n'y est pas exportée : duplication assumée et signalée).
 */
export const ICE_REGEX = /^[0-9]{15}$/;

const MAX_RAISON_SOCIALE = 200;
const MAX_ADRESSE = 500;

/** Validation du compte client (type, raison sociale, ICE, adresse). */
export const profilSchema = z
  .object({
    typeClient: z.enum(["PARTICULIER", "ENTREPRISE"]),
    raisonSociale: z
      .string()
      .trim()
      .max(MAX_RAISON_SOCIALE, "La raison sociale ne peut pas dépasser 200 caractères."),
    ice: z.string().trim(),
    adresse: z.string().trim().max(MAX_ADRESSE, "L'adresse ne peut pas dépasser 500 caractères."),
  })
  .superRefine((valeurs, ctx) => {
    if (valeurs.typeClient !== "ENTREPRISE") return;
    if (valeurs.raisonSociale.length === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["raisonSociale"],
        message: "La raison sociale est obligatoire pour un compte entreprise.",
      });
    }
    if (!ICE_REGEX.test(valeurs.ice)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["ice"],
        message: "L'ICE doit comporter exactement 15 chiffres.",
      });
    }
  });

export type ValeursProfil = z.infer<typeof profilSchema>;

/** Valeurs initiales du formulaire à partir d'un profil serveur. */
export function versValeurs(profil: ProfilClientDto): ValeursProfil {
  return {
    typeClient: profil.typeClient,
    raisonSociale: profil.raisonSociale ?? "",
    ice: profil.ice ?? "",
    adresse: profil.adresse ?? "",
  };
}

/**
 * Corps du PATCH : le serveur fusionne avec les valeurs stockées, donc seuls
 * les champs réellement modifiés partent. Exceptions explicites :
 *  - Particulier → Entreprise : les trois champs partent ensemble ;
 *  - Entreprise → Particulier : seul `typeClient` part (le serveur efface
 *    raison sociale et ICE).
 * Une adresse vide efface volontairement l'adresse enregistrée.
 */
export function construireBody(
  valeurs: ValeursProfil,
  profil: ProfilClientDto,
): ModifierProfilClientBody {
  const body: ModifierProfilClientBody = {};

  if (valeurs.typeClient !== profil.typeClient) {
    if (valeurs.typeClient === "PARTICULIER") {
      body.typeClient = "PARTICULIER";
    } else {
      body.typeClient = "ENTREPRISE";
      body.raisonSociale = valeurs.raisonSociale;
      body.ice = valeurs.ice;
    }
  } else if (valeurs.typeClient === "ENTREPRISE") {
    if (valeurs.raisonSociale !== (profil.raisonSociale ?? "")) {
      body.raisonSociale = valeurs.raisonSociale;
    }
    if (valeurs.ice !== (profil.ice ?? "")) {
      body.ice = valeurs.ice;
    }
  }

  if (valeurs.adresse !== (profil.adresse ?? "")) {
    body.adresse = valeurs.adresse;
  }

  return body;
}
