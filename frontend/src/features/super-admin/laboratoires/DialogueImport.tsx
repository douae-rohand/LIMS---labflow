import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Fragment, useState } from "react";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import {
  analyserImport,
  confirmerImport,
  telechargerModeleImport,
  verifierImport,
  type LigneImportLaboratoire,
  type RapportImport,
} from "@/api/gestionLaboratoires";
import { Dialogue } from "../Fenetres";
import { messageErreur } from "../texte";

const CHAMPS: { cle: keyof LigneImportLaboratoire; libelle: string }[] = [
  { cle: "nomLaboratoire", libelle: "Nom du laboratoire" },
  { cle: "raisonSociale", libelle: "Raison sociale" },
  { cle: "typesLaboratoire", libelle: "Types de laboratoire" },
  { cle: "ice", libelle: "Identifiant ICE" },
  { cle: "ville", libelle: "Ville" },
  { cle: "adresse", libelle: "Adresse" },
  { cle: "region", libelle: "Région" },
  { cle: "codePostal", libelle: "Code postal" },
  { cle: "pays", libelle: "Pays" },
  { cle: "telephoneLaboratoire", libelle: "Téléphone du laboratoire" },
  { cle: "emailLaboratoire", libelle: "Adresse e-mail du laboratoire" },
  { cle: "siteWeb", libelle: "Site web" },
  { cle: "adminPrenom", libelle: "Prénom de l'administrateur" },
  { cle: "adminNom", libelle: "Nom de l'administrateur" },
  { cle: "adminEmail", libelle: "Adresse e-mail de l'administrateur" },
  { cle: "adminTelephone", libelle: "Téléphone de l'administrateur" },
  { cle: "adminFonction", libelle: "Fonction de l'administrateur" },
  { cle: "adminCin", libelle: "CIN de l'administrateur" },
  { cle: "latitude", libelle: "Latitude" },
  { cle: "longitude", libelle: "Longitude" },
];

