/**
 * axios.ts — Client HTTP centralisé (ApiClient).
 *
 * Contrat de sécurité :
 *  - withCredentials: true sur TOUTES les requêtes → le cookie HttpOnly
 *    refresh_token est envoyé automatiquement par le navigateur sur /api/auth/*.
 *  - L'accessToken est stocké dans session.ts (mémoire vive), jamais dans
 *    localStorage, sessionStorage ou un cookie accessible en JS.
 *  - Le refreshToken n'existe PAS en mémoire JS : le backend le lit depuis le cookie.
 *  - Un jeton n'est JAMAIS loggué (pas de console.log/error/warn sur un token).
 *
 * Notes backend intégrées :
 *  1. TENANT : tenantId est porté dans le JWT (claim "tenantId").
 *     setTenant() reste disponible pour l'en-tête X-Tenant-ID si le backend
 *     l'exige encore via TenantInterceptor.java.
 *  2. FORMAT DES RÉPONSES : le backend enveloppe dans ApiResponse<T>
 *     { success, message, data, timestamp }. Le déballage data est fait dans
 *     auth.ts (et les futurs modules métier), pas ici.
 *  3. FLUX AUTH : voir auth.ts pour le détail des endpoints.
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { getAccessToken, updateAccessToken, clearSession } from './session';

// ---------------------------------------------------------------------------
// Types internes
// ---------------------------------------------------------------------------

/** Extension de la config de requête pour marquer les relances post-401. */
interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/** Forme minimale attendue d'une réponse d'erreur du backend (ApiResponse). */
interface ErrorResponseBody {
  message?: unknown;
  code?: unknown;
  data?: unknown;
}

// ---------------------------------------------------------------------------
// Classe d'erreur publique
// ---------------------------------------------------------------------------

