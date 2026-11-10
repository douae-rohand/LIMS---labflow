import { useQuery } from "@tanstack/react-query";
import { libelleTva, useTvaLaboratoire } from "@/api/tvaLaboratoire";
import { ClipboardList, FlaskConical } from "lucide-react";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Button, Skeleton } from "@/components/lab";
import { getEssai, messageErreurCatalogueAdmin } from "@/api/catalogueAdmin";

interface EssaiPanneauProps {
  essaiId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: (id: number) => void;
}

function Valeur({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div>
      <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm text-ink-900">{value ?? "—"}</dd>
    </div>
  );
}

export function EssaiPanneau({ essaiId, open, onOpenChange, onEdit }: EssaiPanneauProps) {
  const tva = useTvaLaboratoire();
  const query = useQuery({
    queryKey: ["catalogue-admin-essai", essaiId],
    queryFn: () => {
      if (essaiId === null) throw new Error("Un essai doit être sélectionné.");
      return getEssai(essaiId);
    },
    enabled: open && essaiId !== null,
  });
  const essai = query.data;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-2xl">
        <SheetHeader className="pr-8 text-left">
          <SheetTitle>{essai ? `${essai.code} · ${essai.designation}` : "Détail de l'essai"}</SheetTitle>
          <SheetDescription>{essai?.domaineLibelle ?? "Informations de l'essai et exigences d'échantillon"}</SheetDescription>
        </SheetHeader>
        <div className="mt-6 space-y-5">
          {query.isLoading && <div className="space-y-3" aria-busy="true"><Skeleton className="h-28 w-full" /><Skeleton className="h-32 w-full" /></div>}
          {query.isError && <p role="alert" className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{messageErreurCatalogueAdmin(query.error)}</p>}
          {essai && (
            <>
              <section className="rounded-2xl border border-sky-200 bg-sky-50/70 p-4">
                <h2 className="mb-4 flex items-center gap-2 font-semibold text-sky-900"><FlaskConical className="size-4" aria-hidden="true" />Informations de l'essai</h2>
                <dl className="grid gap-4 sm:grid-cols-2">
                  <Valeur label="Code" value={essai.code} />
                  <Valeur label="Domaine" value={essai.domaineLibelle} />
                  <Valeur label="Prix TTC" value={tva.data ? `${essai.tarif} MAD · ${libelleTva(essai.tarif, tva.data.tauxEffectif)}` : `${essai.tarif} MAD`} />
                  <Valeur label="Durée estimée" value={essai.dureeEstimee == null ? null : `${essai.dureeEstimee} jours`} />
                  <Valeur label="Méthode" value={essai.methode} />
                  <Valeur label="Unité" value={essai.unite} />
                  <Valeur label="Limites" value={`${essai.limiteMin ?? "—"} – ${essai.limiteMax ?? "—"}`} />
                  <Valeur label="Seuils critiques" value={`${essai.seuilCritiqueMin ?? "—"} – ${essai.seuilCritiqueMax ?? "—"}`} />
                  <div className="sm:col-span-2"><Valeur label="Description" value={essai.description} /></div>
                  <Valeur label="État" value={essai.actif ? "Actif" : "Inactif"} />
                </dl>
              </section>

              <section className="rounded-2xl border border-violet-200 bg-violet-50/70 p-4">
                <h2 className="mb-4 flex items-center gap-2 font-semibold text-violet-900"><ClipboardList className="size-4" aria-hidden="true" />Exigences d'échantillon</h2>
                {essai.exigences.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Aucune exigence définie pour cet essai.</p>
                ) : (
                  <ul className="space-y-2">
                    {essai.exigences.map((exigence) => (
                      <li key={exigence.id} className="rounded-xl border border-violet-900/10 bg-white/80 p-3">
                        <p className="font-semibold text-ink-900">{exigence.nature}</p>
                        <dl className="mt-2 grid gap-2 text-sm sm:grid-cols-2">
                          <div>
                            <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Conservation</dt>
                            <dd className="mt-1 text-ink-900">{exigence.etatConservation ?? "—"}</dd>
                          </div>
                          <div>
                            <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Quantité minimale</dt>
                            <dd className="mt-1 text-ink-900">{exigence.quantiteMinimale ?? "—"} {exigence.unite ?? ""}</dd>
                          </div>
                        </dl>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <div className="flex justify-end">
                <Button type="button" variant="primary" onClick={() => onEdit(essai.id)}>Modifier l'essai et ses exigences</Button>
              </div>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
