import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { Check, LogOut, Monitor, X } from "lucide-react";
import { toast } from "sonner";
import { changerMotDePasse } from "@/api/auth";
import {
  fermerMaSession,
  fermerMesAutresSessions,
  lireMonCompte,
  listerMesSessions,
  modifierMonCompte,
  type ModifierMonCompte,
  type MonCompte,
} from "@/api/monCompte";
import { getSession, renouvelerSession } from "@/api/session";
import { Badge, Button, Input, Label } from "@/components/lab";
import { reglesMotDePasse, usePolitiqueMotDePasse } from "@/hooks/useParametresPublics";
import { EtatVide } from "../EtatVide";
import { messageErreur } from "../texte";

const DATE_HEURE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeStyle: "short" });

function dateHeure(valeur?: string | null): string {
  if (!valeur) return "—";
  const date = new Date(valeur);
  return Number.isNaN(date.getTime()) ? "—" : DATE_HEURE.format(date);
}

export function PageMonCompte() {
  const compte = useQuery({ queryKey: ["mon-compte"], queryFn: lireMonCompte });

  if (compte.isError) return <EtatVide titre="Votre compte n'a pas pu être chargé." />;
  if (!compte.data) return <p className="text-sm text-muted-foreground">Chargement…</p>;

  return (
    <div className="grid gap-5 xl:grid-cols-2">
      <SectionInformations compte={compte.data} />
      <SectionMotDePasse />
      <div className="xl:col-span-2">
        <SectionSessions />
      </div>
    </div>
  );
}

