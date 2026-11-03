import { useEffect, useRef, useState, type ReactNode } from "react";
import { ChevronDown, Search, Upload, X, type LucideIcon } from "lucide-react";
import { Button, Input, Select } from "@/components/lab";

export type FormatExport = {
  icone: LucideIcon;
  libelle: string;
  onClick: () => void;
};

export type PuceFiltre = {
  libelle: string;
  onRetirer: () => void;
};

/** Barre de recherche, filtres, import et export, identique à la page Laboratoires. */
export function BarreListe({
  recherche,
  onRecherche,
  placeholder,
  libelleRecherche,
  filtres,
  filtresActifs,
  onReinitialiser,
  puces = [],
  onImporter,
  formatsExport,
  porteeExport,
  exportEnCours,
  action,
}: {
  recherche: string;
  onRecherche: (valeur: string) => void;
  placeholder: string;
  libelleRecherche: string;
  filtres?: ReactNode;
  filtresActifs: boolean;
  onReinitialiser: () => void;
  puces?: PuceFiltre[];
  onImporter?: () => void;
  formatsExport?: FormatExport[];
  porteeExport?: {
    id: string;
    valeur: string;
    onChange: (valeur: string) => void;
    options: { valeur: string; libelle: string }[];
  };
  exportEnCours?: boolean;
  action?: ReactNode;
}) {
  const exportRef = useRef<HTMLDivElement>(null);
  const [exportOuvert, setExportOuvert] = useState(false);
  const aExporter = (formatsExport?.length ?? 0) > 0;

  useEffect(() => {
    if (!exportOuvert) return;
    const fermer = (event: MouseEvent) => {
      if (!exportRef.current?.contains(event.target as Node)) setExportOuvert(false);
    };
    window.addEventListener("mousedown", fermer);
    return () => window.removeEventListener("mousedown", fermer);
  }, [exportOuvert]);

  return (
    <div className="rounded-2xl border border-brand-900/10 bg-card p-3 sm:p-4">
      <div className="flex flex-col gap-3 xl:flex-row xl:flex-wrap xl:items-center">
        <div className="relative min-w-0 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input
            value={recherche}
            onChange={(event) => onRecherche(event.target.value)}
            placeholder={placeholder}
            aria-label={libelleRecherche}
            className="pl-10"
          />
        </div>
        {filtres && <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">{filtres}</div>}
        <div className="flex flex-wrap gap-2">
          {filtresActifs && (
            <Button variant="ghost" size="sm" onClick={onReinitialiser}>
              Réinitialiser
            </Button>
          )}
          {action}
          {onImporter && (
            <Button
              size="sm"
              icon={<Upload className="size-4" aria-hidden="true" />}
              iconPosition="left"
              onClick={onImporter}
            >
              Importer
            </Button>
          )}
          {aExporter && (
            <div className="relative" ref={exportRef}>
              <Button
                variant="secondary"
                size="sm"
                aria-expanded={exportOuvert}
                aria-haspopup="menu"
                icon={<ChevronDown className="size-4" aria-hidden="true" />}
                onClick={() => setExportOuvert((ouvert) => !ouvert)}
              >
                Exporter
              </Button>
              {exportOuvert && (
                <div role="menu" className="absolute right-0 z-40 mt-2 w-64 rounded-2xl border border-brand-900/10 bg-card p-3 shadow-lg">
                  {porteeExport && (
                    <>
                      <label htmlFor={porteeExport.id} className="text-xs font-semibold text-ink-900">
                        Étendue
                      </label>
                      <Select
                        id={porteeExport.id}
                        className="mt-1"
                        value={porteeExport.valeur}
                        onChange={(event) => porteeExport.onChange(event.target.value)}
                      >
                        {porteeExport.options.map((option) => (
                          <option key={option.valeur} value={option.valeur}>{option.libelle}</option>
                        ))}
                      </Select>
                    </>
                  )}
                  <div className={porteeExport ? "mt-3 flex flex-col gap-1" : "flex flex-col gap-1"}>
                    {formatsExport?.map((format) => (
                      <button
                        key={format.libelle}
                        type="button"
                        role="menuitem"
                        className="flex items-center gap-2 rounded-xl px-2 py-2 text-left text-sm font-semibold text-ink-900 hover:bg-surface-alt focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                        onClick={() => {
                          setExportOuvert(false);
                          format.onClick();
                        }}
                      >
                        <format.icone className="size-4 text-brand-900" aria-hidden="true" />
                        {format.libelle}
                      </button>
                    ))}
                  </div>
                  {exportEnCours && <p className="mt-2 text-xs text-muted-foreground">Préparation du fichier…</p>}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
      {puces.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {puces.map((puce) => (
            <button
              key={puce.libelle}
              type="button"
              onClick={puce.onRetirer}
              className="inline-flex items-center gap-1 rounded-full bg-surface-alt px-3 py-1 text-xs font-semibold text-brand-900"
            >
              {puce.libelle}
              <X className="size-3" aria-hidden="true" />
              <span className="sr-only">Retirer ce filtre</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
