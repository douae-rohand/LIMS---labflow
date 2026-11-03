import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import {
  lirePermissions,
  remplacerPermissions,
  retablirPermissions,
  type LignePermission,
} from "@/api/plateformeUtilisateurs";
import { Dialogue } from "../Fenetres";
import { messageErreur } from "../texte";

const EXIGE: Record<string, string> = {
  EMPLOYES_GERER: "EMPLOYES_CONSULTER",
  CATALOGUE_GERER: "CATALOGUE_CONSULTER",
  FACTURATION_GERER: "FACTURATION_CONSULTER",
  STOCK_GERER: "STOCK_CONSULTER",
  CONFORMITE_GERER: "CONFORMITE_CONSULTER",
  CONFORMITE_DECLARER: "CONFORMITE_CONSULTER",
  DEMANDES_STATUT: "DEMANDES_SUIVI",
};

export function DialoguePermissions({
  compteId,
  nom,
  onFermer,
}: {
  compteId: number | null;
  nom: string;
  onFermer: () => void;
}) {
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [confirmation, setConfirmation] = useState(false);
  const permissions = useQuery({
    queryKey: ["permissions-compte", compteId],
    queryFn: () => lirePermissions(compteId ?? 0),
    enabled: compteId != null,
  });

  useEffect(() => {
    const lignes = permissions.data?.permissions;
    if (!lignes) return;
    setSelection(new Set(lignes.filter((ligne) => ligne.effective).map((ligne) => ligne.code)));
    setConfirmation(false);
  }, [permissions.data]);

  const enregistrer = useMutation({
    mutationFn: () => remplacerPermissions(compteId ?? 0, ecarts(permissions.data?.permissions ?? [], selection)),
    onSuccess: async () => {
      toast.success("Permissions enregistrées.");
      await queryClient.invalidateQueries({ queryKey: ["comptes-plateforme"] });
      await queryClient.invalidateQueries({ queryKey: ["permissions-compte", compteId] });
      onFermer();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Les permissions n'ont pas pu être enregistrées.")),
  });

  const retablir = useMutation({
    mutationFn: () => retablirPermissions(compteId ?? 0),
    onSuccess: async () => {
      toast.success("Permissions du rôle rétablies.");
      setConfirmation(false);
      await queryClient.invalidateQueries({ queryKey: ["comptes-plateforme"] });
      await queryClient.invalidateQueries({ queryKey: ["permissions-compte", compteId] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le rétablissement a échoué.")),
  });

  const lignes = permissions.data?.permissions ?? [];
  const libelles = new Map(lignes.map((ligne) => [ligne.code, ligne.libelle]));

  function basculer(code: string, coche: boolean) {
    const suivante = new Set(selection);
    if (coche) {
      suivante.add(code);
      const prerequis = EXIGE[code];
      if (prerequis) suivante.add(prerequis);
      setSelection(suivante);
      return;
    }
    const aRetirer = Object.entries(EXIGE)
      .filter(([, prerequis]) => prerequis === code)
      .map(([dependante]) => dependante)
      .filter((dependante) => suivante.has(dependante));
    if (aRetirer.length > 0) {
      const noms = aRetirer.map((dependante) => libelles.get(dependante) ?? dependante).join(", ");
      const accepte = window.confirm(
        `Retirer « ${libelles.get(code) ?? code} » retire aussi : ${noms}.`,
      );
      if (!accepte) return;
      aRetirer.forEach((dependante) => suivante.delete(dependante));
    }
    suivante.delete(code);
    setSelection(suivante);
  }

  const modules = grouper(lignes);

  return (
    <Dialogue
      ouvert={compteId != null}
      large
      titre={`Permissions de ${nom}`}
      description="Les cases reprennent les défauts du rôle. Une permission ajoutée ou retirée est indiquée sur sa ligne."
      onFermer={onFermer}
    >
      {permissions.isLoading && <p className="text-sm text-muted-foreground">Chargement des permissions…</p>}
      {permissions.isError && <p className="text-sm text-ink-900" role="alert">{messageErreur(permissions.error, "Les permissions sont indisponibles.")}</p>}
      {permissions.data && !permissions.data.personnalisable && (
        <p className="text-sm text-muted-foreground">Les permissions de ce compte ne sont pas personnalisables.</p>
      )}
      {permissions.data?.personnalisable && (
        <div className="flex flex-col gap-4">
          {[...modules.entries()].map(([module, groupe]) => (
            <fieldset key={module} className="rounded-2xl border border-brand-900/10 p-3">
              <legend className="px-1 text-sm font-semibold text-ink-900">{module}</legend>
              <ul className="flex flex-col gap-2">
                {groupe.map((ligne) => (
                  <li key={ligne.code} className="flex items-center justify-between gap-3">
                    <label className="flex items-center gap-2 text-sm text-ink-900" htmlFor={`perm-${ligne.code}`}>
                      <input
                        id={`perm-${ligne.code}`}
                        type="checkbox"
                        className="size-4 accent-brand-900"
                        checked={selection.has(ligne.code)}
                        onChange={(event) => basculer(ligne.code, event.target.checked)}
                      />
                      {ligne.libelle}
                    </label>
                    <span className="text-xs text-muted-foreground">{origine(ligne, selection)}</span>
                  </li>
                ))}
              </ul>
            </fieldset>
          ))}
          <section className="rounded-2xl bg-surface-alt p-3" aria-label="Actions liées au rôle, non délégables">
            <h3 className="text-sm font-semibold text-ink-900">Actions liées au rôle, non délégables</h3>
            <ul className="mt-2 list-disc pl-5 text-sm text-muted-foreground">
              {permissions.data.actionsNonDelegables.map((action) => <li key={action}>{action}</li>)}
            </ul>
          </section>
          {confirmation ? (
            <div className="rounded-2xl border border-brand-900/10 p-3">
              <p className="text-sm text-ink-900">Les écarts de ce compte seront effacés. Le rôle retrouve ses permissions par défaut.</p>
              <div className="mt-3 flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={() => setConfirmation(false)}>Annuler</Button>
                <Button type="button" disabled={retablir.isPending} onClick={() => retablir.mutate()}>Confirmer le rétablissement</Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-wrap justify-end gap-2">
              <Button type="button" variant="ghost" onClick={() => setConfirmation(true)}>Rétablir les permissions par défaut du rôle</Button>
              <Button type="button" variant="ghost" onClick={onFermer}>Fermer</Button>
              <Button type="button" disabled={enregistrer.isPending} onClick={() => enregistrer.mutate()}>Enregistrer</Button>
            </div>
          )}
        </div>
      )}
    </Dialogue>
  );
}

function origine(ligne: LignePermission, selection: Set<string>): string {
  const effective = selection.has(ligne.code);
  if (effective && !ligne.parDefaut) return "Ajoutée";
  if (!effective && ligne.parDefaut) return "Retirée";
  return "Par défaut";
}

function grouper(lignes: LignePermission[]): Map<string, LignePermission[]> {
  const modules = new Map<string, LignePermission[]>();
  for (const ligne of lignes) {
    const groupe = modules.get(ligne.module) ?? [];
    groupe.push(ligne);
    modules.set(ligne.module, groupe);
  }
  return modules;
}

function ecarts(lignes: LignePermission[], selection: Set<string>): { code: string; effet: "AJOUT" | "RETRAIT" }[] {
  const resultat: { code: string; effet: "AJOUT" | "RETRAIT" }[] = [];
  for (const ligne of lignes) {
    const effective = selection.has(ligne.code);
    if (effective && !ligne.parDefaut) resultat.push({ code: ligne.code, effet: "AJOUT" });
    else if (!effective && ligne.parDefaut) resultat.push({ code: ligne.code, effet: "RETRAIT" });
  }
  return resultat;
}
