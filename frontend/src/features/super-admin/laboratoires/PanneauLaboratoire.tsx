import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { trouverDemandeDuLaboratoire } from "@/api/plateforme";
import { IndicateurEtapes } from "@/components/integration/IndicateurEtapes";
import { LocationPicker } from "@/components/integration/LocationPicker";
import { TYPES_LABORATOIRE } from "@/components/integration/constants";
import {
  modifierAdministrateurLaboratoire,
  modifierLaboratoire,
  type LaboratoireAdmin,
} from "@/api/gestionLaboratoires";
import { ListeDocuments } from "../documents/ListeDocuments";
import { Panneau } from "../Fenetres";
import { formaterDate, messageErreur, texte } from "../texte";

const ETAPES = ["Laboratoire", "Localisation", "Administrateur", "Documents"] as const;

export function PanneauLaboratoire({
  laboratoire,
  modeInitial,
  onFermer,
  onDemanderStatut,
}: {
  laboratoire: LaboratoireAdmin | null;
  modeInitial: "consulter" | "modifier";
  onFermer: () => void;
  onDemanderStatut: (laboratoire: LaboratoireAdmin) => void;
}) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState(modeInitial);
  const [etape, setEtape] = useState(0);
  const [formulaire, setFormulaire] = useState(() => versFormulaire(laboratoire));
  const [erreurNom, setErreurNom] = useState("");
  const [erreurCoordonnees, setErreurCoordonnees] = useState("");
  const [erreurAdmin, setErreurAdmin] = useState("");
  const [confirmerFermeture, setConfirmerFermeture] = useState(false);
  const sale =
    mode === "modifier" &&
    JSON.stringify(formulaire) !== JSON.stringify(versFormulaire(laboratoire));

  useEffect(() => {
    setMode(modeInitial);
    setEtape(0);
    setFormulaire(versFormulaire(laboratoire));
    setErreurNom("");
    setErreurCoordonnees("");
    setErreurAdmin("");
    setConfirmerFermeture(false);
  }, [laboratoire, modeInitial]);

  const dossier = useQuery({
    queryKey: ["demande-laboratoire", laboratoire?.id],
    queryFn: () => trouverDemandeDuLaboratoire(laboratoire?.id as number),
    enabled: laboratoire != null,
  });

  const mutation = useMutation({
    mutationFn: async () => {
      if (!laboratoire) return;
      const latitude = formulaire.latitude.trim() ? Number(formulaire.latitude) : null;
      const longitude = formulaire.longitude.trim() ? Number(formulaire.longitude) : null;
      if ((latitude == null) !== (longitude == null) || Number.isNaN(latitude) || Number.isNaN(longitude)) {
        throw new Error("COORDONNEES");
      }
      await modifierLaboratoire(laboratoire.id, {
        nom: formulaire.nom.trim(),
        ice: formulaire.ice.trim(),
        typeLaboratoire: formulaire.types.join(", "),
        adresse: formulaire.adresse.trim(),
        ville: formulaire.ville.trim(),
        region: formulaire.region.trim(),
        pays: formulaire.pays.trim(),
        codePostal: formulaire.codePostal.trim(),
        telephone: formulaire.telephone.trim(),
        email: formulaire.email.trim(),
        siteWeb: formulaire.siteWeb.trim(),
        latitude,
        longitude,
      });
      if (laboratoire.administrateur) {
        await modifierAdministrateurLaboratoire(laboratoire.id, {
          id: laboratoire.administrateur.id,
          nom: formulaire.adminNom.trim(),
          prenom: formulaire.adminPrenom.trim(),
          email: formulaire.adminEmail.trim(),
          telephone: formulaire.adminTelephone.trim(),
          fonction: formulaire.adminFonction.trim(),
          cin: formulaire.adminCin.trim(),
        });
      }
    },
    onSuccess: async () => {
      toast.success("Les informations ont été enregistrées.");
      await queryClient.invalidateQueries({ queryKey: ["laboratoires-admin"] });
      await queryClient.invalidateQueries({ queryKey: ["statistiques-plateforme"] });
      onFermer();
    },
    onError: (erreur) => {
      if (erreur instanceof Error && erreur.message === "COORDONNEES") {
        setEtape(1);
        setErreurCoordonnees("Indiquez une latitude et une longitude valides, ou laissez les deux vides.");
        return;
      }
      if (erreur instanceof ApiError) {
        toast.error(messageErreur(erreur, "L'enregistrement a échoué."));
      } else {
        toast.error("L'enregistrement a échoué.");
      }
    },
  });

  function demanderFermeture() {
    if (sale) setConfirmerFermeture(true);
    else onFermer();
  }

  function validerEtape(index: number): boolean {
    if (mode !== "modifier" || !laboratoire) return true;
    if (index === 0 && !formulaire.nom.trim()) {
      setErreurNom("Le nom du laboratoire est obligatoire.");
      setEtape(0);
      return false;
    }
    if (index === 1) {
      const latitude = formulaire.latitude.trim() ? Number(formulaire.latitude) : null;
      const longitude = formulaire.longitude.trim() ? Number(formulaire.longitude) : null;
      if ((latitude == null) !== (longitude == null) || Number.isNaN(latitude) || Number.isNaN(longitude)) {
        setErreurCoordonnees("Indiquez une latitude et une longitude valides, ou laissez les deux vides.");
        setEtape(1);
        return false;
      }
    }
    if (index === 2 && laboratoire.administrateur) {
      const emailValide = formulaire.adminEmail.includes("@");
      if (!formulaire.adminPrenom.trim() || !formulaire.adminNom.trim() || !emailValide) {
        setErreurAdmin("Le prénom, le nom et une adresse e-mail valide sont obligatoires.");
        setEtape(2);
        return false;
      }
    }
    return true;
  }

  function aller(index: number) {
    if (index > etape) {
      for (let courant = etape; courant < index; courant += 1) {
        if (!validerEtape(courant)) return;
      }
    }
    setEtape(index);
  }

  function enregistrer() {
    setErreurNom("");
    setErreurCoordonnees("");
    setErreurAdmin("");
    if (![0, 1, 2].every((index) => validerEtape(index))) return;
    mutation.mutate();
  }

  const latitude = formulaire.latitude.trim() ? Number(formulaire.latitude) : null;
  const longitude = formulaire.longitude.trim() ? Number(formulaire.longitude) : null;
  const aDesCoordonnees = latitude != null && longitude != null && !Number.isNaN(latitude) && !Number.isNaN(longitude);

  return (
    <>
      <Panneau
        ouvert={laboratoire !== null}
        large
        titre={mode === "modifier" ? "Modifier le laboratoire" : "Détails du laboratoire"}
        onFermer={demanderFermeture}
      >
        {laboratoire && (
          <div className="flex flex-col gap-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <Badge variant={laboratoire.actif ? "lime" : "outline"}>
                {laboratoire.actif ? "Actif" : "Désactivé"}
              </Badge>
              <div className="flex gap-2">
                {mode === "consulter" && (
                  <Button variant="secondary" size="sm" onClick={() => setMode("modifier")}>
                    Modifier
                  </Button>
                )}
                <Button variant="ghost" size="sm" onClick={() => onDemanderStatut(laboratoire)}>
                  {laboratoire.actif ? "Désactiver" : "Activer"}
                </Button>
              </div>
            </div>

            <IndicateurEtapes etapes={ETAPES} etape={etape} onAller={aller} autoriserSuivantes />

            {etape === 0 && (
              mode === "consulter" ? (
                <Grille>
                  <Lecture libelle="Nom du laboratoire" valeur={laboratoire.nom} />
                  <Lecture libelle="Identifiant" valeur={laboratoire.code} />
                  <Lecture libelle="Identifiant ICE" valeur={texte(laboratoire.ice)} />
                  <Lecture libelle="Types" valeur={laboratoire.typesLaboratoire?.join(", ") || texte(laboratoire.typeLaboratoire)} />
                  <Lecture libelle="Téléphone" valeur={texte(laboratoire.telephone)} />
                  <Lecture libelle="Adresse e-mail" valeur={texte(laboratoire.emailContact)} />
                  <Lecture libelle="Site web" valeur={texte(laboratoire.siteWeb)} />
                  <Lecture libelle="Date d'intégration" valeur={formaterDate(laboratoire.dateCreation)} />
                  <Lecture libelle="Espace dédié" valeur={texte(laboratoire.schemaName)} />
                </Grille>
              ) : (
                <div className="flex flex-col gap-4">
                  <Champ label="Nom du laboratoire" value={formulaire.nom} onChange={(nom) => setFormulaire({ ...formulaire, nom })} required erreur={erreurNom} />
                  <p className="text-sm text-muted-foreground">Identifiant : {laboratoire.code}. Il ne peut pas être modifié.</p>
                  <Champ label="Identifiant ICE" value={formulaire.ice} onChange={(ice) => setFormulaire({ ...formulaire, ice })} />
                  <div>
                    <Label>Types de laboratoire</Label>
                    <div className="mt-2 flex flex-col gap-2">
                      {TYPES_LABORATOIRE.map((type) => (
                        <label key={type} className="flex items-center gap-2 text-sm text-ink-900">
                          <input
                            type="checkbox"
                            checked={formulaire.types.includes(type)}
                            onChange={(event) => {
                              const types = event.target.checked
                                ? [...formulaire.types, type]
                                : formulaire.types.filter((valeur) => valeur !== type);
                              setFormulaire({ ...formulaire, types });
                            }}
                          />
                          {type}
                        </label>
                      ))}
                    </div>
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Champ label="Téléphone" value={formulaire.telephone} onChange={(telephone) => setFormulaire({ ...formulaire, telephone })} />
                    <Champ label="Adresse e-mail" value={formulaire.email} onChange={(email) => setFormulaire({ ...formulaire, email })} type="email" />
                  </div>
                  <Champ label="Site web" value={formulaire.siteWeb} onChange={(siteWeb) => setFormulaire({ ...formulaire, siteWeb })} />
                </div>
              )
            )}

            {etape === 1 && (
              <div className="flex flex-col gap-4">
                {mode === "consulter" ? (
                  <Grille>
                    <Lecture libelle="Adresse" valeur={texte(laboratoire.adresse)} />
                    <Lecture libelle="Ville" valeur={texte(laboratoire.ville)} />
                    <Lecture libelle="Région" valeur={texte(laboratoire.region)} />
                    <Lecture libelle="Code postal" valeur={texte(laboratoire.codePostal)} />
                    <Lecture libelle="Pays" valeur={texte(laboratoire.pays)} />
                  </Grille>
                ) : (
                  <>
                    <Champ label="Adresse" value={formulaire.adresse} onChange={(adresse) => setFormulaire({ ...formulaire, adresse })} />
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Champ label="Ville" value={formulaire.ville} onChange={(ville) => setFormulaire({ ...formulaire, ville })} />
                      <Champ label="Région" value={formulaire.region} onChange={(region) => setFormulaire({ ...formulaire, region })} />
                      <Champ label="Code postal" value={formulaire.codePostal} onChange={(codePostal) => setFormulaire({ ...formulaire, codePostal })} />
                      <Champ label="Pays" value={formulaire.pays} onChange={(pays) => setFormulaire({ ...formulaire, pays })} />
                    </div>
                    {erreurCoordonnees && <p className="text-xs text-red-600">{erreurCoordonnees}</p>}
                  </>
                )}
                {(mode === "modifier" || aDesCoordonnees) && (
                  <LocationPicker
                    readOnly={mode === "consulter"}
                    label={formulaire.nom || laboratoire.nom}
                    addressQuery={[formulaire.adresse, formulaire.ville, formulaire.pays].filter(Boolean).join(", ")}
                    value={{
                      latitude: aDesCoordonnees ? latitude : null,
                      longitude: aDesCoordonnees ? longitude : null,
                    }}
                    onChange={({ latitude: lat, longitude: lng }) =>
                      setFormulaire((actuel) => ({ ...actuel, latitude: String(lat), longitude: String(lng) }))
                    }
                    onAddressResolved={(patch) =>
                      setFormulaire((actuel) => ({
                        ...actuel,
                        adresse: patch.adresse ?? actuel.adresse,
                        ville: patch.ville ?? actuel.ville,
                        region: patch.region ?? actuel.region,
                        pays: patch.pays ?? actuel.pays,
                        codePostal: patch.codePostal ?? actuel.codePostal,
                      }))
                    }
                  />
                )}
                {mode === "consulter" && !aDesCoordonnees && (
                  <p className="text-sm text-muted-foreground">Aucune coordonnée géographique n'est enregistrée.</p>
                )}
              </div>
            )}

            {etape === 2 && (
              mode === "consulter" ? (
                laboratoire.administrateur ? (
                  <Grille>
                    <Lecture libelle="Prénom" valeur={texte(laboratoire.administrateur.prenom)} />
                    <Lecture libelle="Nom" valeur={laboratoire.administrateur.nom} />
                    <Lecture libelle="Adresse e-mail" valeur={laboratoire.administrateur.email} />
                    <Lecture libelle="Téléphone" valeur={texte(laboratoire.administrateur.telephone)} />
                    <Lecture libelle="Fonction" valeur={texte(laboratoire.administrateur.fonction)} />
                    <Lecture libelle="CIN" valeur={texte(laboratoire.administrateur.cin)} />
                    <Lecture
                      libelle="Compte"
                      valeur={laboratoire.administrateur.actif ? "Compte activé" : "Compte en attente d'activation"}
                    />
                  </Grille>
                ) : (
                  <p className="text-sm text-muted-foreground">Aucun compte administrateur n'est rattaché à ce laboratoire.</p>
                )
              ) : laboratoire.administrateur ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <Champ label="Prénom" value={formulaire.adminPrenom} onChange={(adminPrenom) => setFormulaire({ ...formulaire, adminPrenom })} required erreur={erreurAdmin} />
                  <Champ label="Nom" value={formulaire.adminNom} onChange={(adminNom) => setFormulaire({ ...formulaire, adminNom })} required />
                  <Champ label="Adresse e-mail" value={formulaire.adminEmail} onChange={(adminEmail) => setFormulaire({ ...formulaire, adminEmail })} type="email" required />
                  <Champ label="Téléphone" value={formulaire.adminTelephone} onChange={(adminTelephone) => setFormulaire({ ...formulaire, adminTelephone })} />
                  <Champ label="Fonction" value={formulaire.adminFonction} onChange={(adminFonction) => setFormulaire({ ...formulaire, adminFonction })} />
                  <Champ label="CIN" value={formulaire.adminCin} onChange={(adminCin) => setFormulaire({ ...formulaire, adminCin })} />
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">Aucun compte administrateur n'est rattaché à ce laboratoire.</p>
              )
            )}

            {etape === 3 && (
              dossier.isError ? (
                <p className="text-sm text-red-600">Les documents n'ont pas pu être chargés.</p>
              ) : (
                <ListeDocuments
                  demandeId={dossier.data?.id ?? null}
                  documents={dossier.data?.documents}
                  chargement={dossier.isLoading}
                  introuvable={!dossier.isLoading && dossier.data == null}
                />
              )
            )}

            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-brand-900/10 pt-4">
              <Button type="button" variant="ghost" onClick={demanderFermeture}>
                Annuler
              </Button>
              <div className="flex gap-2">
                <Button type="button" variant="secondary" disabled={etape === 0} onClick={() => setEtape((actuel) => Math.max(0, actuel - 1))}>
                  Précédent
                </Button>
                {etape < ETAPES.length - 1 ? (
                  <Button type="button" onClick={() => aller(etape + 1)}>
                    Suivant
                  </Button>
                ) : mode === "modifier" ? (
                  <Button type="button" loading={mutation.isPending} onClick={enregistrer}>
                    Enregistrer
                  </Button>
                ) : null}
              </div>
            </div>
          </div>
        )}
      </Panneau>
      {confirmerFermeture && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-ink-900/40 p-4">
          <div role="alertdialog" aria-labelledby="titre-abandon" className="w-full max-w-md rounded-3xl bg-card p-5">
            <h3 id="titre-abandon" className="text-lg font-extrabold text-ink-900">
              Abandonner les modifications ?
            </h3>
            <p className="mt-2 text-sm text-muted-foreground">Les changements non enregistrés seront perdus.</p>
            <div className="mt-4 flex justify-end gap-2">
              <Button variant="ghost" onClick={() => setConfirmerFermeture(false)}>
                Continuer la saisie
              </Button>
              <Button
                onClick={() => {
                  setConfirmerFermeture(false);
                  onFermer();
                }}
              >
                Abandonner
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function Grille({ children }: { children: ReactNode }) {
  return <dl className="grid gap-3 sm:grid-cols-2">{children}</dl>;
}

function Lecture({ libelle, valeur }: { libelle: string; valeur: string }) {
  return (
    <div className="rounded-2xl bg-surface-alt px-3 py-2">
      <dt className="text-xs font-semibold text-muted-foreground">{libelle}</dt>
      <dd className="text-sm text-ink-900">{valeur}</dd>
    </div>
  );
}

function Champ({
  label,
  value,
  onChange,
  type = "text",
  required,
  erreur,
}: {
  label: string;
  value: string;
  onChange: (valeur: string) => void;
  type?: string;
  required?: boolean;
  erreur?: string;
}) {
  const id = label.toLowerCase().replace(/[^a-z0-9]+/g, "-");
  return (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>
        {label}
        {required ? " *" : ""}
      </Label>
      <Input
        id={id}
        type={type}
        value={value}
        required={required}
        aria-invalid={erreur ? true : undefined}
        aria-describedby={erreur ? `${id}-erreur` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {erreur && (
        <p id={`${id}-erreur`} className="text-xs text-red-600">
          {erreur}
        </p>
      )}
    </div>
  );
}

function versFormulaire(laboratoire: LaboratoireAdmin | null) {
  return {
    nom: laboratoire?.nom ?? "",
    ice: laboratoire?.ice ?? "",
    types: laboratoire?.typesLaboratoire ?? [],
    adresse: laboratoire?.adresse ?? "",
    ville: laboratoire?.ville ?? "",
    region: laboratoire?.region ?? "",
    pays: laboratoire?.pays ?? "",
    codePostal: laboratoire?.codePostal ?? "",
    telephone: laboratoire?.telephone ?? "",
    email: laboratoire?.emailContact ?? "",
    siteWeb: laboratoire?.siteWeb ?? "",
    latitude: laboratoire?.latitude == null ? "" : String(laboratoire.latitude),
    longitude: laboratoire?.longitude == null ? "" : String(laboratoire.longitude),
    adminNom: laboratoire?.administrateur?.nom ?? "",
    adminPrenom: laboratoire?.administrateur?.prenom ?? "",
    adminEmail: laboratoire?.administrateur?.email ?? "",
    adminTelephone: laboratoire?.administrateur?.telephone ?? "",
    adminFonction: laboratoire?.administrateur?.fonction ?? "",
    adminCin: laboratoire?.administrateur?.cin ?? "",
  };
}
