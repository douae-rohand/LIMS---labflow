import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label, Textarea } from "@/components/lab";
import { Switch } from "@/components/ui/switch";
import {
  creerTypeDocument,
  creerTypeLaboratoire,
  enregistrerSecurite,
  lireSecurite,
  listerTypesDocuments,
  listerTypesLaboratoire,
  modifierTypeDocument,
  modifierTypeLaboratoire,
  type ParametresSecurite,
} from "@/api/parametresPlateforme";
import type { TypeDocumentIntegrationDto } from "@/api/plateforme";
import type { RoleUtilisateur } from "@/api/session";
import { Dialogue } from "../Fenetres";
import { EtatVide } from "../EtatVide";
import { messageErreur } from "../texte";

export type OngletParametres = "securite" | "documents" | "types";

const ONGLETS: { id: OngletParametres; libelle: string }[] = [
  { id: "securite", libelle: "Sécurité" },
  { id: "documents", libelle: "Documents d'intégration" },
  { id: "types", libelle: "Types de laboratoire" },
];

const ROLES: { code: RoleUtilisateur; libelle: string }[] = [
  { code: "SUPER_ADMINISTRATEUR", libelle: "Super Administrateur" },
  { code: "ADMINISTRATEUR", libelle: "Administrateur de laboratoire" },
  { code: "RESPONSABLE", libelle: "Responsable de laboratoire" },
  { code: "TECHNICIEN", libelle: "Technicien" },
  { code: "ACCUEIL", libelle: "Personnel d'accueil" },
  { code: "CLIENT", libelle: "Client" },
];

export function PageParametres({
  onglet,
  onChangerOnglet,
}: {
  onglet: OngletParametres;
  onChangerOnglet: (onglet: OngletParametres) => void;
}) {
  return (
    <>
      <div
        className="mb-5 inline-flex w-full flex-wrap gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto"
        role="tablist"
        aria-label="Sections des paramètres"
      >
        {ONGLETS.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={onglet === item.id}
            onClick={() => onChangerOnglet(item.id)}
            className={`inline-flex flex-1 items-center justify-center rounded-xl px-4 py-2 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 sm:flex-none ${
              onglet === item.id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"
            }`}
          >
            {item.libelle}
          </button>
        ))}
      </div>
      {onglet === "securite" && <SectionSecurite />}
      {onglet === "documents" && <SectionDocuments />}
      {onglet === "types" && <SectionTypesLaboratoire />}
    </>
  );
}

