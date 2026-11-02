import { useState, useEffect } from "react";
import { Link } from "@tanstack/react-router";
import { Pencil, Eye, EyeOff, Check, X, CheckCircle } from "lucide-react";
import { Button, Card, Skeleton, Input, Label, Badge, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { messageErreurProfil } from "@/api/profilClient";
import { useModifierProfil, useProfilClient } from "@/hooks/useProfilClient";
import { ProfilConsentement } from "./ProfilConsentement";
import { cn } from "@/lib/utils";
import { formatDateFr } from "@/lib/dates";
import { changerMotDePasse, motDePasseOublie } from "@/api/auth";
import { modifierUtilisateur, type ModifierUtilisateurRequest } from "@/api/utilisateurs";
import { z } from "zod";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError } from "@/api/axios";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { PROFIL_CLIENT_QUERY_KEY } from "@/hooks/useProfilClient";

const politiqueMotDePasse = z
  .string()
  .min(10, "Au moins 10 caractères.")
  .regex(/[a-z]/, "Au moins une lettre minuscule.")
  .regex(/[A-Z]/, "Au moins une lettre majuscule.")
  .regex(/[0-9]/, "Au moins un chiffre.");

const changerMdpSchema = z
  .object({
    ancienMotDePasse: z.string().min(1, "L'ancien mot de passe est obligatoire."),
    nouveauMotDePasse: politiqueMotDePasse,
    confirmation: z.string().min(1, "La confirmation est obligatoire."),
  })
  .refine((d) => d.nouveauMotDePasse === d.confirmation, {
    path: ["confirmation"],
    message: "Les mots de passe ne correspondent pas.",
  });

type ChangerMdpFormValues = z.infer<typeof changerMdpSchema>;

const REGLES = [
  { id: "len",   label: "10 caractères minimum",  test: (v: string) => v.length >= 10 },
  { id: "lower", label: "Une lettre minuscule",    test: (v: string) => /[a-z]/.test(v) },
  { id: "upper", label: "Une lettre majuscule",    test: (v: string) => /[A-Z]/.test(v) },
  { id: "digit", label: "Un chiffre",              test: (v: string) => /[0-9]/.test(v) },
] as const;

const schemaMdpOublie = z.object({
  email: z.string().min(1, "L'adresse e-mail est obligatoire.").email("Format d'e-mail invalide."),
});

type MdpOublieValues = z.infer<typeof schemaMdpOublie>;

// ---------------------------------------------------------------------------
// Composant : champ mot de passe avec bouton œil
// ---------------------------------------------------------------------------

interface PasswordInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  id: string;
  label: string;
  error?: string | undefined;
  errorId?: string | undefined;
}

function PasswordInput({ id, label, error, errorId, className, ...props }: PasswordInputProps) {
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
            "placeholder:text-muted-foreground transition-colors duration-200 focus:border-brand-600",
            "focus:outline-none",
            error && "border-red-400 focus:border-red-500",
            className,
          )}
          {...props}
        />
        <button
          type="button"
          aria-label={visible ? "Masquer le mot de passe" : "Afficher le mot de passe"}
          onClick={() => setVisible((v) => !v)}
          className="absolute inset-y-0 right-3 flex items-center text-muted-foreground hover:text-ink-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:rounded"
        >
          {visible
            ? <EyeOff className="size-4" aria-hidden="true" />
            : <Eye className="size-4" aria-hidden="true" />
          }
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

