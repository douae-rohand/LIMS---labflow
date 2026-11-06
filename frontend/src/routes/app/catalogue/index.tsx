import { createFileRoute, redirect } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { FlaskConical } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button, Select } from "@/components/lab";
import { getSession } from "@/api/session";
import { EssaisTab } from "@/components/admin/catalogue/EssaisTab";
import { DomainesTab } from "@/components/admin/catalogue/DomainesTab";
import { ExigencesTab } from "@/components/admin/catalogue/ExigencesTab";
import * as catalogueAdmin from "@/api/catalogueAdmin";

type Tab = "essais" | "domaines" | "exigences";

export const Route = createFileRoute("/app/catalogue/")({
  head: () => ({ meta: [{ title: "Catalogue — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (!session.user.permissions.includes("CATALOGUE_GERER")) throw redirect({ to: "/app" });
  },
  component: CataloguePage,
});

function CataloguePage() {
  const [tab, setTab] = useState<Tab>("essais");
  const [selectedEssaiId, setSelectedEssaiId] = useState<number | null>(null);

  const essais = useQuery({
    queryKey: ["catalogue-admin-essais-select"],
    queryFn: () => catalogueAdmin.listerEssais({ page: 0, size: 50, actif: true }),
  });

  const ouvrirOngletExigences = () => {
    setSelectedEssaiId((current) => current ?? essais.data?.content[0]?.id ?? null);
    setTab("exigences");
  };

  useEffect(() => {
    if (tab === "exigences" && selectedEssaiId === null && essais.data?.content[0]) {
      setSelectedEssaiId(essais.data.content[0].id);
    } else if (selectedEssaiId !== null && essais.data && !essais.data.content.some((essai) => essai.id === selectedEssaiId)) {
      setSelectedEssaiId(essais.data.content[0]?.id ?? null);
    }
  }, [essais.data, selectedEssaiId, tab]);

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <FlaskConical className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Catalogue</h1>
          <p className="text-sm text-muted-foreground">Gestion des essais, domaines et exigences</p>
        </div>
      </div>

      <div className="flex gap-2 mb-6">
        <Button variant={tab === "essais" ? "primary" : "ghost"} size="sm" onClick={() => setTab("essais")}>
          Essais
        </Button>
        <Button variant={tab === "domaines" ? "primary" : "ghost"} size="sm" onClick={() => setTab("domaines")}>
          Domaines
        </Button>
        <Button variant={tab === "exigences" ? "primary" : "ghost"} size="sm" onClick={ouvrirOngletExigences}>
          Exigences
        </Button>
      </div>

      {tab === "essais" && <EssaisTab />}
      {tab === "domaines" && <DomainesTab />}
      {tab === "exigences" && (
        selectedEssaiId ? (
          <>
            <label htmlFor="exigences-essai" className="mb-3 block max-w-md text-sm font-medium">
              Essai
              <Select
                id="exigences-essai"
                className="mt-1"
                value={selectedEssaiId}
                onChange={(event) => setSelectedEssaiId(Number(event.target.value))}
              >
                {essais.data?.content.map((essai) => (
                  <option key={essai.id} value={essai.id}>{essai.code} — {essai.designation}</option>
                ))}
              </Select>
            </label>
            <ExigencesTab essaiId={selectedEssaiId} />
          </>
        ) : (
          <p className="text-sm text-muted-foreground">
            {essais.isLoading ? "Chargement des essais..." : essais.isError ? "Impossible de charger les essais." : "Créez un essai pour gérer ses exigences."}
          </p>
        )
      )}
    </Container>
  );
}
