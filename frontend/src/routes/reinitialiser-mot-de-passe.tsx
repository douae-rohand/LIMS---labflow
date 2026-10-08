import { createFileRoute, Link } from "@tanstack/react-router";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect, useRef, useState } from "react";
import { Check, CheckCircle, Eye, EyeOff, X, XCircle } from "lucide-react";
import { Button, Card, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { reinitialiserMotDePasse } from "@/api/auth";
import { ApiError } from "@/api/axios";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// Route : lit le token depuis l'URL puis le retire de l'historique
// ---------------------------------------------------------------------------

export const Route = createFileRoute("/reinitialiser-mot-de-passe")({
  head: () => ({
    meta: [
      { title: "Réinitialisation du mot de passe — LabFlow LIMS" },
      // Empêche le token de figurer dans le Referer d'une requête sortante
      { name: "referrer", content: "no-referrer" },
    ],
  }),
  validateSearch: (search: Record<string, unknown>): { token?: string | undefined } => ({
    token: typeof search["token"] === "string" ? search["token"] : undefined,
  }),
  component: ReinitialiserMotDePassePage,
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

const REGLES = [
  { id: "len",   label: "10 caractères minimum", test: (v: string) => v.length >= 10 },
  { id: "lower", label: "Une lettre minuscule",   test: (v: string) => /[a-z]/.test(v) },
  { id: "upper", label: "Une lettre majuscule",   test: (v: string) => /[A-Z]/.test(v) },
  { id: "digit", label: "Un chiffre",             test: (v: string) => /[0-9]/.test(v) },
] as const;

const schema = z
  .object({
    nouveauMotDePasse: politiqueMdp,
    confirmation: z.string().min(1, "La confirmation est obligatoire."),
  })
  .refine((d) => d.nouveauMotDePasse === d.confirmation, {
    path: ["confirmation"],
    message: "Les mots de passe ne correspondent pas.",
  });

type FormValues = z.infer<typeof schema>;

// ---------------------------------------------------------------------------
// Composant champ mot de passe avec œil
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
          {visible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
        </button>
      </div>
      {error && errorId && (
        <p id={errorId} role="alert" aria-live="polite" className="text-xs text-red-600">{error}</p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Checklist critères
// ---------------------------------------------------------------------------

function MdpChecklist({ value }: { value: string }) {
  if (!value) return null;
  return (
    <ul aria-label="Critères du mot de passe" aria-live="polite" className="mt-1 flex flex-col gap-1">
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
              {ok ? <Check className="size-2.5 text-white" strokeWidth={3} /> : <X className="size-2.5 text-muted-foreground" strokeWidth={2.5} />}
            </span>
            <span className={cn("transition-colors", ok ? "text-green-700" : "text-muted-foreground")}>{label}</span>
          </li>
        );
      })}
    </ul>
  );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

type Etat = "formulaire" | "succes" | "lien-invalide";

function ReinitialiserMotDePassePage() {
  const { token: tokenUrl } = Route.useSearch();

  // Stocker le token en ref (mémoire uniquement, pas dans l'URL ni dans un state)
  const tokenRef = useRef<string | undefined>(tokenUrl);

  // Retirer le token de l'URL dès le montage (history.replaceState)
  useEffect(() => {
    if (tokenUrl) {
      const urlSansToken = window.location.pathname;
      window.history.replaceState(null, "", urlSansToken);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [etat, setEtat] = useState<Etat>(tokenUrl ? "formulaire" : "lien-invalide");
  // Message d'erreur API (politique refusée → afficher texte serveur ; lien invalide → générique)
  const [erreurApi, setErreurApi] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setFocus,
    control,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { nouveauMotDePasse: "", confirmation: "" },
  });

  const mdpValue = useWatch({ control, name: "nouveauMotDePasse" });

  useEffect(() => {
    if (errors.nouveauMotDePasse) setFocus("nouveauMotDePasse");
    else if (errors.confirmation) setFocus("confirmation");
  }, [errors.nouveauMotDePasse, errors.confirmation, setFocus]);

  const onSubmit = async (values: FormValues) => {
    const token = tokenRef.current;
    if (!token) { setEtat("lien-invalide"); return; }

    setErreurApi(null);

    try {
      await reinitialiserMotDePasse(token, values.nouveauMotDePasse);
      // Succès : aucune session créée, rediriger manuellement vers /login
      setEtat("succes");
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 400 || err.status === 422) {
          const msg = err.message;
          // REINIT_INVALIDE = jeton consommé, expiré ou inconnu → état lien-invalide
          if (msg.includes('REINIT_INVALIDE')) {
            setEtat("lien-invalide");
          } else {
            // Politique refusée (MOT_DE_PASSE_IDENTIQUE, trop court, etc.)
            // Le jeton est intact → laisser le formulaire actif, afficher le message
            setErreurApi(msg.replace(/^\[.*?\]\s*/, "")); // retirer le préfixe [CODE]
          }
        } else if (err.status === 429) {
          setErreurApi("Trop de tentatives. Réessayez plus tard.");
        } else if (err.status === 0 || err.status === 503) {
          setErreurApi("Serveur indisponible. Réessayez.");
        } else {
          setEtat("lien-invalide");
        }
      } else {
        setErreurApi("Serveur indisponible. Réessayez.");
      }
    }
  };

  const nouveauProps = register("nouveauMotDePasse");
  const confirmProps = register("confirmation");

  return (
    <PublicLayout>
      <Container className="flex justify-center py-16">
        <Card variant="glossy" padding="lg" className="w-full max-w-md">

          {/* ── État : lien invalide / token absent ── */}
          {etat === "lien-invalide" && (
            <>
              <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-red-100">
                <XCircle className="size-8 text-red-500" aria-hidden="true" />
              </div>
              <h1 className="text-center text-2xl font-extrabold text-ink-900">
                Lien invalide ou expiré
              </h1>
              <p className="mt-3 text-center text-sm text-muted-foreground">
                Ce lien est invalide, a expiré ou a déjà été utilisé.
              </p>
              <div className="mt-6 flex flex-col gap-3">
                <Link to="/mot-de-passe-oublie">
                  <Button className="w-full">Demander un nouveau lien</Button>
                </Link>
                <Link to="/login">
                  <Button variant="secondary" className="w-full">Se connecter</Button>
                </Link>
              </div>
            </>
          )}

          {/* ── État : succès ── */}
          {etat === "succes" && (
            <>
              <div className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full bg-green-100">
                <CheckCircle className="size-8 text-green-600" aria-hidden="true" />
              </div>
              <h1 className="text-center text-2xl font-extrabold text-ink-900">
                Mot de passe réinitialisé
              </h1>
              <p className="mt-3 text-center text-sm text-muted-foreground">
                Votre mot de passe a bien été modifié. Vous pouvez maintenant vous connecter.
              </p>
              <div className="mt-6">
                <Link to="/login">
                  <Button className="w-full">Se connecter</Button>
                </Link>
              </div>
            </>
          )}

          {/* ── État : formulaire ── */}
          {etat === "formulaire" && (
            <>
              <h1 className="text-2xl font-extrabold text-ink-900">
                Nouveau mot de passe
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Choisissez un mot de passe sécurisé pour votre compte LabFlow.
              </p>

              {/* Erreur API (politique refusée, réseau, 429) */}
              {erreurApi && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700"
                >
                  {erreurApi}
                </div>
              )}

              <form
                className="mt-6 flex flex-col gap-4"
                onSubmit={handleSubmit(onSubmit)}
                noValidate
              >
                {/* Nouveau mot de passe */}
                <div className="flex flex-col gap-1.5">
                  <PwdField
                    id="nouveauMotDePasse"
                    label="Nouveau mot de passe"
                    autoComplete="new-password"
                    error={errors.nouveauMotDePasse?.message}
                    errorId="mdp-error"
                    {...nouveauProps}
                  />
                  <MdpChecklist value={mdpValue ?? ""} />
                </div>

                {/* Confirmation */}
                <PwdField
                  id="confirmation"
                  label="Confirmer le mot de passe"
                  autoComplete="new-password"
                  error={errors.confirmation?.message}
                  errorId="confirm-error"
                  {...confirmProps}
                />

                <Button
                  type="submit"
                  className="mt-1"
                  disabled={isSubmitting}
                  loading={isSubmitting}
                >
                  {isSubmitting ? "Enregistrement…" : "Réinitialiser le mot de passe"}
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
            </>
          )}

        </Card>
      </Container>
    </PublicLayout>
  );
}
