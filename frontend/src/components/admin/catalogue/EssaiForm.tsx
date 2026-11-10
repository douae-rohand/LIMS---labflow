import { useState } from "react";
import { libelleTva, useTvaLaboratoire } from "@/api/tvaLaboratoire";
import { useQuery } from "@tanstack/react-query";
import { Plus, X } from "lucide-react";
import { Button, Input, Select, Textarea } from "@/components/lab";
import * as catalogueAdmin from "@/api/catalogueAdmin";
import { messageErreurCatalogueAdmin } from "@/api/catalogueAdmin";
import type {
  CreerEssaiRequest,
  DomaineDto,
  ExigenceEchantillonDto,
  ModifierEssaiRequest,
} from "@/api/catalogueAdmin";

export interface ExigenceEssaiFormValue {
  id?: number | undefined;
  nature: string;
  etatConservation: string;
  quantiteMinimale?: number | undefined;
  unite?: string | undefined;
}

interface EssaiFormProps {
  essai?: CreerEssaiRequest | ModifierEssaiRequest | undefined;
  isEditing?: boolean;
  codeEssai?: string | undefined;
  exigencesInitiales?: ExigenceEchantillonDto[] | undefined;
  domaines: DomaineDto[];
  onSubmit: (
    data: CreerEssaiRequest | ModifierEssaiRequest,
    exigences: ExigenceEssaiFormValue[],
    exigencesSupprimees: number[],
  ) => void;
  onSaveStep?: (
    etape: number,
    data: CreerEssaiRequest | ModifierEssaiRequest,
    exigences: ExigenceEssaiFormValue[],
    exigencesSupprimees: number[],
    fermerApresSauvegarde: boolean,
  ) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  error?: string | undefined;
}