function PasswordChecklist({ value }: { value: string }) {
  if (!value) return null;

  return (
    <ul
      aria-label="Contraintes du mot de passe"
      className="mt-1 flex flex-col gap-1"
    >
      {REGLES.map(({ id, label, test }) => {
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

type Onglet = "infos" | "securite";

const ONGLETS: { id: Onglet; label: string }[] = [
  { id: "infos",    label: "Informations personnelles" },
  { id: "securite", label: "Paramètres et sécurité" },
];

// ---------------------------------------------------------------------------
// Composant principal
// ---------------------------------------------------------------------------

const infosPersonnellesSchema = z.object({
  nom: z.string().min(1, "Le nom est obligatoire."),
  prenom: z.string().min(1, "Le prénom est obligatoire."),
  telephone: z.string().optional(),
  adresse: z.string().optional(),
});

type InfosPersonnellesValues = z.infer<typeof infosPersonnellesSchema>;

/** Page « Mon profil » : onglets Informations / Paramètres & sécurité. */
export function ProfilClient() {
  const [onglet, setOnglet] = useState<Onglet>("infos");
  const [modeEdition, setModeEdition] = useState(false);
  const [mdpOuvert, setMdpOuvert] = useState(false);
  const [mdpOublieOuvert, setMdpOublieOuvert] = useState(false);
  const [mdpOublieSucces, setMdpOublieSucces] = useState(false);
  const [mdpOublieCooldown, setMdpOublieCooldown] = useState(0);

  const queryClient = useQueryClient();
  const profilQuery = useProfilClient();
  const enregistrement = useModifierProfil();

  const {
    register: registerInfos,
    handleSubmit: handleSubmitInfos,
    reset: resetInfos,
    formState: { errors: errorsInfos, isDirty: isDirtyInfos },
  } = useForm<InfosPersonnellesValues>({
    resolver: zodResolver(infosPersonnellesSchema),
    defaultValues: {
      nom: profilQuery.data?.nom ?? "",
      prenom: profilQuery.data?.prenom ?? "",
      telephone: profilQuery.data?.telephone ?? "",
      adresse: profilQuery.data?.adresse ?? "",
    },
  });

  // Mettre à jour les valeurs par défaut quand le profil est chargé
  useEffect(() => {
    if (profilQuery.data) {
      resetInfos({
        nom: profilQuery.data.nom ?? "",
        prenom: profilQuery.data.prenom ?? "",
        telephone: profilQuery.data.telephone ?? "",
        adresse: profilQuery.data.adresse ?? "",
      });
    }
  }, [profilQuery.data, resetInfos]);

  const onSubmitInfos = async (values: InfosPersonnellesValues) => {
    if (!profilQuery.data?.utilisateurId) {
      toast.error("Impossible de modifier votre profil : ID utilisateur introuvable.");
      return;
    }

    try {
      const utilisateurBody: ModifierUtilisateurRequest = {};
      if (values.nom !== profilQuery.data.nom) utilisateurBody.nom = values.nom;
      if (values.prenom !== profilQuery.data.prenom) utilisateurBody.prenom = values.prenom;
      if (values.telephone !== profilQuery.data.telephone && values.telephone) {
        utilisateurBody.telephone = values.telephone;
      }

      const clientBody: { adresse?: string } = {};
      if (values.adresse !== profilQuery.data.adresse) {
        clientBody.adresse = values.adresse || "";
      }

      if (Object.keys(utilisateurBody).length === 0 && Object.keys(clientBody).length === 0) {
        setModeEdition(false);
        return;
      }

      // Modifier l'utilisateur
      if (Object.keys(utilisateurBody).length > 0) {
        await modifierUtilisateur(profilQuery.data.utilisateurId, utilisateurBody);
      }

      // Modifier le client (adresse)
      if (Object.keys(clientBody).length > 0) {
        await enregistrement.mutateAsync(clientBody);
      }

      // Rafraîchir le profil client
      await queryClient.invalidateQueries({ queryKey: PROFIL_CLIENT_QUERY_KEY });

      toast.success("Informations personnelles mises à jour.");
      setModeEdition(false);
    } catch (err) {
      if (err instanceof ApiError) {
        toast.error(err.message || "Une erreur est survenue lors de la modification.");
      } else {
        toast.error("Une erreur est survenue lors de la modification.");
      }
    }
  };

  const {
    register,
    handleSubmit,
    setError,
    control,
    reset: resetMdp,
    formState: { errors, isSubmitting },
  } = useForm<ChangerMdpFormValues>({
    resolver: zodResolver(changerMdpSchema),
    defaultValues: { ancienMotDePasse: "", nouveauMotDePasse: "", confirmation: "" },
  });

  const nouveauMdpValue = useWatch({ control, name: "nouveauMotDePasse" });

  const {
    register: registerMdpOublie,
    handleSubmit: handleSubmitMdpOublie,
    setError: setErrorMdpOublie,
    formState: { errors: errorsMdpOublie, isSubmitting: isSubmittingMdpOublie },
  } = useForm<MdpOublieValues>({
    resolver: zodResolver(schemaMdpOublie),
    defaultValues: { email: profilQuery.data?.email ?? "" },
  });

  // Cooldown timer for password forgot
  useEffect(() => {
    if (mdpOublieCooldown <= 0) return;
    const timer = setInterval(() => {
      setMdpOublieCooldown((prev) => {
        if (prev <= 1) { clearInterval(timer); return 0; }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [mdpOublieCooldown]);

  const onSubmitMdpOublie = async (values: MdpOublieValues) => {
    if (mdpOublieCooldown > 0 || isSubmittingMdpOublie) return;

    try {
      await motDePasseOublie(values.email);
      setMdpOublieSucces(true);
      setMdpOublieCooldown(60);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 429) {
          setErrorMdpOublie("root", { message: "Trop de tentatives. Réessayez dans quelques instants." });
          setMdpOublieCooldown(60);
        } else {
          setErrorMdpOublie("root", { message: "Une erreur est survenue. Veuillez réessayer." });
        }
      } else {
        setErrorMdpOublie("root", { message: "Une erreur est survenue. Veuillez réessayer." });
      }
    }
  };

  const onSubmitMdp = async (values: ChangerMdpFormValues) => {
    try {
      await changerMotDePasse({
        ancienMotDePasse: values.ancienMotDePasse,
        nouveauMotDePasse: values.nouveauMotDePasse,
      });
      setError("root", { type: "success", message: "Mot de passe changé avec succès." });
      // Reset form
      (document.getElementById("form-changer-mdp") as HTMLFormElement)?.reset();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("root", { message: err.message });
      } else {
        setError("root", { message: "Une erreur est survenue. Veuillez réessayer." });
      }
    }
  };

  const handleMotDePasseOublie = () => {
    setMdpOublieOuvert(true);
  };

  return (
    <Container className="py-10">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-extrabold text-ink-900">Mon profil</h1>
        {profilQuery.data && (
          <Badge variant="outline" size="sm">
            {profilQuery.data.typeClient === "ENTREPRISE" ? "Entreprise" : "Particulier"}
          </Badge>
        )}
      </div>
      <p className="mt-1 text-sm text-muted-foreground">
        Consultez et mettez à jour vos informations personnelles.
      </p>

      {/* Barre d'onglets */}
      <div
        role="tablist"
        aria-label="Sections du profil"
        className="mt-6 flex gap-1 rounded-2xl border border-brand-900/10 bg-surface-card p-1 w-fit"
      >
        {ONGLETS.map(({ id, label }) => (
          <button
            key={id}
            role="tab"
            aria-selected={onglet === id}
            aria-controls={`panel-${id}`}
            id={`tab-${id}`}
            type="button"
            onClick={() => setOnglet(id)}
            className={cn(
              "rounded-xl px-4 py-2 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
              onglet === id
                ? "bg-brand-900 text-primary-foreground"
                : "text-ink-900/70 hover:text-brand-900 hover:bg-brand-900/8",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Chargement */}
      {profilQuery.isPending && (
        <div aria-busy="true" className="mt-8 flex flex-col gap-6">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      )}

      {/* Erreur */}
      {(profilQuery.isError || (!profilQuery.isPending && profilQuery.data === undefined)) && (
        <Card variant="surface" className="mt-8" role="alert">
          <p className="text-sm font-semibold text-ink-900">
            {messageErreurProfil(profilQuery.error)}
          </p>
          <div className="mt-4">
            <Button variant="secondary" size="sm" onClick={() => void profilQuery.refetch()}>
              Réessayer
            </Button>
          </div>
        </Card>
      )}

      {/* Contenu des onglets */}
      {profilQuery.data !== undefined && (
        <>
          {/* ── Onglet : Informations personnelles ── */}
          <div
            id="panel-infos"
            role="tabpanel"
            aria-labelledby="tab-infos"
            hidden={onglet !== "infos"}
            className="mt-8 flex flex-col gap-8"
          >
            {/* Identité */}
            <Card variant="flat" padding="lg">
              <div className="flex items-start justify-between">
                <div>
                  <h2 className="text-lg font-extrabold text-ink-900">Informations personnelles</h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {modeEdition ? "Modifiez vos informations ci-dessous." : "Vos informations personnelles."}
                  </p>
                </div>
                {!modeEdition && (
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={<Pencil className="size-4" aria-hidden="true" />}
                    iconPosition="left"
                    onClick={() => setModeEdition(true)}
                  >
                    Modifier
                  </Button>
                )}
              </div>

              {modeEdition ? (
                <form className="mt-6 grid gap-4 sm:grid-cols-2" onSubmit={handleSubmitInfos(onSubmitInfos)}>
                  <div className="space-y-2">
                    <Label htmlFor="edit-nom">Nom</Label>
                    <Input
                      id="edit-nom"
                      {...registerInfos("nom")}
                      aria-invalid={errorsInfos.nom ? true : undefined}
                      aria-describedby={errorsInfos.nom ? "edit-nom-erreur" : undefined}
                    />
                    {errorsInfos.nom && (
                      <p id="edit-nom-erreur" role="alert" className="text-sm text-red-600">
                        {errorsInfos.nom.message}
                      </p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-prenom">Prénom</Label>
                    <Input
                      id="edit-prenom"
                      {...registerInfos("prenom")}
                      aria-invalid={errorsInfos.prenom ? true : undefined}
                      aria-describedby={errorsInfos.prenom ? "edit-prenom-erreur" : undefined}
                    />
                    {errorsInfos.prenom && (
                      <p id="edit-prenom-erreur" role="alert" className="text-sm text-red-600">
                        {errorsInfos.prenom.message}
                      </p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-email">Adresse e-mail</Label>
                    <Input id="edit-email" defaultValue={profilQuery.data.email ?? ""} disabled />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-telephone">Téléphone</Label>
                    <Input
                      id="edit-telephone"
                      {...registerInfos("telephone")}
                      aria-invalid={errorsInfos.telephone ? true : undefined}
                      aria-describedby={errorsInfos.telephone ? "edit-telephone-erreur" : undefined}
                    />
                    {errorsInfos.telephone && (
                      <p id="edit-telephone-erreur" role="alert" className="text-sm text-red-600">
                        {errorsInfos.telephone.message}
                      </p>
                    )}
                  </div>
                  <div className="sm:col-span-2 space-y-2">
                    <Label htmlFor="edit-adresse">Adresse</Label>
                    <Textarea
                      id="edit-adresse"
                      rows={3}
                      maxLength={500}
                      {...registerInfos("adresse")}
                      aria-invalid={errorsInfos.adresse ? true : undefined}
                      aria-describedby="edit-adresse-aide"
                    />
                    <p id="edit-adresse-aide" className="text-xs text-muted-foreground">
                      Videz ce champ puis enregistrez pour supprimer l&apos;adresse enregistrée.
                    </p>
                    {errorsInfos.adresse && (
                      <p role="alert" className="text-sm text-red-600">
                        {errorsInfos.adresse.message}
                      </p>
                    )}
                  </div>
                  <div className="sm:col-span-2 flex gap-3 justify-end">
                    <Button type="button" variant="ghost" onClick={() => setModeEdition(false)}>
                      Annuler
                    </Button>
                    <Button type="submit" disabled={!isDirtyInfos}>
                      Enregistrer
                    </Button>
                  </div>
                </form>
              ) : (
                <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                  {([
                    ["Nom", profilQuery.data.nom],
                    ["Prénom", profilQuery.data.prenom],
                    ["Adresse e-mail", profilQuery.data.email],
                    ["Téléphone", profilQuery.data.telephone],
                    ["Adresse", profilQuery.data.adresse],
                    ["Compte créé le", formatDateFr(profilQuery.data.dateCreation)],
                  ] as Array<[string, string | null | undefined]>).map(([lib, val]) => (
                    <div key={lib}>
                      <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        {lib}
                      </dt>
                      <dd className="mt-1 text-sm font-semibold text-ink-900">{val ?? "—"}</dd>
                    </div>
                  ))}
                </dl>
              )}
            </Card>

            {/* Informations entreprise (uniquement pour entreprise) */}
            {profilQuery.data.typeClient === "ENTREPRISE" && (
              <Card variant="flat" padding="lg">
                <h2 className="text-lg font-extrabold text-ink-900">Informations entreprise</h2>
                <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                  {(
                    [
                      ["Raison sociale", profilQuery.data.raisonSociale],
                      ["ICE", profilQuery.data.ice],
                    ] as Array<[string, string | null | undefined]>
                  ).map(([lib, val]) => (
                    <div key={lib}>
                      <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        {lib}
                      </dt>
                      <dd className="mt-1 text-sm font-semibold text-ink-900">{val ?? "—"}</dd>
                    </div>
                  ))}
                </dl>
              </Card>
            )}
          </div>

          {/* ── Onglet : Paramètres et sécurité ── */}
          <div
            id="panel-securite"
            role="tabpanel"
            aria-labelledby="tab-securite"
            hidden={onglet !== "securite"}
            className="mt-8 flex flex-col gap-8"
          >
            {/* Changement de mot de passe */}
            <Card variant="flat" padding="lg">
              <div className="flex items-start justify-between">
                <div>
                  <h2 className="text-lg font-extrabold text-ink-900">Mot de passe</h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Modifiez votre mot de passe ou réinitialisez-le si vous l'avez oublié.
                  </p>
                </div>
                {!mdpOuvert && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setMdpOuvert(true)}
                  >
                    Changer mon mot de passe
                  </Button>
                )}
              </div>

              {mdpOuvert && (
                <div className="mt-6">
                  {/* Erreur globale */}
                  {errors.root?.message && (
                    <div
                      role="alert"
                      aria-live="assertive"
                      className={cn(
                        "mb-4 rounded-2xl px-4 py-3 text-sm",
                        errors.root.type === "success"
                          ? "bg-green-50 text-green-700"
                          : "bg-red-50 text-red-700"
                      )}
                    >
                      {errors.root.message}
                    </div>
                  )}

                  <form
                    id="form-changer-mdp"
                    className="flex flex-col gap-4"
                    onSubmit={handleSubmit(onSubmitMdp)}
                    noValidate
                  >
                    {/* Ancien mot de passe */}
                    <PasswordInput
                      id="ancienMotDePasse"
                      label="Mot de passe actuel"
                      autoComplete="current-password"
                      error={errors.ancienMotDePasse?.message}
                      errorId="ancien-error"
                      {...register("ancienMotDePasse")}
                    />

                    {/* Nouveau mot de passe + checklist */}
                    <div className="flex flex-col gap-1.5">
                      <PasswordInput
                        id="nouveauMotDePasse"
                        label="Nouveau mot de passe"
                        autoComplete="new-password"
                        error={errors.nouveauMotDePasse?.message}
                        errorId="nouveau-error"
                        {...register("nouveauMotDePasse")}
                      />
                      <PasswordChecklist value={nouveauMdpValue ?? ""} />
                    </div>

                    {/* Confirmation */}
                    <PasswordInput
                      id="confirmation"
                      label="Confirmer le nouveau mot de passe"
                      autoComplete="new-password"
                      error={errors.confirmation?.message}
                      errorId="confirm-error"
                      {...register("confirmation")}
                    />

                    <div className="flex gap-3">
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => {
                          setMdpOuvert(false);
                          setError("root", { message: "" });
                          (document.getElementById("form-changer-mdp") as HTMLFormElement)?.reset();
                        }}
                      >
                        Annuler
                      </Button>
                      <Button
                        type="submit"
                        disabled={isSubmitting}
                        loading={isSubmitting}
                      >
                        {isSubmitting ? "Enregistrement…" : "Changer le mot de passe"}
                      </Button>
                    </div>
                  </form>

                  <div className="mt-4 pt-4 border-t border-brand-900/10">
                    {!mdpOublieOuvert ? (
                      <button
                        type="button"
                        onClick={handleMotDePasseOublie}
                        className="text-sm text-muted-foreground underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:rounded"
                      >
                        Mot de passe oublié ?
                      </button>
                    ) : (
                      <div className="flex flex-col gap-4">
                        <h3 className="text-sm font-semibold text-ink-900">Réinitialiser le mot de passe</h3>
                        <p className="text-xs text-muted-foreground">
                          Un lien de réinitialisation sera envoyé à votre adresse e-mail.
                        </p>

                        {errorsMdpOublie.root?.message && (
                          <div
                            role="alert"
                            aria-live="assertive"
                            className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700"
                          >
                            {errorsMdpOublie.root.message}
                          </div>
                        )}

                        {mdpOublieSucces && (
                          <div
                            role="status"
                            aria-live="polite"
                            className="flex items-start gap-3 rounded-2xl bg-green-50 px-4 py-3 text-sm text-green-800 border border-green-200"
                          >
                            <CheckCircle className="size-4 shrink-0 mt-0.5 text-green-600" aria-hidden="true" />
                            <span>
                              Si un compte actif existe pour cette adresse, un lien de réinitialisation
                              a été envoyé. Vérifiez également vos courriers indésirables.
                            </span>
                          </div>
                        )}

                        <form
                          className="flex flex-col gap-3"
                          onSubmit={handleSubmitMdpOublie(onSubmitMdpOublie)}
                          noValidate
                        >
                          <div className="flex flex-col gap-1.5">
                            <Label htmlFor="mdp-oublie-email">Adresse e-mail</Label>
                            <Input
                              id="mdp-oublie-email"
                              type="email"
                              disabled
                              {...registerMdpOublie("email")}
                            />
                          </div>

                          <div className="flex gap-2">
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                setMdpOublieOuvert(false);
                                setMdpOublieSucces(false);
                                setErrorMdpOublie("root", { message: "" });
                              }}
                            >
                              Annuler
                            </Button>
                            <Button
                              type="submit"
                              size="sm"
                              disabled={isSubmittingMdpOublie || mdpOublieCooldown > 0}
                              loading={isSubmittingMdpOublie}
                            >
                              {mdpOublieCooldown > 0
                                ? `Renvoyer (${mdpOublieCooldown} s)`
                                : isSubmittingMdpOublie
                                ? "Envoi..."
                                : "Envoyer le lien"}
                            </Button>
                          </div>
                        </form>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </Card>

            {/* Consentement CNDP */}
            <ProfilConsentement profil={profilQuery.data} />
          </div>
        </>
      )}
    </Container>
  );
}
