import { useQuery } from "@tanstack/react-query";
import { BadgeCheck, CalendarDays, Mail, Phone, ShieldCheck, UserRound } from "lucide-react";
import { useSession } from "@/api/session";
import { trouverUtilisateurParId } from "@/api/utilisateurs";
import { Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { ProfilDeuxFacteurs } from "./ProfilDeuxFacteurs";
import { ProfilSignature } from "./ProfilSignature";

const LIBELLES_ROLES: Record<string, string> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

export function ProfilEmploye() {
  const { session } = useSession();
  const userId = session?.user.userId;
  const profil = useQuery({
    queryKey: ["profil-utilisateur", userId],
    queryFn: () => {
      if (userId === null || userId === undefined) {
        throw new Error("Identifiant utilisateur indisponible.");
      }
      return trouverUtilisateurParId(userId);
    },
    enabled: userId !== null && userId !== undefined,
  });

  const utilisateur = profil.data;
  const initiales = utilisateur
    ? `${utilisateur.prenom?.[0] ?? ""}${utilisateur.nom?.[0] ?? ""}`.toUpperCase()
    : "";

  return (
    <Container className="py-8">
      <header className="mb-7 flex items-center gap-4">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
          <UserRound className="size-6" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Mon profil</h1>
          <p className="mt-1 text-sm text-muted-foreground">Vos informations personnelles et de compte.</p>
        </div>
      </header>

      {profil.isLoading && (
        <Card className="max-w-3xl space-y-4 p-6">
          <Skeleton className="h-16 w-16 rounded-full" />
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-24 w-full" />
        </Card>
      )}
      {profil.isError && (
        <Card className="max-w-3xl p-6 text-sm text-destructive" role="alert">
          Impossible de charger votre profil. Réessayez dans quelques instants.
        </Card>
      )}
      {utilisateur && (
        <div className="grid max-w-3xl gap-5">
          <Card className="overflow-hidden p-0">
            <div className="flex flex-wrap items-center gap-4 border-b border-brand-900/10 bg-surface-card/50 p-6">
              <span className="flex size-16 items-center justify-center rounded-full bg-brand-900 text-lg font-bold text-primary-foreground">
                {initiales || "U"}
              </span>
              <div className="min-w-0 flex-1">
                <h2 className="truncate text-xl font-bold text-ink-900">{utilisateur.nomComplet}</h2>
                <p className="mt-1 text-sm text-muted-foreground">{LIBELLES_ROLES[utilisateur.role] ?? utilisateur.role}</p>
              </div>
              <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${
                utilisateur.actif ? "bg-emerald-50 text-emerald-800" : "bg-surface-card text-muted-foreground"
              }`}>
                <BadgeCheck className="size-4" aria-hidden="true" />
                {utilisateur.actif ? "Compte actif" : "Compte inactif"}
              </span>
            </div>
            <dl className="grid gap-5 p-6 sm:grid-cols-2">
              <Info icon={Mail} label="Adresse e-mail" value={utilisateur.email} />
              <Info icon={Phone} label="Téléphone" value={utilisateur.telephone || "Non renseigné"} />
              <Info icon={CalendarDays} label="Membre depuis" value={formaterDate(utilisateur.dateCreation)} />
              <Info icon={ShieldCheck} label="Authentification à deux facteurs" value={utilisateur.deuxFacteursActif ? "Activée" : "Non activée"} />
            </dl>
          </Card>
          <ProfilDeuxFacteurs active={utilisateur.deuxFacteursActif} />
          {session?.user.role === "RESPONSABLE" && <ProfilSignature />}
        </div>
      )}
    </Container>
  );
}

function Info({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Mail;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
        <Icon className="size-4" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
        <dd className="mt-1 break-words text-sm font-medium text-ink-900">{value}</dd>
      </div>
    </div>
  );
}

function formaterDate(date: string | null): string {
  if (!date) return "Non renseignée";
  const valeur = new Date(date);
  if (Number.isNaN(valeur.getTime())) return date;
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(valeur);
}
