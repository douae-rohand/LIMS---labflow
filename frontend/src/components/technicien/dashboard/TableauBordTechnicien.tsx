import { CalendarClock, FlaskConical } from "lucide-react";
import { Card, Skeleton } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { getSession } from "@/api/session";
import { ApiError } from "@/api/axios";
import { useTechnicienSynthese } from "@/hooks/useTechnicienEssais";
import { CartesSynthese } from "@/components/technicien/dashboard/CartesSynthese";
import { ProchainsEssais } from "@/components/technicien/dashboard/ProchainsEssais";
import { ChargeSemaine } from "@/components/technicien/dashboard/ChargeSemaine";

/** Salutation courte selon l'heure locale. */
function salutation(): string {
  const h = new Date().getHours();
  if (h < 12) return "Bonjour";
  if (h < 18) return "Bon après-midi";
  return "Bonsoir";
}

/** Bloc synthèse : cartes cliquables avec état d'erreur isolé. */
function BlocSynthese() {
  const query = useTechnicienSynthese();
  const { data, isLoading, isError, error } = query;

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-24 rounded-3xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 403) {
      return (
        <Card variant="flat" padding="lg" className="text-center">
          <p className="text-sm font-medium text-amber-700" role="alert">
            Vous n'avez pas la permission de consulter le tableau de bord.
          </p>
        </Card>
      );
    }
    return (
      <Card variant="flat" padding="lg" className="text-center">
        <p className="text-sm font-medium text-red-700" role="alert">
          Impossible de charger le tableau de bord.
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          Vérifiez votre connexion ou réessayez.
        </p>
      </Card>
    );
  }

  if (!data) return null;
  return <CartesSynthese synthese={data} />;
}

/**
 * TableauBordTechnicien — Page d'accueil du technicien sur /app.
 * Salutation, cartes cliquables, prochains essais, charge de la semaine.
 */
export function TableauBordTechnicien() {
  const session = getSession();
  const prenom = session?.user.nomComplet.split(" ")[0] ?? "";

  return (
    <Container className="py-6 sm:py-8">
      {/* Salutation */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-ink-900">
          {salutation()}
          {prenom ? `, ${prenom}` : ""}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Voici vos essais et votre charge du moment.
        </p>
      </div>

      {/* Bloc cartes synthèse (erreur isolée) */}
      <BlocSynthese />

      {/* Deux colonnes : prochains essais + charge semaine */}
      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        {/* Prochains essais */}
        <section aria-labelledby="titre-prochains">
          <div className="mb-3 flex items-center gap-2">
            <FlaskConical className="size-5 text-brand-900" aria-hidden="true" />
            <h2 id="titre-prochains" className="text-lg font-bold text-ink-900">
              Mes prochains essais
            </h2>
          </div>
          <ProchainsEssais />
        </section>

        {/* Charge de la semaine */}
        <section aria-labelledby="titre-charge">
          <div className="mb-3 flex items-center gap-2">
            <CalendarClock className="size-5 text-brand-900" aria-hidden="true" />
            <h2 id="titre-charge" className="text-lg font-bold text-ink-900">
              Charge de la semaine
            </h2>
          </div>
          <ChargeSemaineInline />
        </section>
      </div>
    </Container>
  );
}

/** Charge de la semaine : lit la synthèse et passe la charge au composant. */
function ChargeSemaineInline() {
  const query = useTechnicienSynthese();
  return (
    <ChargeSemaine
      charge={query.data?.chargeSemaine}
      isLoading={query.isLoading}
      isError={query.isError}
      error={query.error}
    />
  );
}
