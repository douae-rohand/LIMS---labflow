import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { DemandeLaboratoire, DemandeResume, StatutDemande } from "./demandes";
import type { PageResponse } from "./utilisateurs";

export type EtatReception = "A_RECEVOIR" | "PARTIELLE" | "COMPLETE";

/** Demande acceptée ou en cours, vue sous l'angle de la réception des échantillons. */
export interface DemandeReception {
  demandeId: number;
  reference: string;
  titre?: string | null;
  client?: string | null;
  patient?: string | null;
  statut: string;
  dateDecision?: string | null;
  derniereReception?: string | null;
  analyses: number;
  enAttente: number;
  recues: number;
  nonConformes: number;
  etat: EtatReception;
}

export interface ExigenceEchantillon {
  nature: string;
  etatConservation?: string | null;
  quantiteMinimale?: number | null;
  unite?: string | null;
}

export interface LigneReception {
  id: number;
  code: string;
  essaiCode?: string | null;
  essai?: string | null;
  domaine?: string | null;
  technicien?: string | null;
  statut?: string | null;
  aRecevoir: boolean;
  echantillonReference?: string | null;
  echantillonConforme?: boolean | null;
  exigences: ExigenceEchantillon[];
}

export interface NatureAttendue {
  nature?: string | null;
  etatConservation?: string | null;
  quantite?: string | null;
  ligneIds: number[];
}

export interface EchantillonRecu {
  id: number;
  reference: string;
  nature: string;
  dateReception: string;
  conforme: boolean;
  motif?: string | null;
  etatConservationConstate?: string | null;
  receptionneur?: string | null;
  demandeId?: number | null;
  demandeReference?: string | null;
  analyses: string[];
}

export interface DetailReception {
  demande: DemandeReception;
  lignes: LigneReception[];
  naturesAttendues: NatureAttendue[];
  echantillons: EchantillonRecu[];
  /** Raison sociale du laboratoire (étiquettes). */
  laboratoire?: string | null;
}

export interface EchantillonARecevoir {
  nature: string;
  conforme: boolean;
  motif?: string | undefined;
  etatConservationConstate?: string | undefined;
  ligneIds: number[];
}

export interface TableauAccueil {
  demandesARecevoir: number;
  receptionsPartielles: number;
  echantillonsRecusAujourdhui: number;
  nonConformesAujourdhui: number;
  facturesAEncaisser: number;
  resteAEncaisser: number;
  prochainesReceptions: DemandeReception[];
  dernieresReceptions: EchantillonRecu[];
}

export async function lireTableauAccueil(): Promise<TableauAccueil> {
  const res = await apiClient.get<ApiResponse<TableauAccueil>>("/accueil/tableau");
  return res.data;
}

export async function listerReceptions(): Promise<DemandeReception[]> {
  const res = await apiClient.get<ApiResponse<DemandeReception[]>>("/accueil/receptions");
  return res.data;
}

export async function lireReception(demandeId: number): Promise<DetailReception> {
  const res = await apiClient.get<ApiResponse<DetailReception>>(`/accueil/receptions/${demandeId}`);
  return res.data;
}

export async function receptionner(demandeId: number, echantillons: EchantillonARecevoir[]): Promise<DetailReception> {
  const res = await apiClient.post<ApiResponse<DetailReception>>(`/accueil/receptions/${demandeId}`, { echantillons });
  return res.data;
}

// ------------------------------------------------------------- caisse (encaissement des factures)
export type ModePaiement = "ESPECES" | "CHEQUE" | "CARTE" | "VIREMENT";

export interface PaiementCaisse {
  id: number;
  code: string;
  date: string;
  montant: number;
  mode: ModePaiement;
  reference?: string | null;
  encaisseur?: string | null;
  /** Écriture d'annulation (montant négatif). */
  contrePassation: boolean;
  /** Encaissement déjà annulé. */
  annule: boolean;
  /** Encaissement du jour encore annulable. */
  annulable: boolean;
}

export interface FactureCaisse {
  id: number;
  numero: string;
  dateFacture?: string | null;
  dateEcheance?: string | null;
  montant: number;
  paye: number;
  reste: number;
  statut: string;
  enRetard: boolean;
  demandeId?: number | null;
  demandeReference?: string | null;
  demandeTitre?: string | null;
  client?: string | null;
  clientIce?: string | null;
  dernierPaiement?: string | null;
  paiements: PaiementCaisse[];
}

