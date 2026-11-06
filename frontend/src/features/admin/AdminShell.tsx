import { Activity, BarChart3, Download, FileText, FlaskConical, Inbox, Settings, Shield, Users } from "lucide-react";
import type { ReactNode } from "react";
import type { RoleUtilisateur, SessionUser } from "@/api/session";
import { PERMISSIONS_EXPORT } from "@/api/exports";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";

type EntreeAdminNavigation = EntreeNavigation & {
  permission?: string;
  permissions?: string[];
};

// ---------------------------------------------------------------------------
// Entrées de navigation de la sidebar admin
// ---------------------------------------------------------------------------

const NAV_ITEMS_ADMIN: EntreeAdminNavigation[] = [
  {
    to: "/app/tableau-de-bord" as const,
    label: "Tableau de bord",
    icon: BarChart3,
    permission: "INDICATEURS_CONSULTER",
  },
  {
    to: "/app/demandes-clients" as const,
    label: "Demandes clients",
    icon: Inbox,
    permission: "DEMANDES_CONSULTER",
  },
  {
    to: "/app/facturation" as const,
    label: "Facturation",
    icon: FileText,
    permission: "FACTURATION_CONSULTER",
  },
  {
    to: "/app/catalogue" as const,
    label: "Catalogue",
    icon: FlaskConical,
    permission: "CATALOGUE_GERER",
  },
  {
    to: "/app/stock" as const,
    label: "Stock",
    icon: Activity,
    permission: "STOCK_CONSULTER",
  },
  {
    to: "/app/satisfaction" as const,
    label: "Satisfaction",
    icon: Shield,
    permission: "SATISFACTION_CONSULTER",
  },
  {
    to: "/app/employes" as const,
    label: "Employés",
    icon: Users,
    permission: "EMPLOYES_GERER",
  },
  {
    to: "/app/audit" as const,
    label: "Journal d'audit",
    icon: Activity,
    permission: "AUDIT_CONSULTER",
  },
  {
    to: "/app/exports" as const,
    label: "Exports",
    icon: Download,
    permissions: PERMISSIONS_EXPORT,
  },
  {
    to: "/app/parametres" as const,
    label: "Paramètres",
    icon: Settings,
    permission: "PARAMETRES_LABORATOIRE_GERER",
  },
] as const;

const NAV_ITEMS_RESPONSABLE: EntreeAdminNavigation[] = [
  {
    to: "/app/employes" as const,
    label: "Employés",
    icon: Users,
    permission: "EMPLOYES_GERER",
  },
  {
    to: "/app/demandes-clients" as const,
    label: "Demandes clients",
    icon: Inbox,
    permission: "DEMANDES_CONSULTER",
  },
  {
    to: "/app/catalogue" as const,
    label: "Catalogue",
    icon: FlaskConical,
    permission: "CATALOGUE_GERER",
  },
  {
    to: "/app/exports" as const,
    label: "Exports",
    icon: Download,
    permissions: PERMISSIONS_EXPORT,
  },
] as const;

const LIBELLE_ROLE: Partial<Record<RoleUtilisateur, string>> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

function entreesVisibles(role: RoleUtilisateur, permissions: readonly string[]): EntreeNavigation[] {
  const items = role === "ADMINISTRATEUR" ? NAV_ITEMS_ADMIN : NAV_ITEMS_RESPONSABLE;
  return items.filter((item) => {
    if (item.permission && !permissions.includes(item.permission)) return false;
    if (item.permissions && !item.permissions.some((permission) => permissions.includes(permission))) return false;
    return true;
  });
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
  const navigation = entreesVisibles(utilisateur.role, utilisateur.permissions);
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
