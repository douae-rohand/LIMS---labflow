import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button, Input, Label } from "@/components/lab";
import { creerClient, rattacherClient, type NouveauClient, type TypeClient } from "@/api/accueil";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { messageErreur } from "@/features/responsable/commun";

const VIDE: NouveauClient = {
  type: "PARTICULIER",
  nom: "",
  prenom: "",
  email: "",
  telephone: "",
  raisonSociale: "",
  ice: "",
  adresse: "",
  consentementCndp: false,
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Mêmes règles que le serveur, pour signaler les erreurs avant l'envoi. */
function erreurs(c: NouveauClient): string[] {
  return [
    !c.nom.trim() ? "Le nom est obligatoire." : null,
    !EMAIL.test(c.email.trim()) ? "Indiquez un e-mail valide : le client y recevra le lien d'activation." : null,
    c.telephone.trim() && !/^\+?[0-9 ]{8,20}$/.test(c.telephone.trim()) ? "Téléphone invalide." : null,
    c.type === "ENTREPRISE" && !c.raisonSociale.trim() ? "La raison sociale est obligatoire pour une entreprise." : null,
    c.ice.trim() && !/^[0-9]{15}$/.test(c.ice.trim()) ? "L'ICE comporte 15 chiffres." : null,
    !c.consentementCndp ? "Le consentement CNDP du client est obligatoire." : null,
  ].filter((m): m is string => m != null);
}

export function DialogueClient({ ouvert, onFermer }: { ouvert: boolean; onFermer: () => void }) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<"nouveau" | "existant">("nouveau");
  const [client, setClient] = useState<NouveauClient>(VIDE);
  const [emailExistant, setEmailExistant] = useState("");
  const [tente, setTente] = useState(false);

  useEffect(() => {
    if (ouvert) {
      setMode("nouveau");
      setClient(VIDE);
      setEmailExistant("");
      setTente(false);
    }
  }, [ouvert]);

  const terminer = async (message: string) => {
    toast.success(message);
    await queryClient.invalidateQueries({ queryKey: ["accueil-clients"] });
    onFermer();
  };
  const creation = useMutation({
    mutationFn: () => creerClient({ ...client, email: client.email.trim() }),
    onSuccess: (cree) => terminer(`Client enregistré. ${cree.email} a reçu le lien pour activer son espace.`),
    onError: (erreur) => toast.error(messageErreur(erreur, "Le client n'a pas pu être enregistré.")),
  });
  const rattachement = useMutation({
    mutationFn: () => rattacherClient(emailExistant.trim()),
    onSuccess: (rattache) => terminer(`${rattache.nomComplet} est maintenant client du laboratoire.`),
    onError: (erreur) => toast.error(messageErreur(erreur, "Le client n'a pas pu être rattaché.")),
  });

  const problemes = erreurs(client);
  const champ = (cle: keyof NouveauClient) => (event: { target: { value: string } }) =>
    setClient((c) => ({ ...c, [cle]: event.target.value }));

  return (
    <Dialogue ouvert={ouvert} titre="Enregistrer un client" onFermer={onFermer}
      description="Le client reçoit un e-mail pour choisir son mot de passe et suivre ses demandes en ligne.">
      <div className="flex flex-col gap-4 text-sm">
        <div className="inline-flex gap-1 rounded-2xl border border-brand-900/10 bg-card p-1" role="tablist" aria-label="Type d'enregistrement">
          {([["nouveau", "Nouveau client"], ["existant", "Client déjà inscrit"]] as const).map(([id, libelle]) => (
            <button key={id} type="button" role="tab" aria-selected={mode === id} onClick={() => setMode(id)}
              className={`flex-1 rounded-xl px-3 py-2 text-sm font-semibold ${mode === id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"}`}>
              {libelle}
            </button>
          ))}
        </div>

        {mode === "existant" ? (
          <form className="flex flex-col gap-3" onSubmit={(event) => {
            event.preventDefault();
            if (EMAIL.test(emailExistant.trim())) rattachement.mutate();
          }}>
            <p className="text-xs text-muted-foreground">
              Le client a déjà un compte LabFlow (inscrit en ligne ou dans un autre laboratoire) : saisissez son e-mail
              exact pour l'ajouter à vos clients, sans créer de doublon.
            </p>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email-existant">E-mail du client</Label>
              <Input id="email-existant" type="email" value={emailExistant} onChange={(e) => setEmailExistant(e.target.value)} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={onFermer}>Annuler</Button>
              <Button type="submit" loading={rattachement.isPending} disabled={!EMAIL.test(emailExistant.trim())}>Rattacher</Button>
            </div>
          </form>
        ) : (
          <form className="flex flex-col gap-3" onSubmit={(event) => {
            event.preventDefault();
            setTente(true);
            if (problemes.length === 0) creation.mutate();
          }}>
            <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Type de client">
              {(["PARTICULIER", "ENTREPRISE"] as TypeClient[]).map((type) => (
                <label key={type} className={`inline-flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 font-semibold ${
                  client.type === type ? "border-brand-900 bg-brand-900/5 text-brand-900" : "border-brand-900/10 text-muted-foreground"}`}>
                  <input type="radio" name="type-client" className="size-4 accent-brand-900" checked={client.type === type}
                    onChange={() => setClient((c) => ({ ...c, type }))} />
                  {type === "PARTICULIER" ? "Particulier" : "Entreprise"}
                </label>
              ))}
            </div>
            {client.type === "ENTREPRISE" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="raison-sociale">Raison sociale</Label>
                  <Input id="raison-sociale" value={client.raisonSociale} maxLength={255} onChange={champ("raisonSociale")} />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="ice-client">ICE (15 chiffres)</Label>
                  <Input id="ice-client" inputMode="numeric" value={client.ice} maxLength={15} onChange={champ("ice")} />
                </div>
              </div>
            )}
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="nom-client">{client.type === "ENTREPRISE" ? "Nom du contact" : "Nom"}</Label>
                <Input id="nom-client" value={client.nom} maxLength={100} onChange={champ("nom")} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prenom-client">Prénom</Label>
                <Input id="prenom-client" value={client.prenom} maxLength={100} onChange={champ("prenom")} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="email-client">E-mail</Label>
                <Input id="email-client" type="email" value={client.email} maxLength={255} onChange={champ("email")} />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="telephone-client">Téléphone</Label>
                <Input id="telephone-client" type="tel" value={client.telephone} placeholder="+212 6…" onChange={champ("telephone")} />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="adresse-client">Adresse (facultatif)</Label>
              <Input id="adresse-client" value={client.adresse} maxLength={500} onChange={champ("adresse")} />
            </div>
            <label className="flex items-start gap-3 rounded-2xl border border-brand-900/10 bg-surface-alt/50 px-3 py-2 text-xs text-ink-900">
              <input type="checkbox" className="mt-0.5 size-4 accent-brand-900" checked={client.consentementCndp}
                onChange={(e) => setClient((c) => ({ ...c, consentementCndp: e.target.checked }))} />
              Le client consent au traitement de ses données personnelles par le laboratoire, conformément à la loi
              09-08 (CNDP). Il a été informé de ses droits d'accès, de rectification et d'opposition.
            </label>
            {tente && problemes.length > 0 && (
              <ul className="list-disc pl-5 text-xs text-red-700">{problemes.map((p) => <li key={p}>{p}</li>)}</ul>
            )}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={onFermer}>Annuler</Button>
              <Button type="submit" loading={creation.isPending}>Enregistrer et inviter</Button>
            </div>
          </form>
        )}
      </div>
    </Dialogue>
  );
}
