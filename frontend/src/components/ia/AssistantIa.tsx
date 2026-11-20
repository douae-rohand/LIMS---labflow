import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from "react";
import { useRouter } from "@tanstack/react-router";
import { Bot, ChevronRight, Database, FileText, FlaskConical, Loader2, MessageCircle, Send, ShoppingCart, X } from "lucide-react";
import {
  messageErreurAssistant,
  poserQuestionAssistant,
  type MessageHistorique,
  type SourceAssistant,
} from "@/api/ia";
import type { RoleUtilisateur } from "@/api/session";

interface Message {
  id: number;
  role: "client" | "assistant";
  texte: string;
  sources?: SourceAssistant[];
  erreur?: boolean;
  /** Instant de lecture des données du laboratoire (réponse appuyée sur les outils de données). */
  donneesA?: string | null;
}

/** Qui utilise l'assistant : visiteur des pages publiques ou rôle de l'utilisateur connecté. */
export type ProfilAssistantUi = "PUBLIC" | RoleUtilisateur;

interface ConfigProfil {
  accueil: string;
  sousTitre: string;
  suggestions: string[];
}

const CONFIGS: Record<ProfilAssistantUi, ConfigProfil> = {
  PUBLIC: {
    accueil: "Bonjour ! Je réponds à vos questions sur LabFlow : laboratoires partenaires, analyses et tarifs, création de compte, déroulement d'une demande.",
    sousTitre: "Découvrir la plateforme et les analyses",
    suggestions: ["Comment créer un compte client ?", "Quelles analyses proposez-vous ?", "Comment intégrer mon laboratoire ?"],
  },
  CLIENT: {
    accueil: "Bonjour ! Je réponds à vos questions sur la plateforme et sur les analyses de vos laboratoires : demandes, échantillons, délais, paiement, rapports.",
    sousTitre: "Plateforme et catalogue de vos laboratoires",
    suggestions: ["Comment préparer un prélèvement de sang ?", "Puis-je payer en plusieurs fois ?", "Que signifie un rapport partiel ?"],
  },
  ACCUEIL: {
    accueil: "Bonjour ! Je vous aide à utiliser votre espace Accueil : patients, réception des échantillons, étiquettes, caisse.",
    sousTitre: "Aide pour l'espace Accueil",
    suggestions: ["Combien ai-je encaissé aujourd'hui ?", "Comment annuler un encaissement ?", "Comment imprimer les étiquettes ?"],
  },
  RESPONSABLE: {
    accueil: "Bonjour ! Je vous aide à utiliser votre espace Responsable : décisions, planification, validation des résultats, rapports.",
    sousTitre: "Aide pour l'espace Responsable",
    suggestions: ["Quelles analyses sont en retard ?", "Quels résultats sont en alerte d'anomalie ?", "Pourquoi je ne peux pas affecter une analyse ?"],
  },
  TECHNICIEN: {
    accueil: "Bonjour ! Je vous aide à utiliser votre espace Technicien : essais, saisie des résultats, planning, import CSV.",
    sousTitre: "Aide pour l'espace Technicien",
    suggestions: ["Quels sont mes essais en retard ?", "Ai-je des résultats retournés ?", "Comment déclarer un essai non réalisable ?"],
  },
  ADMINISTRATEUR: {
    accueil: "Bonjour ! Je vous aide à administrer votre laboratoire : employés, domaines des techniciens, catalogue, stock, facturation.",
    sousTitre: "Aide pour l'administration du laboratoire",
    suggestions: ["Quels produits sont sous le seuil de stock ?", "Combien de factures sont en retard ?", "Comment attribuer un domaine à un technicien ?"],
  },
  SUPER_ADMINISTRATEUR: {
    accueil: "Bonjour ! Je vous aide à gérer la plateforme : demandes d'intégration, laboratoires, administrateurs, double authentification, commission.",
    sousTitre: "Aide pour l'administration de la plateforme",
    suggestions: ["Combien de demandes d'intégration sont en attente ?", "Comment accepter une demande d'intégration ?", "Comment est calculée la commission ?"],
  },
};

