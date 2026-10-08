import { createFileRoute, Link } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { CheckCircle } from "lucide-react";
import { Button, Card, Input, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { motDePasseOublie } from "@/api/auth";
import { getStatus } from "@/api/session";
import { ApiError } from "@/api/axios";

// ---------------------------------------------------------------------------
// Garde
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/mot-de-passe-oublie")({
  head: () => ({
    meta: [{ title: "Mot de passe oublié — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getStatus() === "connecté") {
      throw Route.redirect({ to: "/app" });
    }
  },
  component: MotDePasseOubliePage,
});

// ---------------------------------------------------------------------------
// Schéma
// ---------------------------------------------------------------------------

const schema = z.object({
  email: z
    .string()
    .min(1, "L'adresse e-mail est obligatoire.")
    .email("Format d'e-mail invalide."),
});

type FormValues = z.infer<typeof schema>;

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

function MotDePasseOubliePage() {
  const [succes, setSucces] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  // Focus sur le champ email en cas d'erreur
  useEffect(() => {
    if (errors.email) setFocus("email");
  }, [errors.email, setFocus]);

  // Décompte du cooldown — même logique que RenvoyerLienActivation
  // (pas de redémarrage au re-rendu, nettoyage au démontage)
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) { clearInterval(timer); return 0; }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const onSubmit = async (values: FormValues) => {
    if (cooldown > 0 || isSubmitting) return;

    try {
      await motDePasseOublie(values.email);
      // Toujours 202 — afficher le message générique
      setSucces(true);
      setCooldown(60);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 429) {
          setError("root", { message: "Trop de tentatives. Réessayez dans quelques instants." });
          setCooldown(60);
        } else if (err.status === 0 || err.status === 503) {
          setError("root", { message: "Serveur indisponible. Réessayez." });
        } else {
          setError("root", { message: "Une erreur est survenue. Veuillez réessayer." });
        }
      } else {
        setError("root", { message: "Serveur indisponible. Réessayez." });
      }
    }
  };

  return (
    <PublicLayout>
      <Container className="flex justify-center py-16">
        <Card variant="glossy" padding="lg" className="w-full max-w-md">
          <h1 className="text-2xl font-extrabold text-ink-900">Mot de passe oublié</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Saisissez votre adresse e-mail pour recevoir un lien de réinitialisation.
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

          {/* Message générique de succès (202) */}
          {succes && (
            <div
              role="status"
              aria-live="polite"
              className="mt-4 flex items-start gap-3 rounded-2xl bg-green-50 px-4 py-3 text-sm text-green-800 border border-green-200"
            >
              <CheckCircle className="size-4 shrink-0 mt-0.5 text-green-600" aria-hidden="true" />
              <span>
                Si un compte actif existe pour cette adresse, un lien de réinitialisation
                a été envoyé. Vérifiez également vos courriers indésirables.
              </span>
            </div>
          )}

          <form
            className="mt-6 flex flex-col gap-4"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Adresse e-mail</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="vous@exemple.ma"
                aria-describedby={errors.email ? "email-error" : undefined}
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              {errors.email && (
                <p id="email-error" role="alert" aria-live="polite" className="text-xs text-red-600">
                  {errors.email.message}
                </p>
              )}
            </div>

            <Button
              type="submit"
              className="mt-1"
              disabled={isSubmitting || cooldown > 0}
              loading={isSubmitting}
            >
              {cooldown > 0
                ? `Renvoyer le lien (${cooldown} s)`
                : isSubmitting
                ? "Envoi en cours…"
                : "Envoyer le lien"}
            </Button>
          </form>

          <p className="mt-5 text-center text-sm text-muted-foreground">
            <Link
              to="/login"
              className="underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 rounded"
            >
              Retour à la connexion
            </Link>
          </p>
        </Card>
      </Container>
    </PublicLayout>
  );
}
