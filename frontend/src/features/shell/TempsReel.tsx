import { Client } from "@stomp/stompjs";
import { useQueryClient, type QueryClient } from "@tanstack/react-query";
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import apiClient from "@/api/axios";
import { getAccessToken, useSession } from "@/api/session";

/**
 * Actualisation en temps réel des écrans (remplace le polling).
 *
 * Une seule connexion WebSocket (STOMP sur /ws) par onglet, authentifiée par le JWT. Le serveur
 * envoie dans la file personnelle de l'utilisateur la liste des domaines modifiés (« ANALYSES »,
 * « FACTURES »…) ; on invalide alors uniquement les caches des écrans concernés, qui se rechargent
 * avec les droits habituels de l'utilisateur. Aucune page n'a besoin d'être modifiée.
 */

/** Préfixes des clés de cache rechargées pour chaque domaine modifié. */
const CACHES_PAR_DOMAINE: Record<string, string[]> = {
  DEMANDES: ["demandes-laboratoire", "demande-laboratoire", "demandes-en-cours", "demandes-indicateurs", "stats-demandes",
    "tableau-de-bord", "statistiques-plateforme", "responsable-", "accueil-", "technicien", "demande-client",
    "synthese-client", "paniers"],
  ANALYSES: ["technicien", "responsable-", "accueil-", "demande-laboratoire", "demandes-en-cours", "demande-client",
    "synthese-client", "tableau-de-bord", "stats-demandes"],
  ECHANTILLONS: ["accueil-", "technicien", "responsable-", "demande-laboratoire", "demande-client"],
  VALIDATIONS: ["responsable-", "technicien", "accueil-suivi", "demande-laboratoire", "demande-client"],
  RAPPORTS: ["responsable-", "rapports-client", "synthese-client", "demande-client", "accueil-suivi", "demande-laboratoire"],
  FACTURES: ["factures-admin", "facture-detail", "accueil-", "factures-client", "synthese-client", "tableau-de-bord"],
  STOCK: ["stock-admin", "tableau-de-bord"],
  CATALOGUE: ["catalogue-admin", "laboratoires", "paniers", "technicien"],
  SATISFACTION: ["satisfaction-admin", "enquetes-client", "synthese-client", "tableau-de-bord"],
  CLIENTS: ["accueil-clients", "accueil-patients", "demandes-laboratoire"],
  EMPLOYES: ["employes", "employe", "permissions-compte", "responsable-techniciens", "comptes-plateforme", "sessions-compte"],
  AUDIT: ["journal-audit", "audit-recent"],
  PLATEFORME: ["demandes-integration", "demande-integration", "laboratoires-", "statistiques-plateforme", "tableau-de-bord",
    "historique-laboratoire"],
};

function invalider(queryClient: QueryClient, prefixes: string[]) {
  void queryClient.invalidateQueries({
    predicate: (requete) => {
      const racine = requete.queryKey[0];
      return typeof racine === "string" && prefixes.some((p) => racine === p || racine.startsWith(p));
    },
  });
}

/** Jeton d'accès valide : s'il a expiré, une requête authentifiée déclenche le renouvellement habituel. */
async function jetonValide(): Promise<string | null> {
  const jeton = getAccessToken();
  if (jeton) {
    try {
      const charge = JSON.parse(atob(jeton.split(".")[1]?.replace(/-/g, "+").replace(/_/g, "/") ?? "")) as { exp?: number };
      if (!charge.exp || charge.exp * 1000 > Date.now() + 15_000) return jeton;
    } catch {
      return jeton;
    }
  }
  try {
    await apiClient.get("/notifications/non-lues/count");
  } catch {
    // Session expirée : la redirection vers /login est gérée par apiClient.
  }
  return getAccessToken();
}

const ContexteTempsReel = createContext<{ connecte: boolean }>({ connecte: false });

/** État de la connexion temps réel (« En direct » dans les en-têtes de page). */
export function useTempsReel() {
  return useContext(ContexteTempsReel);
}

export function TempsReelProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const { session } = useSession();
  const utilisateur = session?.user.userId ?? null;
  const [connecte, setConnecte] = useState(false);

  useEffect(() => {
    if (utilisateur == null) return undefined;
    let premiereConnexion = true;
    const protocole = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${protocole}://${window.location.host}/ws`,
      reconnectDelay: 3_000,
      heartbeatIncoming: 20_000,
      heartbeatOutgoing: 20_000,
      beforeConnect: async () => {
        const jeton = await jetonValide();
        client.connectHeaders = jeton ? { Authorization: `Bearer ${jeton}` } : {};
      },
      onConnect: () => {
        setConnecte(true);
        client.subscribe("/user/queue/evenements", (message) => {
          try {
            const { domaines } = JSON.parse(message.body) as { domaines?: string[] };
            const prefixes = new Set((domaines ?? []).flatMap((d) => CACHES_PAR_DOMAINE[d] ?? []));
            if (prefixes.size > 0) invalider(queryClient, [...prefixes]);
          } catch {
            // Message illisible : ignoré.
          }
        });
        client.subscribe("/user/queue/notifications", () => invalider(queryClient, ["notifications-"]));
        // Après une coupure, on recharge ce qui est affiché pour rattraper les changements manqués.
        if (!premiereConnexion) void queryClient.invalidateQueries({ type: "active" });
        premiereConnexion = false;
      },
      onWebSocketClose: () => setConnecte(false),
      onStompError: () => setConnecte(false),
    });
    client.activate();
    return () => {
      setConnecte(false);
      void client.deactivate();
    };
  }, [utilisateur, queryClient]);

  return <ContexteTempsReel.Provider value={{ connecte }}>{children}</ContexteTempsReel.Provider>;
}
