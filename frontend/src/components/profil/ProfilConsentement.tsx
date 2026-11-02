import { Badge, Card } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import type { ProfilClientDto } from "@/api/profilClient";

/**
 * État du consentement CNDP : affiché en lecture seule, aucune action
 * de retrait ou de renouvellement n'est proposée ici.
 */
export function ProfilConsentement({ profil }: { profil: ProfilClientDto }) {
  const accorde = profil.consentementCndp === true;

  return (
    <Card variant="surface" padding="lg">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-extrabold text-ink-900">Consentement CNDP</h2>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Traitement de vos données personnelles conformément à la loi 09-08.
          </p>
        </div>
        <Badge variant={accorde ? "lime" : "outline"} size="sm">
          {accorde ? "Consentement accordé" : "Consentement non accordé"}
        </Badge>
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-2">
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Date du consentement
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatDateFr(profil.dateConsentementCndp)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Version du texte
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {profil.versionConsentementCndp ?? "—"}
          </dd>
        </div>
      </dl>
    </Card>
  );
}
