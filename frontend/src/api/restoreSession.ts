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
export async function restoreSession(): Promise<boolean> {
  try {
    // postForRefresh envoie un POST /auth/refresh sans corps.
    // Le cookie HttpOnly est transmis automatiquement (withCredentials: true).
    const response = await apiClient.postForRefresh<RefreshApiResponse>('/auth/refresh');

    const { accessToken, role, tenantId, mustChangePassword } = response.data.data;

    setSession(accessToken, { role, tenantId, mustChangePassword });

    // Propager le tenant si l'en-tête X-Tenant-ID est requis
    apiClient.setTenant(tenantId);

    return true;
  } catch {
    // Pas de session valide (cookie absent, expiré, révoqué) : état normal.
    clearSession();
    return false;
  }
}
