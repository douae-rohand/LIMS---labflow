import { createFileRoute, Link } from "@tanstack/react-router";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { Building2, Check, Eye, EyeOff, User, X, ArrowLeft, ArrowRight } from "lucide-react";
import { Button, Card, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { inscrire, type InscriptionBody } from "@/api/auth";
import { getStatus } from "@/api/session";
import { ApiError } from "@/api/axios";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// Garde
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/inscription")({
  head: () => ({
    meta: [
      { title: "Créer un compte — LabFlow LIMS" },
      { name: "description", content: "Créez votre compte client LabFlow LIMS." },
    ],
  }),
  beforeLoad: () => {
    if (getStatus() === "connecté") {
      throw Route.redirect({ to: "/app" });
    }
  },
  component: InscriptionPage,
});

// ---------------------------------------------------------------------------
// Politique mot de passe (miroir du backend)
// ---------------------------------------------------------------------------

const politiqueMdp = z
  .string()
  .min(10, "Au moins 10 caractères.")
  .regex(/[a-z]/, "Au moins une lettre minuscule.")
  .regex(/[A-Z]/, "Au moins une lettre majuscule.")
  .regex(/[0-9]/, "Au moins un chiffre.");

const REGLES_MDP = [
  { id: "len",   label: "10 caractères minimum", test: (v: string) => v.length >= 10 },
  { id: "lower", label: "Une lettre minuscule",   test: (v: string) => /[a-z]/.test(v) },
  { id: "upper", label: "Une lettre majuscule",   test: (v: string) => /[A-Z]/.test(v) },
  { id: "digit", label: "Un chiffre",             test: (v: string) => /[0-9]/.test(v) },
] as const;

// ---------------------------------------------------------------------------
// Schéma zod
// ---------------------------------------------------------------------------

const inscriptionSchema = z
  .object({
    typeClient: z.enum(["PARTICULIER", "ENTREPRISE"]),
    nom: z.string().min(1, "Le nom est obligatoire.").max(100, "Max 100 caractères."),
    prenom: z.string().min(1, "Le prénom est obligatoire.").max(100, "Max 100 caractères."),
    email: z
      .string()
      .min(1, "L'adresse e-mail est obligatoire.")
      .email("Format d'e-mail invalide.")
      .max(255, "Max 255 caractères."),
    telephone: z
      .string()
      .min(1, "Le téléphone est obligatoire.")
      .regex(/^\+?[0-9]{8,15}$/, "Numéro invalide (8 à 15 chiffres, + optionnel)."),
    motDePasse: politiqueMdp,
    confirmation: z.string().min(1, "La confirmation est obligatoire."),
    // Consentement obligatoirement true
    consentementCndp: z.boolean().refine((v) => v === true, {
      message: "Le consentement est obligatoire.",
    }),
    // Champs entreprise (présents dans le schéma pour conserver les valeurs au changement de type)
    raisonSociale: z.string().max(255, "Max 255 caractères.").optional(),
    ice: z.union([
      z.string().regex(/^[0-9]{15}$/, "L'ICE doit contenir exactement 15 chiffres."),
      z.literal(""),
    ]).optional(),
    adresse: z.string().max(500, "Max 500 caractères.").optional(),
  })
  .refine((d) => d.motDePasse === d.confirmation, {
    path: ["confirmation"],
    message: "Les mots de passe ne correspondent pas.",
  })
  .refine(
    (d) =>
      d.typeClient !== "ENTREPRISE" ||
      (d.raisonSociale !== undefined && d.raisonSociale.trim().length > 0),
    { path: ["raisonSociale"], message: "La raison sociale est obligatoire pour une entreprise." },
  );

type FormValues = z.infer<typeof inscriptionSchema>;

// ---------------------------------------------------------------------------
// Composant champ texte générique
// ---------------------------------------------------------------------------

interface FieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string;
  label: string;
  error?: string | undefined;
  errorId?: string | undefined;
}

