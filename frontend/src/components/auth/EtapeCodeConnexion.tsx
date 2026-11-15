import { useEffect, useState, type FormEvent } from "react";
import { ArrowLeft, MailCheck } from "lucide-react";
import { Button, Input, Label } from "@/components/lab";
import { renvoyerCode2FA, valider2FA, type LoginResponse } from "@/api/auth";
import { ApiError } from "@/api/axios";

/** Délai imposé par le serveur entre deux envois. */
const DELAI_RENVOI_S = 60;

function messageDe(erreur: unknown, defaut: string): string {
  if (erreur instanceof ApiError) {
    return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "") || defaut;
  }
  return defaut;
}

/**
 * Deuxième étape de la connexion : le code à 6 chiffres envoyé par e-mail (valable 15 minutes).
 * Le jeton temporaire ne donne accès à rien d'autre qu'à cette vérification.
 */
export function EtapeCodeConnexion({ jeton, emailMasque, dureeMinutes, onConnecte, onRetour }: {
  jeton: string;
  emailMasque?: string | undefined;
  dureeMinutes: number;
  onConnecte: (reponse: LoginResponse) => void;
  onRetour: () => void;
}) {
  const [code, setCode] = useState("");
  const [erreur, setErreur] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);
  const [attente, setAttente] = useState(DELAI_RENVOI_S);
  const focaliser = () => document.getElementById("code-connexion")?.focus();

  useEffect(() => {
    if (attente <= 0) return;
    const minuteur = window.setTimeout(() => setAttente((s) => s - 1), 1000);
    return () => window.clearTimeout(minuteur);
  }, [attente]);

  const valide = /^\d{6}$/.test(code);

  async function verifier(event: FormEvent) {
    event.preventDefault();
    if (!valide || enCours) return;
    setEnCours(true);
    setErreur(null);
    setInfo(null);
    try {
      const reponse = await valider2FA({ twoFactorToken: jeton, code });
      onConnecte(reponse);
    } catch (e) {
      setErreur(messageDe(e, "Le code n'a pas pu être vérifié. Réessayez."));
      setCode("");
      focaliser();
    } finally {
      setEnCours(false);
    }
  }

  async function renvoyer() {
    setErreur(null);
    setInfo(null);
    try {
      await renvoyerCode2FA(jeton);
      setInfo("Un nouveau code vient d'être envoyé. L'ancien ne fonctionne plus.");
      setAttente(DELAI_RENVOI_S);
      setCode("");
      focaliser();
    } catch (e) {
      setErreur(messageDe(e, "Le code n'a pas pu être renvoyé."));
    }
  }

  return (
    <div className="mt-6 flex flex-col gap-4">
      <div className="flex items-start gap-3 rounded-2xl bg-mint-200/50 px-4 py-3 text-sm text-ink-900">
        <MailCheck className="mt-0.5 size-5 shrink-0 text-brand-900" aria-hidden="true" />
        <p>
          Un code de vérification à 6 chiffres a été envoyé à{" "}
          <strong>{emailMasque ?? "votre adresse e-mail"}</strong>. Il est valable {dureeMinutes} minutes.
        </p>
      </div>

      {erreur && (
        <p role="alert" aria-live="assertive" className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{erreur}</p>
      )}
      {info && (
        <p role="status" aria-live="polite" className="rounded-2xl bg-surface-alt px-4 py-3 text-sm text-ink-900">{info}</p>
      )}

      <form className="flex flex-col gap-4" onSubmit={(event) => void verifier(event)} noValidate>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="code-connexion">Code de vérification</Label>
          <Input
            id="code-connexion"
            autoFocus
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            value={code}
            onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
            className="text-center font-mono text-xl tracking-[0.5em]"
          />
        </div>
        <Button type="submit" disabled={!valide || enCours} loading={enCours}>
          {enCours ? "Vérification…" : "Vérifier et se connecter"}
        </Button>
      </form>

      <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
        <button type="button" onClick={onRetour}
          className="inline-flex items-center gap-1.5 rounded text-muted-foreground hover:text-ink-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600">
          <ArrowLeft className="size-4" aria-hidden="true" /> Retour
        </button>
        <button type="button" onClick={() => void renvoyer()} disabled={attente > 0}
          className="rounded font-semibold text-brand-900 underline-offset-4 hover:underline disabled:cursor-not-allowed disabled:text-muted-foreground disabled:no-underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600">
          {attente > 0 ? `Renvoyer le code (${attente} s)` : "Renvoyer le code"}
        </button>
      </div>
    </div>
  );
}
