import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import { activer2FA, changerMotDePasse, logout, setup2FA } from "@/api/auth";
import { clearSession, getSession, setSession } from "@/api/session";
import {
  desactiver2FA,
  listerSessions,
  modifierProfil,
  lireProfil,
  revoquerSession,
} from "@/api/compte";
import { formaterDate, messageErreur } from "../texte";

export function MonCompte() {
  const queryClient = useQueryClient();
  const profil = useQuery({ queryKey: ["mon-compte"], queryFn: lireProfil });
  const sessions = useQuery({ queryKey: ["mes-sessions"], queryFn: listerSessions });
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [telephone, setTelephone] = useState("");
  const [fonction, setFonction] = useState("");
  const [ancien, setAncien] = useState("");
  const [nouveau, setNouveau] = useState("");
  const [otpAuthUrl, setOtpAuthUrl] = useState("");
  const [code, setCode] = useState("");

  useEffect(() => {
    if (!profil.data) return;
    setNom(profil.data.nom);
    setPrenom(profil.data.prenom ?? "");
    setTelephone(profil.data.telephone ?? "");
    setFonction(profil.data.fonction ?? "");
  }, [profil.data]);

  const enregistrer = useMutation({
    mutationFn: () => modifierProfil({ nom: nom.trim(), prenom: prenom.trim(), telephone: telephone.trim(), fonction: fonction.trim() }),
    onSuccess: (misAJour) => {
      const session = getSession();
      if (session) {
        const nomComplet = [misAJour.prenom, misAJour.nom].filter(Boolean).join(" ");
        setSession(session.accessToken, { ...session.user, nomComplet, email: misAJour.email });
      }
      toast.success("Profil enregistré.");
      void queryClient.invalidateQueries({ queryKey: ["mon-compte"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le profil n'a pas pu être enregistré.")),
  });

  const motDePasse = useMutation({
    mutationFn: () => changerMotDePasse({ ancienMotDePasse: ancien, nouveauMotDePasse: nouveau }),
    onSuccess: (reponse) => {
      const session = getSession();
      if (session && reponse.accessToken) {
        setSession(reponse.accessToken, {
          ...session.user,
          mustChangePassword: reponse.mustChangePassword ?? false,
        });
      }
      setAncien("");
      setNouveau("");
      toast.success("Mot de passe changé. Les autres sessions ont été révoquées.");
      void queryClient.invalidateQueries({ queryKey: ["mes-sessions"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le mot de passe n'a pas pu être changé.")),
  });

  const preparer2fa = useMutation({
    mutationFn: () => setup2FA({}),
    onSuccess: (reponse) => setOtpAuthUrl(reponse.otpAuthUrl),
    onError: (erreur) => toast.error(messageErreur(erreur, "La préparation de la 2FA a échoué.")),
  });

  const confirmer2fa = useMutation({
    mutationFn: () => activer2FA({ code: code.trim() }),
    onSuccess: () => {
      setCode("");
      setOtpAuthUrl("");
      toast.success("Double authentification activée.");
      void queryClient.invalidateQueries({ queryKey: ["mon-compte"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le code n'a pas pu être validé.")),
  });

  const retirer2fa = useMutation({
    mutationFn: () => desactiver2FA(code.trim()),
    onSuccess: () => {
      setCode("");
      toast.success("Double authentification désactivée.");
      void queryClient.invalidateQueries({ queryKey: ["mon-compte"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La désactivation a échoué.")),
  });

  const revoquer = useMutation({
    mutationFn: (id: number) => revoquerSession(id),
    onSuccess: async (resultat) => {
      if (resultat.courante) {
        clearSession();
        try {
          await logout();
        } catch {
          // Le jeton de renouvellement est déjà révoqué.
        }
        window.location.assign("/login");
        return;
      }
      toast.success("Session révoquée.");
      void queryClient.invalidateQueries({ queryKey: ["mes-sessions"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La session n'a pas pu être révoquée.")),
  });

  const secret = extraireSecret(otpAuthUrl);

  return (
    <div className="flex flex-col gap-4">
      <section className="rounded-2xl border border-brand-900/10 bg-card p-4 sm:p-6">
        <h2 className="text-lg font-extrabold text-ink-900">Profil</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          L'adresse e-mail et le rôle ne se modifient pas ici.
        </p>
        {profil.isLoading && <p className="mt-4 text-sm text-muted-foreground">Chargement du profil…</p>}
        {profil.isError && (
          <Button className="mt-4" size="sm" variant="secondary" onClick={() => void profil.refetch()}>Réessayer</Button>
        )}
        {profil.data && (
          <form className="mt-4 grid gap-4 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); enregistrer.mutate(); }}>
            <Champ label="Nom" value={nom} onChange={setNom} required />
            <Champ label="Prénom" value={prenom} onChange={setPrenom} />
            <Champ label="Téléphone" value={telephone} onChange={setTelephone} />
            <Champ label="Fonction" value={fonction} onChange={setFonction} />
            <div>
              <Label>E-mail</Label>
              <p className="mt-2 text-sm font-medium text-ink-900">{profil.data.email}</p>
            </div>
            <div>
              <Label>Rôle</Label>
              <p className="mt-2"><Badge variant="dark" size="sm">{profil.data.role}</Badge></p>
            </div>
            <div className="sm:col-span-2">
              <Button type="submit" loading={enregistrer.isPending} disabled={nom.trim().length === 0}>Enregistrer</Button>
            </div>
          </form>
        )}
      </section>

      <section className="rounded-2xl border border-brand-900/10 bg-card p-4 sm:p-6">
        <h2 className="text-lg font-extrabold text-ink-900">Mot de passe</h2>
        <form className="mt-4 grid gap-4 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); motDePasse.mutate(); }}>
          <Champ label="Mot de passe actuel" value={ancien} onChange={setAncien} type="password" required />
          <Champ label="Nouveau mot de passe" value={nouveau} onChange={setNouveau} type="password" required />
          <div className="sm:col-span-2">
            <Button type="submit" loading={motDePasse.isPending} disabled={!ancien || !nouveau}>Changer le mot de passe</Button>
          </div>
        </form>
      </section>

      <section className="rounded-2xl border border-brand-900/10 bg-card p-4 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-extrabold text-ink-900">Double authentification</h2>
          <Badge variant={profil.data?.doubleAuthentification ? "lime" : "outline"} size="sm">
            {profil.data?.doubleAuthentification ? "Activée" : "Facultative, inactive"}
          </Badge>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          Pour le super administrateur, la 2FA reste facultative. Saisissez le secret dans une application TOTP, puis confirmez avec un code à 6 chiffres.
        </p>
        {!profil.data?.doubleAuthentification && (
          <div className="mt-4 flex flex-col gap-3">
            <Button variant="secondary" size="sm" loading={preparer2fa.isPending} onClick={() => preparer2fa.mutate()}>
              Préparer l'activation
            </Button>
            {otpAuthUrl && (
              <div className="rounded-2xl bg-surface-alt p-3 text-sm">
                <p className="font-semibold text-ink-900">Secret à saisir manuellement</p>
                <p className="mt-1 break-all font-mono text-ink-900">{secret || "Secret introuvable dans l'URL"}</p>
              </div>
            )}
            <Champ label="Code à 6 chiffres" value={code} onChange={setCode} />
            <Button loading={confirmer2fa.isPending} disabled={!/^\d{6}$/.test(code.trim())} onClick={() => confirmer2fa.mutate()}>
              Activer
            </Button>
          </div>
        )}
        {profil.data?.doubleAuthentification && (
          <div className="mt-4 flex flex-col gap-3 sm:max-w-sm">
            <Champ label="Code actuel pour désactiver" value={code} onChange={setCode} />
            <Button variant="secondary" loading={retirer2fa.isPending} disabled={!/^\d{6}$/.test(code.trim())} onClick={() => retirer2fa.mutate()}>
              Désactiver
            </Button>
          </div>
        )}
      </section>

      <section className="rounded-2xl border border-brand-900/10 bg-card p-4 sm:p-6">
        <h2 className="text-lg font-extrabold text-ink-900">Sessions</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Chaque ligne est un jeton de renouvellement encore valide. L'appareil exact n'est connu que par le navigateur déclaré. Révoquer la session courante déconnecte cet écran.
        </p>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="text-xs font-semibold tracking-wide text-brand-900 uppercase">
                <th className="py-2">Navigateur</th>
                <th className="py-2">Ouverte le</th>
                <th className="py-2">Expire le</th>
                <th className="py-2" />
              </tr>
            </thead>
            <tbody>
              {sessions.isLoading && <tr><td colSpan={4} className="py-4 text-muted-foreground">Chargement des sessions…</td></tr>}
              {sessions.data?.length === 0 && <tr><td colSpan={4} className="py-4 text-muted-foreground">Aucune session active.</td></tr>}
              {sessions.data?.map((session) => (
                <tr key={session.id} className="border-t border-brand-900/10">
                  <td className="max-w-[280px] truncate py-3 text-ink-900" title={session.userAgent ?? ""}>
                    {session.userAgent || "Navigateur non précisé"}
                    {session.courante && <Badge className="ml-2" variant="lime" size="sm">Courante</Badge>}
                  </td>
                  <td className="py-3 whitespace-nowrap">{formaterDate(session.creeLe)}</td>
                  <td className="py-3 whitespace-nowrap">{formaterDate(session.expireLe)}</td>
                  <td className="py-3 text-right">
                    <Button variant="ghost" size="sm" loading={revoquer.isPending} onClick={() => revoquer.mutate(session.id)}>
                      Révoquer
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function Champ({
  label,
  value,
  onChange,
  type = "text",
  required = false,
}: {
  label: string;
  value: string;
  onChange: (valeur: string) => void;
  type?: string;
  required?: boolean;
}) {
  const id = label.toLowerCase().replace(/\s+/g, "-");
  return (
    <div>
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} className="mt-2" type={type} value={value} required={required} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}

function extraireSecret(url: string): string {
  if (!url) return "";
  const secret = /[?&]secret=([^&]+)/.exec(url);
  return secret?.[1] ? decodeURIComponent(secret[1]) : "";
}