/**
 * Assistant IA de LabFlow : bouton flottant en bas à droite, panneau de discussion.
 * Présent dans tous les espaces (selon le rôle) et sur les pages publiques (visiteur).
 * La conversation reste dans la page (aucun enregistrement en base).
 */
export function AssistantIa({ profil }: { profil: ProfilAssistantUi }) {
  const config = CONFIGS[profil] ?? CONFIGS.PUBLIC;
  const publique = profil === "PUBLIC";
  const [ouvert, setOuvert] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [saisie, setSaisie] = useState("");
  const [enCours, setEnCours] = useState(false);
  // Pseudonymes TECH_<id> → noms, cumulés sur la conversation : affichés ici, jamais renvoyés au serveur
  const [noms, setNoms] = useState<Record<string, string>>({});
  const compteur = useRef(0);
  const fil = useRef<HTMLDivElement>(null);
  const bouton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (ouvert) document.getElementById("assistant-ia-saisie")?.focus();
  }, [ouvert]);

  useEffect(() => {
    fil.current?.scrollTo({ top: fil.current.scrollHeight, behavior: "smooth" });
  }, [messages, enCours]);

  function fermer() {
    setOuvert(false);
    bouton.current?.focus();
  }

  async function envoyer(question: string) {
    const texte = question.trim();
    if (texte.length < 3 || enCours) return;
    const historique: MessageHistorique[] = messages
      .filter((m) => !m.erreur)
      .slice(-6)
      .map((m) => ({ role: m.role, texte: m.texte }));
    setMessages((liste) => [...liste, { id: ++compteur.current, role: "client", texte }]);
    setSaisie("");
    setEnCours(true);
    try {
      const reponse = await poserQuestionAssistant(texte, historique, publique);
      if (reponse.noms && Object.keys(reponse.noms).length > 0) {
        setNoms((actuels) => ({ ...actuels, ...reponse.noms }));
      }
      setMessages((liste) => [
        ...liste,
        {
          id: ++compteur.current,
          role: "assistant",
          texte: reponse.reponse,
          sources: reponse.sources,
          donneesA: reponse.donneesA ?? null,
        },
      ]);
    } catch (e) {
      setMessages((liste) => [
        ...liste,
        { id: ++compteur.current, role: "assistant", texte: messageErreurAssistant(e), erreur: true },
      ]);
    } finally {
      setEnCours(false);
      document.getElementById("assistant-ia-saisie")?.focus();
    }
  }

  function soumettre(event: FormEvent) {
    event.preventDefault();
    void envoyer(saisie);
  }

  function toucheSaisie(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void envoyer(saisie);
    }
  }

  return (
    <>
      {!ouvert && (
        <button
          ref={bouton}
          type="button"
          onClick={() => setOuvert(true)}
          aria-label="Ouvrir l'assistant IA"
          // À gauche des flèches de défilement (FlechesDefilement, en bas à droite)
          className="fixed bottom-4 right-20 z-40 inline-flex items-center gap-2 rounded-full bg-brand-900 px-4 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-2"
        >
          <MessageCircle className="size-5" aria-hidden="true" />
          <span className="hidden sm:inline">Une question ?</span>
        </button>
      )}

      {ouvert && (
        <section
          role="dialog"
          aria-label="Assistant IA"
          onKeyDown={(event) => {
            if (event.key === "Escape") fermer();
          }}
          className="fixed inset-x-3 bottom-3 z-50 flex max-h-[min(640px,calc(100dvh-1.5rem))] flex-col overflow-hidden rounded-2xl border border-black/10 bg-white shadow-2xl sm:inset-x-auto sm:right-5 sm:bottom-5 sm:w-[400px]"
        >
          <header className="flex items-center gap-3 bg-brand-900 px-4 py-3 text-white">
            <Bot className="size-5 shrink-0" aria-hidden="true" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold">Assistant LabFlow</p>
              <p className="text-xs text-white/75">{config.sousTitre}</p>
            </div>
            <button
              type="button"
              onClick={fermer}
              aria-label="Fermer l'assistant"
              className="rounded-lg p-1.5 hover:bg-white/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
          </header>

          <div ref={fil} className="flex-1 space-y-3 overflow-y-auto px-4 py-4" aria-live="polite">
            <Bulle role="assistant" texte={config.accueil} />
            {messages.length === 0 && (
              <div className="flex flex-col items-start gap-2 pt-1">
                {config.suggestions.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    onClick={() => void envoyer(suggestion)}
                    className="rounded-full border border-brand-900/25 px-3 py-1.5 text-left text-sm text-brand-900 hover:bg-mint-200/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            )}
            {messages.map((m) => (
              <Bulle
                key={m.id}
                role={m.role}
                texte={m.role === "assistant" ? remettreNoms(m.texte, noms) : m.texte}
                sources={m.sources}
                erreur={m.erreur}
                donneesA={m.donneesA}
              />
            ))}
            {enCours && (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" aria-hidden="true" /> L'assistant réfléchit…
              </p>
            )}
          </div>

          <form onSubmit={soumettre} className="border-t border-black/10 px-3 pt-3 pb-2">
            <div className="flex items-end gap-2">
              <label htmlFor="assistant-ia-saisie" className="sr-only">Votre question</label>
              <textarea
                id="assistant-ia-saisie"
                rows={1}
                maxLength={500}
                value={saisie}
                onChange={(event) => setSaisie(event.target.value)}
                onKeyDown={toucheSaisie}
                placeholder="Posez votre question…"
                className="max-h-28 min-h-[42px] flex-1 resize-none rounded-xl border border-black/15 px-3 py-2.5 text-sm focus:border-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-600/30"
              />
              <button
                type="submit"
                disabled={enCours || saisie.trim().length < 3}
                aria-label="Envoyer la question"
                className="inline-flex size-[42px] shrink-0 items-center justify-center rounded-xl bg-brand-900 text-white hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600"
              >
                <Send className="size-4" aria-hidden="true" />
              </button>
            </div>
            <p className="mt-2 text-[11px] leading-snug text-muted-foreground">
              Réponses automatiques, non contractuelles. Pour une question médicale, consultez votre médecin.
            </p>
          </form>
        </section>
      )}
    </>
  );
}

/**
 * Le modèle met parfois des mots en gras façon Markdown (**Réception**) : on les affiche en gras.
 * Rendu par éléments React (pas de HTML injecté), le reste du texte est affiché tel quel.
 */
function enrichir(texte: string): ReactNode[] {
  return texte.split(/\*\*(.+?)\*\*/g).map((morceau, i) => (i % 2 === 1 ? <strong key={i}>{morceau}</strong> : morceau));
}

/** Remet le nom des personnes à la place des pseudonymes TECH_<id> écrits par le modèle. */
function remettreNoms(texte: string, noms: Record<string, string>): string {
  return texte.replace(/\[?TECH_(\d+)\]?/g, (_, id: string) => noms[`TECH_${id}`] ?? "un membre de l'équipe");
}

function heureLecture(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

function Bulle({ role, texte, sources, erreur, donneesA }: {
  role: "client" | "assistant";
  texte: string;
  sources?: SourceAssistant[] | undefined;
  erreur?: boolean | undefined;
  donneesA?: string | null | undefined;
}) {
  if (role === "client") {
    return (
      <div className="flex justify-end">
        <p className="max-w-[85%] whitespace-pre-line rounded-2xl rounded-br-md bg-brand-900 px-3.5 py-2.5 text-sm text-white">
          {texte}
        </p>
      </div>
    );
  }
  return (
    <div className="flex max-w-[90%] flex-col gap-1.5">
      <p
        role={erreur ? "alert" : undefined}
        className={`whitespace-pre-line rounded-2xl rounded-bl-md px-3.5 py-2.5 text-sm ${
          erreur ? "bg-red-50 text-red-700" : "bg-surface-alt text-ink-900"
        }`}
      >
        {enrichir(texte)}
      </p>
      {donneesA && (
        <p className="flex items-center gap-1 px-1 text-[11px] text-muted-foreground">
          <Database className="size-3" aria-hidden="true" /> Données de votre laboratoire lues à {heureLecture(donneesA)}
        </p>
      )}
      {sources && sources.length > 0 && (
        <ul className="flex flex-col gap-1.5" aria-label="Sources et liens">
          {sources.map((s) => (s.prix ? <CarteAnalyse key={s.titre} source={s} /> : <PuceSource key={s.titre} source={s} />))}
        </ul>
      )}
    </div>
  );
}

/**
 * Lien vers une page de l'application, sans recharger la page (la conversation reste ouverte).
 * Les adresses viennent du serveur ; on n'accepte que des chemins internes.
 */
function LienInterne({ href, className, children }: { href: string; className: string; children: ReactNode }) {
  const router = useRouter();
  if (!href.startsWith("/") || href.startsWith("//")) return null;
  return (
    <a
      href={href}
      className={className}
      onClick={(e) => {
        if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
        e.preventDefault();
        router.history.push(href);
      }}
    >
      {children}
    </a>
  );
}

/** Page de documentation citée : cliquable quand elle correspond à une page de l'espace. */
function PuceSource({ source }: { source: SourceAssistant }) {
  const contenu = (
    <>
      <FileText className="size-3 shrink-0" aria-hidden="true" />
      <span className="truncate">{source.titre}</span>
      {source.lien && <ChevronRight className="size-3 shrink-0" aria-hidden="true" />}
    </>
  );
  const style = "inline-flex max-w-full items-center gap-1 rounded-full bg-mint-200/60 px-2 py-0.5 text-[11px] text-brand-900";
  return (
    <li className="flex">
      {source.lien ? (
        <LienInterne href={source.lien} className={`${style} underline-offset-2 hover:bg-mint-200 hover:underline`}>
          {contenu}
        </LienInterne>
      ) : (
        <span className={style}>{contenu}</span>
      )}
    </li>
  );
}

/** Analyse du catalogue : nom, laboratoire, prix, et liens vers la page du laboratoire et le panier. */
function CarteAnalyse({ source }: { source: SourceAssistant }) {
  const bouton = "inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-medium";
  return (
    <li className="flex flex-col gap-1.5 rounded-xl border border-border bg-background px-3 py-2">
      <div className="flex items-start gap-2">
        <FlaskConical className="mt-0.5 size-3.5 shrink-0 text-brand-900" aria-hidden="true" />
        <p className="min-w-0 flex-1 text-xs text-ink-900">{source.titre}</p>
        <span className="shrink-0 text-xs font-semibold text-brand-900">{source.prix}</span>
      </div>
      {(source.lien || source.lienAjout) && (
        <div className="flex flex-wrap gap-1.5">
          {source.lien && (
            <LienInterne href={source.lien} className={`${bouton} bg-mint-200/60 text-brand-900 hover:bg-mint-200`}>
              Voir la page <ChevronRight className="size-3" aria-hidden="true" />
            </LienInterne>
          )}
          {source.lienAjout && (
            <LienInterne href={source.lienAjout} className={`${bouton} bg-brand-900 text-white hover:bg-brand-900/90`}>
              <ShoppingCart className="size-3" aria-hidden="true" /> Ajouter au panier
            </LienInterne>
          )}
        </div>
      )}
    </li>
  );
}
