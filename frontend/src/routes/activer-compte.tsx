import { createFileRoute, Link } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useState } from "react";
import { Button, Card, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { activerCompte, verifierJetonActivation } from "@/api/auth";
import { ApiError } from "@/api/axios";
import { politiqueMotDePasse } from "@/routes/changer-mot-de-passe";

export const Route = createFileRoute("/activer-compte")({
  head: () => ({
    meta: [{ title: "Activer mon compte — LabFlow LIMS" }],
  }),
  validateSearch: (search: Record<string, unknown>): { token?: string | undefined } => ({
    token: typeof search["token"] === "string" ? search["token"] : undefined,
  }),
  component: ActiverComptePage,
});

const schema = z
  .object({
    motDePasse: politiqueMotDePasse,
    confirmation: z.string().min(1, "La confirmation est obligatoire."),
  })
  .refine((data) => data.motDePasse === data.confirmation, {
    path: ["confirmation"],
    message: "Les mots de passe ne correspondent pas.",
  });

type FormValues = z.infer<typeof schema>;

function ActiverComptePage() {
  const { token } = Route.useSearch();
  const [etat, setEtat] = useState<"verification" | "valide" | "invalide" | "ok">("verification");
  const [message, setMessage] = useState<string | null>(null);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (!token) {
      setEtat("invalide");
      setMessage("Lien d'activation manquant.");
      return;
    }
    verifierJetonActivation(token)
      .then(() => setEtat("valide"))
      .catch((error) => {
        setEtat("invalide");
        setMessage(error instanceof ApiError ? error.message : "Ce lien d'activation est invalide.");
      });
  }, [token]);

  const onSubmit = async (values: FormValues) => {
    if (!token) return;
    try {
      await activerCompte(token, values.motDePasse);
      setEtat("ok");
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Impossible d'activer le compte.");
    }
  };

  return (
    <PublicLayout>
      <Container className="py-16">
        <Card variant="glossy" padding="lg" className="mx-auto max-w-lg">
          <h1 className="text-2xl font-extrabold text-ink-900">Activer mon compte</h1>
          {etat === "verification" && (
            <p className="mt-4 text-sm text-muted-foreground">Vérification du lien…</p>
          )}
          {etat === "invalide" && (
            <p className="mt-4 text-sm text-red-600">{message}</p>
          )}
          {etat === "ok" && (
            <div className="mt-4 flex flex-col gap-4">
              <p className="text-sm text-muted-foreground">
                Votre compte administrateur est activé. Connectez-vous avec l'e-mail de la demande.
              </p>
              <Link to="/login">
                <Button>Se connecter</Button>
              </Link>
            </div>
          )}
          {etat === "valide" && (
            <form className="mt-6 flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
              <p className="text-sm text-muted-foreground">
                Définissez le mot de passe de l'administrateur laboratoire (10 caractères, majuscule,
                minuscule et chiffre).
              </p>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="motDePasse">Nouveau mot de passe</Label>
                <input
                  id="motDePasse"
                  type="password"
                  className="w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm"
                  {...form.register("motDePasse")}
                />
                {form.formState.errors.motDePasse && (
                  <p className="text-sm text-red-600">{form.formState.errors.motDePasse.message}</p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="confirmation">Confirmation</Label>
                <input
                  id="confirmation"
                  type="password"
                  className="w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm"
                  {...form.register("confirmation")}
                />
                {form.formState.errors.confirmation && (
                  <p className="text-sm text-red-600">{form.formState.errors.confirmation.message}</p>
                )}
              </div>
              {message && <p className="text-sm text-red-600">{message}</p>}
              <Button type="submit" loading={form.formState.isSubmitting}>
                Activer le compte
              </Button>
            </form>
          )}
        </Card>
      </Container>
    </PublicLayout>
  );
}
