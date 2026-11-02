import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState, type ReactNode } from "react";
import {
  ArrowDown,
  ArrowUp,
  ChevronDown,
  Download,
  Eye,
  FileSpreadsheet,
  FileText,
  Pencil,
  Search,
  Upload,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label, Select, Textarea, Tooltip } from "@/components/lab";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  activerLaboratoire,
  desactiverLaboratoire,
  exporterLaboratoires,
  listerVillesLaboratoires,
  rechercherLaboratoires,
  suspendreLaboratoire,
  type LaboratoireAdmin,
} from "@/api/gestionLaboratoires";
import { formaterDate, messageErreur, texte } from "../texte";
import { DialogueImport } from "./DialogueImport";
import { PanneauLaboratoire } from "./PanneauLaboratoire";

type Tri = "raisonSociale" | "code" | "ville" | "email" | "dateCreation" | "statut";
type ActionStatut = "activer" | "suspendre" | "desactiver";
type TaillePage = 10 | 20 | 50;

const COLONNES: { cle: Tri | "coordonnees" | "administrateur" | "actions"; libelle: string; tri?: Tri }[] = [
  { cle: "raisonSociale", libelle: "Laboratoire", tri: "raisonSociale" },
  { cle: "code", libelle: "Identifiant", tri: "code" },
  { cle: "ville", libelle: "Ville", tri: "ville" },
  { cle: "coordonnees", libelle: "Coordonnées", tri: "email" },
  { cle: "administrateur", libelle: "Administrateur" },
  { cle: "dateCreation", libelle: "Date d'intégration", tri: "dateCreation" },
  { cle: "statut", libelle: "Statut", tri: "statut" },
  { cle: "actions", libelle: "Actions" },
];

