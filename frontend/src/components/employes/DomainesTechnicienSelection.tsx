import type { DomaineDto } from "@/api/employes";

interface DomainesTechnicienSelectionProps {
  domaines: DomaineDto[];
  selection: number[];
  messageVide: string;
  disabled: boolean;
  onChange: (selection: number[]) => void;
}

export function DomainesTechnicienSelection({
  domaines,
  selection,
  messageVide,
  disabled,
  onChange,
}: DomainesTechnicienSelectionProps) {
  const modifierSelection = (id: number, coche: boolean) => {
    const prochaine = new Set(selection);
    if (coche) {
      prochaine.add(id);
    } else {
      prochaine.delete(id);
    }
    onChange([...prochaine].sort((a, b) => a - b));
  };

  return (
    <fieldset disabled={disabled} className="space-y-2 rounded-xl border border-brand-900/10 p-3">
      <legend className="px-1 text-xs font-semibold text-ink-900">Domaines de compétence</legend>
      {selection.length === 0 && (
        <p className="text-xs text-muted-foreground">{messageVide}</p>
      )}
      {domaines.length === 0 ? (
        <p className="text-xs text-muted-foreground">
          Aucun domaine n'est configuré dans ce laboratoire. Ajoutez-en au catalogue avant
          d'attribuer des analyses à un technicien.
        </p>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {domaines.map((domaine) => (
            <label key={domaine.id} className="flex items-start gap-2 text-sm text-ink-900">
              <input
                type="checkbox"
                checked={selection.includes(domaine.id)}
                onChange={(event) => modifierSelection(domaine.id, event.target.checked)}
                className="mt-0.5 accent-brand-900"
              />
              <span>{domaine.code} — {domaine.libelle}</span>
            </label>
          ))}
        </div>
      )}
    </fieldset>
  );
}
