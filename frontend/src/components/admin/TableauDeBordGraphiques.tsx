import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  Legend,
} from "recharts";
import { Card } from "@/components/lab";
import type { DemandesStatsDto } from "@/api/statsDemandes";
import type { FactureStatsDto } from "@/api/facturesAdmin";

export function TableauDeBordGraphiques(props: {
  demandesStats: DemandesStatsDto | undefined;
  facturesStats: FactureStatsDto | undefined;
}) {
  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <DemandesGraphique demandesStats={props.demandesStats} />
      <FacturesGraphique facturesStats={props.facturesStats} />
    </div>
  );
}

function DemandesGraphique({ demandesStats }: { demandesStats: DemandesStatsDto | undefined }) {
  const chartData = demandesStats?.serieMensuelle.map((item) => ({
    mois: item.mois,
    demandes: item.valeur || 0,
  })) || [];

  return (
    <Card variant="surface" className="p-5">
      <h2 className="text-base font-bold text-ink-900 mb-4">Activité des demandes</h2>
      <div className="h-[200px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis
              dataKey="mois"
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#64748b", fontSize: 11 }}
              dy={8}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#64748b", fontSize: 11 }}
            />
            <Tooltip
              cursor={{ fill: "#f1f5f9" }}
              contentStyle={{
                borderRadius: "8px",
                border: "none",
                boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                fontSize: 12,
              }}
            />
            <Bar
              dataKey="demandes"
              fill="var(--color-brand-900)"
              radius={[4, 4, 0, 0]}
              barSize={28}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
      {demandesStats && (
        <div className="mt-4 pt-4 border-t border-brand-900/10">
          <p className="text-xs font-semibold text-ink-900 mb-2">Répartition par statut</p>
          <div className="flex flex-wrap gap-2">
            {Object.entries(demandesStats.parStatut).map(([statut, count]) => (
              <span
                key={statut}
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-brand-900/5 text-xs font-medium text-ink-900"
              >
                {statut}: {count}
              </span>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}

function FacturesGraphique({ facturesStats }: { facturesStats: FactureStatsDto | undefined }) {
  const chartData = facturesStats?.serieMensuelle.map((item) => ({
    mois: item.mois,
    facture: item.valeur || 0,
  })) || [];

  return (
    <Card variant="surface" className="p-5">
      <h2 className="text-base font-bold text-ink-900 mb-4">Évolution financière</h2>
      <div className="h-[200px]">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis
              dataKey="mois"
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#64748b", fontSize: 11 }}
              dy={8}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fill: "#64748b", fontSize: 11 }}
            />
            <Tooltip
              cursor={{ fill: "#f1f5f9" }}
              contentStyle={{
                borderRadius: "8px",
                border: "none",
                boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                fontSize: 12,
              }}
              formatter={(value: number) => [`${value.toLocaleString("fr-FR")} MAD`]}
            />
            <Line
              type="monotone"
              dataKey="facture"
              stroke="var(--color-brand-900)"
              strokeWidth={2}
              dot={{ fill: "var(--color-brand-900)", r: 4 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      {facturesStats && (
        <div className="mt-4 pt-4 border-t border-brand-900/10 grid grid-cols-3 gap-3">
          <div>
            <p className="text-xs text-muted-foreground">Facturé</p>
            <p className="text-sm font-bold text-ink-900">
              {facturesStats.facture.toLocaleString("fr-FR")} MAD
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Encaissé</p>
            <p className="text-sm font-bold text-emerald-600">
              {facturesStats.encaisse.toLocaleString("fr-FR")} MAD
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Impayé</p>
            <p className="text-sm font-bold text-rose-600">
              {facturesStats.impaye.toLocaleString("fr-FR")} MAD
            </p>
          </div>
        </div>
      )}
    </Card>
  );
}
