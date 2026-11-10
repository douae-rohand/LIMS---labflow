import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Receipt } from "lucide-react";
import { Badge, Button } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { listerFacturesCaisse, type FactureCaisse } from "@/api/accueil";
import {
  Actualisation,
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  Recherche,
} from "@/features/responsable/commun";
import { formaterMontant } from "./commun";
import { DialogueEncaissement } from "./DialogueEncaissement";

type Filtre = "a-encaisser" | "en-retard" | "payees" | "toutes";

const aEncaisser = (f: FactureCaisse) => f.reste > 0;
const aujourdhui = () => new Date().toDateString();

const FILTRES: { id: Filtre; libelle: string; garde: (f: FactureCaisse) => boolean }[] = [
  { id: "a-encaisser", libelle: "À encaisser", garde: aEncaisser },
  { id: "en-retard", libelle: "En retard", garde: (f) => f.enRetard },
  { id: "payees", libelle: "Réglées", garde: (f) => !aEncaisser(f) },
  { id: "toutes", libelle: "Toutes", garde: () => true },
];

export function PageCaisse() {
  const [filtre, setFiltre] = useState<Filtre>("a-encaisser");
  const [recherche, setRecherche] = useState("");
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
  const visibles = donnees.filter(garde).filter((f) => !recherche.trim()
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
          <Actualisation
            misAJour={factures.dataUpdatedAt}
            enCours={factures.isFetching}
            onActualiser={() => void factures.refetch()}
          />
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

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex w-full flex-wrap gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto" role="tablist" aria-label="Filtrer les factures">
          {FILTRES.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={filtre === item.id}
              onClick={() => setFiltre(item.id)}
              className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold sm:flex-none ${
                filtre === item.id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"
              }`}
            >
              {item.libelle}
              <span className={`rounded-full px-2 py-0.5 text-[11px] ${filtre === item.id ? "bg-white/15" : "bg-surface-alt"}`}>
                {compteurs[item.id] ?? 0}
              </span>
            </button>
          ))}
        </div>
        <Recherche valeur={recherche} onChange={setRecherche} placeholder="Rechercher une facture, un client, un ICE…" />
      </div>

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
                {f.reste === 0 ? (
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
            <Button size="sm" variant={f.reste > 0 ? "primary" : "secondary"} onClick={() => setChoisie(f.id)}>
              {f.reste > 0 ? "Encaisser" : "Voir les reçus"}
            </Button>
          </li>
        ))}
      </ul>

      <DialogueEncaissement facture={facture} onFermer={() => setChoisie(null)} />
    </Container>
  );
}
