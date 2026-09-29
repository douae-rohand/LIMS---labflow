import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { createFileRoute } from "@tanstack/react-router";
import { Button, Card, Input, Label } from "@/components/lab";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";
export const Route = createFileRoute("/login")({
    head: () => ({
        meta: [
            { title: "Connexion — LabFlow LIMS" },
            { name: "description", content: "Accédez à votre espace laboratoire LabFlow." },
            { property: "og:title", content: "Connexion — LabFlow LIMS" },
            { property: "og:description", content: "Accédez à votre espace laboratoire LabFlow." },
            { property: "og:type", content: "website" },
            { name: "twitter:card", content: "summary" },
        ],
    }),
    component: LoginPage,
});
function LoginPage() {
    return (_jsx(PublicLayout, { children: _jsxs(Container, { className: "grid items-center gap-10 py-16 lg:grid-cols-2", children: [_jsxs(Card, { variant: "glossy", padding: "lg", className: "mx-auto w-full max-w-md", children: [_jsx("h1", { className: "text-2xl font-extrabold text-ink-900", children: "Se connecter" }), _jsx("p", { className: "mt-1 text-sm text-muted-foreground", children: "Espace s\u00E9curis\u00E9 de votre laboratoire." }), _jsxs("form", { className: "mt-6 flex flex-col gap-4", onSubmit: (e) => e.preventDefault(), children: [_jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "email", children: "Email professionnel" }), _jsx(Input, { id: "email", type: "email", placeholder: "nom@laboratoire.ma" })] }), _jsxs("div", { className: "flex flex-col gap-1.5", children: [_jsx(Label, { htmlFor: "password", children: "Mot de passe" }), _jsx(Input, { id: "password", type: "password" })] }), _jsx(Button, { type: "submit", className: "mt-2", children: "Continuer" })] })] }), _jsx(Photo, { src: photos.technicianMicroscope.src, alt: photos.technicianMicroscope.alt, className: "hidden aspect-[4/3] lg:block" })] }) }));
}
