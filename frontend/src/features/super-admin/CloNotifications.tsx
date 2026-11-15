import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { Bell } from "lucide-react";
import { Button } from "@/components/lab";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  compterNotificationsNonLues,
  listerMesNotifications,
  marquerNotificationLue,
  marquerToutesNotificationsLues,
  type NotificationRecue,
} from "@/api/notifications";


function depuis(valeur: string): string {
  const minutes = Math.round((Date.now() - new Date(valeur).getTime()) / 60_000);
  if (Number.isNaN(minutes)) return "";
  if (minutes < 1) return "à l'instant";
  if (minutes < 60) return `il y a ${minutes} min`;
  const heures = Math.round(minutes / 60);
  if (heures < 24) return `il y a ${heures} h`;
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(new Date(valeur));
}

/** Cloche de notifications : compteur rafraîchi toutes les 30 secondes. */
export function CloNotifications() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const compteur = useQuery({
    queryKey: ["notifications-non-lues"],
    queryFn: compterNotificationsNonLues,
  });
  const liste = useQuery({
    queryKey: ["notifications-mes"],
    queryFn: () => listerMesNotifications(20),
  });

  const invalider = async () => {
    await queryClient.invalidateQueries({ queryKey: ["notifications-non-lues"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications-mes"] });
  };
  const lecture = useMutation({ mutationFn: marquerNotificationLue, onSuccess: invalider });
  const toutLire = useMutation({ mutationFn: marquerToutesNotificationsLues, onSuccess: invalider });

  const nonLues = compteur.data ?? 0;

  const ouvrir = (notification: NotificationRecue) => {
    if (!notification.lue) lecture.mutate(notification.id);
    if (notification.lien?.startsWith("/")) router.history.push(notification.lien);
  };

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="relative"
          aria-label={nonLues > 0 ? `Notifications : ${nonLues} non lue(s)` : "Notifications"}
        >
          <Bell className="size-4" aria-hidden="true" />
          {nonLues > 0 && (
            <span className="absolute -top-0.5 -right-0.5 flex min-w-4 items-center justify-center rounded-full bg-accent-lime px-1 text-[10px] font-bold text-brand-900">
              {nonLues > 99 ? "99+" : nonLues}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between border-b border-brand-900/10 px-4 py-3">
          <p className="text-sm font-bold text-ink-900">Notifications</p>
          <Button variant="ghost" size="sm" disabled={nonLues === 0 || toutLire.isPending} onClick={() => toutLire.mutate()}>
            Tout marquer comme lu
          </Button>
        </div>
        <ul className="max-h-96 overflow-y-auto">
          {liste.isLoading && <li className="px-4 py-6 text-sm text-muted-foreground">Chargement…</li>}
          {liste.data?.length === 0 && <li className="px-4 py-6 text-sm text-muted-foreground">Aucune notification.</li>}
          {liste.data?.map((notification) => (
            <li key={notification.id}>
              <button
                type="button"
                onClick={() => ouvrir(notification)}
                className={`flex w-full flex-col gap-0.5 border-b border-brand-900/5 px-4 py-3 text-left hover:bg-surface-alt focus-visible:bg-surface-alt focus-visible:outline-none ${
                  notification.lue ? "" : "bg-brand-900/5"
                }`}
              >
                <span className="flex items-center gap-2 text-sm font-semibold text-ink-900">
                  {!notification.lue && <span className="size-2 shrink-0 rounded-full bg-brand-900" aria-label="Non lue" />}
                  {notification.titre}
                </span>
                {notification.corps && <span className="text-xs text-muted-foreground">{notification.corps}</span>}
                <span className="text-[11px] text-muted-foreground">{depuis(notification.dateCreation)}</span>
              </button>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
