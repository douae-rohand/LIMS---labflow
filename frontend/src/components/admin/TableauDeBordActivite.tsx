import { Link } from "@tanstack/react-router";
import { Activity } from "lucide-react";
import { Card } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import type { AuditEntryDto } from "@/api/audit";

interface TableauDeBordActiviteProps {
  auditRecent: AuditEntryDto[] | undefined;
}

export function TableauDeBordActivite({ auditRecent }: TableauDeBordActiviteProps) {
  return (
    <Card variant="surface" className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-ink-900">Activité récente</h2>
        <Link
          to="/app/audit"
          className="text-sm font-semibold text-brand-900 hover:underline"
        >
          Voir tout
        </Link>
      </div>
      {!auditRecent || auditRecent.length === 0 ? (
        <p className="text-sm text-muted-foreground italic">
          Aucune activité récente.
        </p>
      ) : (
        <div className="space-y-3">
          {auditRecent.slice(0, 5).map((entry) => (
            <div
              key={entry.code}
              className="flex items-start gap-3 p-3 rounded-xl bg-brand-900/5"
            >
              <div className="rounded-xl bg-brand-900/10 p-2 text-brand-900 shrink-0">
                <Activity className="size-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-ink-900 truncate">
                  {entry.action}
                </p>
                {entry.objet && (
                  <p className="text-xs text-muted-foreground truncate">
                    {entry.objet}
                  </p>
                )}
                <p className="text-xs text-muted-foreground mt-1">
                  {formatDateFr(entry.dateAction)} • {entry.acteurNom}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
