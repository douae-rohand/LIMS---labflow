import { Mail, MapPin, Phone } from "lucide-react";
import type { ReactNode } from "react";
import type { LaboratoirePublic } from "@/api/landing";
import { Card } from "@/components/lab";

/** Carte « Informations générales » d'une fiche laboratoire publique. */
export function InfosLaboratoire({ laboratoire }: { laboratoire: LaboratoirePublic }) {
  const hasCoords = laboratoire.latitude != null && laboratoire.longitude != null;

  return (
    <Card variant="glossy">
      <h2 className="text-lg font-extrabold text-ink-900">Informations générales</h2>
      <dl className="mt-5 grid gap-4 sm:grid-cols-2">
        <InfoItem label="Raison sociale" value={laboratoire.raisonSociale} />
        <InfoItem label="Code" value={laboratoire.code} />
        <InfoItem label="Ville" value={laboratoire.ville} />
        <InfoItem label="Adresse" value={laboratoire.adresse} />
        <InfoItem
          label="Téléphone"
          value={laboratoire.telephone}
          icon={<Phone className="size-3.5 text-brand-600" aria-hidden="true" />}
        />
        <InfoItem
          label="E-mail"
          value={laboratoire.email}
          icon={<Mail className="size-3.5 text-brand-600" aria-hidden="true" />}
        />
        <InfoItem label="ICE" value={laboratoire.ice} />
        {hasCoords ? (
          <InfoItem
            label="Coordonnées GPS"
            value={`${laboratoire.latitude}, ${laboratoire.longitude}`}
            icon={<MapPin className="size-3.5 text-brand-600" aria-hidden="true" />}
          />
        ) : null}
      </dl>
    </Card>
  );
}

function InfoItem({
  label,
  value,
  icon,
}: {
  label: string;
  value?: string | null | undefined;
  icon?: ReactNode | undefined;
}) {
  const text = value?.trim();
  return (
    <div>
      <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{label}</dt>
      <dd className="mt-1 flex items-start gap-2 text-sm font-semibold text-ink-900">
        {icon}
        {text || "Non renseigné"}
      </dd>
    </div>
  );
}
