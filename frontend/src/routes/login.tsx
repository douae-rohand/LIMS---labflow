import { createFileRoute, useRouter } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect } from "react";
import { Button, Card, Input, Label } from "@/components/lab";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";
import { login, toSessionUser } from "@/api/auth";
import { setSession, getSession, getStatus } from "@/api/session";
import { apiClient, ApiError } from "@/api/axios";

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
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="motDePasse">Mot de passe</Label>
              <Input
                id="motDePasse"
                type="password"
                autoComplete="current-password"
                aria-describedby={errors.motDePasse ? "mdp-error" : undefined}
                aria-invalid={!!errors.motDePasse}
                {...register("motDePasse")}
              />
              {errors.motDePasse && (
                <p
                  id="mdp-error"
                  role="alert"
                  aria-live="polite"
                  className="text-xs text-red-600"
                >
                  {errors.motDePasse.message}
                </p>
              )}
            </div>

            <Button
              type="submit"
              className="mt-2"
              disabled={isSubmitting}
              loading={isSubmitting}
            >
              {isSubmitting ? "Connexion…" : "Continuer"}
            </Button>
          </form>
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
