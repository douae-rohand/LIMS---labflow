import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { Button, Input, Label } from "@/components/lab";
import {
  enregistrerFacturation,
  enregistrerIdentite,
  lireFacturation,
  lireIdentite,
  type IdentitePlateforme,
} from "@/api/parametresPlateforme";
import { EtatVide } from "../EtatVide";
import { messageErreur } from "../texte";

function Carte({ titre, description, children }: { titre: string; description: string; children: ReactNode }) {
  return (
    <section className="rounded-3xl border border-brand-900/10 bg-card p-5 sm:p-6">
      <div className="mb-4">
        <h2 className="text-lg font-extrabold text-ink-900">{titre}</h2>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{description}</p>
      </div>
      {children}
    </section>
  );
}

// ---------------------------------------------------------------------------
// Identité de la plateforme
// ---------------------------------------------------------------------------

export function SectionIdentite() {
  const queryClient = useQueryClient();
  const identite = useQuery({ queryKey: ["parametres-identite"], queryFn: lireIdentite });
  const [valeurs, setValeurs] = useState<IdentitePlateforme | null>(null);

  useEffect(() => {
    if (identite.data) setValeurs(identite.data);
  }, [identite.data]);

  const enregistrement = useMutation({
    mutationFn: (donnees: IdentitePlateforme) => enregistrerIdentite(donnees),
    onSuccess: async (donnees) => {
      setValeurs(donnees);
      queryClient.setQueryData(["parametres-identite"], donnees);
      toast.success("Identité de la plateforme enregistrée.");
      await queryClient.invalidateQueries({ queryKey: ["parametres-publics"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  if (identite.isLoading || !valeurs) {
    return identite.isError
      ? <EtatVide titre="L'identité de la plateforme n'a pas pu être chargée." />
      : <p className="text-sm text-muted-foreground">Chargement…</p>;
  }

  const modifie = JSON.stringify(valeurs) !== JSON.stringify(identite.data);
  const champ = (cle: keyof IdentitePlateforme) => ({
    value: valeurs[cle],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) =>
      setValeurs({ ...valeurs, [cle]: event.target.value }),
  });

  return (
    <form
      className="flex flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault();
        if (!valeurs.nom.trim() || !valeurs.expediteurNom.trim()) {
          toast.error("Le nom de la plateforme et le nom d'expéditeur sont obligatoires.");
          return;
        }
        enregistrement.mutate(valeurs);
      }}
    >
      <Carte
        titre="Coordonnées"
        description="Affichées dans le pied de page du site public. Le nom d'expéditeur apparaît dans la boîte de réception des destinataires de tous les e-mails."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="id-nom">Nom de la plateforme</Label>
            <Input id="id-nom" required maxLength={100} {...champ("nom")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="id-expediteur">Nom d'expéditeur des e-mails</Label>
            <Input id="id-expediteur" required maxLength={100} {...champ("expediteurNom")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="id-email">E-mail de contact</Label>
            <Input id="id-email" type="email" maxLength={255} {...champ("emailContact")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="id-telephone">Téléphone</Label>
            <Input id="id-telephone" type="tel" maxLength={50} {...champ("telephone")} />
          </div>
          <div className="flex flex-col gap-1.5 sm:col-span-2">
            <Label htmlFor="id-adresse">Adresse</Label>
            <Input id="id-adresse" maxLength={500} {...champ("adresse")} />
          </div>
        </div>
      </Carte>


      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" disabled={!modifie} onClick={() => identite.data && setValeurs(identite.data)}>
          Annuler les modifications
        </Button>
        <Button type="submit" loading={enregistrement.isPending} disabled={!modifie}>
          Enregistrer
        </Button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// TVA de la plateforme
// ---------------------------------------------------------------------------

const MAD = new Intl.NumberFormat("fr-MA", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function SectionTva() {
  const queryClient = useQueryClient();
  const facturation = useQuery({ queryKey: ["parametres-facturation"], queryFn: lireFacturation });
  const [taux, setTaux] = useState("");

  useEffect(() => {
    if (facturation.data) setTaux(String(facturation.data.tauxTva));
  }, [facturation.data]);

  const enregistrement = useMutation({
    mutationFn: (valeur: number) => enregistrerFacturation({ tauxTva: valeur }),
    onSuccess: (donnees) => {
      queryClient.setQueryData(["parametres-facturation"], donnees);
      toast.success("Taux de TVA enregistré.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  if (facturation.isLoading || !facturation.data) {
    return facturation.isError
      ? <EtatVide titre="Le taux de TVA n'a pas pu être chargé." />
      : <p className="text-sm text-muted-foreground">Chargement…</p>;
  }

  const valeur = Number(taux.replace(",", "."));
  const valide = taux.trim() !== "" && Number.isFinite(valeur) && valeur >= 0 && valeur <= 100;
  const modifie = valide && valeur !== facturation.data.tauxTva;
  const exemple = 120;
  const tvaExemple = valide ? (exemple * valeur) / (100 + valeur) : 0;

  return (
    <form
      className="flex flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault();
        if (valide) enregistrement.mutate(Math.round(valeur * 100) / 100);
      }}
    >
      <Carte
        titre="Taux de TVA de la plateforme"
        description="Les laboratoires saisissent leurs prix TTC. Ce taux sert à calculer la part de TVA comprise dans chaque prix et chaque facture ; un laboratoire peut le remplacer par son propre taux dans ses paramètres. Une facture garde le taux en vigueur à son émission."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex max-w-xs flex-col gap-1.5">
            <Label htmlFor="taux-tva">Taux de TVA (%)</Label>
            <Input
              id="taux-tva"
              type="number"
              inputMode="decimal"
              min={0}
              max={100}
              step="0.01"
              value={taux}
              onChange={(event) => setTaux(event.target.value)}
            />
            <p className="text-xs text-muted-foreground">Entre 0 et 100 %. Au Maroc, le taux normal est de 20 %.</p>
          </div>
          <div className="rounded-2xl border border-brand-900/10 bg-surface-alt p-4 text-sm">
            <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">Exemple</p>
            <p className="mt-2 text-ink-900">Prix TTC saisi : <strong>{MAD.format(exemple)} DH</strong></p>
            <p className="text-ink-900">dont TVA ({valide ? valeur : "—"} %) : <strong>{MAD.format(tvaExemple)} DH</strong></p>
            <p className="text-ink-900">soit HT : <strong>{MAD.format(exemple - tvaExemple)} DH</strong></p>
          </div>
        </div>
      </Carte>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" disabled={!modifie} onClick={() => setTaux(String(facturation.data.tauxTva))}>
          Annuler les modifications
        </Button>
        <Button type="submit" loading={enregistrement.isPending} disabled={!modifie}>
          Enregistrer
        </Button>
      </div>
    </form>
  );
}
