import { createFileRoute, Link } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useRef, useState } from "react";
import { Button, Card, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { activerCompte, verifierJetonActivation } from "@/api/auth";
import { ApiError } from "@/api/axios";
import { politiqueMotDePasse } from "@/routes/changer-mot-de-passe";

// ---------------------------------------------------------------------------
// Cache module : garantit UN SEUL appel réseau de vérification par jeton,
// même en StrictMode (double montage en développement).
// ---------------------------------------------------------------------------
const _verificationCache = new Map<string, Promise<"ok" | "erreur">>();

function verifierUneFois(token: string): Promise<"ok" | "erreur"> {
  const existing = _verificationCache.get(token);
  if (existing !== undefined) return existing;

  const p = verifierJetonActivation(token)
    .then((): "ok" => "ok")
    .catch((): "erreur" => "erreur");

  _verificationCache.set(token, p);
  return p;
}

export const Route = createFileRoute("/activer-compte")({
  head: () => ({
    meta: [
      { title: "Activer mon compte — LabFlow LIMS" },
      // Empêche le token de figurer dans le Referer d'une requête sortante
      { name: "referrer", content: "no-referrer" },
    ],
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
  const { token: tokenUrl } = Route.useSearch();

  // Stocker le token en mémoire uniquement (ref)
  const tokenRef = useRef<string | undefined>(tokenUrl);

  const [etat, setEtat] = useState<"verification" | "valide" | "invalide" | "ok">(
    tokenUrl ? "verification" : "invalide",
  );
  const [message, setMessage] = useState<string | null>(
    tokenUrl ? null : "Lien invalide ou expiré.",
  );

  // Retirer immédiatement le token de l'URL à l'initialisation sans recharger la page
  useEffect(() => {
    if (tokenUrl) {
      const urlSansToken = window.location.pathname;
      window.history.replaceState(null, "", urlSansToken);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  // Appel de vérification unique protégé contre le double effet de React StrictMode
  useEffect(() => {
    const token = tokenRef.current;
    if (!token) {
      setEtat("invalide");
      setMessage("Lien invalide ou expiré.");
      return;
    }

    void verifierUneFois(token).then((res) => {
      if (res === "ok") {
        setEtat("valide");
        setMessage(null);
      } else {
        setEtat("invalide");
        setMessage("Lien invalide ou expiré.");
      }
    });
  }, []);

  const onSubmit = async (values: FormValues) => {
    const token = tokenRef.current;
    if (!token) {
      setEtat("invalide");
      setMessage("Lien invalide ou expiré.");
      return;
    }
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
            <p className="mt-4 text-sm text-red-600">{message ?? "Lien invalide ou expiré."}</p>
          )}
          {etat === "ok" && (
            <div className="mt-4 flex flex-col gap-4">
              <p className="text-sm text-muted-foreground">
                Votre compte est activé. Connectez-vous avec votre email.
              </p>
              <Link to="/login">
                <Button>Se connecter</Button>
              </Link>
            </div>
          )}
          {etat === "valide" && (
            <form className="mt-6 flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
              <p className="text-sm text-muted-foreground">
                Définissez votre mot de passe (10 caractères minimum, majuscule, minuscule et chiffre).
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
