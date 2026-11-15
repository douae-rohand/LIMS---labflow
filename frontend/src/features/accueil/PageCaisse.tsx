import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { BookOpenCheck, Receipt } from "lucide-react";
import { Badge, Button } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { listerFacturesCaisse, ouvrirFactureCaisse, type FactureCaisse } from "@/api/accueil";
import {
  Actualisation,
  contient,
  EnTeteResponsable,
  BarreFiltres,
  EtatChargement,
  FiltreSelect,
  formaterDate,
  Onglets,
  Recherche,
} from "@/features/responsable/commun";
import { formaterMontant } from "./commun";
import { DialogueEncaissement } from "./DialogueEncaissement";
import { DialogueJournalCaisse } from "./DialogueJournalCaisse";

type Filtre = "a-encaisser" | "en-retard" | "payees" | "toutes";

const aEncaisser = (f: FactureCaisse) => f.reste > 0;
const aujourdhui = () => new Date().toDateString();
/** Nombre de jours calendaires écoulés depuis une date AAAA-MM-JJ (ou ISO). */
const joursDepuis = (date: string) => {
  const debut = new Date(date.slice(0, 10) + "T00:00:00");
  const maintenant = new Date();
  maintenant.setHours(0, 0, 0, 0);
  return Math.round((maintenant.getTime() - debut.getTime()) / 86_400_000);
};

const FILTRES: { id: Filtre; libelle: string; garde: (f: FactureCaisse) => boolean }[] = [
  { id: "a-encaisser", libelle: "À encaisser", garde: aEncaisser },
  { id: "en-retard", libelle: "En retard", garde: (f) => f.enRetard },
  { id: "payees", libelle: "Réglées", garde: (f) => !aEncaisser(f) },
  { id: "toutes", libelle: "Toutes", garde: () => true },
];

export function PageCaisse() {
  const [filtre, setFiltre] = useState<Filtre>("a-encaisser");
  const [recherche, setRecherche] = useState("");
  const [periode, setPeriode] = useState("");
  const [journal, setJournal] = useState(false);
  const [choisie, setChoisie] = useState<number | null>(null);
  const factures = useQuery({
    queryKey: ["accueil-caisse"],
    queryFn: listerFacturesCaisse,
  });

  const donnees = factures.data ?? [];
  const compteurs = useMemo(() => Object.fromEntries(FILTRES.map((f) =>
    [f.id, donnees.filter(f.garde).length])) as Record<Filtre, number>, [donnees]);
  const totaux = useMemo(() => ({
    reste: donnees.filter(aEncaisser).reduce((s, f) => s + f.reste, 0),
    encaisseAujourdhui: donnees.flatMap((f) => f.paiements)
      .filter((p) => new Date(p.date).toDateString() === aujourdhui())
      .reduce((s, p) => s + p.montant, 0),
  }), [donnees]);

  const garde = FILTRES.find((f) => f.id === filtre)?.garde ?? (() => true);
  // Le serveur trie : la facture la plus récente en haut.
  const depuis = periode === "JOUR" ? 0 : periode === "SEMAINE" ? 6 : periode === "MOIS" ? 29 : null;
  const visibles = donnees.filter(garde)
    .filter((f) => depuis == null || (f.dateFacture != null && joursDepuis(f.dateFacture) <= depuis))
    .filter((f) => !recherche.trim()
    || contient(f.numero, recherche)
    || contient(f.client, recherche)
    || contient(f.demandeReference, recherche)
    || contient(f.clientIce, recherche));
  const facture = donnees.find((f) => f.id === choisie) ?? null;

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={Receipt}
        titre="Caisse"
        description="Encaissez les factures des clients, en une ou plusieurs fois, et remettez le reçu de paiement."
        action={
          <div className="flex flex-wrap items-center gap-2">
            <Button size="sm" variant="secondary" icon={<BookOpenCheck className="size-4" aria-hidden="true" />}
              iconPosition="left" onClick={() => setJournal(true)}>
              Journal de caisse
            </Button>
            <Actualisation
              misAJour={factures.dataUpdatedAt}
              enCours={factures.isFetching}
              onActualiser={() => void factures.refetch()}
            />
          </div>
        }
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
          <p className="text-sm text-muted-foreground">Reste à encaisser</p>
          <p className="text-xl font-extrabold text-ink-900">{formaterMontant(totaux.reste)}</p>
        </div>
        <div className="rounded-2xl border border-brand-900/10 bg-card px-4 py-3">
          <p className="text-sm text-muted-foreground">Encaissé aujourd'hui</p>
          <p className="text-xl font-extrabold text-ink-900">{formaterMontant(totaux.encaisseAujourdhui)}</p>
        </div>
      </div>

      <BarreFiltres>
        <Onglets libelle="Filtrer les factures" onglets={FILTRES} valeur={filtre} onChange={setFiltre} compteurs={compteurs} />
        <Recherche className="w-full lg:flex-1" valeur={recherche} onChange={setRecherche} placeholder="Rechercher une facture, un client, un ICE…" />
        <FiltreSelect className="w-full lg:w-44" libelle="Filtrer par période" tous="Toutes les dates" valeur={periode}
          onChange={setPeriode} options={[["JOUR", "Émises aujourd'hui"], ["SEMAINE", "7 derniers jours"], ["MOIS", "30 derniers jours"]]} />
      </BarreFiltres>

      <EtatChargement
        chargement={factures.isLoading}
        erreur={factures.isError}
        vide={!factures.isLoading && visibles.length === 0}
        messageVide={donnees.length === 0 ? "Aucune facture pour le moment." : "Aucune facture ne correspond."}
        onReessayer={() => void factures.refetch()}
      />

      <ul className="flex flex-col gap-3">
        {visibles.map((f) => (
          <li key={f.id} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-ink-900">{f.numero}</p>
                {f.statut === "ANNULEE" ? (
                  <Badge size="sm" variant="outline">Annulée (avoir)</Badge>
                ) : f.reste === 0 ? (
                  <Badge size="sm" variant="mint">Réglée</Badge>
                ) : f.paye > 0 ? (
                  <Badge size="sm" variant="soft">Réglée en partie</Badge>
                ) : (
                  <Badge size="sm" variant="lime">À encaisser</Badge>
                )}
                {f.enRetard && (
                  <span className="rounded-full bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700">En retard</span>
                )}
              </div>
              <p className="text-xs text-muted-foreground">
                {f.client ?? "Client"}{f.demandeReference ? ` · demande ${f.demandeReference}` : ""}
                {" "}· émise le {formaterDate(f.dateFacture)} · échéance {formaterDate(f.dateEcheance)}
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm font-bold text-ink-900">{formaterMontant(f.reste)} <span className="font-normal text-muted-foreground">à payer</span></p>
              <p className="text-xs text-muted-foreground">sur {formaterMontant(f.montant)}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button size="sm" variant="ghost" onClick={() => void ouvrirFactureCaisse(f.id, f.statut === "ANNULEE")}>
                {f.statut === "ANNULEE" ? "Avoir PDF" : "Facture PDF"}
              </Button>
              {f.statut !== "ANNULEE" && (
                <Button size="sm" variant={f.reste > 0 ? "primary" : "secondary"} onClick={() => setChoisie(f.id)}>
                  {f.reste > 0 ? "Encaisser" : "Voir les reçus"}
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>

      <DialogueEncaissement facture={facture} onFermer={() => setChoisie(null)} />
      <DialogueJournalCaisse ouvert={journal} onFermer={() => setJournal(false)} />
    </Container>
  );
}