/**
 * Erreur normalisée émise par ApiClient.
 * Toutes les rejections passent par cette classe — jamais d'AxiosError brut côté consommateur.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string | undefined;
  public readonly data: unknown;

  constructor(status: number, message: string, code?: string, data?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

// ---------------------------------------------------------------------------
// Gardes de type
// ---------------------------------------------------------------------------

function isErrorResponseBody(value: unknown): value is ErrorResponseBody {
  return value !== null && typeof value === 'object';
}

function extractErrorMessage(body: ErrorResponseBody, fallback: string): string {
  return typeof body.message === 'string' ? body.message : fallback;
}

function extractErrorCode(body: ErrorResponseBody): string | undefined {
  return typeof body.code === 'string' ? body.code : undefined;
}

// ---------------------------------------------------------------------------
// Classe principale
// ---------------------------------------------------------------------------

export class ApiClient {
  private readonly http: AxiosInstance;

  /**
   * Tenant courant ajouté à chaque requête via X-Tenant-ID.
   * TODO(backend) : si le tenantId est porté dans le JWT, cet en-tête devient
   * redondant. Vérifier TenantInterceptor.java.
   */
  private currentTenant: string | null = null;

  /**
   * Promesse de refresh en cours. Partagée entre les requêtes concurrentes
   * pour éviter plusieurs appels /auth/refresh simultanés.
   */
  private refreshPromise: Promise<string> | null = null;

  /** Callback invoqué quand le refresh échoue (ex. rediriger vers /login). */
  private onAuthFailureCallback: (() => void) | null = null;

  /** Callback invoqué sur 403 PASSWORD_CHANGE_REQUIRED (rediriger vers /changer-mot-de-passe). */
  private onPasswordChangeRequiredCallback: (() => void) | null = null;

  // -------------------------------------------------------------------------
  // Construction
  // -------------------------------------------------------------------------

  constructor() {
    this.http = axios.create({
      baseURL: '/api',
      timeout: 10_000,
      // withCredentials: true est indispensable pour que le navigateur envoie
      // automatiquement le cookie HttpOnly refresh_token sur /api/auth/*.
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    });

    this.registerRequestInterceptor();
    this.registerResponseInterceptor();
  }

  // -------------------------------------------------------------------------
  // Gestion de session (accessToken uniquement — refresh via cookie)
  // -------------------------------------------------------------------------

  /** Indique si un accessToken est disponible en mémoire. */
  hasToken(): boolean {
    return getAccessToken() !== null;
  }

  // -------------------------------------------------------------------------
  // Gestion du tenant
  // -------------------------------------------------------------------------

  /**
   * Définit le tenant courant ajouté à chaque requête via X-Tenant-ID.
   * Passer `null` pour supprimer l'en-tête.
   */
  setTenant(code: string | null): void {
    this.currentTenant = code;
  }

  // -------------------------------------------------------------------------
  // Callback d'échec d'authentification
  // -------------------------------------------------------------------------

  /**
   * Enregistre un callback appelé quand le refresh token est rejeté.
   * Typiquement : redirection vers la page de connexion.
   *
   * @example
   *   apiClient.setOnAuthFailure(() => navigate({ to: '/login' }));
   */
  setOnAuthFailure(callback: () => void): void {
    this.onAuthFailureCallback = callback;
  }

  /**
   * Enregistre un callback appelé sur 403 PASSWORD_CHANGE_REQUIRED.
   * Typiquement : redirection vers /changer-mot-de-passe.
   * La redirection est déclenchée une seule fois par réponse 403 (pas de boucle).
   */
  setOnPasswordChangeRequired(callback: () => void): void {
    this.onPasswordChangeRequiredCallback = callback;
  }

  // -------------------------------------------------------------------------
  // Méthodes HTTP génériques
  // -------------------------------------------------------------------------

  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.http.get<T>(url, config);
    return response.data;
  }

  async post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.http.post<T>(url, body, config);
    return response.data;
  }

  async put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.http.put<T>(url, body, config);
    return response.data;
  }

  async patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.http.patch<T>(url, body, config);
    return response.data;
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.http.delete<T>(url, config);
    return response.data;
  }

  /**
   * POST utilisé UNIQUEMENT pour le refresh.
   * Retourne la réponse Axios brute (pas de déballage) pour que doRefresh()
   * puisse accéder à response.data.data.accessToken selon l'enveloppe ApiResponse.
   * Le cookie HttpOnly est envoyé automatiquement grâce à withCredentials.
   */
  async postForRefresh<T>(url: string): Promise<AxiosResponse<T>> {
    return this.http.post<T>(url);
  }

  // -------------------------------------------------------------------------
  // Upload multipart
  // -------------------------------------------------------------------------

  /**
   * Envoie un FormData en multipart/form-data et retourne la réponse typée T.
   *
   * @param url        Endpoint relatif (ex. "/demandes/42/pieces-jointes")
   * @param formData   Données du formulaire incluant le(s) fichier(s)
   * @param onProgress Callback optionnel avec le pourcentage d'avancement (0–100)
   * @param timeout    Timeout en ms (0 = sans limite, recommandé pour les gros fichiers)
   *
   * TODO(backend) : endpoint à créer — POST /demandes/{id}/pieces-jointes
   */
  async upload<T>(
    url: string,
    formData: FormData,
    onProgress?: (percentage: number) => void,
    timeout = 0,
  ): Promise<T> {
    const progressConfig = onProgress
      ? {
          onUploadProgress: (event: import('axios').AxiosProgressEvent) => {
            if (event.total !== undefined && event.total > 0) {
              onProgress(Math.round((event.loaded * 100) / event.total));
            }
          },
        }
      : {};

    const response = await this.http.post<T>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout,
      ...progressConfig,
    });
    return response.data;
  }

  // -------------------------------------------------------------------------
  // Téléchargement de fichier
  // -------------------------------------------------------------------------

  /**
   * Télécharge un fichier depuis `url` et déclenche la sauvegarde dans le navigateur.
   * Le nom du fichier est extrait de l'en-tête Content-Disposition si présent.
   *
   * TODO(backend) : endpoint à créer — GET /rapports/{id}/pdf
   */
  async download(url: string, nomFichierParDefaut = 'fichier', timeout = 0): Promise<void> {
    const response = await this.http.get<Blob>(url, {
      responseType: 'blob',
      timeout,
    });

    const disposition: string =
      (response.headers as Record<string, string | undefined>)['content-disposition'] ?? '';

    const filenameMatch = /filename\*?=(?:UTF-8''|")?([^";]+)/i.exec(disposition);
    const filename =
      filenameMatch?.[1] !== undefined
        ? decodeURIComponent(filenameMatch[1].trim().replace(/^"|"$/g, ''))
        : nomFichierParDefaut;

    const objectUrl = URL.createObjectURL(response.data);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(objectUrl);
  }

  // -------------------------------------------------------------------------
  // Intercepteur de requête : Authorization header
  // -------------------------------------------------------------------------

  private registerRequestInterceptor(): void {
    this.http.interceptors.request.use(
      (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
        // Authorization ne porte QUE l'accessToken (jamais un twoFactorToken).
        // Les twoFactorToken sont transmis dans le corps JSON des requêtes 2FA.
        const token = getAccessToken();
        if (token !== null) {
          config.headers.set('Authorization', `Bearer ${token}`);
        }
        if (this.currentTenant !== null) {
          config.headers.set('X-Tenant-ID', this.currentTenant);
        }
        return config;
      },
    );
  }

  // -------------------------------------------------------------------------
  // Intercepteur de réponse : normalisation erreur + refresh 401
  // -------------------------------------------------------------------------

  private registerResponseInterceptor(): void {
    this.http.interceptors.response.use(
      // Succès : passe-plat
      (response) => response,

      // Erreur
      async (error: AxiosError): Promise<never> => {
        const originalConfig = error.config as RetryableRequestConfig | undefined;
        const status = error.response?.status;

        // Endpoints d'auth exclus du refresh automatique
        const isAuthEndpoint =
          originalConfig?.url?.includes('/auth/login') === true ||
          originalConfig?.url?.includes('/auth/refresh') === true ||
          originalConfig?.url?.includes('/auth/logout') === true ||
          originalConfig?.url?.includes('/2fa/valider') === true;

        // 403 PASSWORD_CHANGE_REQUIRED → rediriger vers /changer-mot-de-passe (une seule fois)
        if (status === 403) {
          const rawData = error.response?.data;
          if (
            isErrorResponseBody(rawData) &&
            extractErrorCode(rawData) === 'PASSWORD_CHANGE_REQUIRED' &&
            originalConfig?.url?.includes('/auth/mot-de-passe/changer') !== true
          ) {
            this.onPasswordChangeRequiredCallback?.();
          }
          return Promise.reject(this.toApiError(error));
        }

        // Tentative de refresh sur 401 (une seule fois par requête)
        if (
          status === 401 &&
          !isAuthEndpoint &&
          originalConfig !== undefined &&
          originalConfig._retry !== true
        ) {
          originalConfig._retry = true;

          try {
            const newAccessToken = await this.doRefresh();
            originalConfig.headers.set('Authorization', `Bearer ${newAccessToken}`);
            return this.http.request(originalConfig) as Promise<never>;
          } catch {
            // Refresh échoué : effacer la session et notifier l'application
            clearSession();
            this.onAuthFailureCallback?.();
            return Promise.reject(this.toApiError(error));
          }
        }

        return Promise.reject(this.toApiError(error));
      },
    );
  }

  // -------------------------------------------------------------------------
  // Refresh partagé (une seule promesse pour les requêtes concurrentes)
  // -------------------------------------------------------------------------

  /**
   * Déclenche un POST /auth/refresh.
   * Le refresh token voyage exclusivement dans le cookie HttpOnly :
   * aucun corps n'est envoyé, aucun refreshToken n'est lu en JS.
   *
   * @returns Le nouvel accessToken (string).
   */
  private doRefresh(): Promise<string> {
    if (this.refreshPromise !== null) {
      return this.refreshPromise;
    }

    this.refreshPromise = this.http
      .post<{ success: boolean; data: { accessToken: string } }>('/auth/refresh')
      .then((response) => {
        const newAccessToken = response.data.data.accessToken;
        updateAccessToken(newAccessToken);
        return newAccessToken;
      })
      .finally(() => {
        this.refreshPromise = null;
      });

    return this.refreshPromise;
  }

  // -------------------------------------------------------------------------
  // Normalisation d'une AxiosError en ApiError
  // -------------------------------------------------------------------------

  private toApiError(error: AxiosError): ApiError {
    const status = error.response?.status ?? 0;
    const rawData = error.response?.data;

    if (isErrorResponseBody(rawData)) {
      return new ApiError(
        status,
        extractErrorMessage(rawData, error.message),
        extractErrorCode(rawData),
        rawData.data,
      );
    }

    return new ApiError(status, error.message);
  }
}

// ---------------------------------------------------------------------------
// Instance singleton exportée
// ---------------------------------------------------------------------------

/**
 * Instance unique de ApiClient utilisée dans toute l'application.
 *
 * Initialisation typique au démarrage (dans le composant racine ou après login) :
 *
 *   // Après login réussi (cas sans 2FA) :
 *   setSession(response.accessToken, toSessionUser(response));
 *
 *   // Callback de déconnexion forcée (refresh token expiré) :
 *   apiClient.setOnAuthFailure(() => navigate({ to: '/login' }));
 *
 *   // Tenant si nécessaire :
 *   apiClient.setTenant(user.tenantId);
 *
 * Restauration de session au rechargement :
 *   Appeler POST /api/auth/refresh au démarrage de l'application.
 *   Si réussi → setSession(newAccessToken, userFromJwt).
 *   Si échoué → l'utilisateur doit se reconnecter (cookie expiré / révoqué).
 */
export const apiClient = new ApiClient();
export default apiClient;
