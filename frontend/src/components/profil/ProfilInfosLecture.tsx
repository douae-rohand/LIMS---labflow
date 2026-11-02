import { Card } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import type { ProfilClientDto } from "@/api/profilClient";

/** Informations d'identité du compte client, en lecture seule. */
export function ProfilInfosLecture({ profil }: { profil: ProfilClientDto }) {
  const lignes: { libelle: string; valeur: string }[] = [
    { libelle: "Nom", valeur: profil.nom ?? "—" },
    { libelle: "Prénom", valeur: profil.prenom ?? "—" },
    { libelle: "Adresse e-mail", valeur: profil.email ?? "—" },
    { libelle: "Téléphone", valeur: profil.telephone ?? "—" },
    { libelle: "Compte créé le", valeur: formatDateFr(profil.dateCreation) },
  ];

  return (
    <Card variant="flat" padding="lg">
      <h2 className="text-lg font-extrabold text-ink-900">Identité</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Ces informations proviennent de votre compte et ne peuvent pas être modifiées depuis cet
        espace.
      </p>

      <dl className="mt-6 grid gap-4 sm:grid-cols-2">
        {lignes.map((ligne) => (
          <div key={ligne.libelle}>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              {ligne.libelle}
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">{ligne.valeur}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}
