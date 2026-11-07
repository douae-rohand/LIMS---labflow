import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { Download, Eye, FileSpreadsheet, MoreHorizontal, Pencil, SlidersHorizontal, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label, Select } from "@/components/lab";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Switch } from "@/components/ui/switch";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { getSession } from "@/api/session";
import { rechercherLaboratoires } from "@/api/gestionLaboratoires";
import {
  creerAdministrateur,
  desactiverCompte,
  exporterComptes,
  listerComptes,
  lirePermissions,
  modifierCompte,
  reactiverCompte,
  reinitialiserMotDePasseCompte,
  renvoyerInvitationCompte,
  type CompteUtilisateur,
  type FiltreUtilisateurs,
} from "@/api/plateformeUtilisateurs";
import { BarreListe } from "../BarreListe";
import { Dialogue, Panneau } from "../Fenetres";
import { DialoguePermissions } from "./DialoguePermissions";
import { DialogueMotif } from "../DialogueMotif";
import { EnTetePage } from "../EnTetePage";
import { EtatVide } from "../EtatVide";
import { formaterDate, messageErreur, texte } from "../texte";
import { DialogueImportUtilisateurs } from "./DialogueImportUtilisateurs";

export function PageComptes() {
  const queryClient = useQueryClient();
  const session = getSession();
  const [recherche, setRecherche] = useState("");
  const [differee, setDifferee] = useState("");
  const [laboratoireId, setLaboratoireId] = useState("");
  const [laboratoireLibelle, setLaboratoireLibelle] = useState("");
  const [portee, setPortee] = useState<"filtres" | "tous">("filtres");
  const [statut, setStatut] = useState<"" | "ACTIF" | "INACTIF">("");
  const [page, setPage] = useState(0);
  const [importOuvert, setImportOuvert] = useState(false);
  const [creation, setCreation] = useState(false);
  const [edition, setEdition] = useState<CompteUtilisateur | null>(null);
  const [detail, setDetail] = useState<CompteUtilisateur | null>(null);
  const [permissionsCible, setPermissionsCible] = useState<CompteUtilisateur | null>(null);
  const [confirmation, setConfirmation] = useState<CompteUtilisateur | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setDifferee(recherche.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [recherche]);
  useEffect(() => setPage(0), [differee, laboratoireId, statut]);

  const filtre: FiltreUtilisateurs = {
    page,
    size: 20,
    tri: "nom",
    ordre: "asc",
    role: "ADMINISTRATEUR",
    ...(differee ? { recherche: differee } : {}),
    ...(laboratoireId ? { laboratoireId: Number(laboratoireId) } : {}),
    ...(statut ? { statut } : {}),
  };
  const filtresActifs = Boolean(differee || laboratoireId || statut);
  const exportMutation = useMutation({
    mutationFn: (format: "csv" | "xlsx") => exporterComptes(
      format,
      portee === "tous" ? { page: 0, size: 20, role: "ADMINISTRATEUR" } : filtre,
    ),
    onSuccess: () => toast.success("Le fichier a été téléchargé."),
    onError: (erreur) => toast.error(messageErreur(erreur, "L'exportation a échoué.")),
  });
  const comptes = useQuery({
    queryKey: ["comptes-plateforme", filtre],
    queryFn: () => listerComptes(filtre),
  });
  const laboratoiresActifs = useQuery({
    queryKey: ["laboratoires-actifs-creation"],
    queryFn: () => rechercherLaboratoires({ page: 0, size: 100, statut: "ACTIF", tri: "raisonSociale", ordre: "asc" }),
    enabled: creation,
  });

  function invalider() {
    return queryClient.invalidateQueries({ queryKey: ["comptes-plateforme"] });
  }

  const desactivation = useMutation({
    mutationFn: ({ compte, motif }: { compte: CompteUtilisateur; motif: string }) => desactiverCompte(compte.id, motif || undefined),
    onSuccess: async () => {
      toast.success("Compte désactivé.");
      setConfirmation(null);
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La désactivation a échoué.")),
  });
  const activation = useMutation({
    mutationFn: (compte: CompteUtilisateur) => reactiverCompte(compte.id),
    onSuccess: async () => {
      toast.success("Compte activé.");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La réactivation a été refusée.")),
  });

  const action = useMutation({
    mutationFn: async ({ id, type }: { id: number; type: "mdp" | "invitation" }) => {
      if (type === "mdp") return reinitialiserMotDePasseCompte(id);
      return renvoyerInvitationCompte(id);
    },
    onSuccess: async (resultat) => {
      if (resultat && "emailAccepte" in resultat && resultat.emailAccepte === false) {
        toast.error(resultat.message ?? "L'e-mail n'a pas pu être envoyé.");
      } else {
        toast.success("Action effectuée.");
      }
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'action a échoué.")),
  });

  return (
    <div className="px-4 py-6 sm:px-6 lg:px-8">
      <EnTetePage
        titre="Administrateurs"
        description="Administrateurs des laboratoires. Les autres comptes sont gérés dans chaque laboratoire."
        action={
          <Button size="sm" icon={<UserPlus className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => setCreation(true)}>
            Créer un administrateur
          </Button>
        }
      />
      <div className="mb-4">
        <BarreListe
          recherche={recherche}
          onRecherche={setRecherche}
          placeholder="Rechercher par nom, prénom ou e-mail"
          libelleRecherche="Rechercher un administrateur"
          filtresActifs={filtresActifs}
          onReinitialiser={() => { setRecherche(""); setLaboratoireId(""); setLaboratoireLibelle(""); setStatut(""); }}
          onImporter={() => setImportOuvert(true)}
          exportEnCours={exportMutation.isPending}
          porteeExport={{
            id: "portee-export-comptes",
            valeur: portee,
            onChange: (valeur) => setPortee(valeur as "filtres" | "tous"),
            options: [
              { valeur: "filtres", libelle: "Résultats filtrés" },
              { valeur: "tous", libelle: "Tous les administrateurs" },
            ],
          }}
          formatsExport={[
            { icone: FileSpreadsheet, libelle: "Exporter en Excel", onClick: () => exportMutation.mutate("xlsx") },
            { icone: Download, libelle: "Exporter en CSV", onClick: () => exportMutation.mutate("csv") },
          ]}
          puces={[
            ...(differee ? [{ libelle: `Recherche : ${differee}`, onRetirer: () => setRecherche("") }] : []),
            ...(laboratoireId ? [{ libelle: laboratoireLibelle || "Laboratoire", onRetirer: () => { setLaboratoireId(""); setLaboratoireLibelle(""); } }] : []),
            ...(statut ? [{ libelle: statut === "ACTIF" ? "Actif" : "Inactif", onRetirer: () => setStatut("") }] : []),
          ]}
          filtres={
            <>
              <FiltreLaboratoire
                key={laboratoireId || "tous"}
                onChoisir={(id, libelle) => { setLaboratoireId(id); setLaboratoireLibelle(libelle); }}
              />
              <Select value={statut} aria-label="Filtrer par statut" className="xl:w-44" onChange={(event) => setStatut(event.target.value as "" | "ACTIF" | "INACTIF")}>
                <option value="">Tous les statuts</option>
                <option value="ACTIF">Actif</option>
                <option value="INACTIF">Inactif</option>
              </Select>
            </>
          }
        />
      </div>

      {comptes.isLoading ? (
        <p className="text-sm text-muted-foreground" aria-busy="true">Chargement des administrateurs…</p>
      ) : comptes.isError ? (
        <EtatVide titre="Les administrateurs n'ont pas pu être chargés." />
      ) : comptes.data && comptes.data.content.length === 0 ? (
        <EtatVide titre="Aucun administrateur ne correspond." detail="Modifiez la recherche ou les filtres." />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-brand-900/10 bg-card">
          <div className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead>
              <tr className="bg-surface-alt text-xs font-semibold tracking-wide text-brand-900 uppercase">
                <th className="px-4 py-3">Administrateur</th>
                <th className="px-4 py-3">Laboratoire</th>
                <th className="px-4 py-3">Statut</th>
                <th className="px-4 py-3">Dernière connexion</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {comptes.data?.content.map((compte) => {
                const nomAffiche = `${texte(compte.prenom)} ${compte.nom}`.trim();
                const soiMeme = compte.id === session?.user.userId || compte.email === session?.user.email;
                const comptePlateforme = compte.role === "SUPER_ADMINISTRATEUR" || soiMeme;
                const modifiable = compte.role !== "SUPER_ADMINISTRATEUR" && !soiMeme;
                const permissionsOuvertes = compte.role !== "SUPER_ADMINISTRATEUR" && compte.role !== "CLIENT";
                const invitation = compte.statut !== "ACTIF" && ["ADMINISTRATEUR", "RESPONSABLE", "TECHNICIEN", "ACCUEIL"].includes(compte.role);
                const menu = compte.role === "ADMINISTRATEUR" || invitation;
                return (
                  <tr key={compte.id} className="border-t border-brand-900/10 hover:bg-surface-alt/60">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-brand-900/10 text-xs font-bold text-brand-900" aria-hidden="true">{initiales(nomAffiche)}</span>
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-ink-900">{nomAffiche}</p>
                          <p className="truncate text-xs text-muted-foreground">{compte.email}</p>
                          {comptePlateforme && <p className="text-xs font-semibold text-brand-900">Compte plateforme</p>}
                          {compte.personnalise && compte.role !== "SUPER_ADMINISTRATEUR" && compte.role !== "CLIENT" && <Badge variant="outline">Personnalisées</Badge>}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">{texte(compte.laboratoireNom)}</td>
                    <td className="px-4 py-3">
                      <Switch
                        checked={compte.statut === "ACTIF"}
                        disabled={comptePlateforme || desactivation.isPending || activation.isPending}
                        aria-readonly={comptePlateforme}
                        aria-label={compte.statut === "ACTIF" ? `Désactiver le compte de ${nomAffiche}` : `Activer le compte de ${nomAffiche}`}
                        onCheckedChange={(actif) => {
                          if (comptePlateforme) return;
                          if (actif) activation.mutate(compte);
                          else setConfirmation(compte);
                        }}
                      />
                      <span className="sr-only">{compte.statut === "ACTIF" ? "Actif" : "Inactif"}</span>
                    </td>
                    <td className="px-4 py-3">{compte.derniereConnexion ? formaterDate(compte.derniereConnexion) : "—"}</td>
                    <td className="px-4 py-3 text-right">
                      <TooltipProvider>
                        <div className="flex items-center justify-end gap-1">
                          <BoutonAction label="Voir le détail" onClick={() => setDetail(compte)}><Eye className="size-4" aria-hidden="true" /></BoutonAction>
                          {modifiable && <BoutonAction label="Modifier" onClick={() => setEdition(compte)}><Pencil className="size-4" aria-hidden="true" /></BoutonAction>}
                          {permissionsOuvertes && <BoutonAction label="Permissions" onClick={() => setPermissionsCible(compte)}><SlidersHorizontal className="size-4" aria-hidden="true" /></BoutonAction>}
                          {menu && (
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" aria-label={`Autres actions pour ${nomAffiche}`}>
                                  <MoreHorizontal className="size-4" aria-hidden="true" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end">
                                {compte.role === "ADMINISTRATEUR" && (
                                  <>
                                    <DropdownMenuItem onSelect={() => action.mutate({ id: compte.id, type: "mdp" })}>Réinitialiser le mot de passe</DropdownMenuItem>
                                  </>
                                )}
                                {invitation && (
                                  <DropdownMenuItem onSelect={() => action.mutate({ id: compte.id, type: "invitation" })}>Renvoyer l'invitation</DropdownMenuItem>
                                )}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          )}
                        </div>
                      </TooltipProvider>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
          <div className="flex items-center justify-between border-t border-brand-900/10 px-4 py-3 text-sm">
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((valeur) => valeur - 1)}>Précédent</Button>
            <span className="text-muted-foreground">Page {page + 1} / {Math.max(comptes.data?.totalPages ?? 1, 1)}</span>
            <Button variant="ghost" size="sm" disabled={page + 1 >= (comptes.data?.totalPages ?? 1)} onClick={() => setPage((valeur) => valeur + 1)}>Suivant</Button>
          </div>
        </div>
      )}

      <DialogueImportUtilisateurs ouvert={importOuvert} onFermer={() => setImportOuvert(false)} onTermine={() => void invalider()} />
      <FormulaireCompte
        ouvert={creation || edition != null}
        compte={edition}
        laboratoires={laboratoiresActifs.data?.content.map((laboratoire) => ({ id: laboratoire.id, nom: laboratoire.nom })) ?? []}
        onFermer={() => { setCreation(false); setEdition(null); }}
        onEnregistre={() => { setCreation(false); setEdition(null); void invalider(); }}
      />
      <PanneauCompte compte={detail} onFermer={() => setDetail(null)} />
      <DialoguePermissions
        compteId={permissionsCible?.id ?? null}
        nom={permissionsCible ? `${texte(permissionsCible.prenom)} ${permissionsCible.nom}`.trim() : ""}
        onFermer={() => setPermissionsCible(null)}
      />
      <DialogueMotif
        ouvert={confirmation != null}
        titre="Désactiver le compte"
        description="Les sessions ouvertes seront fermées à la prochaine requête. Le statut ne change qu'après confirmation."
        libelleConfirmation="Désactiver"
        enCours={desactivation.isPending}
        onFermer={() => setConfirmation(null)}
        onConfirmer={(motif) => {
          if (confirmation) desactivation.mutate({ compte: confirmation, motif });
        }}
      />
    </div>
  );
}

function FormulaireCompte({
  ouvert, compte, laboratoires, onFermer, onEnregistre,
}: {
  ouvert: boolean;
  compte: CompteUtilisateur | null;
  laboratoires: { id: number; nom: string }[];
  onFermer: () => void;
  onEnregistre: () => void;
}) {
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [email, setEmail] = useState("");
  const [telephone, setTelephone] = useState("");
  const [fonction, setFonction] = useState("");
  const [laboratoireId, setLaboratoireId] = useState("");

  useEffect(() => {
    setNom(compte?.nom ?? "");
    setPrenom(compte?.prenom ?? "");
    setEmail(compte?.email ?? "");
    setTelephone(compte?.telephone ?? "");
    setFonction(compte?.fonction ?? "");
    setLaboratoireId(compte?.laboratoireId ? String(compte.laboratoireId) : "");
  }, [compte, ouvert]);

  const mutation = useMutation({
    mutationFn: () => compte
      ? modifierCompte(compte.id, {
          nom: nom.trim(),
          ...(prenom.trim() ? { prenom: prenom.trim() } : {}),
          ...(telephone.trim() ? { telephone: telephone.trim() } : {}),
          ...(fonction.trim() ? { fonction: fonction.trim() } : {}),
        })
      : creerAdministrateur({
          laboratoireId: Number(laboratoireId),
          nom: nom.trim(),
          ...(prenom.trim() ? { prenom: prenom.trim() } : {}),
          email: email.trim(),
          ...(telephone.trim() ? { telephone: telephone.trim() } : {}),
          ...(fonction.trim() ? { fonction: fonction.trim() } : {}),
        }),
    onSuccess: () => {
      toast.success(compte ? "Compte mis à jour." : "Administrateur créé. L'invitation part après l'enregistrement.");
      onEnregistre();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'enregistrement a échoué.")),
  });

  return (
    <Dialogue ouvert={ouvert} titre={compte ? "Modifier le compte" : "Créer un administrateur"} description={compte ? "L'e-mail, le rôle et le laboratoire sont affichés en lecture seule." : "Le mot de passe n'est jamais affiché. Le compte créé reste inactif jusqu'à l'activation."} onFermer={onFermer}>
      <form className="grid gap-3" onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }}>
        {!compte && (
          <div>
            <Label htmlFor="labo-admin">Laboratoire actif</Label>
            <Select id="labo-admin" value={laboratoireId} required onChange={(event) => setLaboratoireId(event.target.value)}>
              <option value="">Choisir</option>
              {laboratoires.map((laboratoire) => <option key={laboratoire.id} value={laboratoire.id}>{laboratoire.nom}</option>)}
            </Select>
          </div>
        )}
        <div>
          <Label htmlFor="nom-compte">Nom</Label>
          <Input id="nom-compte" value={nom} required onChange={(event) => setNom(event.target.value)} />
        </div>
        <div>
          <Label htmlFor="prenom-compte">Prénom</Label>
          <Input id="prenom-compte" value={prenom} onChange={(event) => setPrenom(event.target.value)} />
        </div>
        <div>
          <Label htmlFor="email-compte">{compte ? "E-mail, en lecture seule" : "E-mail"}</Label>
          <Input id="email-compte" type="email" value={compte ? compte.email : email} required={!compte} readOnly={compte != null} onChange={(event) => setEmail(event.target.value)} />
        </div>
        {compte && (
          <>
            <div>
              <Label htmlFor="role-compte">Rôle, en lecture seule</Label>
              <Input id="role-compte" value={libelleRole(compte.role)} readOnly />
            </div>
            <div>
              <Label htmlFor="labo-compte">Laboratoire, en lecture seule</Label>
              <Input id="labo-compte" value={texte(compte.laboratoireNom)} readOnly />
            </div>
          </>
        )}
        <div>
          <Label htmlFor="tel-compte">Téléphone</Label>
          <Input id="tel-compte" value={telephone} onChange={(event) => setTelephone(event.target.value)} />
        </div>
        <div>
          <Label htmlFor="fonction-compte">Fonction</Label>
          <Input id="fonction-compte" value={fonction} onChange={(event) => setFonction(event.target.value)} />
        </div>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onFermer}>Annuler</Button>
          <Button type="submit" disabled={mutation.isPending || nom.trim().length === 0}>Enregistrer</Button>
        </div>
      </form>
    </Dialogue>
  );
}

function BoutonAction({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={label} onClick={onClick}>{children}</Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}

function PanneauCompte({ compte, onFermer }: { compte: CompteUtilisateur | null; onFermer: () => void }) {
  const permissions = useQuery({
    queryKey: ["permissions-compte", compte?.id],
    queryFn: () => lirePermissions(compte?.id ?? 0),
    enabled: compte != null,
  });
  const nom = compte ? `${texte(compte.prenom)} ${compte.nom}`.trim() : "";
  return (
    <Panneau ouvert={compte != null} titre="Détail du compte" onFermer={onFermer}>
      {compte && (
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Badge
              variant={compte.statut === "ACTIF" ? "lime" : "outline"}
              size="sm"
            >
              {compte.statut === "ACTIF" ? "Actif" : "Inactif"}
            </Badge>
            {compte.role === "SUPER_ADMINISTRATEUR" && (
              <Badge variant="brand" size="sm">Compte plateforme</Badge>
            )}
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-brand-900">Informations personnelles</h3>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">Nom complet</dt>
                <dd className="text-sm text-ink-900">{nom}</dd>
              </div>
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">E-mail</dt>
                <dd className="text-sm text-ink-900">{compte.email}</dd>
              </div>
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">Téléphone</dt>
                <dd className="text-sm text-ink-900">{texte(compte.telephone)}</dd>
              </div>
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">Fonction</dt>
                <dd className="text-sm text-ink-900">{texte(compte.fonction)}</dd>
              </div>
            </div>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-brand-900">Rôle et organisation</h3>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">Rôle</dt>
                <dd className="text-sm text-ink-900">{libelleRole(compte.role)}</dd>
              </div>
              <div className="rounded-2xl bg-surface-alt px-3 py-2">
                <dt className="text-xs font-semibold text-muted-foreground">Laboratoire</dt>
                <dd className="text-sm text-ink-900">{texte(compte.laboratoireNom)}</dd>
              </div>
            </div>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-brand-900">Activité</h3>
            <div className="rounded-2xl bg-surface-alt px-3 py-2">
              <dt className="text-xs font-semibold text-muted-foreground">Dernière connexion</dt>
              <dd className="text-sm text-ink-900">{compte.derniereConnexion ? formaterDate(compte.derniereConnexion) : "Jamais connecté"}</dd>
            </div>
          </div>

          <div>
            <h3 className="mb-3 text-sm font-semibold text-brand-900">Permissions</h3>
            {permissions.isLoading ? (
              <p className="text-sm text-muted-foreground">Chargement des permissions…</p>
            ) : permissions.isError ? (
              <p className="text-sm text-red-600">Les permissions n'ont pas pu être chargées.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {!permissions.data?.personnalisable && (
                  <p className="text-sm text-muted-foreground">Les permissions de ce compte suivent le rôle et ne sont pas personnalisables.</p>
                )}
                {permissions.data?.personnalisees && (
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">Personnalisées</Badge>
                  </div>
                )}
                {permissions.data?.permissions && permissions.data.permissions.length > 0 && (
                  <div className="rounded-2xl bg-surface-alt p-3">
                    <ul className="flex flex-col gap-2">
                      {permissions.data.permissions.filter((ligne) => ligne.effective || ligne.ecart === "RETRAIT").map((ligne) => (
                        <li key={ligne.code} className="flex items-center justify-between gap-3 rounded-xl bg-surface-card px-3 py-2">
                          <span className="text-sm text-ink-900">{ligne.libelle}</span>
                          <span className="text-xs font-semibold">
                            {ligne.ecart === "AJOUT" ? (
                              <span className="text-green-600">Ajoutée</span>
                            ) : ligne.ecart === "RETRAIT" ? (
                              <span className="text-red-600">Retirée</span>
                            ) : (
                              <span className="text-muted-foreground">Par défaut</span>
                            )}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </Panneau>
  );
}

function FiltreLaboratoire({ onChoisir }: { onChoisir: (id: string, libelle: string) => void }) {
  const [saisie, setSaisie] = useState("");
  const [differee, setDifferee] = useState("");
  const [page, setPage] = useState(0);
  const [ouvert, setOuvert] = useState(false);
  const [choisi, setChoisi] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setDifferee(saisie.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [saisie]);
  useEffect(() => setPage(0), [differee]);

  const resultats = useQuery({
    queryKey: ["laboratoires-filtre", differee, page],
    queryFn: () => rechercherLaboratoires({
      page,
      size: 20,
      tri: "raisonSociale",
      ordre: "asc",
      ...(differee ? { recherche: differee } : {}),
    }),
    enabled: ouvert,
  });

  function choisir(id: string, libelle: string) {
    setChoisi(libelle);
    setSaisie("");
    setOuvert(false);
    onChoisir(id, libelle);
  }

  return (
    <div className="relative w-full shrink-0 sm:w-56">
      <Input
        role="combobox"
        aria-expanded={ouvert}
        aria-controls="liste-laboratoires-filtre"
        aria-autocomplete="list"
        aria-label="Filtrer par laboratoire"
        placeholder="Tous les laboratoires"
        value={ouvert ? saisie : choisi}
        onFocus={() => setOuvert(true)}
        onBlur={() => setOuvert(false)}
        onChange={(event) => {
          setSaisie(event.target.value);
          setOuvert(true);
        }}
        onKeyDown={(event) => {
          if (event.key === "Escape") setOuvert(false);
        }}
      />
      {ouvert && (
        <div id="liste-laboratoires-filtre" role="listbox" aria-label="Laboratoires" className="absolute z-20 mt-1 max-h-64 w-full overflow-y-auto rounded-2xl border border-brand-900/10 bg-card p-1 shadow-lg">
          <button
            type="button"
            role="option"
            className="block w-full rounded-xl px-3 py-2 text-left text-sm text-ink-900 hover:bg-surface-alt"
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => choisir("", "")}
          >
            Tous les laboratoires
          </button>
          {resultats.isLoading ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">Recherche…</p>
          ) : resultats.data?.content.length === 0 ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">Aucun laboratoire.</p>
          ) : (
            resultats.data?.content.map((laboratoire) => (
              <button
                key={laboratoire.id}
                type="button"
                role="option"
                className="block w-full rounded-xl px-3 py-2 text-left text-sm text-ink-900 hover:bg-surface-alt"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => choisir(String(laboratoire.id), `${laboratoire.nom} — ${libelleStatutLabo(laboratoire.statut)}`)}
              >
                {laboratoire.nom}
                <span className="ml-2 text-xs text-muted-foreground">{libelleStatutLabo(laboratoire.statut)}</span>
              </button>
            ))
          )}
          {(resultats.data?.totalPages ?? 0) > 1 && (
            <div className="flex items-center justify-between px-2 py-1">
              <Button type="button" variant="ghost" size="sm" disabled={page === 0} onMouseDown={(event) => event.preventDefault()} onClick={() => setPage((valeur) => valeur - 1)}>Précédent</Button>
              <span className="text-xs text-muted-foreground">{page + 1} / {resultats.data?.totalPages}</span>
              <Button type="button" variant="ghost" size="sm" disabled={page + 1 >= (resultats.data?.totalPages ?? 1)} onMouseDown={(event) => event.preventDefault()} onClick={() => setPage((valeur) => valeur + 1)}>Suivant</Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function initiales(nom: string) {
  const lettres = nom.split(" ").filter(Boolean).slice(0, 2).map((partie) => partie[0]?.toUpperCase() ?? "");
  return lettres.join("") || "LF";
}

function libelleStatutLabo(statut: string) {
  if (statut === "ACTIF") return "Actif";
  if (statut === "SUSPENDU") return "Suspendu";
  if (statut === "DESACTIVE") return "Désactivé";
  return statut;
}

function libelleRole(role: string) {
  return role.charAt(0) + role.slice(1).toLowerCase().replace("_", " ");
}
