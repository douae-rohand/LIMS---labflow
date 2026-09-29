import axios from 'axios';

/**
 * Client HTTP Axios centralisé.
 * Utilise le proxy `/api` de Vite en dev local pour router les appels vers le backend.
 */
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Intercepteur global pour extraire le payload de réponse et journaliser les erreurs
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default apiClient;
