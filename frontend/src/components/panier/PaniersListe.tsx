import { Link } from "@tanstack/react-router";
import { FlaskConical } from "lucide-react";
import type { PanierResume } from "@/api/paniers";
import { Badge, Button, Card, IconBox } from "@/components/lab";
import { formatMontant } from "./format";

/** Liste des paniers du client, un par laboratoire. */
export function PaniersListe({ paniers }: { paniers: PanierResume[] }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {paniers.map((panier) => (
        <Card key={panier.laboratoireId} variant="glossy" className="flex h-full flex-col p-4">
          <div className="flex items-start justify-between gap-2">
            <IconBox icon={FlaskConical} variant="mint" size="sm" />
            <Badge size="sm" variant="lime">
              {panier.nombreEssais} essai{panier.nombreEssais > 1 ? "s" : ""}
            </Badge>
          </div>
          <h3 className="mt-3 text-base font-bold text-ink-900">{panier.laboratoireNom}</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Sous-total&nbsp;: {formatMontant(panier.sousTotal)}
          </p>
          <div className="mt-auto pt-4">
            <Link
              to="/app/panier/$laboratoireId"
              params={{ laboratoireId: String(panier.laboratoireId) }}
            >
              <Button size="sm" className="w-full">Ouvrir le panier</Button>
            </Link>
          </div>
        </Card>
      ))}
    </div>
  );
}
