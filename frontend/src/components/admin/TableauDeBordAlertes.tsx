import { Link } from "@tanstack/react-router";
import { ArrowRight, AlertTriangle, Clock } from "lucide-react";
import { Card } from "@/components/lab";

interface TableauDeBordAlertesProps {
  facturesEnRetard: number;
  demandesEnAttente: number;
  alertesStock: number;
}

export function TableauDeBordAlertes({
  facturesEnRetard,
  demandesEnAttente,
  alertesStock,
}: TableauDeBordAlertesProps) {
  const aDesAlertes = facturesEnRetard > 0 || demandesEnAttente > 0 || alertesStock > 0;

  return (
    <Card variant="surface" className="p-6">
      <h2 className="text-lg font-bold text-ink-900 mb-4">Points d'attention</h2>
      {!aDesAlertes ? (
        <p className="text-sm text-muted-foreground italic">
          Aucun point d'attention pour le moment.
        </p>
      ) : (
        <div className="flex flex-col gap-3">
          {facturesEnRetard > 0 && (
            <AlertItem
              to="/app/facturation"
              search={{ echeanceDepassee: true }}
              icon={AlertTriangle}
              color="rose"
              title="Factures en retard"
              description={`${facturesEnRetard} facture${facturesEnRetard > 1 ? "s" : ""} en retard de paiement`}
            />
          )}
          {demandesEnAttente > 0 && (
            <AlertItem
              to="/app/demandes-clients"
              search={{ statut: "NOUVEAU" }}
              icon={Clock}
              color="amber"
              title="Demandes en attente"
              description={`${demandesEnAttente} demande${demandesEnAttente > 1 ? "s" : ""} à traiter`}
            />
          )}
          {alertesStock > 0 && (
            <AlertItem
              to="/app/stock"
              icon={AlertTriangle}
              color="orange"
              title="Alertes de stock"
              description={`${alertesStock} alerte${alertesStock > 1 ? "s" : ""} de stock`}
            />
          )}
        </div>
      )}
    </Card>
  );
}

function AlertItem({
  to,
  search,
  icon: Icon,
  color,
  title,
  description,
}: {
  to: string;
  search?: any;
  icon: React.ElementType;
  color: "rose" | "amber" | "orange";
  title: string;
  description: string;
}) {
  const colorClasses = {
    rose: "bg-rose-50 hover:bg-rose-100",
    amber: "bg-amber-50 hover:bg-amber-100",
    orange: "bg-orange-50 hover:bg-orange-100",
  };

  const iconBgClasses = {
    rose: "bg-rose-500/10 text-rose-600 group-hover:bg-rose-500/20",
    amber: "bg-amber-500/10 text-amber-600 group-hover:bg-amber-500/20",
    orange: "bg-orange-500/10 text-orange-600 group-hover:bg-orange-500/20",
  };

  const arrowColorClasses = {
    rose: "text-rose-600",
    amber: "text-amber-600",
    orange: "text-orange-600",
  };

  return (
    <Link
      to={to as any}
      search={search}
      className={`group flex items-center gap-3 rounded-2xl p-3 transition-all hover:shadow-sm ${colorClasses[color]}`}
    >
      <div className={`rounded-xl p-2 shrink-0 ${iconBgClasses[color]}`}>
        <Icon className="size-5" />
      </div>
      <div className="flex-1">
        <p className="text-sm font-bold text-ink-900">{title}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <ArrowRight className={`size-4 transition-transform group-hover:translate-x-1 ${arrowColorClasses[color]}`} />
    </Link>
  );
}
