/**
 * redirect.ts — Validation du paramètre de redirection post-authentification.
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
