import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Download, Eye, FileSpreadsheet, FileText, X } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Select } from "@/components/lab";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { rechercherLaboratoires } from "@/api/gestionLaboratoires";
import {
  exporterJournalAudit,
  listerActionsAudit,
  listerJournalAudit,
  type EntreeAudit,
} from "@/api/journalAudit";
import { BarreListe } from "../BarreListe";
import { formaterDate, messageErreur, texte } from "../texte";

export function JournalAudit() {
  const [acteur, setActeur] = useState("");
  const [acteurDiffere, setActeurDiffere] = useState("");
  const [action, setAction] = useState("");
  const [laboratoireId, setLaboratoireId] = useState("");
  const [du, setDu] = useState("");
  const [au, setAu] = useState("");
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState<EntreeAudit | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setActeurDiffere(acteur.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [acteur]);

  useEffect(() => {
    setPage(0);
  }, [acteurDiffere, action, laboratoireId, du, au]);

  const filtre = {
    acteur: acteurDiffere || undefined,
    action: action || undefined,
    laboratoireId: laboratoireId ? Number(laboratoireId) : undefined,
    du: du || undefined,
    au: au || undefined,
    page,
    size: 10,
  };

  const journal = useQuery({
    queryKey: ["journal-audit", filtre],
    queryFn: () => listerJournalAudit(filtre),
  });
  const actions = useQuery({
    queryKey: ["journal-audit-actions"],
    queryFn: listerActionsAudit,
  });
  const laboratoires = useQuery({
    queryKey: ["laboratoires-filtre-audit"],
    queryFn: () => rechercherLaboratoires({ page: 0, size: 100, tri: "raisonSociale", ordre: "asc" }),
  });

  const total = journal.data?.totalElements ?? 0;
  const debut = total === 0 ? 0 : page * 10 + 1;
  const fin = Math.min(total, (page + 1) * 10);
  const filtresActifs = Boolean(acteurDiffere || action || laboratoireId || du || au);

  async function exporter(format: "csv" | "xlsx" | "pdf") {
    try {
      await exporterJournalAudit(filtre, format);
      toast.success("Le fichier a été téléchargé.");
    } catch (erreur) {
      toast.error(messageErreur(erreur, "L'export a échoué."));
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <BarreListe
        recherche={acteur}
        onRecherche={setActeur}
        placeholder="E-mail de l'acteur"
        libelleRecherche="Filtrer par acteur"
        filtresActifs={filtresActifs}
        onReinitialiser={() => { setActeur(""); setAction(""); setLaboratoireId(""); setDu(""); setAu(""); }}
        formatsExport={[
          { icone: FileSpreadsheet, libelle: "Exporter en Excel", onClick: () => void exporter("xlsx") },
          { icone: Download, libelle: "Exporter en CSV", onClick: () => void exporter("csv") },
          { icone: FileText, libelle: "Exporter en PDF", onClick: () => void exporter("pdf") },
        ]}
        puces={[
          ...(acteurDiffere ? [{ libelle: `Recherche : ${acteurDiffere}`, onRetirer: () => setActeur("") }] : []),
          ...(action ? [{ libelle: action, onRetirer: () => setAction("") }] : []),
          ...(laboratoireId ? [{
            libelle: laboratoires.data?.content.find((laboratoire) => String(laboratoire.id) === laboratoireId)?.nom ?? "Laboratoire",
            onRetirer: () => setLaboratoireId(""),
          }] : []),
          ...(du ? [{ libelle: `Du ${du}`, onRetirer: () => setDu("") }] : []),
          ...(au ? [{ libelle: `Au ${au}`, onRetirer: () => setAu("") }] : []),
        ]}
        filtres={
          <>
            <Select value={action} aria-label="Filtrer par action" className="xl:w-48" onChange={(event) => setAction(event.target.value)}>
              <option value="">Toutes les actions</option>
              {actions.data?.map((valeur) => (
                <option key={valeur} value={valeur}>{valeur}</option>
              ))}
            </Select>
            <Select value={laboratoireId} aria-label="Filtrer par laboratoire" className="xl:w-52" onChange={(event) => setLaboratoireId(event.target.value)}>
              <option value="">Tous les laboratoires</option>
              {laboratoires.data?.content.map((laboratoire) => (
                <option key={laboratoire.id} value={laboratoire.id}>{laboratoire.nom}</option>
              ))}
            </Select>
            <Input type="date" value={du} aria-label="Date de début" className="xl:w-40" onChange={(event) => setDu(event.target.value)} />
            <Input type="date" value={au} aria-label="Date de fin" className="xl:w-40" onChange={(event) => setAu(event.target.value)} />
          </>
        }
      />
      <p className="text-xs text-muted-foreground">
        Le filtre inclut les actions sur le laboratoire, et les actions nouvelles sur un utilisateur ou une demande de ce laboratoire. Les lignes antérieures ne sont rattachées que lorsqu'elles portent directement sur le laboratoire.
      </p>

      <div className="overflow-x-auto rounded-2xl border border-brand-900/10 bg-card">
        <table className="w-full min-w-[880px] border-separate border-spacing-0 text-left text-sm">
          <thead>
            <tr className="bg-surface-alt text-brand-900">
              {["Date", "Acteur", "Action", "Objet", "Motif", ""].map((colonne) => (
                <th key={colonne || "detail"} className="px-4 py-3 text-xs font-semibold tracking-wide uppercase">{colonne}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {journal.isLoading && (
              <tr><td colSpan={6} className="px-4 py-10 text-sm text-muted-foreground">Chargement du journal…</td></tr>
            )}
            {journal.isError && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">Le journal n'a pas pu être chargé.</p>
                  <Button className="mt-3" size="sm" variant="secondary" onClick={() => void journal.refetch()}>Réessayer</Button>
                </td>
              </tr>
            )}
            {!journal.isLoading && !journal.isError && total === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">
                    {filtresActifs ? "Aucune entrée ne correspond à ces critères." : "Le journal est vide."}
                  </p>
                </td>
              </tr>
            )}
            {journal.data?.content.map((entree) => (
              <tr key={entree.id} className="group">
                <td className="border-t border-brand-900/10 px-4 py-3 whitespace-nowrap text-ink-900">{formaterDate(entree.dateAction)}</td>
                <td className="border-t border-brand-900/10 px-4 py-3">
                  <p className="font-medium text-ink-900">{texte(entree.acteurNom)}</p>
                  <p className="text-xs text-muted-foreground">{texte(entree.acteurEmail)}</p>
                </td>
                <td className="border-t border-brand-900/10 px-4 py-3"><Badge variant="soft" size="sm">{entree.action}</Badge></td>
                <td className="border-t border-brand-900/10 px-4 py-3 whitespace-nowrap text-ink-900">{entree.objet}</td>
                <td className="max-w-[240px] truncate border-t border-brand-900/10 px-4 py-3 text-ink-900" title={entree.motif ?? ""}>{texte(entree.motif)}</td>
                <td className="border-t border-brand-900/10 px-4 py-3 text-right">
                  <Button variant="ghost" size="sm" aria-label={`Détail de ${entree.code}`} icon={<Eye className="size-4" aria-hidden="true" />} onClick={() => setDetail(entree)} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
        <p>{debut}–{fin} sur {total}</p>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((actuel) => Math.max(0, actuel - 1))}>Précédent</Button>
          <Button variant="secondary" size="sm" disabled={!journal.data || page + 1 >= journal.data.totalPages} onClick={() => setPage((actuel) => actuel + 1)}>Suivant</Button>
        </div>
      </div>

      <Dialog open={detail !== null} onOpenChange={(ouvert) => { if (!ouvert) setDetail(null); }}>
        <DialogContent className="glass-card glossy-top max-h-[85vh] max-w-2xl overflow-y-auto rounded-3xl border-0 bg-card p-6">
          <DialogTitle className="text-lg font-extrabold text-ink-900">{detail?.action}</DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {detail?.code} · {detail?.objet} · {texte(detail?.acteurEmail)}
          </DialogDescription>
          {detail?.motif && <p className="text-sm text-ink-900">Motif : {detail.motif}</p>}
          <div className="grid gap-3 md:grid-cols-2">
            <Valeur titre="Valeur avant" contenu={detail?.valeurAvant} />
            <Valeur titre="Valeur après" contenu={detail?.valeurApres} />
          </div>
          <div className="flex justify-end">
            <Button variant="ghost" onClick={() => setDetail(null)} icon={<X className="size-4" aria-hidden="true" />}>Fermer</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Valeur({ titre, contenu }: { titre: string; contenu?: string | null | undefined }) {
  return (
    <div>
      <p className="mb-1 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{titre}</p>
      <pre className="max-h-64 overflow-auto rounded-2xl bg-surface-alt p-3 text-xs whitespace-pre-wrap text-ink-900">
        {contenu && contenu.trim() ? contenu : "—"}
      </pre>
    </div>
  );
}
