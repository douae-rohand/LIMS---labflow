/**
 * session.ts — État de session en mémoire.
 *
 * Règles de sécurité appliquées :
 *  - L'accessToken est conservé UNIQUEMENT en mémoire vive (variable de module).
 *  - Il n'est JAMAIS écrit dans localStorage, sessionStorage, ni aucun cookie.
 *  - Il est perdu à chaque rechargement de page ; c'est intentionnel :
 *    le refresh token (HttpOnly cookie) permet de le renouveler sans interaction.
 *  - Les listeners permettent aux gardes de routes / composants de réagir
 *    aux changements de session sans couplage circulaire.
 */

import { useEffect, useState } from 'react';

// ---------------------------------------------------------------------------
// Types publics
// ---------------------------------------------------------------------------

/**
 * Statut de la session.
 *  - `inconnu`  : état initial avant que restoreSession() ait terminé.
 *  - `connecté` : accessToken valide en mémoire.
 *  - `anonyme`  : pas de session (cookie absent, expiré ou révoqué).
 */
export type SessionStatus = 'inconnu' | 'connecté' | 'anonyme';

export type RoleUtilisateur =
  | 'SUPER_ADMINISTRATEUR'
  | 'ADMINISTRATEUR'
  | 'RESPONSABLE_LABO'
  | 'TECHNICIEN'
  | 'CLIENT';

/** Informations minimales conservées en session après authentification. */
export interface SessionUser {
  role: RoleUtilisateur;
  tenantId: string;
  mustChangePassword: boolean;
  email: string;
  nomComplet: string;
}

/** État complet de la session. `null` = non authentifié. */
export interface Session {
  accessToken: string;
  user: SessionUser;
}

type SessionListener = (session: Session | null) => void;

// ---------------------------------------------------------------------------
// État interne (module-level — perdu au rechargement, volontairement)
// ---------------------------------------------------------------------------

let _session: Session | null = null;
let _status: SessionStatus = 'inconnu';
const _listeners = new Set<SessionListener>();

// ---------------------------------------------------------------------------
// API publique
// ---------------------------------------------------------------------------

/** Retourne la session courante (null si non connecté). */
export function getSession(): Session | null {
  return _session;
}

/** Retourne l'accessToken courant (null si non connecté). */
export function getAccessToken(): string | null {
  return _session?.accessToken ?? null;
}

/** Retourne le statut de session courant. */
export function getStatus(): SessionStatus {
  return _status;
}

/** Ouvre une session après authentification réussie. Notifie les listeners. */
export function setSession(accessToken: string, user: SessionUser): void {
  _session = { accessToken, user };
  _status = 'connecté';
  _notify();
}

/** Met à jour uniquement l'accessToken (après un refresh réussi). */
export function updateAccessToken(accessToken: string): void {
  if (_session === null) return;
  _session = { ..._session, accessToken };
  // Pas de notification : les consommateurs n'ont pas besoin de re-rendre
  // pour un simple renouvellement silencieux du token.
}

/** Ferme la session (logout ou refresh échoué). Notifie les listeners. */
export function clearSession(): void {
  _session = null;
  _status = 'anonyme';
  _notify();
}

/** Retourne true si une session est active. */
export function isAuthenticated(): boolean {
  return _session !== null;
}

/**
 * Abonne un callback aux changements de session.
 * Retourne une fonction de désabonnement.
 *
 * @example
 *   const unsub = subscribeSession((s) => setUser(s?.user ?? null));
 *   // Au démontage :
 *   unsub();
 */
export function subscribeSession(listener: SessionListener): () => void {
  _listeners.add(listener);
  return () => {
    _listeners.delete(listener);
  };
}

/**
 * Hook React — retourne la session et le statut courants.
 * Se re-rend automatiquement à chaque changement de session.
 *
 * @example
 *   const { session, status } = useSession();
 *   if (status === 'inconnu') return <Spinner />;
 *   if (status === 'anonyme') return <Redirect to="/login" />;
 *   return <Dashboard user={session!.user} />;
 */
export function useSession(): { session: Session | null; status: SessionStatus } {
  const [state, setState] = useState<{ session: Session | null; status: SessionStatus }>(
    () => ({ session: _session, status: _status }),
  );

  useEffect(() => {
    // Synchroniser si l'état a changé entre le rendu initial et le montage
    setState({ session: _session, status: _status });
    const unsub = subscribeSession((s) => {
      setState({ session: s, status: _status });
    });
    return unsub;
  }, []);

  return state;
}

/**
 * Vérifie que la session est active et la retourne.
 * Lance une erreur si le statut est `anonyme` ou `inconnu`.
 * Utile dans les `beforeLoad` de routes protégées.
 *
 * @throws {Error} Si la session n'est pas établie.
 *
 * @example
 *   // Dans un beforeLoad TanStack Router :
 *   const session = ensureSession();
 */
export function ensureSession(): Session {
  if (_session === null) {
    throw new Error('Session requise : utilisateur non authentifié.');
  }
  return _session;
}

// ---------------------------------------------------------------------------
// Interne
// ---------------------------------------------------------------------------

function _notify(): void {
  _listeners.forEach((l) => l(_session));
}