function Carte({ titre, description, action, children }: {
  titre: string;
  description: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-3xl border border-brand-900/10 bg-card p-5 sm:p-6">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-extrabold text-ink-900">{titre}</h2>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

// ---------------------------------------------------------------------------
// Sécurité
// ---------------------------------------------------------------------------

function SectionSecurite() {
  const queryClient = useQueryClient();
  const securite = useQuery({ queryKey: ["parametres-securite"], queryFn: lireSecurite });
  const [valeurs, setValeurs] = useState<ParametresSecurite | null>(null);

  useEffect(() => {
    if (securite.data) setValeurs(securite.data);
  }, [securite.data]);

  const enregistrement = useMutation({
    mutationFn: (donnees: ParametresSecurite) => enregistrerSecurite(donnees),
    onSuccess: async (donnees) => {
      setValeurs(donnees);
      toast.success("Paramètres de sécurité enregistrés.");
      await queryClient.invalidateQueries({ queryKey: ["parametres-securite"] });
      await queryClient.invalidateQueries({ queryKey: ["parametres-publics"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  if (securite.isLoading || !valeurs) {
    return securite.isError
      ? <EtatVide titre="Les paramètres de sécurité n'ont pas pu être chargés." />
      : <p className="text-sm text-muted-foreground">Chargement…</p>;
  }

  const modifie = JSON.stringify(valeurs) !== JSON.stringify(securite.data);
  const basculerRole = (role: RoleUtilisateur, coche: boolean) =>
    setValeurs({
      ...valeurs,
      roles2faObligatoire: coche
        ? [...valeurs.roles2faObligatoire, role]
        : valeurs.roles2faObligatoire.filter((code) => code !== role),
    });

  return (
    <form
      className="flex flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault();
        enregistrement.mutate(valeurs);
      }}
    >
      <Carte
        titre="Double authentification obligatoire"
        description="Choisissez les rôles pour lesquels la double authentification sera obligatoire. Ce réglage est enregistré mais n'est pas encore appliqué à la connexion."
      >
        <div className="grid gap-2 sm:grid-cols-2">
          {ROLES.map((role) => (
            <label key={role.code} className="flex items-center gap-3 rounded-2xl border border-brand-900/10 px-4 py-3 text-sm font-semibold text-ink-900">
              <input
                type="checkbox"
                className="size-4 accent-brand-900"
                checked={valeurs.roles2faObligatoire.includes(role.code)}
                onChange={(event) => basculerRole(role.code, event.target.checked)}
              />
              {role.libelle}
            </label>
          ))}
        </div>
      </Carte>

      <Carte
        titre="Mots de passe"
        description="S'applique aux nouveaux mots de passe : inscription, activation, réinitialisation et changement. Les mots de passe existants restent valides."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="longueur-mdp">Longueur minimale</Label>
            <Input
              id="longueur-mdp"
              type="number"
              min={10}
              max={64}
              value={valeurs.longueurMinMotDePasse}
              onChange={(event) => setValeurs({ ...valeurs, longueurMinMotDePasse: Number(event.target.value) })}
            />
            <p className="text-xs text-muted-foreground">Entre 10 et 64 caractères. Une minuscule, une majuscule et un chiffre restent exigés.</p>
          </div>
          <label className="flex items-center justify-between gap-3 rounded-2xl border border-brand-900/10 px-4 py-3 text-sm font-semibold text-ink-900">
            Exiger un caractère spécial
            <Switch
              checked={valeurs.exigerCaractereSpecial}
              onCheckedChange={(coche) => setValeurs({ ...valeurs, exigerCaractereSpecial: coche })}
              aria-label="Exiger un caractère spécial"
            />
          </label>
        </div>
      </Carte>

      <Carte
        titre="Durée des sessions"
        description="Durée pendant laquelle un utilisateur reste connecté sans ressaisir son mot de passe. S'applique aux prochaines connexions."
      >
        <div className="flex max-w-xs flex-col gap-1.5">
          <Label htmlFor="duree-session">Nombre de jours</Label>
          <Input
            id="duree-session"
            type="number"
            min={1}
            max={30}
            value={valeurs.dureeSessionJours}
            onChange={(event) => setValeurs({ ...valeurs, dureeSessionJours: Number(event.target.value) })}
          />
          <p className="text-xs text-muted-foreground">Entre 1 et 30 jours.</p>
        </div>
      </Carte>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" disabled={!modifie} onClick={() => securite.data && setValeurs(securite.data)}>
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
// Documents d'intégration
// ---------------------------------------------------------------------------

type FormulaireDocument = { libelle: string; raison: string; obligatoire: boolean; actif: boolean; ordre: number };

function SectionDocuments() {
  const queryClient = useQueryClient();
  const documents = useQuery({ queryKey: ["parametres-documents"], queryFn: listerTypesDocuments });
  const [edition, setEdition] = useState<TypeDocumentIntegrationDto | "nouveau" | null>(null);
  const [formulaire, setFormulaire] = useState<FormulaireDocument>({
    libelle: "", raison: "", obligatoire: true, actif: true, ordre: 0,
  });

  const ouvrir = (cible: TypeDocumentIntegrationDto | "nouveau") => {
    setEdition(cible);
    setFormulaire(cible === "nouveau"
      ? { libelle: "", raison: "", obligatoire: true, actif: true, ordre: (documents.data?.length ?? 0) + 1 }
      : { libelle: cible.libelle, raison: cible.raison ?? "", obligatoire: cible.obligatoire, actif: cible.actif, ordre: cible.ordre });
  };

  const enregistrement = useMutation({
    mutationFn: () => {
      const corps = { ...formulaire, libelle: formulaire.libelle.trim(), raison: formulaire.raison.trim() };
      return edition === "nouveau" || edition == null
        ? creerTypeDocument(corps)
        : modifierTypeDocument(edition.code, corps);
    },
    onSuccess: async () => {
      toast.success("Document enregistré.");
      setEdition(null);
      await queryClient.invalidateQueries({ queryKey: ["parametres-documents"] });
      await queryClient.invalidateQueries({ queryKey: ["documents-requis-integration"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  return (
    <Carte
      titre="Documents exigés pour l'intégration"
      description="Liste des pièces demandées dans le formulaire public d'intégration. Un document désactivé n'est plus demandé mais reste consultable dans les dossiers qui le contiennent."
      action={
        <Button size="sm" icon={<Plus className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => ouvrir("nouveau")}>
          Ajouter un document
        </Button>
      }
    >
      {documents.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
      {documents.isError && <EtatVide titre="Les documents n'ont pas pu être chargés." />}
      <ul className="flex flex-col gap-2">
        {documents.data?.map((document) => (
          <li key={document.code} className="flex flex-wrap items-start justify-between gap-3 rounded-2xl border border-brand-900/10 px-4 py-3">
            <div className="min-w-0">
              <p className="font-semibold text-ink-900">
                <span className="mr-2 text-xs text-muted-foreground">{document.ordre}.</span>
                {document.libelle}
              </p>
              {document.raison && <p className="text-xs text-muted-foreground">{document.raison}</p>}
              <div className="mt-1 flex flex-wrap gap-1">
                <Badge size="sm" variant={document.actif ? "mint" : "outline"}>{document.actif ? "Actif" : "Désactivé"}</Badge>
                {document.actif && <Badge size="sm" variant="outline">{document.obligatoire ? "Obligatoire" : "Facultatif"}</Badge>}
              </div>
            </div>
            <Button variant="ghost" size="sm" icon={<Pencil className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => ouvrir(document)}>
              Modifier
            </Button>
          </li>
        ))}
      </ul>

      <Dialogue
        ouvert={edition != null}
        titre={edition === "nouveau" ? "Ajouter un document" : "Modifier le document"}
        description="Les demandes déjà déposées ne sont pas modifiées. Un nouveau document obligatoire sera exigé avant l'acceptation des demandes en cours."
        onFermer={() => setEdition(null)}
      >
        <form
          className="flex flex-col gap-4 text-sm"
          onSubmit={(event) => {
            event.preventDefault();
            enregistrement.mutate();
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="libelle-document">Libellé *</Label>
            <Input id="libelle-document" required maxLength={255} value={formulaire.libelle} onChange={(event) => setFormulaire({ ...formulaire, libelle: event.target.value })} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="raison-document">Pourquoi ce document est demandé</Label>
            <Textarea id="raison-document" maxLength={255} className="min-h-20" value={formulaire.raison} onChange={(event) => setFormulaire({ ...formulaire, raison: event.target.value })} />
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="flex items-center justify-between gap-2 rounded-2xl border border-brand-900/10 px-3 py-2 font-semibold text-ink-900">
              Désactivé
              <Switch checked={!formulaire.actif} onCheckedChange={(desactive) => setFormulaire({ ...formulaire, actif: !desactive })} aria-label="Document désactivé" />
            </label>
            <label className="flex items-center justify-between gap-2 rounded-2xl border border-brand-900/10 px-3 py-2 font-semibold text-ink-900">
              Obligatoire
              <Switch checked={formulaire.obligatoire} onCheckedChange={(obligatoire) => setFormulaire({ ...formulaire, obligatoire })} aria-label="Document obligatoire" />
            </label>
            <div className="flex flex-col gap-1">
              <Label htmlFor="ordre-document">Ordre</Label>
              <Input id="ordre-document" type="number" min={0} max={999} value={formulaire.ordre} onChange={(event) => setFormulaire({ ...formulaire, ordre: Number(event.target.value) })} />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => setEdition(null)}>Annuler</Button>
            <Button type="submit" loading={enregistrement.isPending} disabled={!formulaire.libelle.trim()}>Enregistrer</Button>
          </div>
        </form>
      </Dialogue>
    </Carte>
  );
}

// ---------------------------------------------------------------------------
// Types de laboratoire
// ---------------------------------------------------------------------------

function SectionTypesLaboratoire() {
  const queryClient = useQueryClient();
  const types = useQuery({ queryKey: ["parametres-types-laboratoire"], queryFn: listerTypesLaboratoire });
  const [nouveau, setNouveau] = useState("");

  const invalider = async () => {
    await queryClient.invalidateQueries({ queryKey: ["parametres-types-laboratoire"] });
    await queryClient.invalidateQueries({ queryKey: ["parametres-publics"] });
  };

  const creation = useMutation({
    mutationFn: () => creerTypeLaboratoire(nouveau.trim(), (types.data?.length ?? 0) + 1),
    onSuccess: async () => {
      toast.success("Type ajouté.");
      setNouveau("");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'ajout a échoué.")),
  });
  const modification = useMutation({
    mutationFn: ({ id, actif, ordre }: { id: number; actif: boolean; ordre: number }) => modifierTypeLaboratoire(id, actif, ordre),
    onSuccess: invalider,
    onError: (erreur) => toast.error(messageErreur(erreur, "La modification a échoué.")),
  });

  return (
    <Carte
      titre="Types de laboratoire"
      description="Catégories proposées dans le formulaire d'intégration et dans les paramètres des laboratoires. Le libellé est enregistré tel quel sur les laboratoires : il ne se modifie pas, mais un type peut être désactivé."
    >
      <form
        className="mb-4 flex flex-wrap items-end gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          if (nouveau.trim()) creation.mutate();
        }}
      >
        <div className="flex min-w-60 flex-1 flex-col gap-1.5">
          <Label htmlFor="nouveau-type">Nouveau type</Label>
          <Input id="nouveau-type" maxLength={100} value={nouveau} onChange={(event) => setNouveau(event.target.value)} placeholder="Ex. Agroalimentaire" />
        </div>
        <Button type="submit" loading={creation.isPending} disabled={!nouveau.trim()}>Ajouter</Button>
      </form>
      {types.isError && <EtatVide titre="Les types n'ont pas pu être chargés." />}
      <ul className="flex flex-col gap-2">
        {types.data?.map((type) => (
          <li key={type.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 px-4 py-3">
            <span className="font-semibold text-ink-900">{type.libelle}</span>
            <div className="flex items-center gap-3">
              <Input
                type="number"
                aria-label={`Ordre de ${type.libelle}`}
                className="w-20"
                min={0}
                max={999}
                defaultValue={type.ordre}
                onBlur={(event) => {
                  const ordre = Number(event.target.value);
                  if (ordre !== type.ordre) modification.mutate({ id: type.id, actif: type.actif, ordre });
                }}
              />
              <Switch
                checked={type.actif}
                aria-label={type.actif ? `Désactiver ${type.libelle}` : `Activer ${type.libelle}`}
                onCheckedChange={(actif) => modification.mutate({ id: type.id, actif, ordre: type.ordre })}
              />
            </div>
          </li>
        ))}
      </ul>
    </Carte>
  );
}
