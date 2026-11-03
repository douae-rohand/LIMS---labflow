import {
  Building2,
  ClipboardCheck,
  ClipboardList,
  FileText,
  LayoutDashboard,
  Map,
  Receipt,
  ShoppingCart,
} from "lucide-react";
import type { ReactNode } from "react";
import type { SessionUser } from "@/api/session";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";
import { nombreTotalEssais, usePaniers } from "@/hooks/usePanier";
import { useSyntheseClient } from "@/hooks/useSyntheseClient";

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

  // Une erreur de synthèse ne casse pas la navigation : `data` reste undefined
  // et les pastilles sont simplement absentes (aucun toast déclenché ici).
  const syntheseQuery = useSyntheseClient();
  const facturesImpayees = syntheseQuery.data?.facturesImpayees ?? 0;
  const enquetesEnAttente = syntheseQuery.data?.enquetesEnAttente ?? 0;

  const navigation: EntreeNavigation[] = [
    {
      to: "/app",
      label: "Tableau de bord",
      icon: LayoutDashboard,
      exact: true,
    },
    {
      to: "/app/demandes",
      label: "Mes demandes",
      icon: ClipboardList,
    },
    {
      to: "/app/rapports",
      label: "Mes rapports",
      icon: FileText,
    },
    {
      to: "/app/factures",
      label: "Mes factures",
      icon: Receipt,
      badge: facturesImpayees,
    },
    {
      to: "/app/enquetes",
      label: "Enquêtes",
      icon: ClipboardCheck,
      badge: enquetesEnAttente,
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
