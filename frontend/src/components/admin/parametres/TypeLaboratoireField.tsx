import { useState } from "react";
import { Check, Pencil, X } from "lucide-react";
import { Button } from "@/components/lab";

const TYPES_LABORATOIRE = [
  "Médical",
  "Industriel",
  "Environnemental",
  "Pharmaceutique et cosmétique",
];

interface TypeLaboratoireFieldProps {
  value: string;
  onChange: (value: string) => void;
}

export function TypeLaboratoireField({ value, onChange }: TypeLaboratoireFieldProps) {
  const [editionOuverte, setEditionOuverte] = useState(false);
  const [selection, setSelection] = useState<string[]>([]);
  const typesActuels = separerTypes(value);

  function ouvrirEdition() {
    setSelection(typesActuels);
    setEditionOuverte(true);
  }

  function basculer(type: string) {
    setSelection((courant) => courant.includes(type)
      ? courant.filter((element) => element !== type)
      : [...courant, type]);
  }

  function appliquer() {
    onChange(selection.join(","));
    setEditionOuverte(false);
  }

  return (
    <section className="rounded-2xl border border-brand-900/10 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-semibold text-ink-900">Type de laboratoire</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Le type actuel est conservé tant que vous ne le modifiez pas.
          </p>
        </div>
        {!editionOuverte && (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={ouvrirEdition}
            icon={<Pencil className="size-4" aria-hidden="true" />}
            iconPosition="left"
          >
            Modifier le type
          </Button>
        )}
      </div>

      {!editionOuverte ? (
        <div className="mt-4 flex flex-wrap gap-2" aria-label="Types actuels du laboratoire">
          {typesActuels.length > 0 ? typesActuels.map((type) => (
            <span key={type} className="rounded-full bg-brand-900/10 px-3 py-1.5 text-sm font-medium text-brand-900">
              {type}
            </span>
          )) : (
            <span className="text-sm text-muted-foreground">Aucun type renseigné.</span>
          )}
        </div>
      ) : (
        <div className="mt-5 space-y-4">
          <fieldset>
            <legend className="mb-3 text-sm font-medium text-ink-900">
              Sélectionnez les types qui décrivent votre laboratoire
            </legend>
            <div className="grid gap-2 sm:grid-cols-2">
              {TYPES_LABORATOIRE.map((type) => {
                const coche = selection.includes(type);
                return (
                  <label
                    key={type}
                    className={`flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-sm transition ${
                      coche
                        ? "border-brand-900/40 bg-brand-900/5 text-ink-900"
                        : "border-brand-900/10 bg-white text-muted-foreground hover:bg-surface-card/60"
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={coche}
                      onChange={() => basculer(type)}
                      className="size-4 accent-brand-900"
                    />
                    <span className="flex-1">{type}</span>
                    {coche && <Check className="size-4 text-brand-900" aria-hidden="true" />}
                  </label>
                );
              })}
            </div>
          </fieldset>
          <p className="text-xs text-muted-foreground">
            Le changement sera enregistré avec les autres paramètres du laboratoire.
          </p>
          <div className="flex flex-wrap justify-end gap-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setEditionOuverte(false)}
              icon={<X className="size-4" aria-hidden="true" />}
              iconPosition="left"
            >
              Annuler
            </Button>
            <Button type="button" size="sm" onClick={appliquer} disabled={selection.length === 0}>
              Confirmer le type
            </Button>
          </div>
        </div>
      )}
    </section>
  );
}

function separerTypes(value: string): string[] {
  return value.split(",").map((type) => type.trim()).filter(Boolean);
}
