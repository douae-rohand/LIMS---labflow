import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button, Input, Label, Select, Textarea } from "@/components/lab";
import { ApiError } from "@/api/axios";
import {
  activerAnalyse,
  creerAnalyse,
  desactiverAnalyse,
  messageErreurCatalogue,
  modifierAnalyse,
  type AnalyseCatalogue,
  type DomaineCatalogue,
  type EnregistrerAnalysePayload,
} from "@/api/catalogue";

const schema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "Le code est obligatoire.")
    .max(50, "50 caractères maximum.")
    .regex(/^[A-Za-z0-9][A-Za-z0-9_-]*$/, "Lettres, chiffres, tirets et underscores uniquement."),
  designation: z
    .string()
    .trim()
    .min(1, "La désignation est obligatoire.")
    .max(255, "255 caractères maximum."),
  domaineCode: z.string().trim().max(50),
  normeReference: z.string().trim().max(255, "255 caractères maximum."),
  prixUnitaire: z
    .string()
    .trim()
    .refine((valeur) => valeur === "" || /^\d+([.,]\d{1,2})?$/.test(valeur), "Prix invalide."),
  dureeEstimee: z
    .string()
    .trim()
    .min(1, "La durée estimée est obligatoire.")
    .refine((valeur) => /^\d+$/.test(valeur), "Indiquez un nombre de minutes.")
    .refine((valeur) => Number(valeur) <= 43200, "30 jours maximum."),
  uniteMesure: z.string().trim().max(50, "50 caractères maximum."),
  description: z.string().trim().max(4000, "4000 caractères maximum."),
});

type FormValues = z.infer<typeof schema>;

const VIDES: FormValues = {
  code: "",
  designation: "",
  domaineCode: "",
  normeReference: "",
  prixUnitaire: "",
  dureeEstimee: "",
  uniteMesure: "",
  description: "",
};

function versFormulaire(analyse: AnalyseCatalogue | null): FormValues {
  if (!analyse) return VIDES;
  const prix =
    analyse.prixUnitaire == null || analyse.prixUnitaire === ""
      ? ""
      : String(analyse.prixUnitaire);
  return {
    code: analyse.code,
    designation: analyse.designation,
    domaineCode: analyse.domaineCode ?? "",
    normeReference: analyse.normeReference ?? "",
    prixUnitaire: prix,
    dureeEstimee: analyse.dureeEstimee == null ? "" : String(analyse.dureeEstimee),
    uniteMesure: analyse.uniteMesure ?? "",
    description: analyse.description ?? "",
  };
}

function versPayload(values: FormValues): EnregistrerAnalysePayload {
  const prixBrut = values.prixUnitaire.replace(",", ".");
  return {
    code: values.code.trim(),
    designation: values.designation.trim(),
    description: values.description.trim() || undefined,
    normeReference: values.normeReference.trim() || undefined,
    dureeEstimee: Number(values.dureeEstimee),
    prixUnitaire: prixBrut === "" ? undefined : Number(prixBrut),
    uniteMesure: values.uniteMesure.trim() || undefined,
    domaineCode: values.domaineCode.trim() || undefined,
  };
}

interface CatalogueFormulaireProps {
  open: boolean;
  analyse: AnalyseCatalogue | null;
  domaines: DomaineCatalogue[];
  onOpenChange: (open: boolean) => void;
}

