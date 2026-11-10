import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button, Input, Label, Select } from "@/components/lab";
import { enregistrerPatient, type FichePatient, type Patient } from "@/api/accueil";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { messageErreur } from "@/features/responsable/commun";

function ficheDe(patient: Patient | null): FichePatient {
  return {
    nom: patient?.nom ?? "",
    prenom: patient?.prenom ?? "",
    dateNaissance: patient?.dateNaissance ?? null,
    sexe: patient?.sexe ?? "",
    cin: patient?.cin ?? "",
    telephone: patient?.telephone ?? "",
    email: patient?.email ?? "",
    adresse: patient?.adresse ?? "",
    // La fiche existante a été créée avec le consentement du patient.
    consentementCndp: patient != null,
  };
}

function erreurs(f: FichePatient): string[] {
  const aujourdhui = new Date().toISOString().slice(0, 10);
  return [
    !f.nom.trim() ? "Le nom est obligatoire." : null,
    !f.prenom.trim() ? "Le prénom est obligatoire." : null,
    f.dateNaissance && f.dateNaissance >= aujourdhui ? "La date de naissance doit être dans le passé." : null,
    f.cin.trim() && !/^[A-Za-z]{1,2}[0-9]{3,8}$/.test(f.cin.trim()) ? "CIN invalide (ex. AB123456)." : null,
    f.telephone.trim() && !/^\+?[0-9 ]{8,20}$/.test(f.telephone.trim()) ? "Téléphone invalide." : null,
    f.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim()) ? "E-mail invalide." : null,
    !f.consentementCndp ? "Le consentement CNDP du patient est obligatoire." : null,
  ].filter((m): m is string => m != null);
}

/** Création (patient = null) ou modification d'une fiche patient. */
export function DialoguePatient({ ouvert, patient, onFermer }: {
  ouvert: boolean;
  patient: Patient | null;
  onFermer: () => void;
}) {
  const queryClient = useQueryClient();
  const [fiche, setFiche] = useState<FichePatient>(ficheDe(null));
  const [tente, setTente] = useState(false);

  useEffect(() => {
    if (ouvert) {
      setFiche(ficheDe(patient));
      setTente(false);
    }
  }, [ouvert, patient]);

  const envoi = useMutation({
    mutationFn: () => enregistrerPatient({ ...fiche, dateNaissance: fiche.dateNaissance || null }, patient?.id),
    onSuccess: async (enregistre) => {
      toast.success(patient ? "Fiche patient mise à jour." : `Patient enregistré sous le code ${enregistre.code}.`);
      await queryClient.invalidateQueries({ queryKey: ["accueil-patients"] });
      onFermer();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La fiche patient n'a pas pu être enregistrée.")),
  });

  const problemes = erreurs(fiche);
  const champ = (cle: keyof FichePatient) => (event: { target: { value: string } }) =>
    setFiche((f) => ({ ...f, [cle]: event.target.value }));

  return (
    <Dialogue ouvert={ouvert} titre={patient ? `Patient ${patient.code}` : "Enregistrer un patient"} onFermer={onFermer}
      description="Fiche du patient sur lequel portent les analyses médicales.">
      <form className="flex flex-col gap-3 text-sm" onSubmit={(event) => {
        event.preventDefault();
        setTente(true);
        if (problemes.length === 0) envoi.mutate();
      }}>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nom-patient">Nom</Label>
            <Input id="nom-patient" value={fiche.nom} maxLength={100} onChange={champ("nom")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="prenom-patient">Prénom</Label>
            <Input id="prenom-patient" value={fiche.prenom} maxLength={100} onChange={champ("prenom")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="naissance-patient">Date de naissance</Label>
            <Input id="naissance-patient" type="date" max={new Date().toISOString().slice(0, 10)}
              value={fiche.dateNaissance ?? ""} onChange={(e) => setFiche((f) => ({ ...f, dateNaissance: e.target.value || null }))} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="sexe-patient">Sexe</Label>
            <Select id="sexe-patient" value={fiche.sexe} onChange={champ("sexe")}>
              <option value="">Non précisé</option>
              <option value="F">Féminin</option>
              <option value="M">Masculin</option>
            </Select>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="cin-patient">CIN</Label>
            <Input id="cin-patient" value={fiche.cin} maxLength={10} placeholder="AB123456" onChange={champ("cin")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="telephone-patient">Téléphone</Label>
            <Input id="telephone-patient" type="tel" value={fiche.telephone} onChange={champ("telephone")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email-patient">E-mail (facultatif)</Label>
            <Input id="email-patient" type="email" value={fiche.email} maxLength={255} onChange={champ("email")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="adresse-patient">Adresse (facultatif)</Label>
            <Input id="adresse-patient" value={fiche.adresse} maxLength={500} onChange={champ("adresse")} />
          </div>
        </div>
        {!patient && (
          <label className="flex items-start gap-3 rounded-2xl border border-brand-900/10 bg-surface-alt/50 px-3 py-2 text-xs text-ink-900">
            <input type="checkbox" className="mt-0.5 size-4 accent-brand-900" checked={fiche.consentementCndp}
              onChange={(e) => setFiche((f) => ({ ...f, consentementCndp: e.target.checked }))} />
            Le patient (ou son représentant légal) consent au traitement de ses données de santé par le laboratoire,
            conformément à la loi 09-08 (CNDP).
          </label>
        )}
        {tente && problemes.length > 0 && (
          <ul className="list-disc pl-5 text-xs text-red-700">{problemes.map((p) => <li key={p}>{p}</li>)}</ul>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onFermer}>Annuler</Button>
          <Button type="submit" loading={envoi.isPending}>{patient ? "Enregistrer les modifications" : "Enregistrer le patient"}</Button>
        </div>
      </form>
    </Dialogue>
  );
}