export function EssaiForm({
  essai,
  isEditing = false,
  codeEssai,
  exigencesInitiales = [],
  domaines,
  onSubmit,
  onSaveStep,
  onCancel,
  isSubmitting,
  error,
}: EssaiFormProps) {
  const tva = useTvaLaboratoire();
  const [etape, setEtape] = useState(0);
  const [formData, setFormData] = useState<CreerEssaiRequest | ModifierEssaiRequest>(
    essai ?? { code: "", designation: "", tarif: 0, domaineId: 0 },
  );
  const [exigences, setExigences] = useState<ExigenceEssaiFormValue[]>(
    exigencesInitiales.map(({ id, nature, etatConservation, quantiteMinimale, unite }) => ({
      id, nature, etatConservation: etatConservation ?? "",
      quantiteMinimale: quantiteMinimale ?? undefined, unite: unite ?? undefined,
    })),
  );
  const [exigencesSupprimees, setExigencesSupprimees] = useState<number[]>([]);
  const suggestions = useQuery({
    queryKey: ["catalogue-admin-exigences-suggestions"],
    queryFn: catalogueAdmin.suggestionsExigences,
  });
  const suggestionsUniques = suggestions.data?.filter((suggestion, index, list) =>
    list.findIndex((item) =>
      item.nature === suggestion.nature &&
      item.etatConservation === suggestion.etatConservation &&
      item.quantiteMinimale === suggestion.quantiteMinimale &&
      item.unite === suggestion.unite,
    ) === index,
  ) ?? [];

  const mettreAJour = <K extends keyof (CreerEssaiRequest & ModifierEssaiRequest)>(
    champ: K,
    valeur: (CreerEssaiRequest & ModifierEssaiRequest)[K],
  ) => setFormData((actuel) => ({ ...actuel, [champ]: valeur }));

  const soumettre = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const bouton = (event.nativeEvent as SubmitEvent).submitter;
    const sauvegarderEtape = bouton instanceof HTMLButtonElement && bouton.value === "save-step";

    if (isEditing && (sauvegarderEtape || etape === 3)) {
      onSaveStep?.(etape, formData, exigences, exigencesSupprimees, etape === 3);
      return;
    }

    if (etape < 3) {
      setEtape((actuelle) => actuelle + 1);
      return;
    }
    onSubmit(formData, exigences, exigencesSupprimees);
  };

  return (
    <form onSubmit={soumettre} className="space-y-5">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Étape {etape + 1} sur 4</p>
        <div className="mt-2 grid grid-cols-4 gap-2" aria-label="Étapes du formulaire">
          {["Informations", "Description", "Limites", "Exigences"].map((nom, index) => (
            <div key={nom} className={`h-1.5 rounded-full ${index <= etape ? "bg-brand-900" : "bg-brand-900/10"}`} />
          ))}
        </div>
        <h4 className="mt-3 font-semibold text-ink-900">
          {["Informations de l'essai", "Description et méthode", "Durée et limites d'analyse", "Exigences d'échantillon"][etape]}
        </h4>
      </div>

      {etape === 0 && (
        <div className="space-y-4">
          <label className="block space-y-1 text-sm font-medium">
            Code *
            <Input value={"code" in formData ? formData.code : codeEssai ?? ""} onChange={(event) => mettreAJour("code", event.target.value.toUpperCase())} required maxLength={50} pattern="[A-Z0-9_-]+" title="Utilisez uniquement des lettres majuscules, chiffres, tirets ou underscores." />
          </label>
          <label className="block space-y-1 text-sm font-medium">
            Désignation *
            <Input value={formData.designation} onChange={(event) => mettreAJour("designation", event.target.value)} required maxLength={255} />
          </label>
          <label className="block space-y-1 text-sm font-medium">
            Domaine *
            <Select value={formData.domaineId ?? ""} onChange={(event) => mettreAJour("domaineId", Number(event.target.value))} required>
              <option value="">Choisir un domaine</option>
              {domaines.map((domaine) => <option key={domaine.id} value={domaine.id}>{domaine.libelle}</option>)}
            </Select>
          </label>
          <label className="block space-y-1 text-sm font-medium">
            Prix TTC (MAD) *
            <Input type="number" min="0.01" step="0.01" value={formData.tarif ?? 0} onChange={(event) => mettreAJour("tarif", Number(event.target.value))} required />
            {tva.data && (formData.tarif ?? 0) > 0 && (
              <span className="block text-xs font-normal text-muted-foreground">{libelleTva(formData.tarif ?? 0, tva.data.tauxEffectif)}</span>
            )}
          </label>
        </div>
      )}

      {etape === 1 && (
        <div className="space-y-4">
          <label className="space-y-1 text-sm font-medium sm:col-span-2">
            Description
            <Textarea value={formData.description ?? ""} onChange={(event) => mettreAJour("description", event.target.value)} rows={4} />
          </label>
          <label className="block space-y-1 text-sm font-medium sm:col-span-2">
            Méthode
            <Input value={formData.methode ?? ""} onChange={(event) => mettreAJour("methode", event.target.value)} />
          </label>
        </div>
      )}

      {etape === 2 && (
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block space-y-1 text-sm font-medium">
            Durée estimée (jours)
            <Input type="number" min="1" value={formData.dureeEstimee ?? ""} onChange={(event) => mettreAJour("dureeEstimee", event.target.value ? Number(event.target.value) : undefined)} required={essai === undefined} />
          </label>
          <label className="block space-y-1 text-sm font-medium">
            Unité
            <Input value={formData.unite ?? ""} onChange={(event) => mettreAJour("unite", event.target.value)} />
          </label>
          {([
            ["limiteMin", "Limite minimale"],
            ["limiteMax", "Limite maximale"],
            ["seuilCritiqueMin", "Seuil critique min"],
            ["seuilCritiqueMax", "Seuil critique max"],
          ] as const).map(([field, label]) => (
            <label key={field} className="block space-y-1 text-sm font-medium">
              {label}
              <Input type="number" step="0.01" value={formData[field] ?? ""} onChange={(event) => mettreAJour(field, event.target.value ? Number(event.target.value) : undefined)} />
            </label>
          ))}
        </div>
      )}

      {etape === 3 && (
        <div className="space-y-3">
          {suggestions.isError && <p role="alert" className="text-sm text-rose-700">{messageErreurCatalogueAdmin(suggestions.error)}</p>}
          {exigences.map((exigence, index) => (
            <div key={exigence.id ?? `nouvelle-${index}`} className="grid gap-3 rounded-xl border border-brand-900/10 p-3 sm:grid-cols-2">
              <label className="block space-y-1 text-sm font-medium sm:col-span-2">
                Combinaison déjà utilisée
                <Select
                  value=""
                  onChange={(event) => {
                    const suggestion = suggestionsUniques[Number(event.target.value)];
                    if (!suggestion) return;
                    setExigences((items) => items.map((item, i) => i === index ? {
                      ...item,
                      nature: suggestion.nature,
                      etatConservation: suggestion.etatConservation ?? "",
                      quantiteMinimale: suggestion.quantiteMinimale ?? undefined,
                      unite: suggestion.unite ?? undefined,
                    } : item));
                  }}
                >
                  <option value="">Choisir une suggestion ou saisir une nouvelle combinaison</option>
                  {suggestionsUniques.map((suggestion, suggestionIndex) => (
                    <option key={suggestion.id} value={suggestionIndex}>
                      {suggestion.nature} · {suggestion.etatConservation ?? "Conservation non précisée"} · {suggestion.quantiteMinimale ?? "Quantité non précisée"} {suggestion.unite ?? ""}
                    </option>
                  ))}
                </Select>
              </label>
              <label className="block space-y-1 text-sm font-medium sm:col-span-2">
                Nature *
                <Input value={exigence.nature} onChange={(event) => setExigences((items) => items.map((item, i) => i === index ? { ...item, nature: event.target.value } : item))} required maxLength={100} />
              </label>
              <label className="block space-y-1 text-sm font-medium">
                Conservation
                <Input value={exigence.etatConservation} onChange={(event) => setExigences((items) => items.map((item, i) => i === index ? { ...item, etatConservation: event.target.value } : item))} required maxLength={255} />
              </label>
              <label className="block space-y-1 text-sm font-medium">
                Quantité minimale
                <Input type="number" min="0" step="0.01" value={exigence.quantiteMinimale ?? ""} onChange={(event) => setExigences((items) => items.map((item, i) => i === index ? { ...item, quantiteMinimale: event.target.value ? Number(event.target.value) : undefined } : item))} />
              </label>
              <label className="block space-y-1 text-sm font-medium">
                Unité
                <Input value={exigence.unite ?? ""} onChange={(event) => setExigences((items) => items.map((item, i) => i === index ? { ...item, unite: event.target.value || undefined } : item))} />
              </label>
              <div className="flex items-end justify-end">
                <Button type="button" variant="ghost" size="sm" onClick={() => {
                  const id = exigence.id;
                  if (id !== undefined) setExigencesSupprimees((ids) => [...ids, id]);
                  setExigences((items) => items.filter((_, i) => i !== index));
                }}>
                  <X className="size-4" aria-hidden="true" /> Retirer
                </Button>
              </div>
            </div>
          ))}
          <Button type="button" variant="secondary" size="sm" icon={<Plus className="size-4" aria-hidden="true" />} onClick={() => setExigences((items) => [...items, { nature: "", etatConservation: "" }])}>
            Ajouter une exigence
          </Button>
          {exigences.length === 0 && <p className="text-sm text-muted-foreground">Aucune exigence d'échantillon définie.</p>}
        </div>
      )}

      {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}

      <div className="flex justify-between gap-2 border-t border-brand-900/10 pt-4">
        <Button type="button" variant="ghost" size="sm" onClick={etape === 0 ? onCancel : () => setEtape((current) => current - 1)}>
          {etape === 0 ? "Annuler" : "Précédent"}
        </Button>
        <div className="flex gap-2">
          {isEditing && etape < 3 && (
            <Button type="submit" name="action" value="save-step" variant="secondary" size="sm" disabled={isSubmitting}>
              {isSubmitting ? "Enregistrement..." : "Enregistrer l'étape"}
            </Button>
          )}
          <Button type="submit" name="action" value="continue" variant="primary" size="sm" disabled={isSubmitting || (etape === 0 && domaines.length === 0)}>
            {isSubmitting ? "Enregistrement..." : etape < 3 ? "Continuer" : isEditing ? "Enregistrer et terminer" : "Enregistrer"}
          </Button>
        </div>
      </div>
    </form>
  );
}