export function DialogueImport({ ouvert, onFermer }: { ouvert: boolean; onFermer: () => void }) {
  const queryClient = useQueryClient();
  const [rapport, setRapport] = useState<RapportImport | null>(null);
  const [brouillon, setBrouillon] = useState<LigneImportLaboratoire[]>([]);
  const [ligneOuverte, setLigneOuverte] = useState<number | null>(null);

  function reinitialiser() {
    setRapport(null);
    setBrouillon([]);
    setLigneOuverte(null);
  }

  function appliquer(resultat: RapportImport) {
    setRapport(resultat);
    setBrouillon(resultat.lignes.map((ligne) => ({ ...ligne.donnees })));
  }

  const modele = useMutation({
    mutationFn: telechargerModeleImport,
    onError: (erreur) => toast.error(messageErreur(erreur, "Le modèle n'a pas pu être téléchargé.")),
  });

  const analyse = useMutation({
    mutationFn: analyserImport,
    onSuccess: (resultat) => {
      appliquer(resultat);
      toast.success("Le fichier a été vérifié. Aucune demande n'a encore été créée.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le fichier n'a pas pu être analysé.")),
  });

  const verification = useMutation({
    mutationFn: () => verifierImport(brouillon),
    onSuccess: (resultat) => {
      appliquer(resultat);
      toast.success("La vérification est à jour.");
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La vérification a échoué.")),
  });

  const confirmation = useMutation({
    mutationFn: () => confirmerImport(brouillon),
    onSuccess: async (resultat) => {
      appliquer(resultat);
      toast.success(resultat.message);
      await queryClient.invalidateQueries({ queryKey: ["demandes-integration"] });
      await queryClient.invalidateQueries({ queryKey: ["statistiques-plateforme"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'importation a échoué.")),
  });

  return (
    <Dialogue
      ouvert={ouvert}
      large
      titre="Importer des laboratoires"
      description="Chaque ligne valide devient une demande d'intégration incomplète. Les documents justificatifs restent obligatoires et aucun espace laboratoire n'est créé."
      onFermer={() => {
        reinitialiser();
        onFermer();
      }}
    >
      <div className="flex flex-col gap-4">
        <ol className="grid gap-2 text-xs font-semibold sm:grid-cols-3">
          <Etape numero="1" libelle="Préparer le fichier" actif={!rapport} />
          <Etape numero="2" libelle="Vérifier les lignes" actif={Boolean(rapport?.apercu)} />
          <Etape numero="3" libelle="Compte rendu" actif={Boolean(rapport && !rapport.apercu)} />
        </ol>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            size="sm"
            loading={modele.isPending}
            onClick={() => modele.mutate("xlsx")}
          >
            Télécharger le modèle Excel
          </Button>
          <Button
            variant="secondary"
            size="sm"
            loading={modele.isPending}
            onClick={() => modele.mutate("csv")}
          >
            Télécharger le modèle CSV
          </Button>
        </div>

        <div>
          <Label htmlFor="fichier-import">Fichier complété</Label>
          <Input
            id="fichier-import"
            type="file"
            accept=".xlsx,.csv,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            className="mt-1"
            onChange={(event) => {
              const fichier = event.target.files?.[0];
              if (fichier) analyse.mutate(fichier);
            }}
          />
        </div>

        {analyse.isPending && <p className="text-sm text-muted-foreground">Vérification du fichier…</p>}

        {rapport && (
          <>
            <p className="text-sm text-ink-900">{rapport.message}</p>
            <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
              <table className="w-full min-w-[640px] text-left text-sm">
                <thead className="bg-surface-card text-brand-900">
                  <tr>
                    <th className="px-3 py-2">Ligne</th>
                    <th className="px-3 py-2">Laboratoire</th>
                    <th className="px-3 py-2">Administrateur</th>
                    <th className="px-3 py-2">Résultat</th>
                    <th className="px-3 py-2"> </th>
                  </tr>
                </thead>
                <tbody>
                  {rapport.lignes.map((ligne, index) => (
                    <Fragment key={ligne.numeroLigne}>
                      <tr className="border-t border-brand-900/10">
                        <td className="px-3 py-2">{ligne.numeroLigne}</td>
                        <td className="px-3 py-2">{ligne.nomLaboratoire || "—"}</td>
                        <td className="px-3 py-2">{ligne.adminEmail || "—"}</td>
                        <td className="px-3 py-2">
                          {ligne.valide ? (
                            <Badge variant="lime" size="sm">
                              {ligne.numeroDemande ? `Demande ${ligne.numeroDemande}` : "Valide"}
                            </Badge>
                          ) : (
                            <Badge variant="outline" size="sm">
                              À corriger
                            </Badge>
                          )}
                          {ligne.erreurs.length > 0 && (
                            <ul className="mt-1 list-disc pl-4 text-xs text-red-600">
                              {ligne.erreurs.map((erreur) => (
                                <li key={erreur}>{erreur}</li>
                              ))}
                            </ul>
                          )}
                        </td>
                        <td className="px-3 py-2 text-right">
                          {!ligne.numeroDemande && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                setLigneOuverte((actuelle) =>
                                  actuelle === index ? null : index,
                                )
                              }
                            >
                              Corriger
                            </Button>
                          )}
                        </td>
                      </tr>
                      {ligneOuverte === index && brouillon[index] && (
                        <tr key={`edit-${ligne.numeroLigne}`} className="border-t border-brand-900/10 bg-surface-alt">
                          <td colSpan={5} className="px-3 py-3">
                            <div className="grid gap-3 sm:grid-cols-2">
                              {CHAMPS.map((champ) => (
                                <div key={champ.cle}>
                                  <Label htmlFor={`${champ.cle}-${index}`}>{champ.libelle}</Label>
                                  <Input
                                    id={`${champ.cle}-${index}`}
                                    className="mt-1"
                                    value={String(brouillon[index]?.[champ.cle] ?? "")}
                                    onChange={(event) => {
                                      const copie = [...brouillon];
                                      copie[index] = {
                                        ...copie[index],
                                        [champ.cle]: event.target.value,
                                      };
                                      setBrouillon(copie);
                                    }}
                                  />
                                </div>
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
            {rapport.apercu && (
              <div className="flex flex-wrap justify-end gap-2">
                <Button
                  variant="secondary"
                  loading={verification.isPending}
                  onClick={() => verification.mutate()}
                >
                  Vérifier à nouveau
                </Button>
                <Button
                  loading={confirmation.isPending}
                  disabled={rapport.valides === 0}
                  onClick={() => confirmation.mutate()}
                >
                  Importer les lignes valides
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </Dialogue>
  );
}

function Etape({ numero, libelle, actif }: { numero: string; libelle: string; actif: boolean }) {
  return (
    <li
      className={`rounded-xl px-3 py-2 ${actif ? "bg-brand-900 text-primary-foreground" : "bg-surface-alt text-muted-foreground"}`}
    >
      {numero}. {libelle}
    </li>
  );
}
