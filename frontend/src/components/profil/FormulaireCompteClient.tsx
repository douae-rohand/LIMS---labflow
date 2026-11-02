import { zodResolver } from "@hookform/resolvers/zod";
import { useBlocker } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useController, useForm } from "react-hook-form";
import { Button, Input, Label, Select, Textarea } from "@/components/lab";
import type { ModifierProfilClientBody, ProfilClientDto, TypeClient } from "@/api/profilClient";
import { ConfirmationAbandonDialog } from "./ConfirmationAbandonDialog";
import { ConfirmationChangementTypeDialog } from "./ConfirmationChangementTypeDialog";
import { construireBody, profilSchema, versValeurs, type ValeursProfil } from "./profilSchema";

interface FormulaireCompteClientProps {
  profil: ProfilClientDto;
  enregistrement: boolean;
  onEnregistrer: (body: ModifierProfilClientBody) => void;
}

/** Partie modifiable du compte client (type, raison sociale, ICE, adresse). */
export function FormulaireCompteClient({
  profil,
  enregistrement,
  onEnregistrer,
}: FormulaireCompteClientProps) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ValeursProfil>({
    resolver: zodResolver(profilSchema),
    defaultValues: versValeurs(profil),
  });

  const champType = useController({ name: "typeClient", control });
  const champRaison = useController({ name: "raisonSociale", control });
  const champIce = useController({ name: "ice", control });
  const champAdresse = useController({ name: "adresse", control });

  const [dialogAbandon, setDialogAbandon] = useState(false);
  const [dialogType, setDialogType] = useState(false);

  // Le profil renvoyé par le serveur remplace le cache : on y resynchronise le
  // formulaire, ce qui remet `isDirty` à faux après un enregistrement réussi.
  useEffect(() => {
    reset(versValeurs(profil));
  }, [profil, reset]);

  const blocage = useBlocker({
    shouldBlockFn: () => isDirty,
    withResolver: true,
    disabled: !isDirty,
  });

  useEffect(() => {
    if (blocage.status === "blocked") setDialogAbandon(true);
  }, [blocage.status]);

  const entreprise = champType.field.value === "ENTREPRISE";
  const initialEntreprise = profil.typeClient === "ENTREPRISE";

  function changerType(valeur: string) {
    const suivant = valeur as TypeClient;
    if (suivant === "PARTICULIER" && initialEntreprise) {
      setDialogType(true);
      return;
    }
    champType.field.onChange(suivant);
  }

  function soumettre(valeurs: ValeursProfil) {
    const body = construireBody(valeurs, profil);
    if (Object.keys(body).length === 0) return;
    onEnregistrer(body);
  }

  return (
    <>
      <form className="flex flex-col gap-6" noValidate onSubmit={handleSubmit(soumettre)}>
        <div className="space-y-2 sm:max-w-xs">
          <Label htmlFor="profil-type">Type de compte</Label>
          <Select
            id="profil-type"
            value={champType.field.value}
            onChange={(e) => changerType(e.target.value)}
            disabled={enregistrement}
          >
            <option value="PARTICULIER">Particulier</option>
            <option value="ENTREPRISE">Entreprise</option>
          </Select>
        </div>

        {entreprise ? (
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="profil-raison-sociale">Raison sociale</Label>
              <Input
                id="profil-raison-sociale"
                value={champRaison.field.value}
                onChange={champRaison.field.onChange}
                onBlur={champRaison.field.onBlur}
                maxLength={200}
                autoComplete="organization"
                disabled={enregistrement}
                aria-invalid={errors.raisonSociale ? true : undefined}
                aria-describedby={errors.raisonSociale ? "profil-raison-sociale-erreur" : undefined}
              />
              {errors.raisonSociale ? (
                <p id="profil-raison-sociale-erreur" role="alert" className="text-sm text-red-600">
                  {errors.raisonSociale.message}
                </p>
              ) : null}
            </div>

            <div className="space-y-2">
              <Label htmlFor="profil-ice">ICE</Label>
              <Input
                id="profil-ice"
                value={champIce.field.value}
                onChange={champIce.field.onChange}
                onBlur={champIce.field.onBlur}
                inputMode="numeric"
                maxLength={15}
                autoComplete="off"
                disabled={enregistrement}
                aria-invalid={errors.ice ? true : undefined}
                aria-describedby={errors.ice ? "profil-ice-erreur" : undefined}
              />
              {errors.ice ? (
                <p id="profil-ice-erreur" role="alert" className="text-sm text-red-600">
                  {errors.ice.message}
                </p>
              ) : null}
            </div>
          </div>
        ) : null}

        <div className="space-y-2">
          <Label htmlFor="profil-adresse">Adresse</Label>
          <Textarea
            id="profil-adresse"
            rows={3}
            maxLength={500}
            value={champAdresse.field.value}
            onChange={champAdresse.field.onChange}
            onBlur={champAdresse.field.onBlur}
            disabled={enregistrement}
            aria-invalid={errors.adresse ? true : undefined}
            aria-describedby="profil-adresse-aide"
          />
          <p id="profil-adresse-aide" className="text-xs text-muted-foreground">
            Videz ce champ puis enregistrez pour supprimer l&apos;adresse enregistrée.
          </p>
          {errors.adresse ? (
            <p role="alert" className="text-sm text-red-600">
              {errors.adresse.message}
            </p>
          ) : null}
        </div>

        <div>
          <Button type="submit" loading={enregistrement} disabled={enregistrement || !isDirty}>
            Enregistrer
          </Button>
        </div>
      </form>

      <ConfirmationAbandonDialog
        ouvert={dialogAbandon}
        onRester={() => {
          setDialogAbandon(false);
          if (blocage.status === "blocked") blocage.reset();
        }}
        onAbandonner={() => {
          setDialogAbandon(false);
          if (blocage.status === "blocked") blocage.proceed();
        }}
      />

      <ConfirmationChangementTypeDialog
        ouvert={dialogType}
        onAnnuler={() => setDialogType(false)}
        onConfirmer={() => {
          setDialogType(false);
          champType.field.onChange("PARTICULIER");
        }}
      />
    </>
  );
}
