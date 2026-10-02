/// <reference types="vite/client" />

/**
 * Typage des variables d'environnement VITE_* exposées par Vite.
 * Toutes les variables listées ici doivent être définies dans .env.example.
 *
 * @see https://vitejs.dev/guide/env-and-mode.html#intellisense-for-typescript
 */
interface ImportMetaEnv {
  /**
   * URL absolue du backend Spring Boot.
   * Utilisée par vite.config.ts pour configurer le proxy Vite.
   * N'est PAS injectée dans le bundle — uniquement lue côté serveur Vite.
   *
   * @example "http://localhost:8081"
   */
  readonly VITE_BACKEND_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
