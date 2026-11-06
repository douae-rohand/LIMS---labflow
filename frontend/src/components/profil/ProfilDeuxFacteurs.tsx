import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { KeyRound, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { activer2FA, desactiver2FA, setup2FA } from "@/api/auth";
import { ApiError } from "@/api/axios";
import { Button, Card } from "@/components/lab";

export function ProfilDeuxFacteurs({ active }: { active: boolean }) {
  const queryClient = useQueryClient();
  const [secret, setSecret] = useState<string | null>(null);
  const [codeActivation, setCodeActivation] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [codeDesactivation, setCodeDesactivation] = useState("");
  const [chargement, setChargement] = useState(false);

  async function demarrerConfiguration() {
    setChargement(true);
    try {
      const { otpAuthUrl } = await setup2FA();
      const secretTOTP = new URL(otpAuthUrl).searchParams.get("secret");
      if (!secretTOTP) {
        throw new Error("Le secret de configuration n'a pas été fourni.");
      }
      setSecret(secretTOTP);
    } catch (error) {
      toast.error(messageErreur(error));
    } finally {
      setChargement(false);
    }
  }

  async function confirmerActivation(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setChargement(true);
    try {
      await activer2FA({ code: codeActivation });
      setSecret(null);
      setCodeActivation("");
      await queryClient.invalidateQueries({ queryKey: ["profil-utilisateur"] });
      toast.success("La double authentification est activée.");
    } catch (error) {
      toast.error(messageErreur(error));
    } finally {
      setChargement(false);
    }
  }

  async function confirmerDesactivation(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setChargement(true);
    try {
      await desactiver2FA({ motDePasse, code: codeDesactivation });
      setMotDePasse("");
      setCodeDesactivation("");
      await queryClient.invalidateQueries({ queryKey: ["profil-utilisateur"] });
      toast.success("La double authentification est désactivée.");
    } catch (error) {
      toast.error(messageErreur(error));
    } finally {
      setChargement(false);
    }
  }

  return (
    <Card className="space-y-5 p-6">
      <div className="flex items-start gap-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
          <ShieldCheck className="size-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-semibold text-ink-900">Authentification à deux facteurs</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {active
              ? "Votre compte demande un code de votre application d'authentification à la connexion."
              : "Ajoutez une protection supplémentaire avec une application compatible TOTP."}
          </p>
        </div>
      </div>

      {active ? (
        <form onSubmit={confirmerDesactivation} className="grid gap-3 sm:grid-cols-2">
          <label className="space-y-1.5 text-sm font-medium text-ink-900">
            Mot de passe actuel
            <input
              type="password"
              autoComplete="current-password"
              required
              value={motDePasse}
              onChange={(event) => setMotDePasse(event.target.value)}
              className={CHAMP}
            />
          </label>
          <label className="space-y-1.5 text-sm font-medium text-ink-900">
            Code actuel (6 chiffres)
            <input
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              maxLength={6}
              required
              value={codeDesactivation}
              onChange={(event) => setCodeDesactivation(event.target.value.replace(/\D/g, ""))}
              className={CHAMP}
            />
          </label>
          <div className="sm:col-span-2 flex justify-end">
            <Button type="submit" variant="secondary" disabled={chargement}>
              {chargement ? "Désactivation…" : "Désactiver la 2FA"}
            </Button>
          </div>
        </form>
      ) : secret ? (
        <form onSubmit={confirmerActivation} className="space-y-4">
          <div className="rounded-xl border border-brand-900/10 bg-surface-card/60 p-4">
            <p className="text-sm font-medium text-ink-900">Configurer votre application</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Dans Google Authenticator, Microsoft Authenticator ou une application TOTP, ajoutez un compte avec la clé ci-dessous.
            </p>
            <code className="mt-3 block select-all break-all rounded-lg bg-white px-3 py-2 font-mono text-sm font-semibold tracking-wider text-ink-900">
              {secret}
            </code>
          </div>
          <label className="block max-w-xs space-y-1.5 text-sm font-medium text-ink-900">
            Code généré par l'application
            <input
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              maxLength={6}
              required
              value={codeActivation}
              onChange={(event) => setCodeActivation(event.target.value.replace(/\D/g, ""))}
              className={CHAMP}
            />
          </label>
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="ghost" disabled={chargement} onClick={() => setSecret(null)}>
              Annuler
            </Button>
            <Button type="submit" disabled={chargement}>
              {chargement ? "Vérification…" : "Vérifier et activer"}
            </Button>
          </div>
        </form>
      ) : (
        <Button
          type="button"
          variant="secondary"
          disabled={chargement}
          onClick={() => void demarrerConfiguration()}
          icon={<KeyRound className="size-4" aria-hidden="true" />}
          iconPosition="left"
        >
          {chargement ? "Préparation…" : "Configurer la 2FA"}
        </Button>
      )}
    </Card>
  );
}

const CHAMP = "h-10 w-full rounded-xl border border-brand-900/15 bg-white px-3 text-sm text-ink-900 outline-none transition focus:border-brand-900 focus:ring-2 focus:ring-brand-900/10";

function messageErreur(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim() || "L'opération a échoué.";
  }
  if (error instanceof Error) return error.message;
  return "Une erreur inattendue est survenue.";
}
