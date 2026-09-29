import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { Button, Card, Checkbox, Input, Label, Select, Textarea } from "@/components/lab";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";
export const Route = createFileRoute("/integration")({
    head: () => ({
        meta: [
            { title: "Intégrer mon laboratoire — LabFlow LIMS" },
            { name: "description", content: "Demandez le déploiement de LabFlow dans votre laboratoire." },
            { property: "og:title", content: "Intégrer mon laboratoire — LabFlow LIMS" },
            { property: "og:description", content: "Demandez le déploiement de LabFlow dans votre laboratoire." },
            { property: "og:type", content: "website" },
            { name: "twitter:card", content: "summary" },
        ],
    }),
    component: IntegrationPage,
});
function IntegrationPage() {
    const [sent, setSent] = useState(false);
    return (_jsx(PublicLayout, { children: _jsxs(Container, { className: "grid gap-10 py-16 lg:grid-cols-[1fr_1.1fr]", children: [_jsxs("div", { className: "flex flex-col gap-5", children: [_jsx("h1", { className: "text-4xl font-extrabold text-ink-900", children: "Int\u00E9grer mon laboratoire" }), _jsx("p", { className: "text-muted-foreground", children: "D\u00E9crivez votre laboratoire, nous pr\u00E9parons un espace isol\u00E9 avec vos r\u00F4les, votre catalogue d'essais et vos mod\u00E8les de rapports." }), _jsx(Photo, { src: photos.sampling.src, alt: photos.sampling.alt, className: "aspect-[4/3]" })] }), _jsx(Card, { variant: "glossy", padding: "lg", children: sent ? (_jsx("p", { className: "text-lg font-bold text-brand-900", children: "Merci, votre demande a bien \u00E9t\u00E9 enregistr\u00E9e." })) : (_jsxs("form", { className: "flex flex-col gap-4", onSubmit: (e) => { e.preventDefault(); setSent(true); }, children: [_jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "lab", children: "Nom du laboratoire" }), _jsx(Input, { id: "lab", required: true })] }), _jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "mail", children: "Email de contact" }), _jsx(Input, { id: "mail", type: "email", required: true })] }), _jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "domain", children: "Domaine d'analyse" }), _jsxs(Select, { id: "domain", children: [_jsx("option", { children: "M\u00E9dical" }), _jsx("option", { children: "Industriel" }), _jsx("option", { children: "Environnemental" }), _jsx("option", { children: "Pharmaceutique et cosm\u00E9tique" })] })] }), _jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "msg", children: "Besoins" }), _jsx(Textarea, { id: "msg" })] }), _jsx(Checkbox, { id: "iso", label: "Laboratoire accr\u00E9dit\u00E9 ISO/IEC 17025" }), _jsx(Button, { type: "submit", className: "mt-2", children: "Envoyer la demande" })] })) })] }) }));
}
