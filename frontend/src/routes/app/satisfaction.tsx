import { createFileRoute, redirect } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  HeartHandshake,
  MessageSquareText,
  Percent,
  Star,
} from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { useSatisfactionAdmin } from "@/hooks/useSatisfactionAdmin";
import { ExportButton } from "@/components/admin/exports/ExportButton";
import { getSession } from "@/api/session";
import { messageErreurSatisfactionAdmin } from "@/api/satisfactionAdmin";
import type { EnqueteReponseDto } from "@/api/satisfactionAdmin";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const PAGE_SIZE = 10;

function afficherDate(date: string): string {
  const parsedDate = new Date(date);
  return Number.isNaN(parsedDate.getTime())
    ? "—"
    : new Intl.DateTimeFormat("fr-FR").format(parsedDate);
}

function afficherNote(note: number | null): string {
  return note === null ? "Non renseignée" : `${note.toFixed(1)} / 5`;
}

function SatisfactionDetail({ enquete }: { enquete: EnqueteReponseDto }) {
  const criteres = [
    { label: "Note générale", note: enquete.note },
    { label: "Délai", note: enquete.delaiNote },
    { label: "Clarté", note: enquete.clarteNote },
    { label: "Relation", note: enquete.relationNote },
  ];

  return (
    <div className="space-y-5">
      <dl className="grid gap-4 rounded-2xl bg-surface-card p-4 sm:grid-cols-2">
        <div>
          <dt className="text-sm text-muted-foreground">Code enquête</dt>
          <dd className="font-medium text-ink-900">{enquete.code}</dd>
        </div>
        <div>
          <dt className="text-sm text-muted-foreground">Demande</dt>
          <dd className="font-medium text-ink-900">{enquete.demandeNumero}</dd>
        </div>
        <div>
          <dt className="text-sm text-muted-foreground">Client</dt>
          <dd className="font-medium text-ink-900">{enquete.clientNom ?? "Non renseigné"}</dd>
        </div>
        <div>
          <dt className="text-sm text-muted-foreground">Date de réponse</dt>
          <dd className="font-medium text-ink-900">{afficherDate(enquete.dateReponse)}</dd>
        </div>
      </dl>
      <section>
        <h3 className="mb-3 font-semibold text-ink-900">Notes par critère</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {criteres.map(({ label, note }) => (
            <div
              key={label}
              className="flex items-center justify-between gap-3 rounded-xl border border-brand-900/10 p-3"
            >
              <span className="text-sm text-muted-foreground">{label}</span>
              <span className="font-semibold text-ink-900">{afficherNote(note)}</span>
            </div>
          ))}
        </div>
      </section>
      <section>
        <h3 className="mb-2 font-semibold text-ink-900">Commentaire</h3>
        <p className="min-h-16 whitespace-pre-wrap rounded-xl bg-surface-card p-4 text-sm text-ink-900">
          {enquete.commentaire?.trim() || "Aucun commentaire laissé."}
        </p>
      </section>
    </div>
  );
}

