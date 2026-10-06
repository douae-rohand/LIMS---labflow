import { createFileRoute, Link } from "@tanstack/react-router";
import { useState, useEffect, useCallback } from "react";
import {
  Users,
  UserPlus,
  Search,
  RefreshCw,
  ToggleLeft,
  ToggleRight,
  Send,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { Badge, Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  listerUtilisateurs,
  desactiverUtilisateur,
  activerUtilisateur,
  renvoyerInvitation,
  type UtilisateurDto,
} from "@/api/utilisateurs";
import type { RoleUtilisateur } from "@/api/session";
import { ApiError } from "@/api/axios";

export const Route = createFileRoute("/app/utilisateurs/")({
  head: () => ({
    meta: [{ title: "Utilisateurs — LabFlow LIMS" }],
  }),
  component: UtilisateursPage,
});

// ---------------------------------------------------------------------------
// Constantes
// ---------------------------------------------------------------------------

const LIBELLE_ROLE: Record<RoleUtilisateur, string> = {
  SUPER_ADMINISTRATEUR: "Super Administrateur",
  ADMINISTRATEUR: "Administrateur",
  RESPONSABLE: "Responsable laboratoire",
  TECHNICIEN: "Technicien",
  ACCUEIL: "Agent d'accueil",
  CLIENT: "Client",
};

const BADGE_ROLE: Record<string, "lime" | "mint" | "soft" | "dark" | "outline"> = {
  RESPONSABLE: "mint",
  TECHNICIEN: "lime",
  ACCUEIL: "soft",
  ADMINISTRATEUR: "dark",
  SUPER_ADMINISTRATEUR: "dark",
  CLIENT: "outline",
};

// ---------------------------------------------------------------------------
// Composant principal
// ---------------------------------------------------------------------------

function UtilisateursPage() {
  const [utilisateurs, setUtilisateurs] = useState<UtilisateurDto[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionEnCours, setActionEnCours] = useState<number | null>(null);
  const [toast, setToast] = useState<{ message: string; type: "ok" | "err" } | null>(null);

  const afficherToast = (message: string, type: "ok" | "err" = "ok") => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3500);
  };

  const charger = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listerUtilisateurs({
        ...(search ? { search } : {}),
        page,
        size: 15,
      });
      setUtilisateurs(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Impossible de charger les utilisateurs.");
    } finally {
      setLoading(false);
    }
  }, [search, page]);

  useEffect(() => {
    charger();
  }, [charger]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearch(searchInput);
  };

  const handleToggleActif = async (u: UtilisateurDto) => {
    setActionEnCours(u.id);
    try {
      if (u.actif) {
        await desactiverUtilisateur(u.id);
        afficherToast(`${u.nomComplet} désactivé.`);
      } else {
        await activerUtilisateur(u.id);
        afficherToast(`${u.nomComplet} réactivé.`);
      }
      charger();
    } catch (e) {
      afficherToast(e instanceof ApiError ? e.message : "Erreur.", "err");
    } finally {
      setActionEnCours(null);
    }
  };

  const handleRenvoyerInvitation = async (u: UtilisateurDto) => {
    setActionEnCours(u.id);
    try {
      await renvoyerInvitation(u.id);
      afficherToast(`Invitation renvoyée à ${u.email}.`);
    } catch (e) {
      afficherToast(e instanceof ApiError ? e.message : "Erreur.", "err");
    } finally {
      setActionEnCours(null);
    }
  };

  return (
    <Container className="py-10">
      {/* Toast */}
      {toast && (
        <div
          className={`fixed right-6 top-6 z-50 flex items-center gap-3 rounded-2xl px-5 py-3.5 text-sm font-semibold shadow-xl transition-all duration-300 ${
            toast.type === "ok"
              ? "bg-brand-900 text-primary-foreground"
              : "bg-red-600 text-white"
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* En-tête */}
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-brand-900/10">
            <Users className="size-5 text-brand-900" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-ink-900">Utilisateurs</h1>
            <p className="text-sm text-muted-foreground">
              {totalElements} compte{totalElements !== 1 ? "s" : ""} dans votre laboratoire
            </p>
          </div>
        </div>
        <Link to="/app/utilisateurs/nouveau">
          <Button icon={<UserPlus className="size-4" />} iconPosition="left">
            Créer un utilisateur
          </Button>
        </Link>
      </div>

      {/* Barre de recherche */}
      <form onSubmit={handleSearch} className="mb-6 flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            id="recherche-utilisateur"
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Rechercher par nom ou email…"
            className="w-full rounded-full border border-input bg-card py-2.5 pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-brand-900/30"
          />
        </div>
        <Button type="submit" variant="secondary" size="sm">
          Rechercher
        </Button>
        {search && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => { setSearch(""); setSearchInput(""); setPage(0); }}
          >
            Effacer
          </Button>
        )}
      </form>

      {/* Tableau */}
      <Card variant="flat" padding="lg" className="overflow-hidden p-0">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <RefreshCw className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <p className="py-12 text-center text-sm text-red-600">{error}</p>
        ) : utilisateurs.length === 0 ? (
          <div className="py-16 text-center">
            <Users className="mx-auto mb-3 size-10 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">Aucun utilisateur trouvé.</p>
            <Link to="/app/utilisateurs/nouveau" className="mt-4 inline-block">
              <Button size="sm" icon={<UserPlus className="size-4" />} iconPosition="left">
                Créer le premier utilisateur
              </Button>
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-input bg-surface-card/60">
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Nom
                  </th>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Email
                  </th>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Rôle
                  </th>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Statut
                  </th>
                  <th className="px-5 py-3.5 text-right text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-input">
                {utilisateurs.map((u) => (
                  <UserRow
                    key={u.id}
                    utilisateur={u}
                    loading={actionEnCours === u.id}
                    onToggleActif={() => handleToggleActif(u)}
                    onRenvoyerInvitation={() => handleRenvoyerInvitation(u)}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} / {totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              icon={<ChevronLeft className="size-4" />}
              iconPosition="left"
            >
              Précédent
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              icon={<ChevronRight className="size-4" />}
            >
              Suivant
            </Button>
          </div>
        </div>
      )}
    </Container>
  );
}

// ---------------------------------------------------------------------------
// Ligne de tableau
// ---------------------------------------------------------------------------

function UserRow({
  utilisateur: u,
  loading,
  onToggleActif,
  onRenvoyerInvitation,
}: {
  utilisateur: UtilisateurDto;
  loading: boolean;
  onToggleActif: () => void;
  onRenvoyerInvitation: () => void;
}) {
  const enAttenteActivation = !u.compteConfirme;

  return (
    <tr className="group transition-colors hover:bg-surface-card/40">
      {/* Nom */}
      <td className="px-5 py-4">
        <div className="flex items-center gap-3">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-brand-900/10 text-sm font-bold text-brand-900">
            {u.nomComplet.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="font-semibold text-ink-900">{u.nomComplet}</p>
            {u.telephone && (
              <p className="text-xs text-muted-foreground">{u.telephone}</p>
            )}
          </div>
        </div>
      </td>

      {/* Email */}
      <td className="px-5 py-4 text-muted-foreground">{u.email}</td>

      {/* Rôle */}
      <td className="px-5 py-4">
        <Badge variant={BADGE_ROLE[u.role] ?? "soft"} size="sm">
          {LIBELLE_ROLE[u.role] ?? u.role}
        </Badge>
      </td>

      {/* Statut */}
      <td className="px-5 py-4">
        {enAttenteActivation ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700">
            <span className="size-1.5 rounded-full bg-amber-500" />
            En attente d'activation
          </span>
        ) : u.actif ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
            <span className="size-1.5 rounded-full bg-emerald-500" />
            Actif
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
            <span className="size-1.5 rounded-full bg-slate-400" />
            Inactif
          </span>
        )}
      </td>

      {/* Actions */}
      <td className="px-5 py-4">
        <div className="flex items-center justify-end gap-2">
          {enAttenteActivation ? (
            <button
              title="Renvoyer l'invitation"
              disabled={loading}
              onClick={onRenvoyerInvitation}
              className="flex items-center gap-1.5 rounded-full border border-input bg-card px-3 py-1.5 text-xs font-semibold text-brand-900 transition hover:bg-mint-200/60 disabled:opacity-50"
            >
              {loading ? (
                <RefreshCw className="size-3.5 animate-spin" />
              ) : (
                <Send className="size-3.5" />
              )}
              Renvoyer l'invitation
            </button>
          ) : (
            <button
              title={u.actif ? "Désactiver le compte" : "Réactiver le compte"}
              disabled={loading}
              onClick={onToggleActif}
              className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold transition disabled:opacity-50 ${
                u.actif
                  ? "border-red-200 bg-red-50 text-red-600 hover:bg-red-100"
                  : "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
              }`}
            >
              {loading ? (
                <RefreshCw className="size-3.5 animate-spin" />
              ) : u.actif ? (
                <ToggleLeft className="size-3.5" />
              ) : (
                <ToggleRight className="size-3.5" />
              )}
              {u.actif ? "Désactiver" : "Réactiver"}
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}
