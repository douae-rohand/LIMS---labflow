import { useEffect, useMemo, useState } from "react";
import { RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { messageErreurApi } from "@/api/employes";
import type { EcartPermissionEmploye, LignePermissionEmploye } from "@/api/employePermissions";
import { useEmployePermissions } from "./useEmployePermissions";

const DEPENDANCES: Record<string, string> = {
  DEMANDES_STATUT: "DEMANDES_SUIVI",
  CONFORMITE_DECLARER: "CONFORMITE_CONSULTER",
  CONFORMITE_GERER: "CONFORMITE_CONSULTER",
};

const MODULES: Record<string, string> = {
  DEMANDES: "Demandes",
  ECHANTILLONS: "Échantillons",
  ESSAIS: "Essais",
  VALIDATION: "Validation",
  RAPPORTS: "Rapports",
  PLANIFICATION: "Planification",
  STOCK: "Stock",
  CONFORMITE: "Conformité",
  IA: "Analyse assistée",
  EXPORT: "Exports",
};

export function EmployePermissionsSection({
  employeId,
  onDirtyChange,
}: {
  employeId: number;
  onDirtyChange: (dirty: boolean) => void;
}) {
  const { permissions, enregistrer, retablir } = useEmployePermissions(employeId);
  const [selectionManuelle, setSelectionManuelle] = useState<Set<string> | null>(null);
  const [confirmationOuverte, setConfirmationOuverte] = useState(false);

  const lignes = permissions.data?.permissions ?? [];
  const delegables = useMemo(() => lignes.filter((ligne) => ligne.deleguable), [lignes]);
  const codesDelegables = useMemo(() => new Set(delegables.map((ligne) => ligne.code)), [delegables]);
  const selectionInitiale = useMemo(
    () => new Set(delegables.filter((ligne) => ligne.effective).map((ligne) => ligne.code)),
    [delegables],
  );
  const selection = selectionManuelle ?? selectionInitiale;
  const groupes = useMemo(() => grouper(delegables), [delegables]);
  const personnalisees = delegables.some((ligne) => ligne.ecart !== null);
  const modifiees = delegables.some(
    (ligne) => selection.has(ligne.code) !== ligne.parDefaut,
  );
  const ecarts = delegables.reduce<EcartPermissionEmploye[]>((resultat, ligne) => {
    const effective = selection.has(ligne.code);
    if (effective && !ligne.parDefaut) resultat.push({ code: ligne.code, effet: "AJOUT" });
    else if (!effective && ligne.parDefaut) resultat.push({ code: ligne.code, effet: "RETRAIT" });
    return resultat;
  }, []);

  useEffect(() => setSelectionManuelle(null), [selectionInitiale]);
  useEffect(() => {
    onDirtyChange(modifiees);
  }, [modifiees, onDirtyChange]);
  useEffect(() => () => onDirtyChange(false), [onDirtyChange]);

  function basculer(code: string, coche: boolean) {
    setSelectionManuelle((courante) => {
      const suivante = new Set(courante ?? selectionInitiale);
      if (coche) {
        suivante.add(code);
        const prerequis = DEPENDANCES[code];
        if (prerequis && codesDelegables.has(prerequis)) suivante.add(prerequis);
      } else {
        for (const [dependante, prerequis] of Object.entries(DEPENDANCES)) {
          if (prerequis === code && codesDelegables.has(dependante)) suivante.delete(dependante);
        }
        suivante.delete(code);
      }
      return suivante;
    });
  }

  return (
    <section className="space-y-4" aria-label="Permissions délégables de l’employé">
      <header className="flex justify-end">
        {personnalisees && (
          <span className="rounded-full bg-brand-900/10 px-2.5 py-1 text-xs font-medium text-brand-900">
            Personnalisées
          </span>
        )}
      </header>

      {permissions.isLoading && <p className="text-sm text-muted-foreground">Chargement des permissions…</p>}
      {permissions.isError && (
        <p className="text-sm text-destructive" role="alert">{messageErreurApi(permissions.error)}</p>
      )}
      {permissions.data && !permissions.data.personnalisable && (
        <p className="text-sm text-muted-foreground">Ces permissions ne sont pas personnalisables.</p>
      )}
      {permissions.data?.personnalisable && (
        <>
          {groupes.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune permission délégable pour ce rôle.</p>
          ) : (
            <div className="space-y-3">
              {groupes.map(([module, groupe]) => (
                <fieldset key={module} className="rounded-xl border border-brand-900/10 p-3">
                  <legend className="px-1 text-sm font-semibold text-ink-900">
                    {MODULES[module] ?? module}
                  </legend>
                  <ul className="space-y-2">
                    {groupe.map((ligne) => (
                      <li key={ligne.code} className="flex items-start justify-between gap-3">
                        <label className="flex items-start gap-2 text-sm text-ink-900">
                          <input
                            type="checkbox"
                            className="mt-0.5 size-4 accent-brand-900"
                            checked={selection.has(ligne.code)}
                            disabled={enregistrer.isPending || retablir.isPending}
                            onChange={(event) => basculer(ligne.code, event.target.checked)}
                          />
                          <span>{ligne.libelle}</span>
                        </label>
                        <span className="shrink-0 text-xs text-muted-foreground">
                          {etatPermission(ligne, selection)}
                        </span>
                      </li>
                    ))}
                  </ul>
                </fieldset>
              ))}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-brand-900/10 pt-3">
            <span className="text-xs text-muted-foreground" aria-live="polite">
              {modifiees ? "Modifications non enregistrées" : "Les changements s’appliqueront à la prochaine requête."}
            </span>
            <div className="flex flex-wrap justify-end gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={(!personnalisees && !modifiees) || retablir.isPending || enregistrer.isPending}
                onClick={() => setConfirmationOuverte(true)}
              >
                <RotateCcw className="mr-2 size-4" aria-hidden="true" />
                Réinitialiser aux défauts du rôle
              </Button>
              <Button
                type="button"
                size="sm"
                disabled={!modifiees || enregistrer.isPending || retablir.isPending}
                onClick={() => enregistrer.mutate(ecarts)}
              >
                {enregistrer.isPending ? "Enregistrement…" : "Enregistrer"}
              </Button>
            </div>
          </div>
        </>
      )}

      <AlertDialog
        open={confirmationOuverte}
        onOpenChange={(open) => {
          if (!retablir.isPending) setConfirmationOuverte(open);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Réinitialiser les permissions ?</AlertDialogTitle>
            <AlertDialogDescription>
              Les écarts de permissions délégables seront effacés. L’employé retrouvera les défauts de son rôle.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={retablir.isPending}>Annuler</AlertDialogCancel>
            <Button
              type="button"
              disabled={retablir.isPending}
              onClick={() => retablir.mutate(undefined, {
                onSuccess: (data) => {
                  setConfirmationOuverte(false);
                  setSelectionManuelle(new Set(
                    data.permissions.filter((ligne) => ligne.deleguable && ligne.effective)
                      .map((ligne) => ligne.code),
                  ));
                },
              })}
            >
              {retablir.isPending ? "Réinitialisation…" : "Confirmer la réinitialisation"}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  );
}

function etatPermission(ligne: LignePermissionEmploye, selection: Set<string>): string {
  const effective = selection.has(ligne.code);
  if (effective && !ligne.parDefaut) return "Ajoutée";
  if (!effective && ligne.parDefaut) return "Retirée";
  return ligne.parDefaut ? "Accordée par le rôle" : "Non accordée par le rôle";
}

function grouper(lignes: LignePermissionEmploye[]): [string, LignePermissionEmploye[]][] {
  const modules = new Map<string, LignePermissionEmploye[]>();
  for (const ligne of lignes) {
    const module = ligne.module ?? "AUTRES";
    const groupe = modules.get(module) ?? [];
    groupe.push(ligne);
    modules.set(module, groupe);
  }
  return [...modules.entries()].sort(([a], [b]) => a.localeCompare(b, "fr"));
}
