import { zodResolver } from "@hookform/resolvers/zod";
import { Check } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { soumettreDemandeIntegration } from "@/api/plateforme";
import { ApiError } from "@/api/axios";
import { Button, Card, Input, Label, Select, Textarea } from "@/components/lab";
import { cn } from "@/lib/utils";
import { DocumentUploader } from "./DocumentUploader";
import { LocationPicker } from "./LocationPicker";
import {
  REGIONS_MAROC,
  TAILLE_MAX_DOCUMENT,
  TYPES_DOCUMENTS,
  TYPES_LABORATOIRE,
  type TypeDocumentIntegration,
} from "./constants";

const STEPS = [
  "Laboratoire",
  "Localisation",
  "Administrateur",
  "Documents",
  "Récapitulatif",
] as const;

const schema = z.object({
  nomLaboratoire: z.string().min(2, "Le nom du laboratoire est obligatoire."),
  raisonSociale: z.string().min(2, "La raison sociale est obligatoire."),
  typesLaboratoire: z.array(z.string()).min(1, "Sélectionnez au moins un type / catégorie."),
  ice: z.string().optional(),
  telephoneLaboratoire: z.string().min(8, "Le téléphone du laboratoire est obligatoire."),
  emailLaboratoire: z.string().email("E-mail du laboratoire invalide.").optional().or(z.literal("")),
  siteWeb: z.string().url("URL invalide.").optional().or(z.literal("")),
  informationsComplementaires: z.string().optional(),
  adresse: z.string().min(5, "L'adresse est obligatoire."),
  ville: z.string().min(2, "La ville est obligatoire."),
  region: z.string().optional(),
  pays: z.string().min(2, "Le pays est obligatoire."),
  codePostal: z.string().optional(),
  latitude: z.number({ invalid_type_error: "Sélectionnez une position sur la carte." }),
  longitude: z.number({ invalid_type_error: "Sélectionnez une position sur la carte." }),
  adminNom: z.string().min(2, "Le nom est obligatoire."),
  adminPrenom: z.string().min(2, "Le prénom est obligatoire."),
  adminEmail: z.string().email("E-mail de l'administrateur invalide."),
  adminTelephone: z.string().min(8, "Le téléphone est obligatoire."),
  adminFonction: z.string().min(2, "La fonction est obligatoire."),
  adminCin: z.string().min(4, "Le CIN est obligatoire."),
});

type FormValues = z.infer<typeof schema>;

const STEP_FIELDS: (keyof FormValues)[][] = [
  ["nomLaboratoire", "raisonSociale", "typesLaboratoire", "telephoneLaboratoire", "emailLaboratoire"],
  ["adresse", "ville", "pays", "latitude", "longitude"],
  ["adminNom", "adminPrenom", "adminEmail", "adminTelephone", "adminFonction", "adminCin"],
  [],
  [],
];

function FieldError({ message }: { message?: string | undefined }) {
  if (!message) return null;
  return <p className="text-sm text-red-600">{message}</p>;
}

