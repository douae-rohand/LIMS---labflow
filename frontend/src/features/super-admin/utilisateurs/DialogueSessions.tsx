import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Monitor } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/lab";
import {
  listerSessionsCompte,
  revoquerSessionsCompte,
  type CompteUtilisateur,
} from "@/api/plateformeUtilisateurs";
import { Dialogue } from "../Fenetres";
import { messageErreur, texte } from "../texte";

function formaterDateHeure(valeur: string): string {
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

/** Résumé lisible d'un User-Agent : navigateur et système. */
function decrireAppareil(agent?: string | null): string {
  if (!agent) return "Appareil inconnu";
  const navigateur = /Edg\//.test(agent)
    ? "Edge"
    : /Chrome\//.test(agent)
      ? "Chrome"
      : /Firefox\//.test(agent)
        ? "Firefox"
        : /Safari\//.test(agent)
          ? "Safari"
          : "Navigateur";
  const systeme = /Windows/.test(agent)
    ? "Windows"
    : /Android/.test(agent)
      ? "Android"
      : /iPhone|iPad/.test(agent)
        ? "iOS"
        : /Mac OS/.test(agent)
          ? "macOS"
          : /Linux/.test(agent)
            ? "Linux"
            : "";
  return systeme ? `${navigateur} · ${systeme}` : navigateur;
}

export function DialogueSessions({
  compte,
  modifiable,
  onFermer,
}: {
  compte: CompteUtilisateur | null;
  modifiable: boolean;
  onFermer: () => void;
}) {
  const queryClient = useQueryClient();
  const sessions = useQuery({
    queryKey: ["sessions-compte", compte?.id],
    queryFn: () => listerSessionsCompte(compte?.id as number),
    enabled: compte != null,
  });
  const revocation = useMutation({
    mutationFn: () => revoquerSessionsCompte(compte?.id as number),
    onSuccess: async (nombre) => {
      toast.success(nombre > 0 ? `${nombre} session(s) fermée(s).` : "Aucune session à fermer.");
      await queryClient.invalidateQueries({ queryKey: ["sessions-compte", compte?.id] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Les sessions n'ont pas pu être fermées.")),
  });

  const nom = compte ? `${texte(compte.prenom)} ${compte.nom}`.trim() : "";
  const liste = sessions.data ?? [];

  return (
    <Dialogue
      ouvert={compte != null}
      titre={`Sessions de ${nom}`}
      description="Appareils sur lesquels ce compte reste connecté. Fermer les sessions oblige à se reconnecter ; l'accès en cours expire au plus tard 15 minutes après."
      onFermer={onFermer}
    >
      <div className="flex flex-col gap-4 text-sm">
        {sessions.isLoading && <p className="text-muted-foreground">Chargement des sessions…</p>}
        {sessions.isError && <p className="text-red-600">Les sessions n'ont pas pu être chargées.</p>}
        {!sessions.isLoading && !sessions.isError && liste.length === 0 && (
          <p className="rounded-2xl bg-surface-alt px-4 py-3 text-ink-900">Aucune session ouverte.</p>
        )}
        {liste.length > 0 && (
          <ul className="flex flex-col gap-2">
            {liste.map((item) => (
              <li key={item.id} className="flex items-start gap-3 rounded-2xl border border-brand-900/10 px-3 py-2">
                <Monitor className="mt-0.5 size-4 shrink-0 text-brand-900" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="font-semibold text-ink-900">{decrireAppareil(item.appareil)}</p>
                  <p className="text-xs text-muted-foreground">
                    Ouverte le {formaterDateHeure(item.ouverture)} · expire le {formaterDateHeure(item.expiration)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onFermer}>
            Fermer
          </Button>
          {modifiable && (
            <Button
              variant="secondary"
              loading={revocation.isPending}
              disabled={liste.length === 0}
              onClick={() => revocation.mutate()}
            >
              Déconnecter partout
            </Button>
          )}
        </div>
      </div>
    </Dialogue>
  );
}
