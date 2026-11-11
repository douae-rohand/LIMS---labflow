import { useEffect, useState } from "react";
import { Printer, Sheet } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import { ouvrirEtiquettes, urlCodeBarres, type EchantillonRecu, type FormatEtiquettes } from "@/api/accueil";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { formaterDate, messageErreur } from "@/features/responsable/commun";

/** Aperçu à l'échelle de l'étiquette d'un échantillon (70 × 37 mm), avec impression PDF. */
export function DialogueEtiquette({ echantillon, demandeId, demandeReference, demandeur, laboratoire, onFermer }: {
  echantillon: EchantillonRecu | null;
  demandeId: number;
  demandeReference: string;
  demandeur: string;
  laboratoire?: string | null | undefined;
  onFermer: () => void;
}) {
  const [codeBarres, setCodeBarres] = useState<string | null>(null);
  const [erreur, setErreur] = useState(false);
  const [impression, setImpression] = useState<FormatEtiquettes | null>(null);
  const echantillonId = echantillon?.id;

  useEffect(() => {
    if (echantillonId == null) return;
    let actif = true;
    let url: string | null = null;
    setCodeBarres(null);
    setErreur(false);
    urlCodeBarres(demandeId, echantillonId)
      .then((u) => {
        url = u;
        if (actif) setCodeBarres(u);
        else URL.revokeObjectURL(u);
      })
      .catch(() => { if (actif) setErreur(true); });
    return () => {
      actif = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [demandeId, echantillonId]);

  // Échap ferme seulement cette fenêtre, pas le panneau de réception dessous.
  useEffect(() => {
    if (echantillonId == null) return;
    const gerer = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.stopImmediatePropagation();
        onFermer();
      }
    };
    window.addEventListener("keydown", gerer, true);
    return () => window.removeEventListener("keydown", gerer, true);
  }, [echantillonId, onFermer]);

  const imprimer = (format: FormatEtiquettes) => {
    if (echantillonId == null) return;
    setImpression(format);
    void ouvrirEtiquettes(demandeId, { echantillonId, format })
      .catch((e: unknown) => toast.error(messageErreur(e, "L'étiquette n'a pas pu être générée.")))
      .finally(() => setImpression(null));
  };

  return (
    <Dialogue ouvert={echantillon != null} titre="Étiquette de l'échantillon"
      description="Aperçu à l'échelle ; le code-barres se lit avec une douchette." onFermer={onFermer}>
      {echantillon && (
        <div className="flex flex-col items-center gap-4">
          {/* 70 × 37 mm, rendu proche de l'impression. */}
          <div className="w-full max-w-[22rem] overflow-hidden rounded-lg border border-slate-300 bg-white text-black shadow-sm"
            style={{ aspectRatio: "70 / 37" }}>
            <div className="flex items-center justify-between gap-2 bg-brand-900 px-2.5 py-1 text-[10px] text-white">
              <span className="truncate font-bold">{laboratoire || "Laboratoire"}</span>
              <span className="shrink-0">Dem. {demandeReference}</span>
            </div>
            <div className="flex h-[30%] items-center justify-center px-6 pt-1.5">
              {codeBarres ? (
                <img src={codeBarres} alt={`Code-barres ${echantillon.reference}`} className="h-full w-full object-fill [image-rendering:pixelated]" />
              ) : (
                <div className={`h-full w-full rounded ${erreur ? "bg-red-50" : "animate-pulse bg-slate-100"}`} />
              )}
            </div>
            <p className="text-center font-mono text-[13px] font-bold tracking-wide">{echantillon.reference}</p>
            <div className="px-3 leading-tight">
              <p className="truncate text-[11px] font-bold">{demandeur}</p>
              <p className="truncate text-[9px] text-slate-600">
                {echantillon.nature} · {formaterDate(echantillon.dateReception, true)}
                {echantillon.etatConservationConstate ? ` · ${echantillon.etatConservationConstate}` : ""}
              </p>
              {echantillon.analyses.length > 0 && (
                <p className="truncate text-[9px] text-slate-600">Analyses : {echantillon.analyses.map((x) => /\(([^)]+)\)$/.exec(x)?.[1] ?? x).join(", ")}</p>
              )}
            </div>
            <div className="flex justify-end px-3 pt-0.5">
              <span className={`rounded-sm px-1 text-[8px] font-bold ${echantillon.conforme ? "bg-mint-200 text-brand-900" : "bg-red-700 text-white"}`}>
                {echantillon.conforme ? "CONFORME" : "NON CONFORME"}
              </span>
            </div>
          </div>
          {erreur && <p className="text-xs text-red-700">Le code-barres n'a pas pu être chargé.</p>}

          <div className="flex w-full flex-wrap justify-end gap-2">
            <Button variant="ghost" onClick={onFermer}>Fermer</Button>
            <Button variant="secondary" icon={<Sheet className="size-4" aria-hidden="true" />} iconPosition="left"
              loading={impression === "PLANCHE"} disabled={impression != null} onClick={() => imprimer("PLANCHE")}>
              Planche A4
            </Button>
            <Button icon={<Printer className="size-4" aria-hidden="true" />} iconPosition="left"
              loading={impression === "UNITAIRE"} disabled={impression != null} onClick={() => imprimer("UNITAIRE")}>
              Imprimer l'étiquette
            </Button>
          </div>
        </div>
      )}
    </Dialogue>
  );
}