export interface Encaissement {
  facture: FactureCaisse;
  paiement: PaiementCaisse | null;
}

export async function listerFacturesCaisse(): Promise<FactureCaisse[]> {
  const res = await apiClient.get<ApiResponse<FactureCaisse[]>>("/accueil/caisse");
  return res.data;
}

export async function encaisser(
  factureId: number,
  paiement: { montant: number; mode: ModePaiement; reference?: string | undefined },
): Promise<Encaissement> {
  const res = await apiClient.post<ApiResponse<Encaissement>>(`/accueil/caisse/${factureId}/paiements`, paiement);
  return res.data;
}

/** Ouvre le reçu PDF d'un paiement dans un nouvel onglet. */
export async function ouvrirRecu(paiementId: number): Promise<void> {
  const blob = await apiClient.getBlob(`/accueil/caisse/paiements/${paiementId}/recu`);
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Annule un encaissement du jour par contre-passation (motif obligatoire). */
export async function annulerEncaissement(paiementId: number, motif: string): Promise<Encaissement> {
  const res = await apiClient.post<ApiResponse<Encaissement>>(`/accueil/caisse/paiements/${paiementId}/annuler`, { motif });
  return res.data;
}

export interface LigneJournal {
  paiementId: number;
  code: string;
  date: string;
  montant: number;
  mode: ModePaiement;
  facture?: string | null;
  client?: string | null;
  encaisseur?: string | null;
  contrePassation: boolean;
  reference?: string | null;
}

export interface JournalCaisse {
  jour: string;
  lignes: LigneJournal[];
  totaux: { mode: ModePaiement; operations: number; montant: number }[];
  total: number;
  annulations: number;
}

export async function lireJournalCaisse(jour: string): Promise<JournalCaisse> {
  const res = await apiClient.get<ApiResponse<JournalCaisse>>(`/accueil/caisse/journal?jour=${encodeURIComponent(jour)}`);
  return res.data;
}

/** Journal de caisse PDF (clôture du jour). */
export async function ouvrirJournalCaisse(jour: string): Promise<void> {
  const blob = await apiClient.getBlob(`/accueil/caisse/journal/pdf?jour=${encodeURIComponent(jour)}`);
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export type FormatEtiquettes = "PLANCHE" | "UNITAIRE";

/**
 * Étiquettes code-barres des échantillons reçus d'une demande (PDF) : planche A4 ou une étiquette
 * par page (imprimante d'étiquettes) ; un seul échantillon si {@code echantillonId} est donné.
 */
export async function ouvrirEtiquettes(
  demandeId: number,
  options: { echantillonId?: number; format?: FormatEtiquettes } = {},
): Promise<void> {
  const params = new URLSearchParams();
  if (options.echantillonId != null) params.set("echantillonId", String(options.echantillonId));
  if (options.format) params.set("format", options.format);
  const suffixe = params.toString() ? `?${params.toString()}` : "";
  const blob = await apiClient.getBlob(`/accueil/receptions/${demandeId}/etiquettes${suffixe}`);
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Code-barres PNG d'un échantillon, en URL locale (à révoquer après usage). */
export async function urlCodeBarres(demandeId: number, echantillonId: number): Promise<string> {
  const blob = await apiClient.getBlob(`/accueil/receptions/${demandeId}/echantillons/${echantillonId}/code-barres`);
  return URL.createObjectURL(new Blob([blob], { type: "image/png" }));
}

// ------------------------------------------------------------- clients et patients
export type TypeClient = "PARTICULIER" | "ENTREPRISE";

export interface ClientAccueil {
  utilisateurId: number;
  nomComplet: string;
  email: string;
  telephone?: string | null;
  type: TypeClient;
  raisonSociale?: string | null;
  ice?: string | null;
  adresse?: string | null;
  compteActive: boolean;
  datePremierContact?: string | null;
}

export interface NouveauClient {
  type: TypeClient;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  raisonSociale: string;
  ice: string;
  adresse: string;
  consentementCndp: boolean;
}

export interface Patient {
  id: number;
  code: string;
  nom: string;
  prenom: string;
  dateNaissance?: string | null;
  sexe?: string | null;
  cin?: string | null;
  telephone?: string | null;
  email?: string | null;
  adresse?: string | null;
}

export interface FichePatient {
  nom: string;
  prenom: string;
  dateNaissance: string | null;
  sexe: string;
  cin: string;
  telephone: string;
  email: string;
  adresse: string;
  consentementCndp: boolean;
}

export async function listerClients(): Promise<ClientAccueil[]> {
  const res = await apiClient.get<ApiResponse<ClientAccueil[]>>("/accueil/clients");
  return res.data;
}

export async function creerClient(client: NouveauClient): Promise<ClientAccueil> {
  const res = await apiClient.post<ApiResponse<ClientAccueil>>("/accueil/clients", client);
  return res.data;
}

export async function rattacherClient(email: string): Promise<ClientAccueil> {
  const res = await apiClient.post<ApiResponse<ClientAccueil>>("/accueil/clients/rattacher", { email });
  return res.data;
}

export async function renvoyerInvitationClient(utilisateurId: number): Promise<ClientAccueil> {
  const res = await apiClient.post<ApiResponse<ClientAccueil>>(`/accueil/clients/${utilisateurId}/invitation`, {});
  return res.data;
}

export async function listerPatients(): Promise<Patient[]> {
  const res = await apiClient.get<ApiResponse<Patient[]>>("/accueil/patients");
  return res.data;
}

export async function enregistrerPatient(fiche: FichePatient, id?: number): Promise<Patient> {
  const res = id
    ? await apiClient.put<ApiResponse<Patient>>(`/accueil/patients/${id}`, fiche)
    : await apiClient.post<ApiResponse<Patient>>("/accueil/patients", fiche);
  return res.data;
}

// ------------------------------------------------------------- suivi des demandes (lecture seule)

export interface EtapeSuivi {
  libelle: string;
  action: string;
  statutAvant?: string | null;
  statutApres?: string | null;
  motif?: string | null;
  date: string;
  acteur: string;
}

export interface SuiviDemande {
  demande: DemandeLaboratoire;
  avancement: { analyses: number; attenteEchantillon: number; enCours: number; terminees: number; nonRealisables: number };
  historique: EtapeSuivi[];
  facture: { id: number; numero: string; montant: number; paye: number; reste: number; statut: string } | null;
  rapport: { numero: string; statut: string; dateSignature?: string | null; diffuse: boolean } | null;
}

export async function listerDemandesSuivi(params: {
  q?: string | undefined;
  statut?: StatutDemande | undefined;
  page: number;
}): Promise<PageResponse<DemandeResume>> {
  const res = await apiClient.get<ApiResponse<PageResponse<DemandeResume>>>("/accueil/demandes", {
    params: { q: params.q || undefined, statut: params.statut, page: params.page, size: 20 },
  });
  return res.data;
}

export async function lireSuiviDemande(id: number): Promise<SuiviDemande> {
  const res = await apiClient.get<ApiResponse<SuiviDemande>>(`/accueil/demandes/${id}`);
  return res.data;
}

// ------------------------------------------------------------- demande sur site
/** Essai du catalogue proposé à l'accueil. */
export interface EssaiCatalogueAccueil {
  id: number;
  code: string;
  designation: string;
  domaineCode?: string | null;
  domaineLibelle?: string | null;
  prix: number;
  dureeEstimeeMinutes?: number | null;
}

/** CLIENT : compte client, décision du responsable. PATIENT : patient sur site, acceptée d'office. */
export type TypeDemandeAccueil = "CLIENT" | "PATIENT";

export interface NouvelleDemandeAccueil {
  type: TypeDemandeAccueil;
  clientUtilisateurId?: number | undefined;
  patientId?: number | undefined;
  titre: string;
  objectif?: string | undefined;
  essaiIds: number[];
}

export interface DemandeCreee {
  id: number;
  numero: string;
  nombreAnalyses: number;
  totalTtc: number;
  statut: string;
}

export async function listerCatalogueAccueil(): Promise<EssaiCatalogueAccueil[]> {
  const res = await apiClient.get<ApiResponse<EssaiCatalogueAccueil[]>>("/accueil/catalogue");
  return res.data;
}

export async function creerDemandeAccueil(demande: NouvelleDemandeAccueil): Promise<DemandeCreee> {
  const res = await apiClient.post<ApiResponse<DemandeCreee>>("/accueil/demandes", demande);
  return res.data;
}

/** Facture ou avoir PDF remis au guichet. */
export async function ouvrirFactureCaisse(factureId: number, avoir = false): Promise<void> {
  const blob = await apiClient.getBlob(`/accueil/caisse/factures/${factureId}/${avoir ? "avoir" : "pdf"}`);
  const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
