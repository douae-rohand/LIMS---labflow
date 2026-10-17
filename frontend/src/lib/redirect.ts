/**
 * redirect.ts — Validation du paramètre de redirection post-authentification
 * et conversion des chemins publics vers l'espace client.
 *
 * Seuls les chemins internes absolus sont acceptés. Sont refusés :
 *  - les schémas d'URL (http:, https:, javascript:, data:…) ;
 *  - les URL protocol-relative (`//host`) ;
 *  - l'antislash `\` (séparateur interprété par certains navigateurs) ;
 *  - les caractères de contrôle (0x00–0x1F et 0x7F).
 *
 * Retourne `null` si la valeur est absente ou invalide : l'appelant applique
 * alors sa destination par défaut.
 */

import type { RoleUtilisateur } from "@/api/session";
import { getSession } from "@/api/session";

export function sanitizeRedirect(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const path = value.trim();
  if (path.length === 0) return null;
  // Schéma d'URL explicite ("http:", "javascript:", "data:…") : refusé.
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(path)) return null;
  // Chemin interne absolu obligatoire.
  if (!path.startsWith("/")) return null;
  // URL protocol-relative ("//host") : refusée.
  if (path.startsWith("//")) return null;
  // Antislash : refusé.
  if (path.includes("\\")) return null;
  // Caractères de contrôle : refusés.
  if (/[\u0000-\u001F\u007F]/.test(path)) return null;
  return path;
}

// ---------------------------------------------------------------------------
// Conversion public → espace client
// ---------------------------------------------------------------------------

/** Chemin de base du catalogue : espace public ou espace client. */
export type BaseCatalogue = "/laboratoires" | "/app/laboratoires";

const PREFIXE_PUBLIC_LABORATOIRES = /^\/laboratoires(?=$|\/|\?|#)/;
const PREFIXE_PUBLIC_CARTE = /^\/carte(?=$|\?|#)/;

/**
 * Convertit un chemin public en son équivalent dans l'espace client :
 *  - `/`                     → `/app`
 *  - `/laboratoires[?…|#…]`  → `/app/laboratoires[?…|#…]`
 *  - `/carte[?…|#…]`         → `/app/carte[?…|#…]`
 *
 * Toute autre valeur est renvoyée inchangée : la fonction est donc idempotente
 * sur les chemins `/app/...` (aucune boucle de redirection possible). Le
 * `search` et le `hash` sont préservés.
 */
export function versEspaceClient(href: string): string {
  if (href === "/") return "/app";
  if (PREFIXE_PUBLIC_LABORATOIRES.test(href)) return `/app${href}`;
  if (PREFIXE_PUBLIC_CARTE.test(href)) return `/app/carte${href.slice("/carte".length)}`;
  return href;
}

/**
 * Destination après une connexion réussie.
 * Le paramètre `redirect` est revalidé (même validation que `sanitizeRedirect`)
 * puis converti vers l'espace client uniquement pour le rôle CLIENT.
 */
export function cibleApresConnexion(
  role: RoleUtilisateur | undefined,
  redirect: unknown,
  defaut = "/app",
): string {
  const chemin = sanitizeRedirect(redirect) ?? defaut;
  return role === "CLIENT" ? versEspaceClient(chemin) : chemin;
}

/**
 * Garde des pages publiques : si un CLIENT est connecté, renvoie la destination
 * correspondante dans son espace (`/changer-mot-de-passe` restant prioritaire).
 * Renvoie `null` pour un visiteur ou un membre du personnel.
 */
export function redirectionClientConnecte(href: string): string | null {
  const session = getSession();
  if (session === null) return null;
  if (session.user.role !== "CLIENT") return null;
  if (session.user.mustChangePassword) return "/changer-mot-de-passe";
  return versEspaceClient(href);
}