function Field({ id, label, error, errorId, className, ...props }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      <input
        id={id}
        aria-describedby={error && errorId ? errorId : undefined}
        aria-invalid={!!error}
        className={cn(
          "w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm text-ink-900",
          "placeholder:text-muted-foreground transition-colors focus:border-brand-600 focus:outline-none",
          error && "border-red-400 focus:border-red-500",
          className,
        )}
        {...props}
      />
      {error && errorId && (
        <p id={errorId} role="alert" aria-live="polite" className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Champ mot de passe avec icône œil
// ---------------------------------------------------------------------------

interface PwdFieldProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "type"> {
  id: string;
  label: string;
  error?: string | undefined;
  errorId?: string | undefined;
}

function PwdField({ id, label, error, errorId, ...props }: PwdFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <input
          id={id}
          type={visible ? "text" : "password"}
          aria-describedby={error && errorId ? errorId : undefined}
          aria-invalid={!!error}
          className={cn(
            "w-full rounded-2xl border border-input bg-card px-4 py-3 pr-11 text-sm text-ink-900",
            "placeholder:text-muted-foreground transition-colors focus:border-brand-600 focus:outline-none",
            error && "border-red-400 focus:border-red-500",
          )}
          {...props}
        />
        <button
          type="button"
          aria-label={visible ? "Masquer le mot de passe" : "Afficher le mot de passe"}
          onClick={() => setVisible((v) => !v)}
          className="absolute inset-y-0 right-3 flex items-center text-muted-foreground hover:text-ink-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
        >
          {visible
            ? <EyeOff className="size-4" aria-hidden="true" />
            : <Eye className="size-4" aria-hidden="true" />}
        </button>
      </div>
      {error && errorId && (
        <p id={errorId} role="alert" aria-live="polite" className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Checklist des critères de mot de passe
// ---------------------------------------------------------------------------

function MdpChecklist({ value }: { value: string }) {
  if (!value) return null;
  return (
    <ul aria-label="Critères du mot de passe" aria-live="polite" className="mt-1 flex flex-col gap-1">
      {REGLES_MDP.map(({ id, label, test }) => {
        const ok = test(value);
        return (
          <li key={id} className="flex items-center gap-2 text-xs">
            <span
              className={cn(
                "flex size-4 shrink-0 items-center justify-center rounded-full transition-colors",
                ok ? "bg-green-500" : "bg-muted",
              )}
              aria-hidden="true"
            >
              {ok
                ? <Check className="size-2.5 text-white" strokeWidth={3} />
                : <X className="size-2.5 text-muted-foreground" strokeWidth={2.5} />}
            </span>
            <span className={cn("transition-colors", ok ? "text-green-700" : "text-muted-foreground")}>
              {label}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

// ---------------------------------------------------------------------------
// Cartes de sélection du type
// ---------------------------------------------------------------------------

interface TypeCardProps {
  value: "PARTICULIER" | "ENTREPRISE";
  selected: boolean;
  onSelect: () => void;
  icon: React.ReactNode;
  title: string;
  description: string;
}

function TypeCard({ value, selected, onSelect, icon, title, description }: TypeCardProps) {
  return (
    <label
      htmlFor={`type-${value}`}
      className={cn(
        "flex cursor-pointer flex-col gap-2 rounded-2xl border-2 p-4 transition-all select-none",
        selected
          ? "border-brand-900 bg-brand-900/5"
          : "border-brand-900/15 bg-card hover:border-brand-900/40",
      )}
    >
      <input
        type="radio"
        id={`type-${value}`}
        name="typeClient"
        value={value}
        checked={selected}
        onChange={onSelect}
        className="sr-only"
      />
      <span
        className={cn(
          "flex items-center gap-2 text-sm font-semibold",
          selected ? "text-brand-900" : "text-ink-900",
        )}
      >
        {icon}
        {title}
      </span>
      <span className="text-xs text-muted-foreground">{description}</span>
    </label>
  );
}

// ---------------------------------------------------------------------------
// Carte de succès
// ---------------------------------------------------------------------------

function CarteSucces({ onReinscription }: { onReinscription: () => void }) {
  return (
    <Card variant="glossy" padding="lg" className="mx-auto w-full max-w-md text-center">
      <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-green-100">
        <Check className="size-7 text-green-600" aria-hidden="true" />
      </div>
      <h1 className="text-2xl font-extrabold text-ink-900">Vérifiez votre boîte e-mail</h1>
      <p className="mt-3 text-sm text-muted-foreground">
        Si votre adresse e-mail est valide, vous allez recevoir un lien d'activation dans quelques
        minutes.
      </p>
      <p className="mt-2 text-sm text-muted-foreground">
        Rien reçu au bout de quelques minutes ? Vérifiez vos{" "}
        <strong>courriers indésirables</strong>, puis réessayez avec la même adresse.
      </p>
      <div className="mt-6 flex flex-col gap-3">
        <Link to="/login">
          <Button className="w-full">Se connecter</Button>
        </Link>
        <button
          type="button"
          onClick={onReinscription}
          className="text-sm text-muted-foreground underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
        >
          Réessayer avec une autre adresse
        </button>
      </div>
    </Card>
  );
}

// ---------------------------------------------------------------------------
// Mapping champs backend → noms du formulaire
// ---------------------------------------------------------------------------

const MAPPING_CHAMPS: Partial<Record<string, keyof FormValues>> = {
  nom: "nom",
  prenom: "prenom",
  email: "email",
  telephone: "telephone",
  motDePasse: "motDePasse",
  typeClient: "typeClient",
  raisonSociale: "raisonSociale",
  raisonSocialeValide: "raisonSociale",
  ice: "ice",
  adresse: "adresse",
  consentementCndp: "consentementCndp",
};

// ---------------------------------------------------------------------------
// Configuration des étapes
// ---------------------------------------------------------------------------

const STEPS = [
  "Type de compte",
  "Informations personnelles",
  "Mot de passe",
] as const;

const STEP_FIELDS: (keyof FormValues)[][] = [
  ["typeClient", "raisonSociale", "ice", "adresse"],
  ["nom", "prenom", "email", "telephone"],
  ["motDePasse", "confirmation", "consentementCndp"],
];

// ---------------------------------------------------------------------------
// Page principale
// ---------------------------------------------------------------------------

function InscriptionPage() {
  const [succes, setSucces] = useState(false);
  const [step, setStep] = useState(0);

  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    control,
    watch,
    setValue,
    reset,
    trigger,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(inscriptionSchema),
    defaultValues: {
      typeClient: "PARTICULIER",
      nom: "",
      prenom: "",
      email: "",
      telephone: "",
      motDePasse: "",
      confirmation: "",
      consentementCndp: false,
      raisonSociale: "",
      ice: "",
      adresse: "",
    },
  });

  const typeClient = watch("typeClient");
  const mdpValue = useWatch({ control, name: "motDePasse" });

  // Navigation entre étapes
  const goNext = async () => {
    const fields = STEP_FIELDS[step] ?? [];
    const valid = await trigger(fields);
    if (!valid) return;
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  const goPrevious = () => {
    setStep((current) => Math.max(current - 1, 0));
  };

  // Focus sur le premier champ en erreur après soumission
  const ORDRE_FOCUS: Array<keyof FormValues> = [
    "raisonSociale", "ice", "adresse",
    "nom", "prenom", "email", "telephone",
    "motDePasse", "confirmation", "consentementCndp",
  ];
  useEffect(() => {
    for (const champ of ORDRE_FOCUS) {
      if (errors[champ]) { setFocus(champ); return; }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [errors, setFocus]);

  // -----------------------------------------------------------------------
  // Soumission
  // -----------------------------------------------------------------------

  const onSubmit = async (values: FormValues) => {
    const body: InscriptionBody = {
      typeClient: values.typeClient,
      nom: values.nom,
      prenom: values.prenom,
      email: values.email,
      telephone: values.telephone,
      motDePasse: values.motDePasse,
      consentementCndp: true,
      raisonSociale:
        values.typeClient === "ENTREPRISE" && values.raisonSociale !== ""
          ? values.raisonSociale
          : undefined,
      ice: values.ice !== "" ? values.ice : undefined,
      adresse: values.adresse !== "" ? values.adresse : undefined,
    };

    try {
      await inscrire(body);
      setSucces(true);
    } catch (err) {
      if (!(err instanceof ApiError)) {
        setError("root", { message: "Serveur indisponible, réessayez." });
        return;
      }
      if (err.status === 429) {
        setError("root", { message: "Trop de tentatives, réessayez plus tard." });
        return;
      }
      if (err.status === 400) {
        const data = err.data;
        if (data !== null && data !== undefined && typeof data === "object") {
          const map = data as Record<string, string>;
          let premier: keyof FormValues | undefined;
          for (const [cle, msg] of Object.entries(map)) {
            const champ = MAPPING_CHAMPS[cle];
            if (champ) {
              setError(champ, { message: msg });
              if (!premier) premier = champ;
            }
          }
          if (premier) { setFocus(premier); return; }
        }
        setError("root", { message: err.message });
        return;
      }
      if (err.status === 0 || err.status === 503) {
        setError("root", { message: "Serveur indisponible, réessayez." });
        return;
      }
      setError("root", { message: "Une erreur est survenue. Veuillez réessayer." });
    }
  };

  // -----------------------------------------------------------------------
  // Rendu succès
  // -----------------------------------------------------------------------

  if (succes) {
    return (
      <PublicLayout>
        <Container className="flex justify-center py-16">
          <CarteSucces
            onReinscription={() => {
              reset();
              setSucces(false);
            }}
          />
        </Container>
      </PublicLayout>
    );
  }

  // -----------------------------------------------------------------------
  // Rendu formulaire
  // -----------------------------------------------------------------------

  return (
    <PublicLayout>
      <Container className="flex justify-center py-12 sm:py-16">
        <Card variant="glossy" padding="lg" className="w-full max-w-lg">
          <h1 className="text-2xl font-extrabold text-ink-900">Créer un compte</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Accédez aux services analytiques de votre laboratoire.
          </p>

          {/* Indicateur d'étapes */}
          <ol className="mb-6 mt-6 grid grid-cols-3 gap-2">
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

          {/* Erreur globale */}
          {errors.root?.message && (
            <div
              role="alert"
              aria-live="assertive"
              className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {errors.root.message}
            </div>
          )}

          <form
            className="mt-6 flex flex-col gap-5"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {/* ── Étape 1 : Type de compte ── */}
            {step === 0 && (
              <>
                <fieldset>
                  <legend className="mb-3 text-sm font-semibold text-ink-900">
                    Type de compte
                  </legend>
                  <div className="grid grid-cols-2 gap-3">
                    <TypeCard
                      value="PARTICULIER"
                      selected={typeClient === "PARTICULIER"}
                      onSelect={() => setValue("typeClient", "PARTICULIER")}
                      icon={<User className="size-4" aria-hidden="true" />}
                      title="Particulier"
                      description="Vous déposez des analyses en votre nom."
                    />
                    <TypeCard
                      value="ENTREPRISE"
                      selected={typeClient === "ENTREPRISE"}
                      onSelect={() => setValue("typeClient", "ENTREPRISE")}
                      icon={<Building2 className="size-4" aria-hidden="true" />}
                      title="Entreprise"
                      description="Vous représentez une organisation."
                    />
                  </div>
                  {errors.typeClient && (
                    <p role="alert" aria-live="polite" className="mt-1 text-xs text-red-600">
                      {errors.typeClient.message}
                    </p>
                  )}
                </fieldset>

                {/* ── Champs entreprise (conditionnels) ── */}
                {typeClient === "ENTREPRISE" && (
                  <div className="flex flex-col gap-4 rounded-2xl border border-brand-900/10 bg-surface-card p-4">
                    <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                      Informations de l'entreprise
                    </p>
                    <Field
                      id="raisonSociale"
                      label="Raison sociale"
                      autoComplete="organization"
                      placeholder="SARL Acme Analyses"
                      error={errors.raisonSociale?.message}
                      errorId="rs-error"
                      {...register("raisonSociale")}
                    />
                    <Field
                      id="ice"
                      label="ICE (optionnel)"
                      inputMode="numeric"
                      placeholder="15 chiffres"
                      maxLength={15}
                      error={errors.ice?.message}
                      errorId="ice-error"
                      {...register("ice")}
                    />
                    <Field
                      id="adresse"
                      label="Adresse (optionnelle)"
                      autoComplete="street-address"
                      error={errors.adresse?.message}
                      errorId="adresse-error"
                      {...register("adresse")}
                    />
                  </div>
                )}
              </>
            )}

            {/* ── Étape 2 : Informations personnelles ── */}
            {step === 1 && (
              <>
                <div className="grid grid-cols-2 gap-4">
                  <Field
                    id="prenom"
                    label="Prénom"
                    autoComplete="given-name"
                    error={errors.prenom?.message}
                    errorId="prenom-error"
                    {...register("prenom")}
                  />
                  <Field
                    id="nom"
                    label="Nom"
                    autoComplete="family-name"
                    error={errors.nom?.message}
                    errorId="nom-error"
                    {...register("nom")}
                  />
                </div>

                <Field
                  id="email"
                  label="Adresse e-mail"
                  type="email"
                  autoComplete="email"
                  placeholder="vous@exemple.ma"
                  error={errors.email?.message}
                  errorId="email-error"
                  {...register("email")}
                />

                <Field
                  id="telephone"
                  label="Téléphone"
                  type="tel"
                  autoComplete="tel"
                  inputMode="tel"
                  placeholder="+212600000000"
                  error={errors.telephone?.message}
                  errorId="tel-error"
                  {...register("telephone")}
                />
              </>
            )}

            {/* ── Étape 3 : Mot de passe ── */}
            {step === 2 && (
              <>
                <div>
                  <PwdField
                    id="motDePasse"
                    label="Mot de passe"
                    autoComplete="new-password"
                    error={errors.motDePasse?.message}
                    errorId="mdp-error"
                    {...register("motDePasse")}
                  />
                  <MdpChecklist value={mdpValue ?? ""} />
                </div>

                <PwdField
                  id="confirmation"
                  label="Confirmer le mot de passe"
                  autoComplete="new-password"
                  error={errors.confirmation?.message}
                  errorId="confirm-error"
                  {...register("confirmation")}
                />

                {/* ── Consentement CNDP ── */}
                <div className="flex flex-col gap-1.5">
                  <label className="flex cursor-pointer items-start gap-3">
                    <input
                      type="checkbox"
                      id="consentementCndp"
                      aria-describedby={errors.consentementCndp ? "cndp-error" : undefined}
                      aria-invalid={!!errors.consentementCndp}
                      className="mt-0.5 size-4 shrink-0 rounded border-input accent-brand-900"
                      {...register("consentementCndp")}
                    />
                    <span className="text-sm leading-relaxed text-muted-foreground">
                      J'accepte que mes données personnelles soient traitées par LabFlow
                      conformément à la loi 09-08 (CNDP).
                    </span>
                  </label>
                  {errors.consentementCndp && (
                    <p
                      id="cndp-error"
                      role="alert"
                      aria-live="polite"
                      className="text-xs text-red-600"
                    >
                      {errors.consentementCndp.message}
                    </p>
                  )}
                </div>
              </>
            )}

            {/* ── Navigation entre étapes ── */}
            <div className="flex gap-3">
              {step > 0 && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={goPrevious}
                  className="flex-1"
                >
                  <ArrowLeft className="mr-2 size-4" />
                  Précédent
                </Button>
              )}
              {step < STEPS.length - 1 ? (
                <Button
                  type="button"
                  onClick={goNext}
                  className="flex-1"
                >
                  Suivant
                  <ArrowRight className="ml-2 size-4" />
                </Button>
              ) : (
                <Button
                  type="submit"
                  className="flex-1"
                  disabled={isSubmitting}
                  loading={isSubmitting}
                >
                  {isSubmitting ? "Envoi en cours…" : "Créer mon compte"}
                </Button>
              )}
            </div>
          </form>

          <p className="mt-5 text-center text-sm text-muted-foreground">
            Déjà un compte ?{" "}
            <Link
              to="/login"
              className="font-semibold text-brand-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
            >
              Se connecter
            </Link>
          </p>
        </Card>
      </Container>
    </PublicLayout>
  );
}
