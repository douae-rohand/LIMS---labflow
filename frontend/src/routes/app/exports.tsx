import { createFileRoute, redirect } from "@tanstack/react-router";
import {
  ClipboardList,
  Download,
  FileSpreadsheet,
  FileText,
  Package,
  ReceiptText,
  ScrollText,
  Users,
} from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { useExports } from "@/hooks/useExports";
import { PERMISSION_EXPORT, PERMISSIONS_EXPORT, type ExportJeu, type ExportFormat } from "@/api/exports";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/exports")({
  head: () => ({ meta: [{ title: "Exports — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (!PERMISSIONS_EXPORT.some((permission) => session.user.permissions.includes(permission))) {
      throw redirect({ to: "/app" });
    }
  },
  component: ExportsPage,
});

const JEUX: {
  jeu: ExportJeu;
  label: string;
  description: string;
  Icone: typeof ClipboardList;
}[] = [
  { jeu: "demandes", label: "Demandes", description: "Suivi et informations des demandes du laboratoire.", Icone: ClipboardList },
  { jeu: "factures", label: "Factures", description: "Factures émises et données de facturation.", Icone: ReceiptText },
  { jeu: "employes", label: "Employés", description: "Liste des membres et informations de leur compte.", Icone: Users },
  { jeu: "enquetes", label: "Enquêtes de satisfaction", description: "Réponses et résultats des enquêtes.", Icone: FileText },
  { jeu: "produits", label: "Produits avec stock", description: "Produits, quantités et informations de stock.", Icone: Package },
  { jeu: "audit", label: "Journal d'audit", description: "Historique des actions enregistrées.", Icone: ScrollText },
];

const FORMATS: { format: ExportFormat; label: string; description: string }[] = [
  { format: "csv", label: "CSV", description: "Format léger, compatible avec les tableurs." },
  { format: "xlsx", label: "Excel", description: "Classeur prêt à consulter ou à modifier." },
  { format: "pdf", label: "PDF", description: "Document facile à partager et à imprimer." },
];

function ExportsPage() {
  const { exporter } = useExports();
  const permissions = getSession()?.user.permissions ?? [];

  return (
    <Container className="py-8">
      <div className="mb-8 flex items-center gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
          <Download className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Exports</h1>
          <p className="text-sm text-muted-foreground">
            Choisissez un jeu de données et le format souhaité. Jusqu'à 50 000 lignes par export.
          </p>
        </div>
      </div>

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {JEUX.filter(({ jeu }) => permissions.includes(PERMISSION_EXPORT[jeu])).map(
          ({ jeu, label, description, Icone }) => (
            <section
              key={jeu}
              className="overflow-hidden rounded-2xl border border-brand-900/10 bg-white shadow-sm"
              aria-labelledby={`export-${jeu}-titre`}
            >
              <header className="flex min-h-24 items-start gap-3 border-b border-brand-900/10 bg-surface-card/50 p-5">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
                  <Icone className="size-5" aria-hidden="true" />
                </div>
                <div>
                  <h2 id={`export-${jeu}-titre`} className="font-semibold text-ink-900">{label}</h2>
                  <p className="mt-1 text-sm leading-5 text-muted-foreground">{description}</p>
                </div>
              </header>

              <div className="divide-y divide-brand-900/10 px-5">
                {FORMATS.map(({ format, label: formatLabel, description: formatDescription }) => (
                  <div key={format} className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg border border-brand-900/10 bg-white text-brand-900">
                        {format === "csv" ? (
                          <FileText className="size-4" aria-hidden="true" />
                        ) : format === "xlsx" ? (
                          <FileSpreadsheet className="size-4" aria-hidden="true" />
                        ) : (
                          <ScrollText className="size-4" aria-hidden="true" />
                        )}
                      </span>
                      <div>
                        <p className="text-sm font-semibold text-ink-900">{formatLabel}</p>
                        <p className="text-xs text-muted-foreground">{formatDescription}</p>
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="shrink-0"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => exporter.mutate({ jeu, format })}
                      disabled={exporter.isPending}
                    >
                      Télécharger
                    </Button>
                  </div>
                ))}
              </div>
            </section>
          ),
        )}
      </div>
    </Container>
  );
}
