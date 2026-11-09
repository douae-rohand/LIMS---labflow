import { useEffect, useRef, useState } from "react";
import { ShieldCheck } from "lucide-react";
import { Button, Label } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { activer2FA, setup2FA, valider2FA, type LoginResponse } from "@/api/auth";

const CHAMP_CODE =
  "w-full rounded-2xl border border-input bg-card px-4 py-3 text-center font-mono text-lg tracking-[0.4em] text-ink-900 focus:border-brand-600 focus:outline-none";

function messageErreur(erreur: unknown): string {
  if (erreur instanceof ApiError) {
    if (erreur.status === 0 || erreur.status >= 500) return "Serveur indisponible, réessayez.";
    return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "") || "Code incorrect.";
  }
  return "Serveur indisponible, réessayez.";
}

/**
 * Seconde étape de la connexion : saisie du code TOTP, ou configuration de
 * l'application d'authentification quand la 2FA est obligatoire mais pas encore activée.
 * Le jeton temporaire (5 minutes) reste en mémoire et n'est jamais affiché.
 */
export function EtapeDeuxFacteurs({
  twoFactorToken,
  configurationRequise,
  onConnecte,
  onAnnuler,
}: {
  twoFactorToken: string;
  configurationRequise: boolean;
  onConnecte: (reponse: LoginResponse) => void;
  onAnnuler: () => void;
}) {
  const [code, setCode] = useState("");
  const [secret, setSecret] = useState<string | null>(null);
  const [lienApplication, setLienApplication] = useState<string | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);
  const champ = useRef<HTMLInputElement>(null);
  const preparation = useRef(false);

  useEffect(() => {
    if (!configurationRequise || preparation.current) return;
    preparation.current = true;
    setChargement(true);
    setup2FA({ twoFactorToken })
      .then(({ otpAuthUrl }) => {
        setLienApplication(otpAuthUrl);
        setSecret(new URL(otpAuthUrl).searchParams.get("secret"));
      })
      .catch((e: unknown) => setErreur(messageErreur(e)))
      .finally(() => setChargement(false));
  }, [configurationRequise, twoFactorToken]);

  useEffect(() => {
    champ.current?.focus();
  }, [secret]);

  const valider = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!/^\d{6}$/.test(code)) {
      setErreur("Saisissez les 6 chiffres affichés par votre application.");
      return;
    }
    setErreur(null);
    setChargement(true);
    try {
      const reponse = configurationRequise
        ? await activer2FA({ twoFactorToken, code })
        : await valider2FA({ twoFactorToken, code });
      if (reponse?.accessToken) {
        onConnecte(reponse);
      } else {
        setErreur("La connexion n'a pas pu être finalisée. Recommencez.");
      }
    } catch (e) {
      setErreur(messageErreur(e));
      setCode("");
    } finally {
      setChargement(false);
    }
  };

  return (
    <div className="mt-6 flex flex-col gap-4">
      <div className="flex items-start gap-3 rounded-2xl bg-surface-card px-4 py-3">
        <ShieldCheck className="mt-0.5 size-5 shrink-0 text-brand-900" aria-hidden="true" />
        <p className="text-sm text-ink-900">
          {configurationRequise
            ? "La double authentification est obligatoire pour votre compte. Ajoutez LabFlow dans votre application d'authentification (Google Authenticator, Microsoft Authenticator…), puis saisissez le code affiché."
            : "Saisissez le code à 6 chiffres affiché par votre application d'authentification."}
        </p>
      </div>

      {configurationRequise && secret && (
        <div className="rounded-2xl border border-brand-900/10 p-4">
          <p className="text-sm font-semibold text-ink-900">Clé de configuration</p>
          <code className="mt-2 block select-all break-all rounded-lg bg-surface-alt px-3 py-2 font-mono text-sm font-semibold tracking-wider text-ink-900">
            {secret}
          </code>
          {lienApplication && (
            <a
              href={lienApplication}
              className="mt-2 inline-block text-xs font-semibold text-brand-900 underline-offset-4 hover:underline"
            >
              Ouvrir dans l'application (sur téléphone)
            </a>
          )}
        </div>
      )}

      {erreur && (
        <p role="alert" aria-live="assertive" className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">
          {erreur}
        </p>
      )}

      {(!configurationRequise || secret) && (
        <form className="flex flex-col gap-4" onSubmit={(event) => void valider(event)} noValidate>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="code-2fa">Code de vérification</Label>
            <input
              ref={champ}
              id="code-2fa"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              maxLength={6}
              className={CHAMP_CODE}
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
            />
          </div>
          <Button type="submit" loading={chargement} disabled={chargement || code.length !== 6}>
            {configurationRequise ? "Activer et se connecter" : "Vérifier"}
          </Button>
        </form>
      )}

      {configurationRequise && !secret && chargement && (
        <p className="text-sm text-muted-foreground">Préparation de la configuration…</p>
      )}

      <Button type="button" variant="ghost" onClick={onAnnuler}>
        Revenir à la saisie du mot de passe
      </Button>
    </div>
  );
}
