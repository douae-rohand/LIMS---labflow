/**
 * Squelette d'initialisation, à aligner sur le backend.
 *
 * TODO(backend) — points à vérifier / finaliser une fois le backend stabilisé :
 *
 *  1. TENANT : si le tenantId est porté dans le JWT (claim "tenantId"), l'en-tête
 *     X-Tenant-ID devient redondant et la méthode setTenant() pourra être supprimée.
 *     Vérifier dans JwtTokenProvider.java (claim "tenantId") et TenantInterceptor.java.
 *
 *  2. FORMAT DES RÉPONSES : le backend enveloppe les données dans ApiResponse<T>
 *     { success, message, data, timestamp }. Les méthodes get/post/put/patch/delete
 *     retournent aujourd'hui T = corps brut. Si le déballage ApiResponse<T> → T est
 *     souhaité ici (plutôt que dans chaque hook), remplacer dans les méthodes :
 *       return response.data;
 *     par :
 *       return (response.data as ApiResponse<T>).data;
 *     et définir l'interface ApiResponse<T> dans ce fichier.
 *
 *  3. TOKENS & FLUX AUTH : vérifier dans AuthController.java / LoginResponse.java :
 *     - Nom exact des champs (accessToken, refreshToken, tokenType, expiresIn…).
 *     - Endpoint de refresh : POST /auth/refresh avec corps { refreshToken: string }.
 *     - Endpoint de logout  : POST /auth/logout  avec corps { refreshToken: string }.
 *       → à créer côté backend (AuthController).
 *     - Flux 2FA : POST /auth/2fa/valider.
 *       TODO(backend) : la forme exacte de LoginResponse et le déroulé complet du
 *       flux 2FA (champs de la réponse, code HTTP intermédiaire, corps de la
 *       requête de validation) sont à vérifier dans le backend avant d'implémenter
 *       le consommateur côté frontend.
 *
 *  4. ENDPOINTS MANQUANTS CÔTÉ BACKEND (à créer dans les controllers) :
 *     - POST /auth/logout
 *     - POST /demandes/{id}/pieces-jointes  (upload de pièces jointes)
 *     - GET  /rapports/{id}/pdf             (téléchargement du rapport PDF)
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios';

// ---------------------------------------------------------------------------
// Types internes
// ---------------------------------------------------------------------------

/** Extension de la config de requête Axios pour marquer les relances post-401. */
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

  constructor(
    status: number,
    message: string,
    code?: string,
    data?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

// ---------------------------------------------------------------------------
// Garde de type : vérifie qu'un corps de réponse inconnu a la forme ErrorResponseBody
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

  // TODO(sécurité) : ces tokens sont en mémoire vive (perdus au rechargement de page).
  // Options pour la persistance : sessionStorage (XSS-sensible), cookie httpOnly (CSRF-sensible
  // mais atténuable avec SameSite=Strict), ou refresh via un endpoint dédié sans stocker
  // l'accessToken du tout. À décider selon la politique de sécurité du projet.
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  /** Tenant courant ajouté à chaque requête via X-Tenant-ID. */
  private currentTenant: string | null = null;

  /**
   * Promesse de refresh en cours. Partagée entre les requêtes concurrentes
   * pour éviter de déclencher plusieurs appels /auth/refresh simultanément.
   */
  private refreshPromise: Promise<string> | null = null;

  /** Callback invoqué quand le refresh échoue (ex. rediriger vers /login). */
  private onAuthFailureCallback: (() => void) | null = null;

  // -------------------------------------------------------------------------
  // Construction
  // -------------------------------------------------------------------------

  constructor() {
    this.http = axios.create({
      baseURL: '/api',
      timeout: 10_000,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    });

    this.registerRequestInterceptor();
    this.registerResponseInterceptor();
  }

  // -------------------------------------------------------------------------
  // Gestion des tokens
  // -------------------------------------------------------------------------

  setTokens(access: string, refresh: string): void {
    this.accessToken = access;
    this.refreshToken = refresh;
  }

  clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
  }

  hasToken(): boolean {
    return this.accessToken !== null;
  }

  // -------------------------------------------------------------------------
  // Gestion du tenant
  // -------------------------------------------------------------------------

  /**
   * Définit le tenant courant.
   * Passer `null` pour supprimer l'en-tête X-Tenant-ID.
   *
   * TODO(backend) : voir note 1 en tête de fichier (tenant dans le JWT).
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

  // -------------------------------------------------------------------------
  // Upload multipart
  // -------------------------------------------------------------------------

  /**
   * Envoie un FormData en multipart/form-data et retourne la réponse typée T.
   *
   * @param url        Endpoint relatif (ex. "/demandes/42/pieces-jointes")
   * @param formData   Données du formulaire incluant le(s) fichier(s)
   * @param onProgress Callback optionnel avec le pourcentage d'avancement (0–100)
   *
   * TODO(backend) : endpoint à créer — POST /demandes/{id}/pieces-jointes
   */
  async upload<T>(
    url: string,
    formData: FormData,
    onProgress?: (percentage: number) => void,
    /**
     * Timeout en ms pour cet envoi. 0 = sans limite (recommandé pour les
     * pièces jointes volumineuses, ex. 50 Mo). Si omis, aucune limite n'est
     * appliquée afin d'éviter une coupure prématurée du transfert.
     */
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
   * Le nom du fichier est extrait de l'en-tête Content-Disposition si présent,
   * sinon `nomFichierParDefaut` est utilisé (défaut : "fichier").
   *
   * TODO(backend) : endpoint à créer — GET /rapports/{id}/pdf
   */
  async download(
    url: string,
    nomFichierParDefaut = 'fichier',
    /**
     * Timeout en ms pour ce téléchargement. 0 = sans limite (recommandé pour
     * les rapports PDF volumineux). Si omis, aucune limite n'est appliquée
     * afin d'éviter une coupure prématurée du transfert.
     */
    timeout = 0,
  ): Promise<void> {
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
  // Intercepteur de requête (JWT + tenant)
  // -------------------------------------------------------------------------

  private registerRequestInterceptor(): void {
    this.http.interceptors.request.use(
      (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
        if (this.accessToken !== null) {
          config.headers.set('Authorization', `Bearer ${this.accessToken}`);
        }
        if (this.currentTenant !== null) {
          config.headers.set('X-Tenant-ID', this.currentTenant);
        }
        return config;
      },
    );
  }

  // -------------------------------------------------------------------------
  // Intercepteur de réponse (normalisation erreur + refresh 401)
  // -------------------------------------------------------------------------

  private registerResponseInterceptor(): void {
    this.http.interceptors.response.use(
      // Succès : passe-plat (le déballage est fait dans les méthodes get/post/…)
      (response) => response,

      // Erreur
      async (error: AxiosError): Promise<never> => {
        const originalConfig = error.config as RetryableRequestConfig | undefined;
        const status = error.response?.status;

        // --- Tentative de refresh sur 401 ---
        const isAuthEndpoint =
          originalConfig?.url?.includes('/auth/login') === true ||
          originalConfig?.url?.includes('/auth/refresh') === true;

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
            this.clearTokens();
            this.onAuthFailureCallback?.();
            return Promise.reject(this.toApiError(error));
          }
        }

        // --- Normalisation en ApiError ---
        return Promise.reject(this.toApiError(error));
      },
    );
  }

  // -------------------------------------------------------------------------
  // Refresh partagé (une seule promesse pour les requêtes concurrentes)
  // -------------------------------------------------------------------------

  private doRefresh(): Promise<string> {
    if (this.refreshPromise !== null) {
      return this.refreshPromise;
    }

    this.refreshPromise = this.http
      .post<{ accessToken: string; refreshToken: string }>(
        '/auth/refresh',
        { refreshToken: this.refreshToken },
      )
      .then((response) => {
        const { accessToken, refreshToken } = response.data;
        this.setTokens(accessToken, refreshToken);
        return accessToken;
      })
      .finally(() => {
        this.refreshPromise = null;
      });

    return this.refreshPromise;
  }

  // -------------------------------------------------------------------------
  // Normalisation d'une AxiosError en ApiError (sans any)
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
 * Initialisation à faire au démarrage (ex. dans AuthProvider) :
 *   apiClient.setTokens(accessToken, refreshToken);
 *   apiClient.setTenant(tenantCode);
 *   apiClient.setOnAuthFailure(() => navigate({ to: '/login' }));
 *
 * TODO(2FA / backend) : le flux 2FA s'insère après une première réponse de
 *   POST /auth/login. L'endpoint de validation est POST /auth/2fa/valider.
 *   La forme exacte de LoginResponse (champs de la réponse intermédiaire,
 *   corps de la requête de validation, code HTTP attendu) est à vérifier dans
 *   le backend (AuthController / LoginResponse) avant d'implémenter ce flux
 *   côté frontend — TODO(backend).
 */
export const apiClient = new ApiClient();
export default apiClient;
