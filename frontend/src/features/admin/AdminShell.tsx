import {
  Activity,
  BarChart3,
  CalendarClock,
  ClipboardCheck,
  Download,
  FileSignature,
  FileText,
  FlaskConical,
  Gauge,
  Inbox,
  LayoutDashboard,
  PackageCheck,
  Receipt,
  UserPlus,
  Settings,
  Shield,
  Users,
} from "lucide-react";
import type { ReactNode } from "react";
import type { RoleUtilisateur, SessionUser } from "@/api/session";
import { PERMISSIONS_EXPORT } from "@/api/exports";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";

type EntreeAdminNavigation = EntreeNavigation & {
  permission?: string;
  permissions?: string[];
  /** Entrée réservée à ces rôles (la liste est partagée avec le technicien et l'accueil). */
  roles?: RoleUtilisateur[];
  /** Entrée masquée pour ces rôles, même s'ils ont la permission. */
  rolesExclus?: RoleUtilisateur[];
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
    to: "/app/suivi" as const,
    label: "Tableau de suivi",
    icon: Gauge,
    permission: "PLANIFICATION_GERER",
    roles: ["RESPONSABLE"],
  },
  {
    to: "/app/demandes-clients" as const,
    label: "Demandes clients",
    icon: Inbox,
    permission: "DEMANDES_CONSULTER",
  },
  {
    to: "/app/planification" as const,
    label: "Planification",
    icon: CalendarClock,
    permission: "PLANIFICATION_GERER",
    roles: ["RESPONSABLE"],
  },
  {
    to: "/app/validation-resultats" as const,
    label: "Validation des résultats",
    icon: ClipboardCheck,
    permission: "VALIDATION_GERER",
    roles: ["RESPONSABLE"],
  },
  {
    to: "/app/rapports-laboratoire" as const,
    label: "Rapports",
    icon: FileSignature,
    permission: "RAPPORTS_GERER",
    roles: ["RESPONSABLE"],
  },
  {
    to: "/app/employes" as const,
    label: "Employés",
    icon: Users,
    permission: "EMPLOYES_GERER",
  },
  {
    to: "/app/catalogue" as const,
    label: "Catalogue",
    icon: FlaskConical,
    permission: "CATALOGUE_GERER",
    // Le catalogue relève de l'administrateur : masqué pour le responsable.
    rolesExclus: ["RESPONSABLE"],
  },
  {
    to: "/app/exports" as const,
    label: "Exports",
    icon: Download,
    permissions: PERMISSIONS_EXPORT,
  },
] as const;

// Entrées de l'agent d'accueil : tableau de bord et réception des échantillons.
const NAV_ITEMS_ACCUEIL: EntreeAdminNavigation[] = [
  {
    to: "/app" as const,
    label: "Tableau de bord",
    icon: LayoutDashboard,
    exact: true,
  },
  {
    to: "/app/reception" as const,
    label: "Réception",
    icon: PackageCheck,
    permission: "ECHANTILLONS_GERER",
  },
  {
    to: "/app/caisse" as const,
    label: "Caisse",
    icon: Receipt,
    permission: "FACTURATION_PAYER",
  },
  {
    to: "/app/clients-accueil" as const,
    label: "Clients et patients",
    icon: UserPlus,
  },
];

const LIBELLE_ROLE: Partial<Record<RoleUtilisateur, string>> = {
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
};

function entreesVisibles(role: RoleUtilisateur, permissions: readonly string[]): EntreeNavigation[] {
  const items = role === "ADMINISTRATEUR" ? NAV_ITEMS_ADMIN
    : role === "ACCUEIL" ? NAV_ITEMS_ACCUEIL
    : NAV_ITEMS_RESPONSABLE;
  return items.filter((item) => {
    if (item.roles && !item.roles.includes(role)) return false;
    if (item.rolesExclus?.includes(role)) return false;
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
