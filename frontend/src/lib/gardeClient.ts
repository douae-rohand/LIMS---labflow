import { redirect } from "@tanstack/react-router";
import { getSession, getStatus } from "@/api/session";

/**
 * Garde commune des routes de l'espace client (rapports, factures, enquêtes,
 * profil). Reprend exactement le comportement de `routes/app/panier.tsx` :
 *  - visiteur anonyme → /login avec le chemin demandé en paramètre `redirect` ;
 *  - rôle différent de CLIENT → /app.
 *
 * À appeler depuis un `beforeLoad` : la fonction lève une redirection.
 */
export function exigerSessionClient(location: { href: string }): void {
  if (getStatus() === "anonyme") {
    throw redirect({ to: "/login", search: { redirect: location.href } });
  }
  if (getSession()?.user.role !== "CLIENT") {
    throw redirect({ to: "/app" });
  }
}
