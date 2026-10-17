import { Inbox, Users } from "lucide-react";
import type { ReactNode } from "react";
import type { RoleUtilisateur, SessionUser } from "@/api/session";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";

// ---------------------------------------------------------------------------
// Entrées de navigation de la sidebar admin
// ---------------------------------------------------------------------------

const NAV_ITEMS = [
  {
    to: "/app/employes" as const,
    label: "Employés",
    icon: Users,
    roles: ["ADMINISTRATEUR"] as const,
  },
  {
    to: "/app/demandes-clients" as const,
    label: "Demandes clients",
    icon: Inbox,
    roles: ["ADMINISTRATEUR", "RESPONSABLE"] as const,
  },
] as const;

const LIBELLE_ROLE: Partial<Record<RoleUtilisateur, string>> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
};

function entreesVisibles(role: RoleUtilisateur): EntreeNavigation[] {
  return NAV_ITEMS.filter((item) => (item.roles as readonly string[]).includes(role));
}

// ---------------------------------------------------------------------------
// Composant
// ---------------------------------------------------------------------------

export function AdminShell({
  children,
  utilisateur,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  onLogout: () => void;
}) {
  const navigation = entreesVisibles(utilisateur.role);
  const accueil = navigation[0]?.to ?? "/app";
  const libelleRole = LIBELLE_ROLE[utilisateur.role] ?? "Laboratoire";

  return (
    <EspaceShell
      utilisateur={utilisateur}
      libelleRole={libelleRole}
      accueil={accueil}
      titreSection="Laboratoire"
      navigation={navigation}
      onLogout={onLogout}
    >
      {children}
    </EspaceShell>
  );
}
