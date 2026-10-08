import { createFileRoute, Link, useRouter } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button, Card, Input, Label } from "@/components/lab";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";
import { login, toSessionUser } from "@/api/auth";
import { setSession, getSession, getStatus } from "@/api/session";
import { apiClient, ApiError } from "@/api/axios";
import { cn } from "@/lib/utils";
import { RenvoyerLienActivation } from "@/components/auth/RenvoyerLienActivation";

// ---------------------------------------------------------------------------
// Garde : si déjà connecté → /app (ou /changer-mot-de-passe)
// restoreSession() a tourné avant createRoot → statut est final ici
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Connexion — LabFlow LIMS" },
      { name: "description", content: "Accédez à votre espace laboratoire LabFlow." },
    ],
  }),
  beforeLoad: () => {
    const status = getStatus();
    if (status === "connecté") {
      const session = getSession();
      if (session?.user.mustChangePassword) {
        throw Route.redirect({ to: "/changer-mot-de-passe" });
      }
      throw Route.redirect({ to: "/app" });
    }
  },
  component: LoginPage,
});

// ---------------------------------------------------------------------------
// Composant champ mot de passe avec icône œil
// ---------------------------------------------------------------------------

function PwdField({ id, label, error, errorId, ...props }: any) {
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
// Schéma de validation
// ---------------------------------------------------------------------------

const loginSchema = z.object({
  email: z
    .string()
    .min(1, "L'email est obligatoire.")
    .email("Format d'email invalide."),
  motDePasse: z.string().min(1, "Le mot de passe est obligatoire."),
});

type LoginFormValues = z.infer<typeof loginSchema>;

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

function LoginPage() {
  const router = useRouter();

  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  // Focus sur le premier champ en erreur après soumission
  useEffect(() => {
    if (errors.email) {
      setFocus("email");
    } else if (errors.motDePasse) {
      setFocus("motDePasse");
    }
  }, [errors.email, errors.motDePasse, setFocus]);

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const response = await login({
        email: values.email,
        motDePasse: values.motDePasse,
      });

      // Cas 2FA (a) ou (b) : n'enregistre rien, n'expose jamais le twoFactorToken
      if (response.requiresTwoFactor) {
        setError("root", {
          message:
            "La double authentification n'est pas encore disponible dans cette interface.",
        });
        return;
      }

      // Cas (c) : accès direct
      if (response.accessToken) {
        const user = toSessionUser(response);
        setSession(response.accessToken, user);
        apiClient.setTenant(user.tenantId);

        if (user.mustChangePassword) {
          await router.navigate({ to: "/changer-mot-de-passe" });
        } else {
          await router.navigate({ to: "/app" });
        }
      }
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          // Message unique — jamais "email inconnu" vs "mauvais mot de passe"
          setError("root", { message: "Identifiants invalides." });
        } else if (err.status === 0 || err.status === 503) {
          setError("root", { message: "Serveur indisponible, réessayez." });
        } else if (
          (err.status === 422 || err.status === 403) &&
          err.message.includes('COMPTE_NON_ACTIVE')
        ) {
          setError("root", {
            type: "compteNonActive",
            message:
              "Votre compte n'est pas encore activé. Vérifiez votre boîte e-mail " +
              "(et vos courriers indésirables) ou refaites votre inscription pour recevoir un nouveau lien.",
          });
        } else {
          setError("root", {
            message: "Une erreur est survenue. Veuillez réessayer.",
          });
        }
      } else {
        setError("root", {
          message: "Serveur indisponible, réessayez.",
        });
      }
    }
  };
  return (
    <PublicLayout>
      <Container className="grid items-center gap-10 py-16 lg:grid-cols-2">
        <Card variant="glossy" padding="lg" className="mx-auto w-full max-w-md">
          <h1 className="text-2xl font-extrabold text-ink-900">Se connecter</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Espace sécurisé de votre laboratoire.
          </p>

          {/* Erreur globale accessible */}
          {errors.root?.message && (
            <div
              role="alert"
              aria-live="assertive"
              className={`mt-4 rounded-2xl px-4 py-3 text-sm ${
                errors.root.type === "compteNonActive"
                  ? "bg-amber-50 text-amber-800 border border-amber-200"
                  : "bg-red-50 text-red-700"
              }`}
            >
              <p>{errors.root.message}</p>
              {errors.root.type === "compteNonActive" && (
                <RenvoyerLienActivation emailInitial={getValues("email")} />
              )}
            </div>
          )}

          <form
            className="mt-6 flex flex-col gap-4"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {/* Email */}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email professionnel</Label>
              <Input
                id="email"
                type="email"
                placeholder="nom@laboratoire.ma"
                autoComplete="username"
                aria-describedby={errors.email ? "email-error" : undefined}
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              {errors.email && (
                <p
                  id="email-error"
                  role="alert"
                  aria-live="polite"
                  className="text-xs text-red-600"
                >
                  {errors.email.message}
                </p>
              )}
            </div>

            {/* Mot de passe */}
            <PwdField
              id="motDePasse"
              label="Mot de passe"
              autoComplete="current-password"
              error={errors.motDePasse?.message}
              errorId="mdp-error"
              {...register("motDePasse")}
            />

            <Button
              type="submit"
              className="mt-2"
              disabled={isSubmitting}
              loading={isSubmitting}
            >
              {isSubmitting ? "Connexion…" : "Continuer"}
            </Button>
          </form>

          <p className="mt-5 text-center text-sm text-muted-foreground">
            Vous n'avez pas encore de compte ?{" "}
            <Link
              to="/inscription"
              className="font-semibold text-brand-900 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
            >
              S'inscrire
            </Link>
          </p>
          <p className="mt-2 text-center text-sm text-muted-foreground">
            <Link
              to="/mot-de-passe-oublie"
              className="underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
            >
              Mot de passe oublié ?
            </Link>
          </p>
        </Card>

        <Photo
          src={photos.technicianMicroscope.src}
          alt={photos.technicianMicroscope.alt}
          className="hidden aspect-[4/3] lg:block"
        />
      </Container>
    </PublicLayout>
  );
}
