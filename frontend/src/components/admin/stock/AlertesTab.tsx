import { AlertTriangle, CalendarClock, CalendarX2, CheckCircle2, PackageCheck } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import * as stockAdmin from "@/api/stockAdmin";
import { messageErreurStockAdmin } from "@/api/stockAdmin";

function afficherDate(date: string): string {
  const [annee, mois, jour] = date.split("-").map(Number);
  if (!annee || !mois || !jour) return date;
  return new Intl.DateTimeFormat("fr-FR").format(new Date(annee, mois - 1, jour));
}

export function AlertesTab() {
  const alertes = useQuery({
    queryKey: ["stock-admin-alertes"],
    queryFn: stockAdmin.getAlertes,
  });
  const donnees = alertes.data;
  const indicateurs = donnees
    ? [
        {
          titre: "Sous le seuil",
          description: "Produits à réapprovisionner",
          valeur: donnees.sousSeuil.length,
          icon: AlertTriangle,
          couleur:
            donnees.sousSeuil.length > 0
              ? "border-amber-200 bg-amber-50 text-amber-800"
              : "border-brand-900/10 bg-card text-ink-900",
        },
        {
          titre: "Rupture de stock",
          description: "Produits sans stock disponible",
          valeur: donnees.rupture.length,
          icon: PackageCheck,
          couleur:
            donnees.rupture.length > 0
              ? "border-rose-200 bg-rose-50 text-rose-800"
              : "border-brand-900/10 bg-card text-ink-900",
        },
        {
          titre: "Produits périmés",
          description: "Lots dont la date est dépassée",
          valeur: donnees.perimes.length,
          icon: CalendarX2,
          couleur:
            donnees.perimes.length > 0
              ? "border-rose-200 bg-rose-50 text-rose-800"
              : "border-brand-900/10 bg-card text-ink-900",
        },
        {
          titre: "Péremption proche",
          description: "Lots expirant dans les 30 jours",
          valeur: donnees.peremptionProche.length,
          icon: CalendarClock,
          couleur:
            donnees.peremptionProche.length > 0
              ? "border-amber-200 bg-amber-50 text-amber-800"
              : "border-brand-900/10 bg-card text-ink-900",
        },
      ]
    : [];
  const nombreAlertes = indicateurs.reduce((total, indicateur) => total + indicateur.valeur, 0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold">Alertes de stock</h2>
          <p className="text-sm text-muted-foreground">
            Suivez les produits à réapprovisionner et les dates de péremption.
          </p>
        </div>
        {donnees && (
          <p className="text-sm font-medium text-muted-foreground">
            {nombreAlertes === 0
              ? "Tout est à jour"
              : `${nombreAlertes} alerte${nombreAlertes > 1 ? "s" : ""} à traiter`}
          </p>
        )}
      </div>

      {alertes.isLoading && (
        <p className="rounded-2xl border border-brand-900/10 bg-card p-4 text-sm text-muted-foreground">
          Chargement des alertes...
        </p>
      )}
      {alertes.isError && (
        <p
          role="alert"
          className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"
        >
          {messageErreurStockAdmin(alertes.error)}
        </p>
      )}

      {donnees && (
        <>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {indicateurs.map(({ titre, description, valeur, icon: Icon, couleur }) => (
              <div key={titre} className={`rounded-2xl border p-4 ${couleur}`}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium">{titre}</p>
                    <p className="mt-1 text-xs opacity-80">{description}</p>
                  </div>
                  <Icon className="size-5 shrink-0 opacity-80" aria-hidden="true" />
                </div>
                <p className="mt-3 text-3xl font-semibold tabular-nums">{valeur}</p>
              </div>
            ))}
          </div>

          {nombreAlertes === 0 && (
            <div className="flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-800">
              <CheckCircle2 className="size-5 shrink-0" aria-hidden="true" />
              <div>
                <p className="font-medium">Aucune alerte à traiter</p>
                <p className="text-sm text-emerald-700">
                  Les stocks et les dates de péremption sont à jour.
                </p>
              </div>
            </div>
          )}

          <div className="grid gap-4 xl:grid-cols-2">
            <section className="rounded-2xl border border-brand-900/10 bg-card p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h3 className="font-semibold">À réapprovisionner</h3>
                <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-800">
                  {donnees.sousSeuil.length}
                </span>
              </div>
              {donnees.sousSeuil.length === 0 ? (
                <p className="rounded-xl bg-surface-card p-3 text-sm text-muted-foreground">
                  Aucun produit sous son seuil minimal.
                </p>
              ) : (
                <ul className="space-y-2">
                  {donnees.sousSeuil.map((produit) => (
                    <li
                      key={produit.id}
                      className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-amber-50/70 p-3"
                    >
                      <div>
                        <p className="font-medium text-ink-900">{produit.nom}</p>
                        <p className="text-xs text-muted-foreground">
                          Référence : {produit.reference}
                        </p>
                      </div>
                      <p className="text-sm text-amber-900">
                        Stock {produit.stockCourant} {produit.unite} · Seuil{" "}
                        {produit.seuilMinimal ?? "—"} {produit.unite}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-2xl border border-brand-900/10 bg-card p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h3 className="font-semibold">En rupture de stock</h3>
                <span className="rounded-full bg-rose-100 px-2.5 py-1 text-xs font-semibold text-rose-800">
                  {donnees.rupture.length}
                </span>
              </div>
              {donnees.rupture.length === 0 ? (
                <p className="rounded-xl bg-surface-card p-3 text-sm text-muted-foreground">
                  Aucun produit en rupture.
                </p>
              ) : (
                <ul className="space-y-2">
                  {donnees.rupture.map((produit) => (
                    <li key={produit.id} className="rounded-xl bg-rose-50/70 p-3">
                      <p className="font-medium text-ink-900">{produit.nom}</p>
                      <p className="text-xs text-muted-foreground">
                        Référence : {produit.reference}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-2xl border border-brand-900/10 bg-card p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h3 className="font-semibold">Lots périmés</h3>
                <span className="rounded-full bg-rose-100 px-2.5 py-1 text-xs font-semibold text-rose-800">
                  {donnees.perimes.length}
                </span>
              </div>
              {donnees.perimes.length === 0 ? (
                <p className="rounded-xl bg-surface-card p-3 text-sm text-muted-foreground">
                  Aucun lot périmé.
                </p>
              ) : (
                <ul className="space-y-2">
                  {donnees.perimes.map((lot) => (
                    <li key={lot.id} className="rounded-xl bg-rose-50/70 p-3">
                      <p className="font-medium text-ink-900">Lot {lot.numero}</p>
                      <p className="text-sm text-muted-foreground">
                        {lot.produitReference} · Périmé le {afficherDate(lot.peremption)}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-2xl border border-brand-900/10 bg-card p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h3 className="font-semibold">Péremption dans les 30 jours</h3>
                <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-800">
                  {donnees.peremptionProche.length}
                </span>
              </div>
              {donnees.peremptionProche.length === 0 ? (
                <p className="rounded-xl bg-surface-card p-3 text-sm text-muted-foreground">
                  Aucun lot n’approche de sa date de péremption.
                </p>
              ) : (
                <ul className="space-y-2">
                  {donnees.peremptionProche.map((lot) => (
                    <li key={lot.id} className="rounded-xl bg-amber-50/70 p-3">
                      <p className="font-medium text-ink-900">Lot {lot.numero}</p>
                      <p className="text-sm text-muted-foreground">
                        {lot.produitReference} · À périmer le {afficherDate(lot.peremption)}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
}
