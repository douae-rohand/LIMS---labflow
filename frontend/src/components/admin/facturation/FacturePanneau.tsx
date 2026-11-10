import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ClipboardList, FileText, FlaskConical, Wallet } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/lab";
import { getFacture, messageErreurFacturesAdmin } from "@/api/facturesAdmin";
import { formatMontant } from "@/components/panier/format";
import { EnregistrerPaiementDialog } from "./EnregistrerPaiementDialog";

interface FacturePanneauProps {
  factureId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const LIBELLES_STATUT: Record<string, string> = {
  EMISE: "Émise",
  PAYEE: "Payée",
  EN_RETARD: "En retard",
  IMPAYEE: "Impayée",
};

export function FacturePanneau({
  factureId,
  open,
  onOpenChange,
}: FacturePanneauProps) {
  const queryClient = useQueryClient();
  const factureQuery = useQuery({
    queryKey: ["facture-detail", factureId],
    queryFn: () => {
      if (factureId === null) throw new Error("Une facture doit être sélectionnée.");
      return getFacture(factureId);
    },
    enabled: open && factureId !== null,
  });
  const facture = factureQuery.data;
  const totalPaiements = facture?.paiements.reduce((total, paiement) => total + paiement.montant, 0) ?? 0;
  const resteAPayer = (facture?.montantHt ?? 0) - totalPaiements;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-2xl">
        <SheetHeader className="pr-8 text-left">
          <SheetTitle className="text-ink-900">
            {facture?.numero ?? "Détail de la facture"}
          </SheetTitle>
          <SheetDescription>
            {facture?.clientNom ?? "Informations de la facture et de la demande associée"}
          </SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-5">
          {factureQuery.isLoading && (
            <div className="space-y-3" aria-busy="true">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          )}

          {factureQuery.isError && (
            <p className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800" role="alert">
              {messageErreurFacturesAdmin(factureQuery.error)}
            </p>
          )}

          {facture && (
            <>
              <div className="flex items-center justify-between gap-3">
                <span className={`rounded-full px-3 py-1 text-sm font-semibold ${
                  facture.statut === "PAYEE"
                    ? "bg-emerald-100 text-emerald-800"
                    : facture.statut === "EN_RETARD"
                      ? "bg-rose-100 text-rose-800"
                      : "bg-amber-100 text-amber-800"
                }`}>
                  {LIBELLES_STATUT[facture.statut] ?? facture.statut}
                </span>
                <span className="text-sm font-semibold text-ink-900">
                  Total : {formatMontant(facture.montantHt)}
                </span>
              </div>

              <section className="rounded-2xl border border-sky-200 bg-sky-50/70 p-4">
                <h2 className="mb-3 flex items-center gap-2 font-semibold text-sky-900">
                  <ClipboardList className="size-4" aria-hidden="true" />
                  Demande associée
                </h2>
                <dl className="grid gap-3 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-muted-foreground">Référence</dt>
                    <dd className="mt-1 font-mono font-semibold">{facture.demandeReference ?? "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-muted-foreground">Intitulé</dt>
                    <dd className="mt-1 font-semibold">{facture.demandeTitre ?? "—"}</dd>
                  </div>
                </dl>
              </section>

              <section className="rounded-2xl border border-brand-900/10 bg-white p-4">
                <h2 className="mb-3 flex items-center gap-2 font-semibold text-brand-900">
                  <FileText className="size-4" aria-hidden="true" />
                  Facture · {LIBELLES_STATUT[facture.statut] ?? facture.statut}
                </h2>
                <dl className="grid gap-3 text-sm sm:grid-cols-2">
                  <div><dt className="text-muted-foreground">Date d&apos;émission</dt><dd className="mt-1">{facture.dateFacture}</dd></div>
                  <div><dt className="text-muted-foreground">Échéance</dt><dd className="mt-1">{facture.dateEcheance ?? "—"}</dd></div>
                  <div><dt className="text-muted-foreground">Client</dt><dd className="mt-1">{facture.clientNom ?? "—"}</dd></div>
                  <div><dt className="text-muted-foreground">ICE client</dt><dd className="mt-1">{facture.clientIce ?? "—"}</dd></div>
                  <div><dt className="text-muted-foreground">Total TTC</dt><dd className="mt-1 font-semibold">{formatMontant(facture.montantHt)}</dd></div>
                  <div><dt className="text-muted-foreground">dont TVA ({facture.tauxTva} %)</dt><dd className="mt-1 font-semibold">−{formatMontant(facture.montantTva)}</dd></div>
                  <div><dt className="text-muted-foreground">Total hors taxes</dt><dd className="mt-1">{formatMontant(facture.montantHorsTaxes)}</dd></div>
                </dl>
              </section>

              <section className="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
                <h2 className="mb-3 flex items-center gap-2 font-semibold text-emerald-900">
                  <FlaskConical className="size-4" aria-hidden="true" />
                  Essais facturés
                </h2>
                {facture.lignes.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucun essai associé.</p>
                ) : (
                  <ul className="space-y-2">
                    {facture.lignes.map((ligne) => (
                      <li key={ligne.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-emerald-900/10 bg-white/80 p-3 text-sm">
                        <span className="font-medium">{ligne.designation}</span>
                        <span>{formatMontant(ligne.montant)}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="rounded-2xl border border-amber-200 bg-amber-50/70 p-4">
                <h2 className="mb-3 flex items-center gap-2 font-semibold text-amber-900">
                  <Wallet className="size-4" aria-hidden="true" />
                  Paiements
                </h2>
                {facture.paiements.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucun paiement.</p>
                ) : (
                  <ul className="space-y-3">
                    {facture.paiements.map((paiement) => (
                      <li key={paiement.id} className="flex flex-wrap justify-between gap-x-4 gap-y-1 border-b border-amber-900/10 pb-2 text-sm last:border-0">
                        <span>{paiement.mode} · {paiement.datePaiement}{paiement.reference ? ` · ${paiement.reference}` : ""}</span>
                        <span className="font-medium">{formatMontant(paiement.montant)}</span>
                      </li>
                    ))}
                  </ul>
                )}
                <div className="mt-3 flex flex-wrap justify-between gap-2 border-t border-amber-900/10 pt-3 text-sm">
                  <p><span className="text-muted-foreground">Payé : </span><span className="font-semibold">{formatMontant(totalPaiements)}</span></p>
                  <p><span className="text-muted-foreground">Reste : </span><span className="font-semibold">{formatMontant(resteAPayer)}</span></p>
                </div>
                {resteAPayer > 0 && (
                  <div className="mt-4 flex justify-end">
                    <EnregistrerPaiementDialog
                      factureId={facture.id}
                      resteAPayer={resteAPayer}
                      numero={facture.numero}
                      onSuccess={() => {
                        void factureQuery.refetch();
                        void queryClient.invalidateQueries({ queryKey: ["factures-admin"] });
                        void queryClient.invalidateQueries({ queryKey: ["factures-admin-stats"] });
                      }}
                    />
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
