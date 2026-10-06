import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { CheckCircle, XCircle } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { confirmerActivation } from "@/api/auth";

// ---------------------------------------------------------------------------
// Cache module : garantit UN SEUL appel réseau par jeton, même en StrictMode.
// React StrictMode monte les effets deux fois en développement ; sans ce cache
// le second appel consommerait un jeton déjà utilisé et afficherait une fausse erreur.
// ---------------------------------------------------------------------------

const _cache = new Map<string, Promise<"ok" | "erreur">>();

function confirmerUneFois(token: string): Promise<"ok" | "erreur"> {
  const existing = _cache.get(token);
  if (existing !== undefined) return existing;

  const p = confirmerActivation(token)
    .then((): "ok" => "ok")
    .catch((): "erreur" => "erreur");

  _cache.set(token, p);
  return p;
}

// ---------------------------------------------------------------------------
// Route
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/activation")({
  head: () => ({
    meta: [
      { title: "Activation du compte — LabFlow LIMS" },
      // Empêche le navigateur d'inclure le jeton dans le Referer d'une requête sortante
      { name: "referrer", content: "no-referrer" },
    ],
  }),
  validateSearch: (
    search: Record<string, unknown>,
  ): { token?: string | undefined } => ({
    token: typeof search["token"] === "string" ? search["token"] : undefined,
  }),
  component: ActivationPage,
});

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

type Etat = "chargement" | "succes" | "erreur";

function ActivationPage() {
  const { token } = Route.useSearch();
  const [etat, setEtat] = useState<Etat>(token ? "chargement" : "erreur");

  useEffect(() => {
    if (!token) {
      setEtat("erreur");
      return;
    }
    void confirmerUneFois(token).then((res) =>
      setEtat(res === "ok" ? "succes" : "erreur"),
    );
    // token est stable (lu depuis l'URL au montage)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <PublicLayout>
      <Container className="flex justify-center py-16">
        <Card variant="glossy" padding="lg" className="w-full max-w-md text-center">

          {etat === "chargement" && (
            <div
              aria-busy="true"
              aria-label="Activation du compte en cours…"
              className="flex flex-col items-center gap-4 py-4"
            >
              <span className="size-10 animate-spin rounded-full border-4 border-brand-900/20 border-t-brand-900" />
              <p className="text-sm text-muted-foreground">Activation en cours…</p>
            </div>
          )}

          {etat === "succes" && (
            <>
              <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-green-100">
                <CheckCircle className="size-8 text-green-600" aria-hidden="true" />
              </div>
              <h1 className="text-2xl font-extrabold text-ink-900">Compte activé</h1>
              <p className="mt-3 text-sm text-muted-foreground">
                Votre adresse e-mail a été confirmée. Vous pouvez maintenant vous connecter.
              </p>
              <div className="mt-6">
                <Link to="/login">
                  <Button className="w-full">Se connecter</Button>
                </Link>
              </div>
            </>
          )}

          {etat === "erreur" && (
            <>
              <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-red-100">
                <XCircle className="size-8 text-red-500" aria-hidden="true" />
              </div>
              <h1 className="text-2xl font-extrabold text-ink-900">Lien invalide ou expiré</h1>
              <p className="mt-3 text-sm text-muted-foreground">
                Ce lien est invalide, expiré ou a déjà été utilisé. Si vous avez déjà
                activé votre compte, vous pouvez vous connecter directement.
              </p>
              <div className="mt-6 flex flex-col gap-3">
                <Link to="/login">
                  <Button className="w-full">Se connecter</Button>
                </Link>
                <Link to="/inscription">
                  <Button variant="secondary" className="w-full">
                    Refaire mon inscription
                  </Button>
                </Link>
              </div>
            </>
          )}

        </Card>
      </Container>
    </PublicLayout>
  );
}
