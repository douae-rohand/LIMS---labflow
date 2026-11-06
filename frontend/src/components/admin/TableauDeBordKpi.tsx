import { Clock, DollarSign, AlertTriangle, Activity } from "lucide-react";
import { Card } from "@/components/lab";
import { cn } from "@/lib/utils";

interface TableauDeBordKpiProps {
  demandesRecues: number;
  demandesEnCours: number;
  facturesEnRetard: number;
  caEncaisse: number;
}

export function TableauDeBordKpi({
  demandesRecues,
  demandesEnCours,
  facturesEnRetard,
  caEncaisse,
}: TableauDeBordKpiProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <KpiCard
        label="Demandes reçues"
        value={demandesRecues}
        icon={Activity}
        variant="info"
      />
      <KpiCard
        label="Demandes en cours"
        value={demandesEnCours}
        icon={Clock}
        variant="warning"
      />
      <KpiCard
        label="Factures en retard"
        value={facturesEnRetard}
        icon={AlertTriangle}
        variant="error"
      />
      <KpiCard
        label="CA encaissé"
        value={caEncaisse}
        valueSuffix="MAD"
        icon={DollarSign}
        variant="success"
      />
    </div>
  );
}

function KpiCard({
  label,
  value,
  valueSuffix,
  icon: Icon,
  variant,
}: {
  label: string;
  value: number;
  valueSuffix?: string;
  icon: React.ElementType;
  variant: "brand" | "success" | "warning" | "error" | "info";
}) {
  const variants = {
    brand: "bg-brand-900/10 text-brand-900",
    success: "bg-emerald-500/10 text-emerald-600",
    warning: "bg-amber-500/10 text-amber-600",
    error: "bg-rose-500/10 text-rose-600",
    info: "bg-sky-500/10 text-sky-600",
  };

  return (
    <Card
      variant="surface"
      className="h-full transition-all duration-200 hover:-translate-y-1 hover:shadow-lg"
      padding="md"
    >
      <div className="flex h-full flex-col justify-between gap-4">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="text-sm font-medium text-muted-foreground">{label}</p>
            <div className="mt-2 flex flex-wrap items-baseline gap-1">
              <span className="text-3xl font-extrabold text-ink-900">
                {value.toLocaleString("fr-FR")}
              </span>
              {valueSuffix && (
                <span className="text-sm font-bold text-muted-foreground">
                  {valueSuffix}
                </span>
              )}
            </div>
          </div>
          <div
            className={cn("shrink-0 rounded-xl p-2.5 transition-colors", variants[variant])}
          >
            <Icon className="size-5" />
          </div>
        </div>
      </div>
    </Card>
  );
}
