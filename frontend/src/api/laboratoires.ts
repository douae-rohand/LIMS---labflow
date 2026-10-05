import { apiClient } from "./axios";
import {
  unwrapApi,
  type ApiEnvelope,
  type LaboratoirePublic,
} from "./landing";

export type AnalysePublique = {
  id: number;
  code: string;
  designation: string;
  description?: string | null;
  methode?: string | null;
  domaineCode?: string | null;
  domaineLibelle?: string | null;
  tarif?: number | string | null;
  dureeEstimee?: number | null;
  unite?: string | null;
  actif?: boolean | null;
};

export async function fetchLaboratoiresPublics(): Promise<LaboratoirePublic[]> {
  const envelope = await apiClient.get<ApiEnvelope<LaboratoirePublic[]>>(
    "/public/laboratoires",
  );
  return unwrapApi(envelope, "Impossible de charger les laboratoires") ?? [];
}

export async function fetchLaboratoirePublic(id: number): Promise<LaboratoirePublic> {
  const envelope = await apiClient.get<ApiEnvelope<LaboratoirePublic>>(
    `/public/laboratoires/${id}`,
  );
  return unwrapApi(envelope, "Laboratoire introuvable");
}

export async function fetchAnalysesLaboratoire(id: number): Promise<AnalysePublique[]> {
  const envelope = await apiClient.get<ApiEnvelope<AnalysePublique[]>>(
    `/public/laboratoires/${id}/analyses`,
  );
  return unwrapApi(envelope, "Impossible de charger les analyses") ?? [];
}