function Carte({ titre, description, action, children }: {
  titre: string;
  description: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-3xl border border-brand-900/10 bg-card p-5 sm:p-6">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-extrabold text-ink-900">{titre}</h2>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

// ---------------------------------------------------------------------------
// Informations personnelles
// ---------------------------------------------------------------------------

function versFormulaire(compte: MonCompte): ModifierMonCompte {
  return {
    nom: compte.nom ?? "",
    prenom: compte.prenom ?? "",
  };
}

function SectionInformations({ compte }: { compte: MonCompte }) {
  const queryClient = useQueryClient();
  const [valeurs, setValeurs] = useState<ModifierMonCompte>(() => versFormulaire(compte));

  useEffect(() => {
    setValeurs(versFormulaire(compte));
  }, [compte]);

  const enregistrement = useMutation({
    mutationFn: () => modifierMonCompte({
      nom: valeurs.nom.trim(),
      prenom: valeurs.prenom.trim(),
    }),
    onSuccess: (misAJour) => {
      queryClient.setQueryData(["mon-compte"], misAJour);
      const session = getSession();
      if (session) {
        renouvelerSession(session.accessToken, { ...session.user, nomComplet: misAJour.nomComplet });
      }
      toast.success("Informations enregistrées.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  const modifie = JSON.stringify(valeurs) !== JSON.stringify(versFormulaire(compte));
  const champ = (cle: keyof ModifierMonCompte) => ({
    value: valeurs[cle],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValeurs({ ...valeurs, [cle]: event.target.value }),
  });

  return (
    <Carte titre="Informations personnelles" description="Nom affiché dans la plateforme et dans le journal d'audit.">
      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (!valeurs.nom.trim()) {
            toast.error("Le nom est obligatoire.");
            return;
          }
          enregistrement.mutate();
        }}
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mc-nom">Nom</Label>
            <Input id="mc-nom" required maxLength={100} {...champ("nom")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mc-prenom">Prénom</Label>
            <Input id="mc-prenom" maxLength={100} {...champ("prenom")} />
          </div>
          <div className="flex flex-col gap-1.5 sm:col-span-2">
            <Label htmlFor="mc-email">Adresse e-mail, en lecture seule</Label>
            <Input id="mc-email" value={compte.email} readOnly />
          </div>
        </div>
        <dl className="grid gap-2 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs text-muted-foreground">Compte créé le</dt>
            <dd className="font-medium text-ink-900">{dateHeure(compte.dateCreation)}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground">Dernière connexion</dt>
            <dd className="font-medium text-ink-900">{dateHeure(compte.derniereConnexion)}</dd>
          </div>
        </dl>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" disabled={!modifie} onClick={() => setValeurs(versFormulaire(compte))}>
            Annuler
          </Button>
          <Button type="submit" loading={enregistrement.isPending} disabled={!modifie}>
            Enregistrer
          </Button>
        </div>
      </form>
    </Carte>
  );
}

// ---------------------------------------------------------------------------
// Mot de passe
// ---------------------------------------------------------------------------

function SectionMotDePasse() {
  const queryClient = useQueryClient();
  const politique = usePolitiqueMotDePasse();
  const [ancien, setAncien] = useState("");
  const [nouveau, setNouveau] = useState("");
  const [confirmation, setConfirmation] = useState("");

  const regles = reglesMotDePasse(politique);
  const conforme = regles.every((regle) => regle.test(nouveau));
  const identiques = nouveau.length > 0 && nouveau === confirmation;

  const changement = useMutation({
    mutationFn: () => changerMotDePasse({ ancienMotDePasse: ancien, nouveauMotDePasse: nouveau }),
    onSuccess: async (reponse) => {
      const session = getSession();
      if (session && reponse?.accessToken) renouvelerSession(reponse.accessToken, session.user);
      setAncien("");
      setNouveau("");
      setConfirmation("");
      toast.success("Mot de passe changé. Vos autres sessions ont été fermées.");
      await queryClient.invalidateQueries({ queryKey: ["mes-sessions"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le changement a échoué.")),
  });

  return (
    <Carte
      titre="Mot de passe"
      description="Le changement ferme toutes vos autres sessions. Les règles suivent les paramètres de sécurité de la plateforme."
    >
      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (conforme && identiques && ancien) changement.mutate();
        }}
      >
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="mc-ancien">Mot de passe actuel</Label>
          <Input id="mc-ancien" type="password" autoComplete="current-password" value={ancien} onChange={(e) => setAncien(e.target.value)} required />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mc-nouveau">Nouveau mot de passe</Label>
            <Input id="mc-nouveau" type="password" autoComplete="new-password" value={nouveau} onChange={(e) => setNouveau(e.target.value)} required />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mc-confirmation">Confirmation</Label>
            <Input id="mc-confirmation" type="password" autoComplete="new-password" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} required />
          </div>
        </div>
        <ul className="grid gap-1 text-xs sm:grid-cols-2" aria-label="Règles du mot de passe">
          {[...regles, { id: "same", label: "Confirmation identique", test: () => identiques }].map((regle) => {
            const ok = regle.test(nouveau);
            return (
              <li key={regle.id} className={`flex items-center gap-1.5 ${ok ? "text-emerald-700" : "text-muted-foreground"}`}>
                {ok ? <Check className="size-3.5" aria-hidden="true" /> : <X className="size-3.5" aria-hidden="true" />}
                {regle.label}
              </li>
            );
          })}
        </ul>
        <div className="flex justify-end">
          <Button type="submit" loading={changement.isPending} disabled={!ancien || !conforme || !identiques}>
            Changer le mot de passe
          </Button>
        </div>
      </form>
    </Carte>
  );
}

// ---------------------------------------------------------------------------
// Sessions
// ---------------------------------------------------------------------------

function appareil(userAgent: string | null): string {
  if (!userAgent) return "Appareil inconnu";
  const navigateur = /Edg\//.test(userAgent) ? "Edge"
    : /OPR\//.test(userAgent) ? "Opera"
    : /Firefox\//.test(userAgent) ? "Firefox"
    : /Chrome\//.test(userAgent) ? "Chrome"
    : /Safari\//.test(userAgent) ? "Safari"
    : "Navigateur";
  const systeme = /Windows/.test(userAgent) ? "Windows"
    : /Android/.test(userAgent) ? "Android"
    : /iPhone|iPad/.test(userAgent) ? "iOS"
    : /Mac OS X/.test(userAgent) ? "macOS"
    : /Linux/.test(userAgent) ? "Linux"
    : "système inconnu";
  return `${navigateur} sur ${systeme}`;
}

function SectionSessions() {
  const queryClient = useQueryClient();
  const sessions = useQuery({ queryKey: ["mes-sessions"], queryFn: listerMesSessions });
  const actualiser = () => queryClient.invalidateQueries({ queryKey: ["mes-sessions"] });

  const fermeture = useMutation({
    mutationFn: (id: number) => fermerMaSession(id),
    onSuccess: async () => {
      toast.success("Session fermée.");
      await actualiser();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La fermeture a échoué.")),
  });

  const fermetureAutres = useMutation({
    mutationFn: fermerMesAutresSessions,
    onSuccess: async (nombre) => {
      toast.success(nombre > 0 ? `${nombre} session(s) fermée(s).` : "Aucune autre session ouverte.");
      await actualiser();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La fermeture a échoué.")),
  });

  const autres = (sessions.data ?? []).filter((session) => !session.courante).length;

  return (
    <Carte
      titre="Mes sessions"
      description="Appareils connectés à votre compte. Une session fermée ne peut plus se renouveler ; l'accès en cours expire au plus tard 15 minutes après."
      action={(
        <Button
          variant="secondary"
          size="sm"
          disabled={autres === 0}
          loading={fermetureAutres.isPending}
          onClick={() => fermetureAutres.mutate()}
        >
          <LogOut className="size-4" aria-hidden="true" /> Déconnecter les autres sessions
        </Button>
      )}
    >
      {sessions.isError && <EtatVide titre="Les sessions n'ont pas pu être chargées." />}
      {sessions.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
      {sessions.data && sessions.data.length === 0 && <EtatVide titre="Aucune session ouverte." />}
      {sessions.data && sessions.data.length > 0 && (
        <ul className="divide-y divide-brand-900/10 rounded-2xl border border-brand-900/10">
          {sessions.data.map((session) => (
            <li key={session.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
                <Monitor className="size-4" aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="flex flex-wrap items-center gap-2 text-sm font-semibold text-ink-900">
                  {appareil(session.appareil)}
                  {session.courante && <Badge variant="dark" size="sm">Cette session</Badge>}
                </p>
                <p className="text-xs text-muted-foreground">
                  Ouverte le {dateHeure(session.ouverture)} · expire le {dateHeure(session.expiration)}
                </p>
              </div>
              {!session.courante && (
                <Button
                  variant="ghost"
                  size="sm"
                  loading={fermeture.isPending && fermeture.variables === session.id}
                  onClick={() => fermeture.mutate(session.id)}
                >
                  Révoquer
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </Carte>
  );
}
