import { useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/lab";
import { messageErreurApi, type EmployeDetailDto } from "@/api/employes";
import { useDomainesLaboratoire } from "@/hooks/useDomainesLaboratoire";
import { DomainesTechnicienSelection } from "./DomainesTechnicienSelection";

interface EmployeDomainesProps {
  employe: EmployeDetailDto;
  isSaving: boolean;
  onSave: (domaines: number[]) => void;
}

export function EmployeDomaines({ employe, isSaving, onSave }: EmployeDomainesProps) {
  const domainesQuery = useDomainesLaboratoire();
  const selectionServeur = useMemo(
    () => employe.domaines.map((domaine) => domaine.id).sort((a, b) => a - b),
    [employe.domaines],
  );
  const [selection, setSelection] = useState<number[]>(() =>
    selectionServeur,
  );
  const sauvegardeEnCoursRef = useRef(false);
  const valeursEgales =
    selection.length === selectionServeur.length &&
    selection.every((id, index) => id === selectionServeur[index]);

  useEffect(() => {
    setSelection(selectionServeur);
  }, [employe.id, selectionServeur]);

  useEffect(() => {
    if (!isSaving) {
      sauvegardeEnCoursRef.current = false;
    }
  }, [isSaving]);

  const enregistrer = () => {
    if (valeursEgales || isSaving || sauvegardeEnCoursRef.current || domainesQuery.isError) {
      return;
    }
    sauvegardeEnCoursRef.current = true;
    onSave([...selection]);
  };

  return (
    <section className="space-y-3 border-t border-brand-900/10 pt-4">
      <h3 className="text-sm font-semibold text-ink-900">Domaines de compétence</h3>
      {domainesQuery.isLoading ? (
        <p className="text-xs text-muted-foreground">Chargement des domaines…</p>
      ) : domainesQuery.isError ? (
        <p role="alert" className="text-xs text-rose-700">
          {messageErreurApi(domainesQuery.error)}
        </p>
      ) : (
        <DomainesTechnicienSelection
          domaines={domainesQuery.data ?? []}
          selection={selection}
          messageVide="Aucun domaine affecté."
          disabled={isSaving}
          onChange={setSelection}
        />
      )}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="secondary"
          disabled={isSaving || valeursEgales}
          onClick={() => setSelection(selectionServeur)}
        >
          Annuler
        </Button>
        <Button
          type="button"
          variant="primary"
          disabled={
            isSaving || valeursEgales || domainesQuery.isLoading ||
            domainesQuery.isError || sauvegardeEnCoursRef.current
          }
          onClick={enregistrer}
        >
          {isSaving ? "Enregistrement…" : "Enregistrer les domaines"}
        </Button>
      </div>
    </section>
  );
}
