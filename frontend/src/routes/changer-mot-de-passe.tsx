import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { Eye, EyeOff, Check, X } from "lucide-react";
import { Button, Card, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { changerMotDePasse, logout, toSessionUser } from "@/api/auth";
import { setSession, clearSession, getStatus, getSession } from "@/api/session";
import { apiClient, ApiError } from "@/api/axios";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// Politique du nouveau mot de passe — miroir exact de la politique backend
// ---------------------------------------------------------------------------

export const politiqueMotDePasse = z
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

// ---------------------------------------------------------------------------
// Règles affichées dans le checklist
// ---------------------------------------------------------------------------

const REGLES = [
  { id: "len",   label: "10 caractères minimum",  test: (v: string) => v.length >= 10 },
  { id: "lower", label: "Une lettre minuscule",    test: (v: string) => /[a-z]/.test(v) },
  { id: "upper", label: "Une lettre majuscule",    test: (v: string) => /[A-Z]/.test(v) },
  { id: "digit", label: "Un chiffre",              test: (v: string) => /[0-9]/.test(v) },
] as const;

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
// Composant : checklist des contraintes
// ---------------------------------------------------------------------------

function PasswordChecklist({ value }: { value: string }) {
  // N'afficher la liste que si l'utilisateur a commencé à taper
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

// ---------------------------------------------------------------------------
// Garde
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/changer-mot-de-passe")({
  head: () => ({
    meta: [{ title: "Changement de mot de passe — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getStatus() === "anonyme") {
      throw Route.redirect({ to: "/login" });
    }
  },
  component: ChangerMotDePassePage,
});

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

function ChangerMotDePassePage() {
  const router = useRouter();
  const session = getSession();

  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ChangerMdpFormValues>({
    resolver: zodResolver(changerMdpSchema),
    defaultValues: { ancienMotDePasse: "", nouveauMotDePasse: "", confirmation: "" },
  });

  // Valeur en direct du champ "nouveauMotDePasse" pour le checklist
  const nouveauValue = useWatch({ control, name: "nouveauMotDePasse" });

  useEffect(() => {
    if (errors.ancienMotDePasse) setFocus("ancienMotDePasse");
    else if (errors.nouveauMotDePasse) setFocus("nouveauMotDePasse");
    else if (errors.confirmation) setFocus("confirmation");
  }, [errors.ancienMotDePasse, errors.nouveauMotDePasse, errors.confirmation, setFocus]);

  const onSubmit = async (values: ChangerMdpFormValues) => {
    try {
      const response = await changerMotDePasse({
        ancienMotDePasse: values.ancienMotDePasse,
        nouveauMotDePasse: values.nouveauMotDePasse,
      });
      if (response.accessToken) {
        const user = toSessionUser(response);
        setSession(response.accessToken, user);
        apiClient.setTenant(user.tenantId);
        await router.navigate({ to: "/app" });
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("root", { message: err.message });
      } else if (err instanceof ApiError && (err.status === 0 || err.status === 503)) {
        setError("root", { message: "Serveur indisponible, réessayez." });
      } else {
        setError("root", { message: "Une erreur est survenue. Veuillez réessayer." });
      }
    }
  };

  const handleLogout = async () => {
    try { await logout(); } catch { /* déconnexion côté client quand même */ }
    finally {
      clearSession();
      await router.navigate({ to: "/login" });
    }
  };

  // Extraire les props register pour les passer à PasswordInput
  const ancienProps = register("ancienMotDePasse");
  const nouveauProps = register("nouveauMotDePasse");
  const confirmProps = register("confirmation");

  return (
    <PublicLayout>
      <Container className="flex justify-center py-16">
        <Card variant="glossy" padding="lg" className="w-full max-w-md">
          <h1 className="text-2xl font-extrabold text-ink-900">
            Changement de mot de passe
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {session?.user.mustChangePassword
              ? "Vous devez définir un nouveau mot de passe avant de continuer."
              : "Définissez un nouveau mot de passe sécurisé."}
          </p>

          {/* Erreur globale */}
          {errors.root?.message && (
            <div
              role="alert"
              aria-live="assertive"
              className="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {errors.root.message}
            </div>
          )}

          <form
            className="mt-6 flex flex-col gap-4"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {/* Ancien mot de passe */}
            <PasswordInput
              id="ancienMotDePasse"
              label="Mot de passe actuel"
              autoComplete="current-password"
              error={errors.ancienMotDePasse?.message}
              errorId="ancien-error"
              {...ancienProps}
            />

            {/* Nouveau mot de passe + checklist */}
            <div className="flex flex-col gap-1.5">
              <PasswordInput
                id="nouveauMotDePasse"
                label="Nouveau mot de passe"
                autoComplete="new-password"
                error={errors.nouveauMotDePasse?.message}
                errorId="nouveau-error"
                {...nouveauProps}
              />
              <PasswordChecklist value={nouveauValue ?? ""} />
            </div>

            {/* Confirmation */}
            <PasswordInput
              id="confirmation"
              label="Confirmer le nouveau mot de passe"
              autoComplete="new-password"
              error={errors.confirmation?.message}
              errorId="confirm-error"
              {...confirmProps}
            />

            <Button
              type="submit"
              className="mt-2"
              disabled={isSubmitting}
              loading={isSubmitting}
            >
              {isSubmitting ? "Enregistrement…" : "Changer le mot de passe"}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <button
              type="button"
              onClick={() => void handleLogout()}
              className="text-sm text-muted-foreground underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:rounded"
            >
              Se déconnecter
            </button>
          </div>
        </Card>
      </Container>
    </PublicLayout>
  );
}
