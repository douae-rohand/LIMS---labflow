import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { Building2, ContactRound, Save, Settings } from "lucide-react";
import { toast } from "sonner";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import {
  messageErreurParametres,
  type LaboratoireParametresDto,
  type ModifierLaboratoireParametresRequest,
} from "@/api/laboratoireParametres";
import { useLaboParametres } from "@/hooks/useLaboParametres";
import { getSession } from "@/api/session";
import { TypeLaboratoireField } from "@/components/admin/parametres/TypeLaboratoireField";
import { TvaLaboratoireSection } from "@/components/admin/parametres/TvaLaboratoireSection";

export const Route = createFileRoute("/app/parametres")({
  head: () => ({ meta: [{ title: "Paramètres — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "ADMINISTRATEUR") throw redirect({ to: "/app" });
  },
  component: ParametresPage,
});

function ParametresPage() {
  const { parametres, modifier } = useLaboParametres();
  const [form, setForm] = useState<Partial<LaboratoireParametresDto>>({});

  useEffect(() => {
    if (parametres.data) {
      setForm({
        raisonSociale: parametres.data.raisonSociale,
        adresse: parametres.data.adresse,
        ville: parametres.data.ville,
        region: parametres.data.region,
        codePostal: parametres.data.codePostal,
        pays: parametres.data.pays,
        telephone: parametres.data.telephone,
        email: parametres.data.email,
        siteWeb: parametres.data.siteWeb,
        typeLaboratoire: parametres.data.typeLaboratoire,
        latitude: parametres.data.latitude,
        longitude: parametres.data.longitude,
      });
    }
  }, [parametres.data]);

  const hasChanges = Object.keys(form).some(key => form[key as keyof LaboratoireParametresDto] !== (parametres.data?.[key as keyof LaboratoireParametresDto]));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const toSubmit: ModifierLaboratoireParametresRequest = {
      raisonSociale: form.raisonSociale ?? undefined,
      adresse: form.adresse ?? undefined,
      ville: form.ville ?? undefined,
      region: form.region ?? undefined,
      codePostal: form.codePostal ?? undefined,
      pays: form.pays ?? undefined,
      telephone: form.telephone ?? undefined,
      email: form.email ?? undefined,
      siteWeb: form.siteWeb ?? undefined,
      typeLaboratoire: form.typeLaboratoire ?? undefined,
      latitude: form.latitude ?? undefined,
      longitude: form.longitude ?? undefined,
    };
    modifier.mutate(toSubmit, {
      onSuccess: () => toast.success("Paramètres du laboratoire enregistrés."),
    });
  };

  return (
    <Container className="py-8">
      <div className="mb-8 flex items-center gap-3">
        <div className="flex size-12 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
          <Settings className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Paramètres du laboratoire</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Gérez les informations et coordonnées de votre laboratoire.
          </p>
        </div>
      </div>

      {parametres.isLoading && (
        <div className="rounded-2xl border border-brand-900/10 bg-white p-6 text-sm text-muted-foreground shadow-sm" role="status">
          Chargement des paramètres du laboratoire…
        </div>
      )}
      {parametres.isError && (
        <div className="rounded-2xl border border-destructive/20 bg-white p-6 text-sm text-destructive" role="alert">
          {messageErreurParametres(parametres.error)}
        </div>
      )}
      {parametres.data && (
        <form onSubmit={handleSubmit} className="max-w-4xl space-y-5">
          <section className="rounded-2xl border border-brand-900/10 bg-white p-5 shadow-sm sm:p-6">
            <div className="mb-5 flex items-center gap-3">
              <span className="flex size-9 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
                <Building2 className="size-4" aria-hidden="true" />
              </span>
              <div>
                <h2 className="font-semibold text-ink-900">Informations générales</h2>
                <p className="text-sm text-muted-foreground">Identité et localisation du laboratoire.</p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className={LABEL}>Code du laboratoire</label>
                <input disabled value={parametres.data.code} className={`${CHAMP} bg-surface-card text-muted-foreground`} />
              </div>
              <div>
                <label className={LABEL}>Raison sociale</label>
                <input
                  value={form.raisonSociale || ""}
                  onChange={(e) => setForm({ ...form, raisonSociale: e.target.value })}
                  className={CHAMP}
                />
              </div>
              <div>
                <label className={LABEL}>Adresse</label>
                <input
                  value={form.adresse || ""}
                  onChange={(e) => setForm({ ...form, adresse: e.target.value })}
                  className={CHAMP}
                />
              </div>
              <div>
                <label className={LABEL}>Ville</label>
                <input
                  value={form.ville || ""}
                  onChange={(e) => setForm({ ...form, ville: e.target.value })}
                  className={CHAMP}
                />
              </div>
              <div>
                <label className={LABEL}>Code postal</label>
                <input
                  value={form.codePostal || ""}
                  onChange={(e) => setForm({ ...form, codePostal: e.target.value })}
                  className={CHAMP}
                />
              </div>
              <div>
                <label className={LABEL}>Région</label>
                <input value={form.region || ""} onChange={(e) => setForm({ ...form, region: e.target.value })} className={CHAMP} />
              </div>
              <div>
                <label className={LABEL}>Pays</label>
                <input value={form.pays || ""} onChange={(e) => setForm({ ...form, pays: e.target.value })} className={CHAMP} />
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-brand-900/10 bg-white p-5 shadow-sm sm:p-6">
            <div className="mb-5 flex items-center gap-3">
              <span className="flex size-9 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
                <ContactRound className="size-4" aria-hidden="true" />
              </span>
              <div>
                <h2 className="font-semibold text-ink-900">Coordonnées</h2>
                <p className="text-sm text-muted-foreground">Moyens de contact de votre laboratoire.</p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className={LABEL}>Téléphone</label>
                <input value={form.telephone || ""} onChange={(e) => setForm({ ...form, telephone: e.target.value })} className={CHAMP} />
              </div>
              <div>
                <label className={LABEL}>E-mail</label>
                <input type="email" value={form.email || ""} onChange={(e) => setForm({ ...form, email: e.target.value })} className={CHAMP} />
              </div>
              <div className="sm:col-span-2">
                <label className={LABEL}>Site web</label>
                <input type="url" value={form.siteWeb || ""} onChange={(e) => setForm({ ...form, siteWeb: e.target.value })} className={CHAMP} />
              </div>
            </div>
          </section>

          <TypeLaboratoireField
            value={form.typeLaboratoire ?? ""}
            onChange={(typeLaboratoire) => setForm((courant) => ({ ...courant, typeLaboratoire }))}
          />

          {modifier.isError && (
            <p className="text-sm text-destructive" role="alert">
              {messageErreurParametres(modifier.error)}
            </p>
          )}
          <div className="sticky bottom-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-white/95 p-4 shadow-lg backdrop-blur">
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <Save className="size-4 text-brand-900" aria-hidden="true" />
              {hasChanges ? "Des modifications ne sont pas encore enregistrées." : "Vos paramètres sont à jour."}
            </p>
            <Button type="submit" variant="primary" disabled={!hasChanges || modifier.isPending}>
              {modifier.isPending ? "Enregistrement…" : "Enregistrer les modifications"}
            </Button>
          </div>
        </form>
      )}
      {parametres.data && (
        <div className="mt-5">
          <TvaLaboratoireSection />
        </div>
      )}
    </Container>
  );
}

const LABEL = "mb-1.5 block text-sm font-medium text-ink-900";
const CHAMP = "h-10 w-full rounded-xl border border-brand-900/15 bg-white px-3 text-sm text-ink-900 outline-none transition focus:border-brand-900 focus:ring-2 focus:ring-brand-900/10 disabled:cursor-not-allowed";
