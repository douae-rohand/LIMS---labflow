import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Download } from "lucide-react";
import { toast } from "sonner";
import {
  exporterFinancesLaboratoire,
  lireFinancesLaboratoire,
  type FiltreFinances,
  type PeriodeFinance,
  type Regroupement,
  lireFactureCommission,
  ouvrirFactureCommission,
} from "@/api/financesLaboratoire";
import type { FormatExport } from "@/api/plateforme";
import { Button, Input, Label } from "@/components/lab";
import { messageErreur } from "../texte";

const MAD = new Intl.NumberFormat("fr-MA", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const COMPACT = new Intl.NumberFormat("fr-MA", { notation: "compact", maximumFractionDigits: 1 });
const COURT = new Intl.DateTimeFormat("fr-FR", { day: "2-digit", month: "2-digit" });
const MOIS = new Intl.DateTimeFormat("fr-FR", { month: "short", year: "2-digit" });

type Raccourci = "aujourdhui" | "7j" | "30j" | "mois" | "moisPrecedent" | "annee" | "personnalisee";

const RACCOURCIS: { id: Raccourci; libelle: string }[] = [
  { id: "aujourdhui", libelle: "Aujourd'hui" },
  { id: "7j", libelle: "7 jours" },
  { id: "30j", libelle: "30 jours" },
  { id: "mois", libelle: "Ce mois" },
  { id: "moisPrecedent", libelle: "Mois dernier" },
  { id: "annee", libelle: "Cette année" },
  { id: "personnalisee", libelle: "Période…" },
];

function iso(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const j = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${m}-${j}`;
}

function bornes(raccourci: Exclude<Raccourci, "personnalisee">): { du: string; au: string } {
  const auj = new Date();
  const decale = (jours: number) => new Date(auj.getFullYear(), auj.getMonth(), auj.getDate() - jours);
  switch (raccourci) {
    case "aujourdhui": return { du: iso(auj), au: iso(auj) };
    case "7j": return { du: iso(decale(6)), au: iso(auj) };
    case "30j": return { du: iso(decale(29)), au: iso(auj) };
    case "mois": return { du: iso(new Date(auj.getFullYear(), auj.getMonth(), 1)), au: iso(auj) };
    case "moisPrecedent": return {
      du: iso(new Date(auj.getFullYear(), auj.getMonth() - 1, 1)),
      au: iso(new Date(auj.getFullYear(), auj.getMonth(), 0)),
    };
    case "annee": return { du: iso(new Date(auj.getFullYear(), 0, 1)), au: iso(auj) };
  }
}

function montant(valeur: number): string {
  return `${MAD.format(valeur)} MAD`;
}

function dateFr(valeur: string): string {
  const [a, m, j] = valeur.split("-");
  return `${j}/${m}/${a}`;
}

/** Libellé court sous une barre du graphique. */
function etiquette(periode: PeriodeFinance, regroupement: Regroupement): string {
  const [a, m, j] = periode.debut.split("-").map(Number);
  const date = new Date(a ?? 2000, (m ?? 1) - 1, j ?? 1);
  return regroupement === "MOIS" ? MOIS.format(date) : COURT.format(date);
}

/** Commission de la plateforme sur les factures du laboratoire, sur une période, par jour, semaine ou mois. */
export function OngletFinances({ id, code }: { id: number; code: string }) {
  const [raccourci, setRaccourci] = useState<Raccourci>("mois");
  const [filtre, setFiltre] = useState<FiltreFinances>({ ...bornes("mois"), regroupement: "" });
  const [exportEnCours, setExportEnCours] = useState<FormatExport | null>(null);

  const finances = useQuery({
    queryKey: ["laboratoires-finances", id, filtre],
    queryFn: () => lireFinancesLaboratoire(id, filtre),
    placeholderData: keepPreviousData,
    retry: false,
  });

  function choisir(valeur: Raccourci) {
    setRaccourci(valeur);
    if (valeur !== "personnalisee") setFiltre((courant) => ({ ...courant, ...bornes(valeur) }));
  }

  async function exporter(format: FormatExport) {
    setExportEnCours(format);
    try {
      await exporterFinancesLaboratoire(id, code, filtre, format);
    } catch (erreur) {
      toast.error(messageErreur(erreur, "L'export a échoué."));
    } finally {
      setExportEnCours(null);
    }
  }

  const d = finances.data;

  return (
    <div className="flex flex-col gap-5">
      {/* Filtres */}
      <div className="flex flex-col gap-3 rounded-2xl border border-brand-900/10 bg-surface-alt p-3">
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Période">
          {RACCOURCIS.map((r) => (
            <button
              key={r.id}
              type="button"
              aria-pressed={raccourci === r.id}
              onClick={() => choisir(r.id)}
              className={`rounded-full px-3 py-1.5 text-xs font-semibold focus-visible:outline-2 focus-visible:outline-brand-900 ${
                raccourci === r.id ? "bg-brand-900 text-primary-foreground" : "bg-card text-brand-900 hover:bg-brand-900/10"
              }`}
            >
              {r.libelle}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-end gap-3">
          {raccourci === "personnalisee" && (
            <>
              <div className="flex flex-col gap-1">
                <Label htmlFor="fin-du">Du</Label>
                <Input id="fin-du" type="date" value={filtre.du} max={filtre.au}
                  onChange={(e) => e.target.value && setFiltre({ ...filtre, du: e.target.value })} />
              </div>
              <div className="flex flex-col gap-1">
                <Label htmlFor="fin-au">Au</Label>
                <Input id="fin-au" type="date" value={filtre.au} min={filtre.du}
                  onChange={(e) => e.target.value && setFiltre({ ...filtre, au: e.target.value })} />
              </div>
            </>
          )}
          <p className="pb-2 text-xs text-muted-foreground">Du {dateFr(filtre.du)} au {dateFr(filtre.au)}</p>
        </div>
      </div>

      {finances.isError && (
        <p className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800" role="alert">
          {messageErreur(finances.error, "Données financières indisponibles.")}
        </p>
      )}
      {!d && finances.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}

      {d && (
        <>
          {/* Sur la période */}
          <section aria-labelledby="fin-periode">
            <h3 id="fin-periode" className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Sur la période · commission actuelle : {d.tauxCommission} %
            </h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <Carte libelle="Commission plateforme" valeur={montant(d.commission)}
                detail={`sur ${d.demandesFacturees} facture(s) · ${MAD.format(d.facture)} MAD facturés`} couleur="bg-brand-900" mise />
              <Carte libelle="Commission encaissée" valeur={montant(d.commissionEncaissee)} detail="sur les paiements reçus" couleur="bg-emerald-500" />
              <Carte libelle="Moyenne par facture" valeur={d.demandesFacturees > 0 ? montant(d.commission / d.demandesFacturees) : "—"}
                detail="commission par demande facturée" />
            </div>
          </section>

          {/* À ce jour */}
          <section aria-labelledby="fin-jour">
            <h3 id="fin-jour" className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">À ce jour (toutes périodes)</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Carte libelle="Commission à percevoir" valeur={montant(d.commissionAPercevoir)} detail="sur les factures non encore payées" />
              <Carte libelle="dont en retard" valeur={montant(d.commissionEnRetard)} detail="échéance dépassée" alerte={d.commissionEnRetard > 0} />
            </div>
          </section>

          <Graphique details={d.details} regroupement={d.regroupement} />
          <Tableau details={d.details} total={{ demandes: d.demandesFacturees, facture: d.facture, commission: d.commission, commissionEncaissee: d.commissionEncaissee }} />

          <div className="flex flex-wrap items-center justify-end gap-2 border-t border-brand-900/10 pt-4">
            {(["pdf", "xlsx", "csv"] as const).map((format) => (
              <Button key={format} variant="secondary" size="sm" loading={exportEnCours === format}
                disabled={exportEnCours !== null} onClick={() => void exporter(format)}>
                <Download className="size-4" aria-hidden="true" /> {format === "xlsx" ? "Excel" : format.toUpperCase()}
              </Button>
            ))}
          </div>
        </>
      )}

      <FactureCommissionMensuelle id={id} />
    </div>
  );
}

/** Mois précédent au format AAAA-MM. */
function moisPrecedent(): string {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

/** Facture de commission du mois émise par la plateforme au laboratoire. */
function FactureCommissionMensuelle({ id }: { id: number }) {
  const [mois, setMois] = useState(moisPrecedent);
  const valide = /^\d{4}-\d{2}$/.test(mois);
  const requete = useQuery({
    queryKey: ["finances-laboratoire", id, "commission", mois],
    queryFn: () => lireFactureCommission(id, mois),
    enabled: valide,
  });
  const f = requete.data;
  const moisCourant = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  })();

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-brand-900/10 bg-card p-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h3 className="font-bold text-ink-900">Facture de commission mensuelle</h3>
          <p className="text-xs text-muted-foreground">
            Facture de la plateforme au laboratoire : commission sur ses factures du mois, annulées exclues.
          </p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={`mois-commission-${id}`}>Mois</Label>
            <Input id={`mois-commission-${id}`} type="month" value={mois} max={moisCourant}
              onChange={(event) => setMois(event.target.value)} />
          </div>
          <Button variant="secondary" size="sm" disabled={!f}
            onClick={() => void ouvrirFactureCommission(id, mois)
              .catch((erreur: unknown) => toast.error(messageErreur(erreur, "La facture n'a pas pu être générée.")))}>
            <Download className="size-4" aria-hidden="true" /> Facture PDF
          </Button>
        </div>
      </div>
      {requete.isError && <p className="text-sm text-rose-800">{messageErreur(requete.error, "Aperçu indisponible.")}</p>}
      {f && (
        <div className="grid gap-3 sm:grid-cols-3">
          <Carte libelle="Numéro" valeur={f.numero} detail={`Échéance ${dateFr(f.dateEcheance)}`} />
          <Carte libelle="Factures du laboratoire" valeur={montant(f.base)} detail={`${f.factures} facture(s)`} />
          <Carte libelle="Commission due" valeur={montant(f.commission)} mise />
        </div>
      )}
    </section>
  );
}

function Carte({ libelle, valeur, detail, alerte = false, couleur, mise = false }: {
  libelle: string;
  valeur: string;
  detail?: string;
  alerte?: boolean;
  couleur?: string;
  /** Indicateur principal, mis en avant. */
  mise?: boolean;
}) {
  return (
    <div className={`rounded-2xl border p-4 ${alerte ? "border-rose-200 bg-rose-50/60" : mise ? "border-brand-900/30 bg-brand-900/5" : "border-brand-900/10 bg-card"}`}>
      <p className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
        {couleur && <span className={`size-2.5 rounded-sm ${couleur}`} aria-hidden="true" />}
        {libelle}
      </p>
      <p className={`mt-1 text-lg font-extrabold ${alerte ? "text-rose-800" : "text-ink-900"}`}>{valeur}</p>
      {detail && <p className="mt-0.5 text-xs text-muted-foreground">{detail}</p>}
    </div>
  );
}

/** Histogramme vertical : une paire de barres (facturé, encaissé) par jour, semaine ou mois. */
function Graphique({ details, regroupement }: { details: PeriodeFinance[]; regroupement: Regroupement }) {
  const max = Math.max(0, ...details.map((p) => Math.max(p.commission, p.commissionEncaissee)));
  if (max === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-brand-900/15 p-6 text-center text-sm text-muted-foreground">
        Aucune commission sur cette période.
      </p>
    );
  }
  const graduations = [1, 0.75, 0.5, 0.25, 0];
  const large = details.length > 16;

  return (
    <section aria-label="Évolution sur la période" className="rounded-2xl border border-brand-900/10 bg-card p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-sm font-extrabold text-ink-900">
          Commission {regroupement === "JOUR" ? "par jour" : regroupement === "SEMAINE" ? "par semaine" : "par mois"}
        </h3>
        <div className="flex gap-3 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1.5"><span className="size-2.5 rounded-sm bg-brand-900" aria-hidden="true" />Commission</span>
          <span className="inline-flex items-center gap-1.5"><span className="size-2.5 rounded-sm bg-emerald-500" aria-hidden="true" />Commission encaissée</span>
        </div>
      </div>
      <div className="flex gap-2">
        <div className="flex h-44 flex-col justify-between pb-5 text-right text-[10px] text-muted-foreground" aria-hidden="true">
          {graduations.map((g) => <span key={g}>{COMPACT.format(max * g)}</span>)}
        </div>
        <div className="min-w-0 flex-1 overflow-x-auto">
          <div className="relative flex h-44 items-end gap-1 border-b border-l border-brand-900/15 pb-0"
            style={{ minWidth: large ? `${details.length * 28}px` : undefined }}>
            {graduations.slice(0, -1).map((g) => (
              <span key={g} className="pointer-events-none absolute left-0 right-0 border-t border-dashed border-brand-900/10"
                style={{ bottom: `${g * 100}%` }} aria-hidden="true" />
            ))}
            {details.map((p) => (
              <div key={p.debut} className="group relative flex h-full flex-1 flex-col justify-end"
                title={`${p.libelle}\nCommission : ${montant(p.commission)}\nCommission encaissée : ${montant(p.commissionEncaissee)}\nFacturé : ${montant(p.facture)} (${p.demandesFacturees} facture(s))`}>
                <div className="flex h-full items-end justify-center gap-0.5">
                  <span className="w-1/2 max-w-4 rounded-t bg-brand-900 group-hover:opacity-80" style={{ height: `${(p.commission / max) * 100}%` }} />
                  <span className="w-1/2 max-w-4 rounded-t bg-emerald-500 group-hover:opacity-80" style={{ height: `${(p.commissionEncaissee / max) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
          <div className="flex gap-1 pt-1 text-[10px] text-muted-foreground" style={{ minWidth: large ? `${details.length * 28}px` : undefined }}>
            {details.map((p, i) => (
              <span key={p.debut} className="flex-1 truncate text-center">
                {!large || i % Math.ceil(details.length / 12) === 0 ? etiquette(p, regroupement) : ""}
              </span>
            ))}
          </div>
        </div>
      </div>
      <p className="mt-2 text-xs text-muted-foreground">Survolez une barre pour voir le détail.</p>
    </section>
  );
}

/** Détail chiffré, ligne par ligne, avec le total de la période. */
function Tableau({ details, total }: {
  details: PeriodeFinance[];
  total: { demandes: number; facture: number; commission: number; commissionEncaissee: number };
}) {
  return (
    <div className="max-h-80 overflow-auto rounded-2xl border border-brand-900/10">
      <table className="w-full border-collapse text-left text-sm">
        <thead className="sticky top-0 bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-3 py-2 font-semibold">Période</th>
            <th scope="col" className="px-3 py-2 text-right font-semibold">Demandes</th>
            <th scope="col" className="px-3 py-2 text-right font-semibold">Facturé</th>
            <th scope="col" className="px-3 py-2 text-right font-semibold">Commission</th>
            <th scope="col" className="px-3 py-2 text-right font-semibold">Commission encaissée</th>
          </tr>
        </thead>
        <tbody>
          {details.map((p) => {
            const vide = p.commission === 0 && p.commissionEncaissee === 0;
            return (
              <tr key={p.debut} className={`border-t border-brand-900/10 ${vide ? "text-muted-foreground" : "text-ink-900"}`}>
                <td className="px-3 py-2">{p.libelle}</td>
                <td className="px-3 py-2 text-right">{p.demandesFacturees || "—"}</td>
                <td className="px-3 py-2 text-right">{p.facture ? MAD.format(p.facture) : "—"}</td>
                <td className="px-3 py-2 text-right font-semibold">{p.commission ? MAD.format(p.commission) : "—"}</td>
                <td className="px-3 py-2 text-right">{p.commissionEncaissee ? MAD.format(p.commissionEncaissee) : "—"}</td>
              </tr>
            );
          })}
        </tbody>
        <tfoot className="sticky bottom-0 bg-surface-card font-bold text-ink-900">
          <tr className="border-t border-brand-900/20">
            <td className="px-3 py-2">Total</td>
            <td className="px-3 py-2 text-right">{total.demandes}</td>
            <td className="px-3 py-2 text-right">{MAD.format(total.facture)}</td>
            <td className="px-3 py-2 text-right">{MAD.format(total.commission)}</td>
            <td className="px-3 py-2 text-right">{MAD.format(total.commissionEncaissee)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
