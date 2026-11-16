import { CheckCircle2, XCircle } from "lucide-react";
import { Card } from "@/components/lab";
import {
  classeConformite,
  libelleConformite,
} from "@/components/technicien/libelles";
import type { ImportApercuResponse } from "@/api/technicien/types";
import { cn } from "@/lib/utils";

/** Un code d'erreur en français lisible. */
function libelleErreurImport(code: string): string {
  const MAP: Record<string, string> = {
    IMPORT_LIGNE_MAL_FORMEE: "Ligne mal formée (moins de 2 colonnes)",
    IMPORT_FORMULE_INTERDITE: "Valeur interdite (commence par = + - @)",
    IMPORT_CODE_VIDE: "Code de ligne manquant",
    IMPORT_VALEUR_VIDE: "Valeur manquante",
    IMPORT_LIGNE_INTROUVABLE: "Code de ligne introuvable parmi vos essais",
    IMPORT_ESSAI_NON_EN_COURS: "L'essai n'est pas en cours",
    IMPORT_DEJA_VALIDE_RESPONSABLE: "Résultat déjà validé par le Responsable",
    IMPORT_MOTIF_REQUIS: "Commentaire obligatoire pour modification",
  };
  return MAP[code] ?? code;
}

/** Tableau de l'aperçu des lignes valides. */
function TableauApercu({ apercu }: { apercu: ImportApercuResponse["apercu"] }) {
  if (apercu.length === 0) return null;
  return (
    <div className="overflow-x-auto rounded-3xl border border-brand-900/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-surface-alt text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          <tr>
            <th scope="col" className="px-3 py-2">Ligne</th>
            <th scope="col" className="px-3 py-2">Code ligne</th>
            <th scope="col" className="px-3 py-2">Valeur</th>
            <th scope="col" className="px-3 py-2">Commentaire</th>
            <th scope="col" className="px-3 py-2">Conformité calculée</th>
          </tr>
        </thead>
        <tbody>
          {apercu.map((l) => (
            <tr key={l.numeroLigne} className="border-t border-brand-900/5 hover:bg-brand-900/[0.02]">
              <td className="px-3 py-2 text-muted-foreground">{l.numeroLigne}</td>
              <td className="px-3 py-2 font-mono font-semibold text-ink-900">{l.codeLigne}</td>
              <td className="px-3 py-2 text-ink-900">{l.valeur}</td>
              <td className="px-3 py-2 text-muted-foreground">{l.commentaire ?? "—"}</td>
              <td className="px-3 py-2">
                <span
                  className={cn(
                    "inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold",
                    classeConformite(l.niveauConformite),
                  )}
                >
                  {libelleConformite(l.niveauConformite)}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Liste des erreurs par ligne et colonne. */
function ListeErreurs({ erreurs }: { erreurs: ImportApercuResponse["erreurs"] }) {
  return (
    <ul className="space-y-2">
      {erreurs.map((e, i) => (
        <li
          key={`${e.numeroLigne}-${e.colonne}-${i}`}
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 px-3 py-2"
        >
          <XCircle className="mt-0.5 size-4 shrink-0 text-red-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-red-900">
              Ligne {e.numeroLigne} · colonne{" "}
              <code className="font-mono">{e.colonne}</code>
            </p>
            <p className="text-xs text-red-800">{libelleErreurImport(e.code)}</p>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Résumé et détail de l'aperçu CSV. */
export function ApercuImport({ apercu }: { apercu: ImportApercuResponse }) {
  const { lignesLues, lignesOk, lignesErreur, pret, erreurs, apercu: lignesApercu } = apercu;
  return (
    <div className="space-y-5">
      {/* Résumé global */}
      <Card
        variant="flat"
        padding="md"
        className={cn(
          "flex items-center gap-4",
          pret ? "border-green-200 bg-green-50" : "border-amber-200 bg-amber-50",
        )}
      >
        {pret ? (
          <CheckCircle2 className="size-8 shrink-0 text-green-700" aria-hidden="true" />
        ) : (
          <XCircle className="size-8 shrink-0 text-amber-700" aria-hidden="true" />
        )}
        <div>
          <p className={cn("font-semibold", pret ? "text-green-900" : "text-amber-900")}>
            {pret
              ? `${lignesOk} ligne${lignesOk > 1 ? "s" : ""} valide${lignesOk > 1 ? "s" : ""} — prêt à confirmer`
              : `${lignesErreur} erreur${lignesErreur > 1 ? "s" : ""} sur ${lignesLues} ligne${lignesLues > 1 ? "s" : ""}`}
          </p>
          <p className="text-xs text-muted-foreground">
            {lignesLues} ligne{lignesLues > 1 ? "s" : ""} lue{lignesLues > 1 ? "s" : ""}, {lignesOk} valide{lignesOk > 1 ? "s" : ""}, {lignesErreur} en erreur.
          </p>
        </div>
      </Card>

      {/* Erreurs */}
      {erreurs.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-semibold text-red-700" role="alert">
            Erreurs à corriger avant de confirmer :
          </p>
          <ListeErreurs erreurs={erreurs} />
        </div>
      )}

      {/* Aperçu lignes valides */}
      {lignesApercu.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-900">
            Aperçu des lignes valides :
          </p>
          <TableauApercu apercu={lignesApercu} />
        </div>
      )}
    </div>
  );
}
