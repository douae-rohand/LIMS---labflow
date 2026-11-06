import { Link } from "@tanstack/react-router";
import { Calendar } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { TableauDeBordKpi } from "./TableauDeBordKpi";
import { TableauDeBordGraphiques } from "./TableauDeBordGraphiques";
import { TableauDeBordAlertes } from "./TableauDeBordAlertes";
import { TableauDeBordActivite } from "./TableauDeBordActivite";
import type { DemandesStatsDto } from "@/api/statsDemandes";
import type { FactureStatsDto } from "@/api/facturesAdmin";
import type { AlertesStockDto } from "@/api/stockAdmin";
import type { AuditEntryDto } from "@/api/audit";

interface TableauDeBordAdminProps {
  prenom: string;
  dateAujourdhui: string;
  demandesStats: DemandesStatsDto | undefined;
  facturesStats: FactureStatsDto | undefined;
  stockAlertes: AlertesStockDto | undefined;
  auditRecent: AuditEntryDto[] | undefined;
  demandesEnCours: number;
  facturesEnRetard: number;
}

export function TableauDeBordAdmin({
  prenom,
  dateAujourdhui,
  demandesStats,
  facturesStats,
  stockAlertes,
  auditRecent,
  demandesEnCours,
  facturesEnRetard,
}: TableauDeBordAdminProps) {
  const demandesRecues = demandesStats?.nombreSurPeriode ?? 0;
  const caEncaisse = facturesStats?.encaisse ?? 0;

  return (
    <Container className="space-y-8 py-10">
      <TableauDeBordHeader prenom={prenom} dateAujourdhui={dateAujourdhui} />
      <TableauDeBordKpi
        demandesRecues={demandesRecues}
        demandesEnCours={demandesEnCours}
        facturesEnRetard={facturesEnRetard}
        caEncaisse={caEncaisse}
      />
      <TableauDeBordGraphiques
        demandesStats={demandesStats}
        facturesStats={facturesStats}
      />
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        <TableauDeBordAlertes
          facturesEnRetard={facturesStats?.nombreEnRetard ?? 0}
          demandesEnAttente={demandesStats?.parStatut["NOUVEAU"] ?? 0}
          alertesStock={
            (stockAlertes?.sousSeuil?.length ?? 0) +
            (stockAlertes?.rupture?.length ?? 0) +
            (stockAlertes?.perimes?.length ?? 0)
          }
        />
        <TableauDeBordActivite auditRecent={auditRecent} />
      </div>
    </Container>
  );
}

function TableauDeBordHeader({
  prenom,
  dateAujourdhui,
}: {
  prenom: string;
  dateAujourdhui: string;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-ink-900">
          Bonjour, {prenom || "Administrateur"}
        </h1>
        <p className="mt-1 text-muted-foreground">
          Vue d'ensemble de l'activité du laboratoire
        </p>
      </div>
      <div className="flex items-center gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-2 text-sm font-medium text-ink-900 shadow-sm">
        <Calendar className="size-4 text-brand-900" />
        {dateAujourdhui}
      </div>
    </div>
  );
}
