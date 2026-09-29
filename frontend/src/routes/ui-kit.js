import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { createFileRoute } from "@tanstack/react-router";
import { FlaskConical } from "lucide-react";
import { Avatar, Badge, Button, Card, IconBox, Modal, Skeleton, Table, Tabs, Tooltip, } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
export const Route = createFileRoute("/ui-kit")({
    head: () => ({
        meta: [
            { title: "Design system — LabFlow LIMS" },
            { name: "description", content: "Composants visuels de la plateforme LabFlow." },
            { property: "og:title", content: "Design system — LabFlow LIMS" },
            { property: "og:description", content: "Composants visuels de la plateforme LabFlow." },
            { property: "og:type", content: "website" },
            { name: "twitter:card", content: "summary" },
        ],
    }),
    component: UiKit,
});
function UiKit() {
    return (_jsx(PublicLayout, { children: _jsxs(Container, { className: "flex flex-col gap-8 py-16", children: [_jsx("h1", { className: "text-4xl font-extrabold text-ink-900", children: "Design system" }), _jsxs(Card, { className: "flex flex-wrap gap-3", children: [_jsx(Button, { children: "Primaire" }), _jsx(Button, { variant: "secondary", children: "Secondaire" }), _jsx(Button, { variant: "lime", children: "Lime" }), _jsx(Button, { variant: "ghost", children: "Ghost" }), _jsx(Button, { loading: true, children: "Chargement" })] }), _jsxs(Card, { className: "flex flex-wrap items-center gap-3", children: [_jsx(Badge, { children: "Lime" }), _jsx(Badge, { variant: "mint", children: "Mint" }), _jsx(Badge, { variant: "soft", children: "Soft" }), _jsx(Badge, { variant: "dark", children: "Dark" }), _jsx(Badge, { variant: "outline", children: "Outline" }), _jsx(IconBox, { icon: FlaskConical, variant: "brand" }), _jsx(IconBox, { icon: FlaskConical, variant: "lime", shape: "circle" }), _jsx(Avatar, { initials: "SA" }), _jsx(Tooltip, { content: "Infobulle", children: _jsx(Button, { variant: "secondary", size: "sm", children: "Survoler" }) }), _jsx(Modal, { title: "Fen\u00EAtre", trigger: _jsx(Button, { size: "sm", children: "Ouvrir" }), children: "Contenu de la fen\u00EAtre." })] }), _jsxs("div", { className: "grid gap-4 md:grid-cols-3", children: [_jsx(Card, { variant: "glass", children: "Glass" }), _jsx(Card, { variant: "dark", children: "Dark" }), _jsx(Card, { variant: "lime", children: "Lime" })] }), _jsx(Tabs, { items: [{ value: "a", label: "Onglet A", content: "Contenu A" }, { value: "b", label: "Onglet B", content: "Contenu B" }] }), _jsx(Table, { columns: ["Demande", "Statut", "Délai"], rows: [["DEM-0412", "EN_VALIDATION", "2 j"], ["DEM-0409", "EN_COURS", "1 j"]] }), _jsxs("div", { className: "flex gap-3", children: [_jsx(Skeleton, { className: "h-10 w-40" }), _jsx(Skeleton, { className: "h-10 w-24" })] })] }) }));
}