export function IntegrationWizard() {
  const [step, setStep] = useState(0);
  const [files, setFiles] = useState<Partial<Record<TypeDocumentIntegration, File>>>({});
  const [fileErrors, setFileErrors] = useState<Partial<Record<TypeDocumentIntegration, string>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [sentNumero, setSentNumero] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      pays: "Maroc",
      typesLaboratoire: [],
      adminFonction: "Biologiste responsable",
    },
  });

  const values = form.watch();

  const documentsComplets = useMemo(
    () => TYPES_DOCUMENTS.every((type) => Boolean(files[type])),
    [files],
  );

  const goNext = async () => {
    const fields = STEP_FIELDS[step] ?? [];
    const valid = fields.length === 0 ? true : await form.trigger(fields);
    if (step === 3) {
      const nextErrors: Partial<Record<TypeDocumentIntegration, string>> = {};
      TYPES_DOCUMENTS.forEach((type) => {
        const file = files[type];
        if (!file) nextErrors[type] = "Document obligatoire.";
        else if (file.size > TAILLE_MAX_DOCUMENT) nextErrors[type] = "Fichier trop volumineux (10 Mo max).";
      });
      setFileErrors(nextErrors);
      if (Object.keys(nextErrors).length > 0) return;
    }
    if (!valid) return;
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  const onSubmit = async (data: FormValues) => {
    if (!confirmed) {
      setSubmitError("Veuillez confirmer l'exactitude des informations avant l'envoi.");
      return;
    }
    if (!documentsComplets) {
      setSubmitError("Les 6 documents obligatoires doivent être joints.");
      return;
    }
    setSubmitError(null);
    try {
      const fichiers = files as Record<TypeDocumentIntegration, File>;
      const result = await soumettreDemandeIntegration(
        {
          ...data,
          typesLaboratoire: data.typesLaboratoire,
          ice: data.ice || undefined,
          emailLaboratoire: data.emailLaboratoire || undefined,
          siteWeb: data.siteWeb || undefined,
          informationsComplementaires: data.informationsComplementaires || undefined,
        },
        fichiers,
        setProgress,
      );
      setSentNumero(result.numero);
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "La demande n'a pas pu être envoyée.";
      setSubmitError(message);
    } finally {
      setProgress(null);
    }
  };

  if (sentNumero) {
    return (
      <Card variant="glossy" padding="lg">
        <h2 className="text-2xl font-extrabold text-ink-900">Demande envoyée</h2>
        <p className="mt-3 text-muted-foreground">
          Votre demande <strong>{sentNumero}</strong> a été transmise au Super Administrateur.
          Elle est en attente de vérification des informations et des 6 documents.
          L'espace laboratoire ne sera créé qu'après acceptation.
        </p>
      </Card>
    );
  }

  return (
    <Card variant="glossy" padding="lg">
      <ol className="mb-8 grid grid-cols-2 gap-2 sm:grid-cols-5">
        {STEPS.map((label, index) => (
          <li key={label}>
            <button
              type="button"
              className={cn(
                "flex w-full items-center gap-2 rounded-full px-3 py-2 text-left text-xs font-semibold",
                index === step
                  ? "bg-brand-900 text-primary-foreground"
                  : index < step
                    ? "bg-mint-200 text-brand-900"
                    : "bg-surface-card text-muted-foreground",
              )}
              onClick={() => {
                if (index < step) setStep(index);
              }}
            >
              <span className="flex size-5 items-center justify-center rounded-full bg-white/20">
                {index < step ? <Check className="size-3" /> : index + 1}
              </span>
              {label}
            </button>
          </li>
        ))}
      </ol>

      <form className="flex flex-col gap-5" onSubmit={form.handleSubmit(onSubmit)}>
        {step === 0 && (
          <>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="nomLaboratoire">Nom du laboratoire</Label>
                <Input id="nomLaboratoire" {...form.register("nomLaboratoire")} />
                <FieldError message={form.formState.errors.nomLaboratoire?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="raisonSociale">Raison sociale</Label>
                <Input id="raisonSociale" {...form.register("raisonSociale")} />
                <FieldError message={form.formState.errors.raisonSociale?.message} />
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <Label>Type / catégorie</Label>
              <p className="text-xs text-muted-foreground">
                Un laboratoire peut être admis dans plusieurs catégories. Cochez toutes celles qui
                s'appliquent.
              </p>
              <div className="grid gap-2 sm:grid-cols-2">
                {TYPES_LABORATOIRE.map((type) => {
                  const selected = (values.typesLaboratoire ?? []).includes(type);
                  return (
                    <label
                      key={type}
                      className={cn(
                        "flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-3 text-sm font-semibold",
                        selected
                          ? "border-brand-900 bg-mint-200 text-brand-900"
                          : "border-input bg-card text-ink-900",
                      )}
                    >
                      <input
                        type="checkbox"
                        className="size-4 accent-brand-900"
                        checked={selected}
                        onChange={() => {
                          const current = values.typesLaboratoire ?? [];
                          const next = selected
                            ? current.filter((item) => item !== type)
                            : [...current, type];
                          form.setValue("typesLaboratoire", next, {
                            shouldValidate: true,
                            shouldDirty: true,
                          });
                        }}
                      />
                      {type}
                    </label>
                  );
                })}
              </div>
              <FieldError message={form.formState.errors.typesLaboratoire?.message} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="ice">ICE / identifiant administratif</Label>
              <Input id="ice" {...form.register("ice")} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="telephoneLaboratoire">Téléphone</Label>
                <Input id="telephoneLaboratoire" {...form.register("telephoneLaboratoire")} />
                <FieldError message={form.formState.errors.telephoneLaboratoire?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="emailLaboratoire">E-mail du laboratoire</Label>
                <Input id="emailLaboratoire" type="email" {...form.register("emailLaboratoire")} />
                <FieldError message={form.formState.errors.emailLaboratoire?.message} />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="siteWeb">Site web (optionnel)</Label>
              <Input id="siteWeb" placeholder="https://" {...form.register("siteWeb")} />
              <FieldError message={form.formState.errors.siteWeb?.message} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="informationsComplementaires">Informations complémentaires</Label>
              <Textarea id="informationsComplementaires" {...form.register("informationsComplementaires")} />
            </div>
          </>
        )}

        {step === 1 && (
          <>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5 sm:col-span-2">
                <Label htmlFor="adresse">Adresse</Label>
                <Input id="adresse" {...form.register("adresse")} />
                <FieldError message={form.formState.errors.adresse?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="ville">Ville</Label>
                <Input id="ville" {...form.register("ville")} />
                <FieldError message={form.formState.errors.ville?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="region">Région</Label>
                <Select id="region" {...form.register("region")}>
                  <option value="">Sélectionner</option>
                  {REGIONS_MAROC.map((region) => (
                    <option key={region}>{region}</option>
                  ))}
                </Select>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="pays">Pays</Label>
                <Input id="pays" {...form.register("pays")} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="codePostal">Code postal</Label>
                <Input id="codePostal" {...form.register("codePostal")} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="latitude">Latitude</Label>
                <Input
                  id="latitude"
                  type="number"
                  step="any"
                  value={values.latitude ?? ""}
                  onChange={(event) => {
                    const next = event.target.value === "" ? undefined : Number(event.target.value);
                    if (next == null || Number.isNaN(next)) return;
                    form.setValue("latitude", next, { shouldValidate: true });
                  }}
                />
                <FieldError message={form.formState.errors.latitude?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="longitude">Longitude</Label>
                <Input
                  id="longitude"
                  type="number"
                  step="any"
                  value={values.longitude ?? ""}
                  onChange={(event) => {
                    const next = event.target.value === "" ? undefined : Number(event.target.value);
                    if (next == null || Number.isNaN(next)) return;
                    form.setValue("longitude", next, { shouldValidate: true });
                  }}
                />
                <FieldError message={form.formState.errors.longitude?.message} />
              </div>
            </div>
            <LocationPicker
              label={values.nomLaboratoire || "Position du laboratoire"}
              addressQuery={[values.adresse, values.ville, values.region, values.codePostal, values.pays]
                .filter(Boolean)
                .join(", ")}
              value={{ latitude: values.latitude ?? null, longitude: values.longitude ?? null }}
              onChange={({ latitude, longitude }) => {
                form.setValue("latitude", latitude, { shouldValidate: true });
                form.setValue("longitude", longitude, { shouldValidate: true });
              }}
              onAddressResolved={(patch) => {
                if (patch.adresse && !values.adresse) form.setValue("adresse", patch.adresse);
                if (patch.ville && !values.ville) form.setValue("ville", patch.ville);
                if (patch.region && !values.region) form.setValue("region", patch.region);
                if (patch.pays) form.setValue("pays", patch.pays);
                if (patch.codePostal && !values.codePostal) form.setValue("codePostal", patch.codePostal);
              }}
            />
          </>
        )}

        {step === 2 && (
          <>
            <p className="text-sm text-muted-foreground">
              Ces informations serviront à créer le compte Administrateur laboratoire.
              Aucun mot de passe n'est demandé maintenant. Le compte administrateur sera activé après acceptation.
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminNom">Nom</Label>
                <Input id="adminNom" {...form.register("adminNom")} />
                <FieldError message={form.formState.errors.adminNom?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminPrenom">Prénom</Label>
                <Input id="adminPrenom" {...form.register("adminPrenom")} />
                <FieldError message={form.formState.errors.adminPrenom?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminEmail">E-mail</Label>
                <Input id="adminEmail" type="email" {...form.register("adminEmail")} />
                <FieldError message={form.formState.errors.adminEmail?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminTelephone">Téléphone</Label>
                <Input id="adminTelephone" {...form.register("adminTelephone")} />
                <FieldError message={form.formState.errors.adminTelephone?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminFonction">Fonction</Label>
                <Input id="adminFonction" {...form.register("adminFonction")} />
                <FieldError message={form.formState.errors.adminFonction?.message} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="adminCin">CIN</Label>
                <Input id="adminCin" {...form.register("adminCin")} />
                <FieldError message={form.formState.errors.adminCin?.message} />
              </div>
            </div>
          </>
        )}

        {step === 3 && (
          <DocumentUploader
            files={files}
            errors={fileErrors}
            onChange={(type, file) => {
              setFiles((current) => {
                const next = { ...current };
                if (file) next[type] = file;
                else delete next[type];
                return next;
              });
              setFileErrors((current) => ({ ...current, [type]: undefined }));
            }}
          />
        )}

        {step === 4 && (
          <div className="flex flex-col gap-6 text-sm">
            <section>
              <h3 className="text-base font-bold text-ink-900">Laboratoire</h3>
              <dl className="mt-2 grid gap-2 sm:grid-cols-2">
                <div>Nom : {values.nomLaboratoire}</div>
                <div>Raison sociale : {values.raisonSociale}</div>
                <div>Type : {(values.typesLaboratoire ?? []).join(", ") || "—"}</div>
                <div>ICE : {values.ice || "—"}</div>
                <div>Téléphone : {values.telephoneLaboratoire}</div>
                <div>E-mail : {values.emailLaboratoire || "—"}</div>
              </dl>
            </section>
            <section>
              <h3 className="text-base font-bold text-ink-900">Adresse et localisation</h3>
              <p className="mt-2">
                {values.adresse}, {values.ville}
                {values.region ? `, ${values.region}` : ""} {values.codePostal} — {values.pays}
              </p>
              <p className="text-muted-foreground">
                Coordonnées : {values.latitude}, {values.longitude}
              </p>
              {values.latitude != null && values.longitude != null && (
                <LocationPicker
                  readOnly
                  label={values.nomLaboratoire || "Position du laboratoire"}
                  value={{ latitude: values.latitude, longitude: values.longitude }}
                  onChange={() => undefined}
                />
              )}
            </section>
            <section>
              <h3 className="text-base font-bold text-ink-900">Administrateur laboratoire</h3>
              <p className="mt-2">
                {values.adminPrenom} {values.adminNom} · {values.adminFonction}
              </p>
              <p>
                {values.adminEmail} · {values.adminTelephone} · CIN {values.adminCin}
              </p>
            </section>
            <section>
              <h3 className="text-base font-bold text-ink-900">Documents déposés</h3>
              <ul className="mt-2 list-disc pl-5">
                {TYPES_DOCUMENTS.map((type) => (
                  <li key={type}>{files[type]?.name ?? "Manquant"}</li>
                ))}
              </ul>
            </section>
            <label className="flex items-start gap-3 rounded-2xl bg-surface-card p-4">
              <input
                type="checkbox"
                checked={confirmed}
                onChange={(event) => setConfirmed(event.target.checked)}
              />
              <span>
                Je confirme que les informations et les 6 documents sont exacts. La demande sera
                examinée par le Super Administrateur avant toute création d'espace laboratoire.
              </span>
            </label>
          </div>
        )}

        {submitError && <p className="text-sm text-red-600">{submitError}</p>}
        {progress != null && (
          <p className="text-sm text-muted-foreground">Envoi en cours… {progress}%</p>
        )}

        <div className="mt-2 flex justify-between gap-3">
          <Button
            type="button"
            variant="secondary"
            disabled={step === 0 || form.formState.isSubmitting}
            onClick={() => setStep((current) => Math.max(0, current - 1))}
          >
            Retour
          </Button>
          {step < STEPS.length - 1 ? (
            <Button type="button" onClick={() => void goNext()}>
              Continuer
            </Button>
          ) : (
            <Button type="submit" loading={form.formState.isSubmitting}>
              Envoyer la demande
            </Button>
          )}
        </div>
      </form>
    </Card>
  );
}
