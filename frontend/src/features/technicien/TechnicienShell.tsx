import { CalendarDays, FlaskConical, LayoutDashboard, Upload } from "lucide-react";
import type { ReactNode } from "react";
import type { SessionUser } from "@/api/session";
import { EspaceShell, type EntreeNavigation } from "@/features/shell/EspaceShell";
import { useTechnicienSynthese } from "@/hooks/useTechnicienEssais";

/**
 * Shell de l'espace Technicien : sidebar EspaceShell avec 4 entrées.
 * Le badge « Mes essais » affiche le nombre d'essais en retard (masqué si 0).
 */
export function TechnicienShell({
  children,
  utilisateur,
  onLogout,
}: {
  children: ReactNode;
  utilisateur: SessionUser;
  onLogout: () => void;
}) {
  const syntheseQuery = useTechnicienSynthese();
  const enRetardCount = syntheseQuery.data?.enRetardCount ?? 0;

  const navigation: EntreeNavigation[] = [
    { to: "/app", label: "Tableau de bord", icon: LayoutDashboard, exact: true },
    { to: "/app/essais", label: "Mes essais", icon: FlaskConical, badge: enRetardCount },
    { to: "/app/planning", label: "Planning", icon: CalendarDays },
    { to: "/app/import", label: "Import CSV", icon: Upload },
  ];

  return (
    <EspaceShell
      utilisateur={utilisateur}
      libelleRole="Technicien"
      accueil="/app"
      titreSection="Mon espace"
      navigation={navigation}
      onLogout={onLogout}
    >
      {children}
    </EspaceShell>
  );
}
