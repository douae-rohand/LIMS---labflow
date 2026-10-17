import { Building2, ClipboardList, LayoutDashboard, Map, ShoppingCart } from "lucide-react";
import type { ReactNode } from "react";
import type { SessionUser } from "@/api/session";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";
import { nombreTotalEssais, usePaniers } from "@/hooks/usePanier";

/**
 * Espace client isolé : sidebar dédiée, sans Navbar/Footer publics.
 * Rendu par `routes/app.tsx` pour le rôle CLIENT uniquement : c'est le seul
 * endroit d'où le panier est interrogé (`usePaniers`).
 */
export function ClientShell({
  children,
  utilisateur,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  onLogout: () => void;
}) {
  const paniersQuery = usePaniers();
  const nombrePanier = nombreTotalEssais(paniersQuery.data);

  const navigation: EntreeNavigation[] = [
    {
      to: "/app",
      label: "Tableau de bord",
      icon: LayoutDashboard,
      exact: true,
    },
    {
      to: "/app/demandes",
      label: "Mes commandes",
      icon: ClipboardList,
    },
    {
      to: "/app/laboratoires",
      label: "Laboratoires",
      icon: Building2,
    },
    {
      to: "/app/carte",
      label: "Carte",
      icon: Map,
    },
    {
      to: "/app/panier",
      label: "Mon panier",
      icon: ShoppingCart,
      badge: nombrePanier,
    },
  ];

  return (
    <EspaceShell
      utilisateur={utilisateur}
      libelleRole="Client"
      accueil="/app"
      titreSection="Mon espace"
      navigation={navigation}
      onLogout={onLogout}
    >
      {children}
    </EspaceShell>
  );
}
