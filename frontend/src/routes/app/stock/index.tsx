import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { Package } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { Button } from "@/components/lab";
import { getSession } from "@/api/session";
import { ProduitsTab } from "@/components/admin/stock/ProduitsTab";
import { AlertesTab } from "@/components/admin/stock/AlertesTab";

export const Route = createFileRoute("/app/stock/")({
  head: () => ({ meta: [{ title: "Stock — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (session.user.role !== "ADMINISTRATEUR") throw redirect({ to: "/app" });
  },
  component: StockPage,
});

function StockPage() {
  const [tab, setTab] = useState<"produits" | "alertes">("produits");

  return (
    <Container className="py-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-brand-900/10 text-brand-900">
          <Package className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Stock</h1>
          <p className="text-sm text-muted-foreground">Gestion des produits et alertes</p>
        </div>
      </div>

      <div className="flex gap-2 mb-6">
        <Button
          variant={tab === "produits" ? "primary" : "ghost"}
          size="sm"
          onClick={() => setTab("produits")}
        >
          Produits
        </Button>
        <Button
          variant={tab === "alertes" ? "primary" : "ghost"}
          size="sm"
          onClick={() => setTab("alertes")}
        >
          Alertes
        </Button>
      </div>

      {tab === "produits" && <ProduitsTab />}
      {tab === "alertes" && <AlertesTab />}
    </Container>
  );
}
