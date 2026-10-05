/**
 * restoreSession.ts — Restauration de session au rechargement de page.
 *
 * Pourquoi :
 *   L'accessToken est en mémoire vive et perdu à chaque rechargement.
 *   Le refresh token HttpOnly survit (il est dans un cookie).
 *   Cette fonction tente un refresh silencieux au démarrage :
 *     - Succès → la session est restaurée, l'utilisateur reste connecté.
 *     - Échec  → pas de session (cookie absent, expiré ou révoqué) : OK.
 *
 * Usage :
 *   Appeler `await restoreSession()` dans main.tsx avant le premier rendu,
 *   ou dans un `beforeLoad` de la route racine TanStack Router.
 *
 * @example
 *   // main.tsx
 *   await restoreSession();
 *   createRoot(rootEl).render(<StrictMode><RouterProvider router={router} /></StrictMode>);
 */

import apiClient from './axios';
import { setSession, clearSession } from './session';
import type { RoleUtilisateur } from './session';

// ---------------------------------------------------------------------------
// Types locaux (sous-ensemble de ApiResponse<RefreshResponse>)
// ---------------------------------------------------------------------------

interface RefreshApiResponse {
  success: boolean;
  data: {
    accessToken: string;
    role: RoleUtilisateur;
    tenantId: string;
    mustChangePassword: boolean;
  };
}

// ---------------------------------------------------------------------------
// Restauration
// ---------------------------------------------------------------------------

/**
 * Tente de restaurer la session via le cookie refresh_token HttpOnly.
 *
 * @returns `true` si la session a été restaurée, `false` sinon.
 */
/** Durée maximale (ms) accordée au refresh de démarrage avant abandon silencieux. */
const STARTUP_REFRESH_TIMEOUT_MS = 5_000;

export async function restoreSession(): Promise<boolean> {
  // Délai maximal de 5 s : si le backend ne répond pas, on passe directement
  // à l'état 'anonyme' sans bloquer l'affichage ni afficher d'erreur.
  const timeoutPromise = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error('startup-refresh-timeout')), STARTUP_REFRESH_TIMEOUT_MS),
  );

  try {
    // Race : refresh vs timeout
    const response = await Promise.race([
      apiClient.postForRefresh<RefreshApiResponse>('/auth/refresh'),
      timeoutPromise,
    ]);

    const { accessToken, role, tenantId, mustChangePassword } = response.data.data;

    setSession(accessToken, { role, tenantId, mustChangePassword });

    // Propager le tenant si l'en-tête X-Tenant-ID est requis
    apiClient.setTenant(tenantId);

    return true;
  } catch {
    // Pas de session valide (cookie absent, expiré, révoqué, ou timeout 5 s).
    // On appelle clearSession() pour passer le statut à 'anonyme'.
    // IMPORTANT : onAuthFailure N'EST PAS déclenché ici — ce n'est pas une déconnexion
    // forcée mais simplement l'absence de cookie (première visite ou session expirée).
    // L'intercepteur 401 dans ApiClient est le seul qui déclenche onAuthFailure.
    clearSession();
    return false;
  }
}

