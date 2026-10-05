import { QueryClient } from "@tanstack/react-query";
import { RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import apiClient from "./api/axios";
import { restoreSession } from "./api/restoreSession";
import { getRouter } from "./router";
import "./styles.css";

const queryClient = new QueryClient();
const router = getRouter(queryClient);

const rootEl = document.getElementById("root");
if (!rootEl) throw new Error("Root element #root not found in index.html");

// Configurer le callback de déconnexion forcée (refresh token expiré / révoqué).
// La navigation effective vers /login sera branchée une fois TanStack Router initialisé.
apiClient.setOnAuthFailure(() => {
  // router.navigate est disponible dès que l'instance existe.
  void router.navigate({ to: "/login" });
});

// Tentative de restauration silencieuse de session avant le premier rendu.
// Si le cookie refresh_token est présent et valide, la session est restaurée.
// Sinon, l'utilisateur reste non connecté (état initial normal).
restoreSession().finally(() => {
  createRoot(rootEl).render(
    <StrictMode>
      <RouterProvider router={router} />
    </StrictMode>,
  );
});
