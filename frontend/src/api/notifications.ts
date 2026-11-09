import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export interface NotificationRecue {
  id: number;
  titre: string;
  corps?: string | null;
  lien?: string | null;
  type?: string | null;
  dateCreation: string;
  lue: boolean;
  dateLecture?: string | null;
}

export async function listerMesNotifications(limite = 20): Promise<NotificationRecue[]> {
  const res = await apiClient.get<ApiResponse<NotificationRecue[]>>(`/notifications/mes?limite=${limite}`);
  return res.data;
}

export async function compterNotificationsNonLues(): Promise<number> {
  const res = await apiClient.get<ApiResponse<number>>("/notifications/non-lues/count");
  return res.data;
}

export async function marquerNotificationLue(id: number): Promise<void> {
  await apiClient.patch<ApiResponse<null>>(`/notifications/${id}/lire`);
}

export async function marquerToutesNotificationsLues(): Promise<void> {
  await apiClient.patch<ApiResponse<null>>("/notifications/lire-tout");
}