export function CatalogueFormulaire({
  open,
  analyse,
  domaines,
  onOpenChange,
}: CatalogueFormulaireProps) {
  const queryClient = useQueryClient();
  const edition = analyse !== null;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: versFormulaire(analyse),
    mode: "onBlur",
  });

  const { reset } = form;
  useEffect(() => {
    if (open) {
      reset(versFormulaire(analyse));
    }
  }, [open, analyse, reset]);

  function invalider() {
    void queryClient.invalidateQueries({ queryKey: ["catalogue"] });
  }

  function appliquerErreursServeur(err: unknown) {
    if (!(err instanceof ApiError) || err.status !== 400 || !err.data || typeof err.data !== "object") {
      toast.error(messageErreurCatalogue(err));
      return;
    }
    const champs = err.data as Record<string, unknown>;
    let premier: keyof FormValues | null = null;
    for (const [champ, message] of Object.entries(champs)) {
      if (typeof message !== "string") continue;
      const cible = champ === "delaiJours" ? "dureeEstimee" : champ;
      if (cible in form.getValues()) {
        const cle = cible as keyof FormValues;
        form.setError(cle, { type: "server", message });
        if (!premier) premier = cle;
      }
    }
    if (premier) {
      form.setFocus(premier);
      return;
    }
    toast.error(messageErreurCatalogue(err));
  }

  const enregistrement = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = versPayload(values);
      return edition && analyse ? modifierAnalyse(analyse.id, payload) : creerAnalyse(payload);
    },
    onSuccess: () => {
      invalider();
      toast.success(edition ? "Analyse mise à jour." : "Analyse ajoutée au catalogue.");
      onOpenChange(false);
    },
    onError: appliquerErreursServeur,
  });

  const activation = useMutation({
    mutationFn: () => {
      if (!analyse) throw new Error("Aucune analyse sélectionnée.");
      return analyse.actif ? desactiverAnalyse(analyse.id) : activerAnalyse(analyse.id);
    },
    onSuccess: (maj) => {
      invalider();
      toast.success(maj.actif ? "Analyse visible aux clients." : "Analyse retirée du catalogue public.");
      onOpenChange(false);
    },
    onError: (err: unknown) => toast.error(messageErreurCatalogue(err)),
  });

  const enCours = enregistrement.isPending || activation.isPending;
  const erreurs = form.formState.errors;

  return (
    <Dialog open={open} onOpenChange={(suivant) => !enCours && onOpenChange(suivant)}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl sm:rounded-3xl">
        <DialogHeader>
          <DialogTitle>{edition ? "Modifier l'analyse" : "Nouvelle analyse"}</DialogTitle>
          <DialogDescription>
            Cette fiche est enregistrée dans le catalogue de votre laboratoire. Elle n'apparaît
            pas dans les catalogues des autres laboratoires.
          </DialogDescription>
        </DialogHeader>

        <form
          className="grid gap-4 sm:grid-cols-2"
          onSubmit={form.handleSubmit((values) => enregistrement.mutate(values))}
          noValidate
        >
          <div className="space-y-1.5">
            <Label htmlFor="analyse-code">Code</Label>
            <Input id="analyse-code" autoComplete="off" {...form.register("code")} />
            {erreurs.code && <p className="text-xs text-red-600">{erreurs.code.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="analyse-domaine">Domaine</Label>
            <Select id="analyse-domaine" {...form.register("domaineCode")}>
              <option value="">Sans domaine</option>
              {domaines.map((domaine) => (
                <option key={domaine.code} value={domaine.code}>
                  {domaine.libelle}
                </option>
              ))}
            </Select>
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor="analyse-designation">Désignation</Label>
            <Input id="analyse-designation" {...form.register("designation")} />
            {erreurs.designation && (
              <p className="text-xs text-red-600">{erreurs.designation.message}</p>
            )}
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor="analyse-methode">Méthode ou norme</Label>
            <Input id="analyse-methode" {...form.register("normeReference")} />
            {erreurs.normeReference && (
              <p className="text-xs text-red-600">{erreurs.normeReference.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="analyse-prix">Prix (MAD)</Label>
            <Input id="analyse-prix" inputMode="decimal" placeholder="0,00" {...form.register("prixUnitaire")} />
            {erreurs.prixUnitaire && (
              <p className="text-xs text-red-600">{erreurs.prixUnitaire.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="analyse-duree">Durée estimée (minutes)</Label>
            <Input id="analyse-duree" inputMode="numeric" {...form.register("dureeEstimee")} />
            {erreurs.dureeEstimee && (
              <p className="text-xs text-red-600">{erreurs.dureeEstimee.message}</p>
            )}
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor="analyse-unite">Unité de mesure</Label>
            <Input id="analyse-unite" placeholder="g/L, mmol/L…" {...form.register("uniteMesure")} />
            {erreurs.uniteMesure && (
              <p className="text-xs text-red-600">{erreurs.uniteMesure.message}</p>
            )}
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <Label htmlFor="analyse-description">Description</Label>
            <Textarea id="analyse-description" {...form.register("description")} />
            {erreurs.description && (
              <p className="text-xs text-red-600">{erreurs.description.message}</p>
            )}
          </div>

          <DialogFooter className="sm:col-span-2 sm:justify-between">
            {edition && analyse ? (
              <Button
                type="button"
                variant="secondary"
                loading={activation.isPending}
                disabled={enCours}
                onClick={() => activation.mutate()}
              >
                {analyse.actif ? "Retirer du catalogue" : "Rendre visible"}
              </Button>
            ) : (
              <span />
            )}
            <div className="flex gap-2">
              <Button
                type="button"
                variant="ghost"
                disabled={enCours}
                onClick={() => onOpenChange(false)}
              >
                Annuler
              </Button>
              <Button type="submit" loading={enregistrement.isPending} disabled={enCours}>
                {edition ? "Enregistrer" : "Ajouter"}
              </Button>
            </div>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
