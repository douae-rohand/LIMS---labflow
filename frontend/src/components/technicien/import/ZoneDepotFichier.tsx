import { useRef } from "react";
import { Upload } from "lucide-react";
import { cn } from "@/lib/utils";

/** Génère un fichier CSV de modèle côté client et le télécharge. */
function telechargerModele() {
  const contenu = "code_ligne;valeur;commentaire\nLE-0001;0.92;Résultat normal\nLE-0002;2.60;Valeur élevée";
  const blob = new Blob([contenu], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "modele-import-resultats.csv";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  // Révocation immédiate après usage
  setTimeout(() => URL.revokeObjectURL(url), 100);
}

/**
 * Zone de dépôt de fichier CSV accessible au clavier.
 * - Clic ou touche Entrée/Espace → ouvre le sélecteur de fichier.
 * - Glisser-déposer accepté.
 * - Rappel du format et lien « Télécharger un modèle ».
 */
export function ZoneDepotFichier({
  onFichier,
  fichierNom,
  desactive,
}: {
  onFichier: (fichier: File) => void;
  fichierNom: string | undefined;
  desactive: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const traiterFichier = (fichier: File) => {
    if (!fichier.name.toLowerCase().endsWith(".csv")) return;
    onFichier(fichier);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) traiterFichier(f);
    // Reset pour permettre de re-sélectionner le même fichier
    e.target.value = "";
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (desactive) return;
    const f = e.dataTransfer.files[0];
    if (f) traiterFichier(f);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      inputRef.current?.click();
    }
  };

  return (
    <div className="space-y-3">
      <div
        role="button"
        tabIndex={desactive ? -1 : 0}
        aria-label="Zone de dépôt : cliquez ou glissez un fichier CSV ici"
        aria-disabled={desactive}
        onClick={() => !desactive && inputRef.current?.click()}
        onKeyDown={handleKeyDown}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
        className={cn(
          "flex min-h-40 cursor-pointer flex-col items-center justify-center gap-3",
          "rounded-3xl border-2 border-dashed p-6 text-center transition-colors",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900",
          desactive
            ? "cursor-not-allowed border-brand-900/10 opacity-50"
            : fichierNom
              ? "border-brand-900/40 bg-brand-900/5"
              : "border-brand-900/20 bg-card hover:border-brand-900/40 hover:bg-brand-900/5",
        )}
      >
        <Upload
          className={cn("size-10", fichierNom ? "text-brand-900" : "text-muted-foreground")}
          aria-hidden="true"
        />
        {fichierNom ? (
          <div>
            <p className="text-sm font-semibold text-brand-900">Fichier sélectionné</p>
            <p className="mt-0.5 max-w-xs truncate text-xs text-muted-foreground">
              {fichierNom}
            </p>
          </div>
        ) : (
          <div>
            <p className="text-sm font-semibold text-ink-900">
              Glissez un fichier CSV ici ou cliquez pour parcourir
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Format : CSV UTF-8, séparateur ; ou , — Taille max : 1 Mo, 500 lignes
            </p>
          </div>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept=".csv,text/csv"
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
        onChange={handleChange}
      />

      <div className="flex flex-wrap items-center gap-3 text-sm">
        <p className="text-muted-foreground">
          Colonnes requises : <code className="rounded bg-surface-alt px-1.5 py-0.5 font-mono text-xs">code_ligne ; valeur ; commentaire</code>
        </p>
        <button
          type="button"
          onClick={telechargerModele}
          className="font-medium text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 rounded"
        >
          Télécharger un modèle
        </button>
      </div>
    </div>
  );
}
