import { createFileRoute, redirect } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Activity, CalendarDays, ChevronDown, ChevronLeft, ChevronRight, Filter, Search, X } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { messageErreurAudit, type ListerAuditParams } from "@/api/audit";
import { useAudit } from "@/hooks/useAudit";
import { getSession } from "@/api/session";
import { ExportButton } from "@/components/admin/exports/ExportButton";
import { Button } from "@/components/lab";

export const Route = createFileRoute("/app/audit")({
  head: () => ({ meta: [{ title: "Journal d'audit — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (!session.user.permissions.includes("AUDIT_CONSULTER")) throw redirect({ to: "/app" });
  },
  component: AuditPage,
});

const ROLES = [
  { value: "ADMINISTRATEUR", label: "Administrateur" },
  { value: "RESPONSABLE", label: "Responsable" },
  { value: "TECHNICIEN", label: "Technicien" },
  { value: "ACCUEIL", label: "Accueil" },
  { value: "CLIENT", label: "Client" },
];

const FILTRE_VIDE: ListerAuditParams = { acteur: "", role: "", action: "", typeObjet: "", debut: "", fin: "", q: "" };

function AuditPage() {
  const [brouillon, setBrouillon] = useState(FILTRE_VIDE);
  const [filtres, setFiltres] = useState(FILTRE_VIDE);
  const [page, setPage] = useState(0);
  const [taille, setTaille] = useState(10);
  const { liste } = useAudit({ ...filtres, page, size: taille });
  const totalPages = liste.data?.totalPages ?? 0;
  const totalElements = liste.data?.totalElements ?? 0;

  function modifier<K extends keyof ListerAuditParams>(cle: K, valeur: ListerAuditParams[K]) {
    setBrouillon((courant) => ({ ...courant, [cle]: valeur }));
  }

  function appliquerFiltres(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFiltres(brouillon);
    setPage(0);
  }

  function reinitialiserFiltres() {
    setBrouillon(FILTRE_VIDE);
    setFiltres(FILTRE_VIDE);
    setPage(0);
  }

  const filtresActifs = Object.values(filtres).some((valeur) => Boolean(valeur?.trim()));

  return (
    <Container className="py-8">
      <div className="mb-7 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
            <Activity className="size-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-ink-900">Journal d'audit</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Consultez les actions réalisées dans votre laboratoire.
            </p>
          </div>
        </div>
        <ExportButton jeu="audit" disabled={liste.isLoading} />
      </div>

      <form onSubmit={appliquerFiltres} className="mb-5 rounded-2xl border border-brand-900/10 bg-white p-4 shadow-sm sm:p-5">
        <div className="mb-4 flex items-center gap-2 text-sm font-semibold text-ink-900">
          <Filter className="size-4 text-brand-900" aria-hidden="true" />
          Filtrer le journal
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Acteur
            <input value={brouillon.acteur ?? ""} onChange={(event) => modifier("acteur", event.target.value)}
              placeholder="Nom ou courriel" className={CHAMP} />
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Rôle
            <span className="relative block">
              <select value={brouillon.role ?? ""} onChange={(event) => modifier("role", event.target.value)}
                className={`${CHAMP} appearance-none pr-10`}>
                <option value="">Tous les rôles</option>
                {ROLES.map((role) => <option key={role.value} value={role.value}>{role.label}</option>)}
              </select>
              <ChevronDown className={CHEVRON} aria-hidden="true" />
            </span>
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Action
            <input value={brouillon.action ?? ""} onChange={(event) => modifier("action", event.target.value)}
              placeholder="Ex. MODIFICATION_ESSAI" className={CHAMP} />
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Type d'objet
            <input value={brouillon.typeObjet ?? ""} onChange={(event) => modifier("typeObjet", event.target.value)}
              placeholder="Ex. Demande, Facture" className={CHAMP} />
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground sm:col-span-2">
            Rechercher dans les objets
            <span className="relative block">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <input value={brouillon.q ?? ""} onChange={(event) => modifier("q", event.target.value)}
                placeholder="Nom ou référence de l'objet" className={`${CHAMP} pl-9`} />
            </span>
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Du
            <span className="relative block">
              <CalendarDays className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <input type="date" value={brouillon.debut ?? ""} onChange={(event) => modifier("debut", event.target.value)}
                className={`${CHAMP} pl-9`} />
            </span>
          </label>
          <label className="space-y-1.5 text-xs font-medium text-muted-foreground">
            Au
            <span className="relative block">
              <CalendarDays className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <input type="date" value={brouillon.fin ?? ""} onChange={(event) => modifier("fin", event.target.value)}
                className={`${CHAMP} pl-9`} />
            </span>
          </label>
        </div>
        <div className="mt-4 flex flex-wrap justify-end gap-2">
          {filtresActifs && (
            <Button type="button" variant="ghost" size="sm" onClick={reinitialiserFiltres}
              icon={<X className="size-4" aria-hidden="true" />} iconPosition="left">
              Effacer les filtres
            </Button>
          )}
          <Button type="submit" size="sm" icon={<Search className="size-4" aria-hidden="true" />} iconPosition="left">
            Appliquer
          </Button>
        </div>
      </form>

      <section className="overflow-hidden rounded-2xl border border-brand-900/10 bg-white shadow-sm" aria-label="Entrées du journal d'audit">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-900/10 px-5 py-4">
          <div>
            <h2 className="font-semibold text-ink-900">Activité récente</h2>
            <p className="text-sm text-muted-foreground">
              {liste.isLoading ? "Chargement des entrées…" : `${totalElements} entrée${totalElements === 1 ? "" : "s"}`}
            </p>
          </div>
          <label className="flex items-center gap-2 text-sm text-muted-foreground">
            Lignes
            <span className="relative block">
              <select value={taille} onChange={(event) => { setTaille(Number(event.target.value)); setPage(0); }}
                className="h-9 appearance-none rounded-lg border border-brand-900/15 bg-white py-2 pl-3 pr-9 text-sm text-ink-900">
                {[10, 25, 50].map((valeur) => <option key={valeur} value={valeur}>{valeur}</option>)}
              </select>
              <ChevronDown className={CHEVRON} aria-hidden="true" />
            </span>
          </label>
        </div>

        {liste.isError && (
          <p className="p-6 text-sm text-destructive" role="alert">{messageErreurAudit(liste.error)}</p>
        )}
        {liste.isLoading && !liste.data && <p className="p-6 text-sm text-muted-foreground">Chargement du journal…</p>}
        {liste.data && liste.data.content.length === 0 && (
          <div className="px-6 py-14 text-center">
            <Activity className="mx-auto mb-3 size-8 text-muted-foreground/60" aria-hidden="true" />
            <p className="font-medium text-ink-900">Aucune entrée trouvée</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {filtresActifs ? "Essayez d'élargir ou d'effacer vos filtres." : "Les actions enregistrées apparaîtront ici."}
            </p>
          </div>
        )}
        {liste.data && liste.data.content.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] border-collapse text-left">
                <thead className="bg-surface-card/70">
                  <tr className="text-xs uppercase tracking-wide text-muted-foreground">
                    <th scope="col" className="px-5 py-3 font-semibold">Date et heure</th>
                    <th scope="col" className="px-5 py-3 font-semibold">Action</th>
                    <th scope="col" className="px-5 py-3 font-semibold">Objet</th>
                    <th scope="col" className="px-5 py-3 font-semibold">Acteur</th>
                    <th scope="col" className="px-5 py-3 font-semibold">Rôle</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-900/10">
                  {liste.data.content.map((entree) => (
                    <tr key={entree.code} className="transition-colors hover:bg-surface-card/40">
                      <td className="whitespace-nowrap px-5 py-4 text-sm text-muted-foreground">{formaterDate(entree.dateAction)}</td>
                      <td className="px-5 py-4">
                        <span className="inline-flex rounded-full bg-brand-900/8 px-2.5 py-1 text-xs font-semibold text-brand-900">
                          {entree.action?.replaceAll("_", " ") ?? "Action"}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-sm font-medium text-ink-900">{entree.objet ?? "—"}</td>
                      <td className="px-5 py-4 text-sm text-ink-900">{entree.acteurNom ?? "Acteur inconnu"}</td>
                      <td className="px-5 py-4 text-xs text-muted-foreground">{libelleRole(entree.acteurRole)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-900/10 px-5 py-4">
              <p className="text-sm text-muted-foreground">
                {totalElements > 0 ? `${page * taille + 1}–${Math.min((page + 1) * taille, totalElements)} sur ${totalElements}` : "0 entrée"}
              </p>
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">
                  Page {page + 1}{totalPages > 0 ? ` sur ${totalPages}` : ""}
                </span>
                <Button type="button" variant="secondary" size="sm" disabled={page === 0 || liste.isFetching}
                  onClick={() => setPage((courant) => Math.max(courant - 1, 0))}
                  icon={<ChevronLeft className="size-4" aria-hidden="true" />} iconPosition="left">
                  Précédent
                </Button>
                <Button type="button" variant="secondary" size="sm"
                  disabled={page + 1 >= totalPages || liste.isFetching}
                  onClick={() => setPage((courant) => courant + 1)}
                  icon={<ChevronRight className="size-4" aria-hidden="true" />} iconPosition="right">
                  Suivant
                </Button>
              </div>
            </div>
          </>
        )}
      </section>
    </Container>
  );
}

const CHAMP = "h-10 w-full rounded-xl border border-brand-900/15 bg-white px-3 text-sm text-ink-900 outline-none transition focus:border-brand-900 focus:ring-2 focus:ring-brand-900/10";
const CHEVRON = "pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground";

function formaterDate(valeur: string): string {
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return valeur;
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function libelleRole(role: string): string {
  return ROLES.find((option) => option.value === role)?.label ?? role;
}
