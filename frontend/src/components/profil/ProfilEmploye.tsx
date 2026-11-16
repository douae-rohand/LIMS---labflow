import { useQuery } from "@tanstack/react-query";
import { BadgeCheck, CalendarDays, Mail, Phone, UserRound } from "lucide-react";
import { useSession } from "@/api/session";
import { trouverUtilisateurParId } from "@/api/utilisateurs";
import { Badge, Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { ProfilSignature } from "./ProfilSignature";
import { useTechnicienDomaines } from "@/hooks/useTechnicienDomaines";

const LIBELLES_ROLES: Record<string, string> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

export function ProfilEmploye() {
  const { session } = useSession();
  const estTechnicien = session?.user.role === "TECHNICIEN";
  const domainesQuery = useTechnicienDomaines(estTechnicien);
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
        <Card className={`mx-auto w-full space-y-4 p-6 ${estTechnicien ? "max-w-4xl" : "max-w-3xl"}`}>
          <Skeleton className="h-16 w-16 rounded-full" />
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-24 w-full" />
        </Card>
      )}
      {profil.isError && (
        <Card className={`mx-auto w-full p-6 text-sm text-destructive ${estTechnicien ? "max-w-4xl" : "max-w-3xl"}`} role="alert">
          Impossible de charger votre profil. Réessayez dans quelques instants.
        </Card>
      )}
      {utilisateur && (
        <div className={`mx-auto grid w-full gap-5 ${estTechnicien ? "max-w-4xl" : "max-w-3xl"}`}>
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
            </dl>
            {estTechnicien && (
              <section aria-labelledby="domaines-technicien" className="border-t border-brand-900/10 px-6 py-5 text-center">
                <h3 id="domaines-technicien" className="text-sm font-semibold text-ink-900">Domaines de compétence</h3>
                {domainesQuery.isLoading ? (
                  <div className="mt-3 flex justify-center gap-2" aria-label="Chargement des domaines">
                    <Skeleton className="h-7 w-24 rounded-full" />
                    <Skeleton className="h-7 w-32 rounded-full" />
                  </div>
                ) : domainesQuery.isError ? (
                  <p className="mt-2 text-sm text-destructive" role="alert">Impossible de charger vos domaines.</p>
                ) : domainesQuery.data?.length ? (
                  <div className="mt-3 flex flex-wrap justify-center gap-2">
                    {domainesQuery.data.map((domaine) => (
                      <Badge key={domaine.id} variant="soft" size="sm">{domaine.libelle}</Badge>
                    ))}
                  </div>
                ) : (
                  <p className="mt-2 text-sm text-muted-foreground">Aucun domaine ne vous est attribué.</p>
                )}
              </section>
            )}
          </Card>
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
