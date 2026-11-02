import { NOTE_MAX, NOTE_MIN } from "./libelles";

const NOTES = Array.from(
  { length: NOTE_MAX - NOTE_MIN + 1 },
  (_, index) => NOTE_MIN + index,
);

interface NotesRadioProps {
  id: string;
  legende: string;
  valeur: number | null;
  onChange: (valeur: number) => void;
  erreur?: string | undefined;
  obligatoire?: boolean | undefined;
  disabled?: boolean | undefined;
}

/** Sélection d'une note de 1 à 5 (boutons radio natifs accessibles). */
export function NotesRadio({
  id,
  legende,
  valeur,
  onChange,
  erreur,
  obligatoire = false,
  disabled = false,
}: NotesRadioProps) {
  const messageId = `${id}-erreur`;

  return (
    <fieldset
      disabled={disabled}
      aria-describedby={erreur !== undefined ? messageId : undefined}
    >
      <legend className="text-sm font-semibold text-ink-900">
        {legende}
        {obligatoire ? <span aria-hidden="true"> *</span> : null}
      </legend>
      <div className="mt-2 flex flex-wrap gap-2">
        {NOTES.map((note) => (
          <label
            key={note}
            className="cursor-pointer rounded-xl border border-brand-900/20 px-3.5 py-2 text-sm font-semibold text-brand-900 transition-colors hover:bg-surface-card has-[:checked]:border-brand-900 has-[:checked]:bg-brand-900 has-[:checked]:text-primary-foreground focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-brand-900"
          >
            <input
              type="radio"
              name={id}
              value={note}
              checked={valeur === note}
              onChange={() => onChange(note)}
              className="sr-only"
            />
            <span>{note}</span>
          </label>
        ))}
      </div>
      {erreur !== undefined ? (
        <p id={messageId} role="alert" className="mt-2 text-sm text-red-600">
          {erreur}
        </p>
      ) : null}
    </fieldset>
  );
}