export function TableauLaboratoires() {
  const queryClient = useQueryClient();
  const exportRef = useRef<HTMLDivElement>(null);
  const [recherche, setRecherche] = useState("");
  const [rechercheDifferee, setRechercheDifferee] = useState("");
  const [statut, setStatut] = useState<"" | "ACTIF" | "SUSPENDU" | "DESACTIVE">("");
  const [ville, setVille] = useState("");
  const [page, setPage] = useState(0);
  const [taille, setTaille] = useState<TaillePage>(10);
  const [tri, setTri] = useState<Tri>("dateCreation");
  const [ordre, setOrdre] = useState<"asc" | "desc">("desc");
  const [portee, setPortee] = useState<"filtres" | "tous">("filtres");
  const [exportOuvert, setExportOuvert] = useState(false);
  const [importOuvert, setImportOuvert] = useState(false);
  const [selection, setSelection] = useState<{
    laboratoire: LaboratoireAdmin;
    mode: "consulter" | "modifier";
  } | null>(null);
  const [confirmation, setConfirmation] = useState<{
    laboratoire: LaboratoireAdmin;
    action: ActionStatut;
  } | null>(null);
  const [motif, setMotif] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setRechercheDifferee(recherche.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [recherche]);

  useEffect(() => {
    setPage(0);
  }, [rechercheDifferee, statut, ville, tri, ordre, taille]);

  useEffect(() => {
    if (!exportOuvert) return;
    const fermer = (event: MouseEvent) => {
      if (!exportRef.current?.contains(event.target as Node)) setExportOuvert(false);
    };
    window.addEventListener("mousedown", fermer);
    return () => window.removeEventListener("mousedown", fermer);
  }, [exportOuvert]);

  const filtre = {
    recherche: rechercheDifferee || undefined,
    statut,
    ville: ville || undefined,
    page,
    size: taille,
    tri,
    ordre,
  };

  const laboratoires = useQuery({
    queryKey: ["laboratoires-admin", filtre],
    queryFn: () => rechercherLaboratoires(filtre),
  });

  const villes = useQuery({
    queryKey: ["laboratoires-villes"],
    queryFn: listerVillesLaboratoires,
  });

  const invalider = async () => {
    await queryClient.invalidateQueries({ queryKey: ["laboratoires-admin"] });
    await queryClient.invalidateQueries({ queryKey: ["laboratoires-villes"] });
    await queryClient.invalidateQueries({ queryKey: ["statistiques-plateforme"] });
  };

  const statutMutation = useMutation({
    mutationFn: ({ laboratoire, action, motif }: { laboratoire: LaboratoireAdmin; action: ActionStatut; motif?: string }) => {
      if (action === "activer") {
        return activerLaboratoire(laboratoire.id);
      } else if (action === "suspendre") {
        return suspendreLaboratoire(laboratoire.id, motif as string);
      } else {
        return desactiverLaboratoire(laboratoire.id, motif as string);
      }
    },
    onSuccess: async (misAJour, { laboratoire, action }) => {
      toast.success(
        action === "activer" ? "Le laboratoire a été activé." :
        action === "suspendre" ? "Le laboratoire a été suspendu." :
        "Le laboratoire a été désactivé."
      );
      setConfirmation(null);
      setMotif("");
      setSelection((actuel) =>
        actuel && actuel.laboratoire.id === misAJour.id ? { ...actuel, laboratoire: misAJour } : actuel,
      );
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le changement de statut a échoué.")),
  });

  const exportMutation = useMutation({
    mutationFn: (format: "pdf" | "xlsx" | "csv") => exporterLaboratoires(format, portee, filtre),
    onSuccess: () => {
      toast.success("Le fichier a été téléchargé.");
      setExportOuvert(false);
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'exportation a échoué.")),
  });

  function changerTri(champ: Tri) {
    if (tri === champ) setOrdre((actuel) => (actuel === "asc" ? "desc" : "asc"));
    else {
      setTri(champ);
      setOrdre("asc");
    }
  }

  const total = laboratoires.data?.totalElements ?? 0;
  const debut = total === 0 ? 0 : page * taille + 1;
  const fin = Math.min(total, (page + 1) * taille);
  const filtresActifs = Boolean(rechercheDifferee || statut || ville);
  const aucuneDonnee = !laboratoires.isLoading && !laboratoires.isError && total === 0;

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-2xl border border-brand-900/10 bg-card p-3 sm:p-4">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-center">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              value={recherche}
              onChange={(event) => setRecherche(event.target.value)}
              placeholder="Rechercher par nom, identifiant, ville ou e-mail"
              aria-label="Rechercher un laboratoire"
              className="pl-10"
            />
          </div>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 xl:flex">
            <Select
              value={statut}
              aria-label="Filtrer par statut"
              className="xl:w-44"
              onChange={(event) => setStatut(event.target.value as "" | "ACTIF" | "SUSPENDU" | "DESACTIVE")}
            >
              <option value="">Tous les statuts</option>
              <option value="ACTIF">Actif</option>
              <option value="SUSPENDU">Suspendu</option>
              <option value="DESACTIVE">Désactivé</option>
            </Select>
            <Select
              value={ville}
              aria-label="Filtrer par ville"
              className="xl:w-48"
              onChange={(event) => setVille(event.target.value)}
            >
              <option value="">Toutes les villes</option>
              {(villes.data ?? []).map((nom) => (
                <option key={nom} value={nom}>
                  {nom}
                </option>
              ))}
            </Select>
          </div>
          <div className="flex flex-wrap gap-2">
            {filtresActifs && (
              <Button variant="ghost" size="sm" onClick={() => { setRecherche(""); setStatut(""); setVille(""); }}>
                Réinitialiser
              </Button>
            )}
            <Button
              size="sm"
              icon={<Upload className="size-4" aria-hidden="true" />}
              iconPosition="left"
              onClick={() => setImportOuvert(true)}
            >
              Importer
            </Button>
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
                  <label htmlFor="portee-export" className="text-xs font-semibold text-ink-900">
                    Étendue
                  </label>
                  <Select
                    id="portee-export"
                    className="mt-1"
                    value={portee}
                    onChange={(event) => setPortee(event.target.value as "filtres" | "tous")}
                  >
                    <option value="filtres">Résultats filtrés</option>
                    <option value="tous">Tous les laboratoires</option>
                  </Select>
                  <div className="mt-3 flex flex-col gap-1">
                    <BoutonExport icone={FileText} libelle="Exporter en PDF" onClick={() => exportMutation.mutate("pdf")} />
                    <BoutonExport icone={FileSpreadsheet} libelle="Exporter en Excel" onClick={() => exportMutation.mutate("xlsx")} />
                    <BoutonExport icone={Download} libelle="Exporter en CSV" onClick={() => exportMutation.mutate("csv")} />
                  </div>
                  {exportMutation.isPending && <p className="mt-2 text-xs text-muted-foreground">Préparation du fichier…</p>}
                </div>
              )}
            </div>
          </div>
        </div>
        {filtresActifs && (
          <div className="mt-3 flex flex-wrap gap-2">
            {rechercheDifferee && (
              <FiltreActif libelle={`Recherche : ${rechercheDifferee}`} onRetirer={() => setRecherche("")} />
            )}
            {statut && (
              <FiltreActif
                libelle={statut === "ACTIF" ? "Actif" : statut === "SUSPENDU" ? "Suspendu" : "Désactivé"}
                onRetirer={() => setStatut("")}
              />
            )}
            {ville && <FiltreActif libelle={ville} onRetirer={() => setVille("")} />}
          </div>
        )}
      </div>

      <div className="overflow-x-auto rounded-2xl border border-brand-900/10 bg-card">
        <table className="w-full min-w-[1080px] border-separate border-spacing-0 text-left text-sm">
          <thead>
            <tr className="bg-surface-alt text-brand-900">
              {COLONNES.map((colonne) => {
                const estActions = colonne.cle === "actions";
                const actif = colonne.tri != null && tri === colonne.tri;
                return (
                  <th
                    key={colonne.cle}
                    scope="col"
                    aria-sort={actif ? (ordre === "asc" ? "ascending" : "descending") : undefined}
                    className={
                      estActions
                        ? "sticky right-0 z-30 bg-surface-alt px-3 py-3 text-right text-xs font-semibold tracking-wide shadow-[-8px_0_12px_-12px_rgba(36,42,46,0.45)]"
                        : "px-4 py-3 text-xs font-semibold tracking-wide whitespace-nowrap"
                    }
                  >
                    {colonne.tri ? (
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
                        onClick={() => changerTri(colonne.tri as Tri)}
                      >
                        {colonne.libelle}
                        {actif && (ordre === "asc" ? <ArrowUp className="size-3.5" aria-hidden="true" /> : <ArrowDown className="size-3.5" aria-hidden="true" />)}
                      </button>
                    ) : (
                      colonne.libelle
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {laboratoires.isLoading &&
              Array.from({ length: 5 }).map((_, index) => (
                <tr key={index}>
                  <td colSpan={8} className="border-t border-brand-900/10 px-4 py-4">
                    <span className="block h-4 w-2/3 animate-pulse rounded-full bg-surface-alt" />
                  </td>
                </tr>
              ))}
            {laboratoires.isError && (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">Les laboratoires n'ont pas pu être chargés.</p>
                  <Button className="mt-3" size="sm" variant="secondary" onClick={() => void laboratoires.refetch()}>
                    Réessayer
                  </Button>
                </td>
              </tr>
            )}
            {aucuneDonnee && (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center">
                  <p className="font-semibold text-ink-900">
                    {filtresActifs ? "Aucun laboratoire ne correspond à ces critères." : "Aucun laboratoire n'est encore enregistré."}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {filtresActifs
                      ? "Modifiez la recherche ou retirez un filtre."
                      : "Les laboratoires apparaissent ici après l'acceptation d'une demande d'intégration."}
                  </p>
                </td>
              </tr>
            )}
            {laboratoires.data?.content.map((laboratoire) => (
              <tr key={laboratoire.id} className="group">
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
                  <p className="max-w-[220px] truncate font-semibold text-ink-900" title={laboratoire.nom}>
                    {laboratoire.nom}
                  </p>
                  {laboratoire.ice && (
                    <p className="text-xs text-muted-foreground">ICE {laboratoire.ice}</p>
                  )}
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 font-medium whitespace-nowrap text-ink-900 group-hover:bg-surface-alt">
                  {laboratoire.code}
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 whitespace-nowrap text-ink-900 group-hover:bg-surface-alt">
                  {texte(laboratoire.ville)}
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
                  <p className="max-w-[220px] truncate text-ink-900" title={laboratoire.emailContact ?? ""}>
                    {texte(laboratoire.emailContact)}
                  </p>
                  <p className="text-xs text-muted-foreground">{texte(laboratoire.telephone)}</p>
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
                  <p className="max-w-[180px] truncate font-medium text-ink-900" title={laboratoire.administrateur?.nomComplet}>
                    {texte(laboratoire.administrateur?.nomComplet)}
                  </p>
                  {laboratoire.administrateur?.email && (
                    <p className="max-w-[180px] truncate text-xs text-muted-foreground" title={laboratoire.administrateur.email}>
                      {laboratoire.administrateur.email}
                    </p>
                  )}
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 whitespace-nowrap text-ink-900 group-hover:bg-surface-alt">
                  {formaterDate(laboratoire.dateCreation)}
                </td>
                <td className="border-t border-brand-900/10 bg-card px-4 py-3 group-hover:bg-surface-alt">
                  <div className="flex items-center gap-2">
                    <BadgeStatut statut={laboratoire.statut} />
                    <MenuStatut
                      nom={laboratoire.nom}
                      statut={laboratoire.statut}
                      onChoisir={(action) => {
                        setMotif("");
                        setConfirmation({ laboratoire, action });
                      }}
                    />
                  </div>
                </td>
                <td className="sticky right-0 z-10 border-t border-brand-900/10 bg-card px-2 py-3 text-right shadow-[-8px_0_12px_-12px_rgba(36,42,46,0.35)] group-hover:bg-surface-alt">
                  <div className="inline-flex justify-end">
                    <ActionIcone label={`Consulter ${laboratoire.nom}`} info="Consulter" onClick={() => setSelection({ laboratoire, mode: "consulter" })}>
                      <Eye className="size-4" aria-hidden="true" />
                    </ActionIcone>
                    <ActionIcone label={`Modifier ${laboratoire.nom}`} info="Modifier" onClick={() => setSelection({ laboratoire, mode: "modifier" })}>
                      <Pencil className="size-4" aria-hidden="true" />
                    </ActionIcone>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
        <p>
          {debut}–{fin} sur {total}
        </p>
        <div className="flex flex-wrap items-center gap-2">
          <label htmlFor="taille-page" className="text-xs font-semibold text-ink-900">
            Par page
          </label>
          <Select
            id="taille-page"
            className="w-20 py-2"
            value={String(taille)}
            onChange={(event) => setTaille(Number(event.target.value) as TaillePage)}
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </Select>
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((actuel) => Math.max(0, actuel - 1))}>
            Précédent
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={!laboratoires.data || page + 1 >= laboratoires.data.totalPages}
            onClick={() => setPage((actuel) => actuel + 1)}
          >
            Suivant
          </Button>
        </div>
      </div>

      <PanneauLaboratoire
        laboratoire={selection?.laboratoire ?? null}
        modeInitial={selection?.mode ?? "consulter"}
        onFermer={() => setSelection(null)}
        onDemanderStatut={(laboratoire, action) => {
          setMotif("");
          setConfirmation({ laboratoire, action });
        }}
      />
      <DialogueImport ouvert={importOuvert} onFermer={() => setImportOuvert(false)} />
      <DialogueStatut
        confirmation={confirmation}
        motif={motif}
        onMotif={setMotif}
        chargement={statutMutation.isPending}
        onFermer={() => {
          if (statutMutation.isPending) return;
          setConfirmation(null);
          setMotif("");
        }}
        onConfirmer={() => {
          if (!confirmation) return;
          statutMutation.mutate({
            laboratoire: confirmation.laboratoire,
            action: confirmation.action,
            motif: motif.trim(),
          });
        }}
      />
    </div>
  );
}

function BadgeStatut({ statut }: { statut: string }) {
  if (statut === "SUSPENDU") return <Badge variant="mint" size="sm">Suspendu</Badge>;
  if (statut === "DESACTIVE") return <Badge variant="outline" size="sm">Désactivé</Badge>;
  return <Badge variant="lime" size="sm">Actif</Badge>;
}

function MenuStatut({
  nom,
  statut,
  onChoisir,
}: {
  nom: string;
  statut: string;
  onChoisir: (action: ActionStatut) => void;
}) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label={`Actions de statut pour ${nom}`}
          className="flex size-8 items-center justify-center rounded-full text-brand-900 hover:bg-surface-card focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
        >
          <ChevronDown className="size-4" aria-hidden="true" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="rounded-2xl border-brand-900/10 bg-card p-1">
        <DropdownMenuItem disabled={statut === "ACTIF"} onSelect={() => onChoisir("activer")}>
          Activer
        </DropdownMenuItem>
        <DropdownMenuItem disabled={statut === "SUSPENDU"} onSelect={() => onChoisir("suspendre")}>
          Suspendre
        </DropdownMenuItem>
        <DropdownMenuItem disabled={statut === "DESACTIVE"} onSelect={() => onChoisir("desactiver")}>
          Désactiver
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function DialogueStatut({
  confirmation,
  motif,
  onMotif,
  chargement,
  onFermer,
  onConfirmer,
}: {
  confirmation: { laboratoire: LaboratoireAdmin; action: ActionStatut } | null;
  motif: string;
  onMotif: (valeur: string) => void;
  chargement: boolean;
  onFermer: () => void;
  onConfirmer: () => void;
}) {
  const action = confirmation?.action;
  const motifRequis = action === "suspendre" || action === "desactiver";
  const motifValide = !motifRequis || motif.trim().length >= 10;
  const titre = action === "suspendre"
    ? "Suspendre ce laboratoire ?"
    : action === "desactiver"
      ? "Désactiver ce laboratoire ?"
      : "Activer ce laboratoire ?";
  const description = action === "activer"
    ? "L'administrateur et les utilisateurs pourront de nouveau accéder à cet espace. Aucune donnée n'a été supprimée."
    : "L'effet est immédiat, y compris pour une session déjà ouverte. Les données et l'environnement du laboratoire sont conservés.";
  return (
    <Dialog open={confirmation !== null} onOpenChange={(ouvert) => { if (!ouvert) onFermer(); }}>
      <DialogContent className="glass-card glossy-top max-w-lg rounded-3xl border-0 bg-card p-6">
        <DialogTitle className="text-lg font-extrabold text-ink-900">{titre}</DialogTitle>
        <DialogDescription className="text-sm text-muted-foreground">{description}</DialogDescription>
        {motifRequis && (
          <div className="mt-4">
            <Label htmlFor="motif-statut">Motif</Label>
            <Textarea
              id="motif-statut"
              value={motif}
              onChange={(event) => onMotif(event.target.value)}
              placeholder="Décrivez le motif (10 caractères minimum)"
              className="mt-2 min-h-24"
            />
          </div>
        )}
        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <Button variant="ghost" onClick={onFermer} disabled={chargement}>Annuler</Button>
          <Button loading={chargement} disabled={!motifValide} onClick={onConfirmer}>
            {action === "suspendre" ? "Suspendre" : action === "desactiver" ? "Désactiver" : "Activer"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function BoutonExport({
  icone: Icone,
  libelle,
  onClick,
}: {
  icone: typeof FileText;
  libelle: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      role="menuitem"
      className="flex items-center gap-2 rounded-xl px-2 py-2 text-left text-sm font-semibold text-ink-900 hover:bg-surface-alt focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900"
      onClick={onClick}
    >
      <Icone className="size-4 text-brand-900" aria-hidden="true" />
      {libelle}
    </button>
  );
}

function FiltreActif({ libelle, onRetirer }: { libelle: string; onRetirer: () => void }) {
  return (
    <button
      type="button"
      onClick={onRetirer}
      className="inline-flex items-center gap-1 rounded-full bg-surface-alt px-3 py-1 text-xs font-semibold text-brand-900"
    >
      {libelle}
      <X className="size-3" aria-hidden="true" />
      <span className="sr-only">Retirer ce filtre</span>
    </button>
  );
}

function ActionIcone({
  label,
  info,
  onClick,
  children,
}: {
  label: string;
  info: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <Tooltip content={info}>
      <Button variant="ghost" size="sm" className="size-9 px-0" aria-label={label} onClick={onClick}>
        {children}
      </Button>
    </Tooltip>
  );
}