export const Route = createFileRoute("/app/satisfaction")({
  head: () => ({ meta: [{ title: "Satisfaction — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "ADMINISTRATEUR") throw redirect({ to: "/app" });
  },
  component: SatisfactionPage,
});

function SatisfactionPage() {
  const [page, setPage] = useState(0);
  const [enqueteSelectionnee, setEnqueteSelectionnee] = useState<EnqueteReponseDto | null>(null);
  const { stats, enquetes } = useSatisfactionAdmin(undefined, undefined, page, PAGE_SIZE);

  useEffect(() => {
    if (enquetes.data && page >= Math.max(enquetes.data.totalPages, 1)) {
      setPage(Math.max(enquetes.data.totalPages - 1, 0));
    }
  }, [enquetes.data, page]);

  return (
    <Container className="py-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
            <HeartHandshake className="size-6" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Satisfaction client</h1>
            <p className="text-sm text-muted-foreground">
              Consultez les avis et les notes laissés après les demandes.
            </p>
          </div>
        </div>
        <ExportButton jeu="enquetes" disabled={enquetes.isLoading} />
      </div>

      {stats.isError && (
        <p
          role="alert"
          className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"
        >
          {messageErreurSatisfactionAdmin(stats.error)}
        </p>
      )}
      {enquetes.isError && (
        <p
          role="alert"
          className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"
        >
          {messageErreurSatisfactionAdmin(enquetes.error)}
        </p>
      )}

      <div className="mb-8 grid grid-cols-1 gap-4 md:grid-cols-3">
        {[
          {
            label: "Enquêtes envoyées",
            value: stats.data?.envoyees ?? "—",
            icon: MessageSquareText,
          },
          {
            label: "Réponses reçues",
            value: stats.data?.repondues ?? "—",
            icon: Star,
          },
          {
            label: "Taux de réponse",
            value:
              stats.data?.tauxReponse == null
                ? "—"
                : `${(stats.data.tauxReponse * 100).toFixed(1)}%`,
            icon: Percent,
          },
        ].map(({ label, value, icon: Icon }) => (
          <div key={label} className="rounded-2xl border border-brand-900/10 bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm text-muted-foreground">{label}</p>
              <span className="flex size-9 items-center justify-center rounded-xl bg-brand-900/8 text-brand-900">
                <Icon className="size-5" aria-hidden="true" />
              </span>
            </div>
            <p className="mt-3 text-3xl font-semibold tabular-nums text-ink-900">
              {stats.isLoading ? "…" : value}
            </p>
          </div>
        ))}
      </div>

      <section className="overflow-hidden rounded-2xl border border-brand-900/10 bg-card shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-900/10 px-5 py-4">
          <div>
            <h2 className="font-semibold text-ink-900">Réponses aux enquêtes</h2>
            <p className="text-sm text-muted-foreground">
              Sélectionnez une réponse pour consulter ses détails.
            </p>
          </div>
          <span className="inline-flex items-center gap-2 rounded-full bg-surface-card px-3 py-1.5 text-sm text-muted-foreground">
            <CalendarDays className="size-4" aria-hidden="true" />
            {enquetes.data?.totalElements ?? 0} réponse(s)
          </span>
        </div>
        {enquetes.isLoading && (
          <p className="p-5 text-sm text-muted-foreground">Chargement des réponses...</p>
        )}
        {enquetes.data && enquetes.data.content.length === 0 && (
          <div className="p-8 text-center">
            <div className="mx-auto mb-3 flex size-11 items-center justify-center rounded-full bg-surface-card text-muted-foreground">
              <MessageSquareText className="size-5" aria-hidden="true" />
            </div>
            <p className="font-medium text-ink-900">Aucune réponse pour le moment</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Les réponses aux enquêtes apparaîtront ici.
            </p>
          </div>
        )}
        {enquetes.data && enquetes.data.content.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left">
                <thead className="bg-surface-card">
                  <tr>
                    <th
                      scope="col"
                      className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                    >
                      Code enquête
                    </th>
                    <th
                      scope="col"
                      className="px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                    >
                      Date de réponse
                    </th>
                    <th
                      scope="col"
                      className="px-5 py-3 text-right text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                    >
                      Note
                    </th>
                    <th
                      scope="col"
                      className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                    >
                      Commentaire
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {enquetes.data.content.map((enquete) => (
                    <tr
                      key={enquete.id}
                      className="border-t border-brand-900/10 transition-colors hover:bg-surface-card/70"
                    >
                      <td className="px-5 py-3">
                        <button
                          type="button"
                          className="font-medium text-brand-900 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-brand-900"
                          onClick={() => setEnqueteSelectionnee(enquete)}
                        >
                          {enquete.code}
                        </button>
                      </td>
                      <td className="whitespace-nowrap px-5 py-3 text-sm text-muted-foreground">
                        {afficherDate(enquete.dateReponse)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-3 text-right">
                        <span className="inline-flex items-center justify-end gap-1 font-medium text-ink-900">
                          <Star
                            className="size-4 fill-amber-400 text-amber-500"
                            aria-hidden="true"
                          />
                          {enquete.note === null ? "—" : `${enquete.note}/5`}
                        </span>
                      </td>
                      <td className="max-w-md px-5 py-3 text-sm text-muted-foreground">
                        <button
                          type="button"
                          className="block max-w-full truncate text-left hover:text-ink-900 focus-visible:outline-2 focus-visible:outline-brand-900"
                          onClick={() => setEnqueteSelectionnee(enquete)}
                          aria-label={`Afficher les détails de l'enquête ${enquete.code}`}
                        >
                          {enquete.commentaire?.trim() || "Aucun commentaire"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {enquetes.data.totalPages > 1 && (
              <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-900/10 px-5 py-4">
                <p className="text-sm text-muted-foreground">
                  Page {page + 1} sur {enquetes.data.totalPages}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={page === 0 || enquetes.isFetching}
                    onClick={() => setPage((current) => current - 1)}
                  >
                    <ChevronLeft className="size-4" aria-hidden="true" /> Précédent
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={page + 1 >= enquetes.data.totalPages || enquetes.isFetching}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    Suivant <ChevronRight className="size-4" aria-hidden="true" />
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </section>

      <Dialog
        open={enqueteSelectionnee !== null}
        onOpenChange={(open) => {
          if (!open) setEnqueteSelectionnee(null);
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto rounded-3xl border-brand-900/10 bg-card sm:max-w-xl">
          <DialogHeader className="pr-8">
            <DialogTitle>Détail de la réponse</DialogTitle>
            <DialogDescription>
              Notes et commentaire laissés pour cette enquête de satisfaction.
            </DialogDescription>
          </DialogHeader>
          {enqueteSelectionnee && <SatisfactionDetail enquete={enqueteSelectionnee} />}
        </DialogContent>
      </Dialog>
    </Container>
  );
}
