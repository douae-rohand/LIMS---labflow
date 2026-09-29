import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from "@tanstack/react-router";
import { FlaskConical, Menu, X } from "lucide-react";
import { useState } from "react";
import { Container } from "./Container";
import { Button } from "@/components/lab";
import { anchors } from "@/data/navigation";
import { useScrollSpy, useScrolled } from "@/hooks/useScrollSpy";
import { cn } from "@/lib/utils";
export function Navbar() {
    const [open, setOpen] = useState(false);
    const scrolled = useScrolled();
    const activeId = useScrollSpy(anchors.map((a) => a.id));
    return (_jsxs("header", { className: cn("sticky top-0 z-40 border-b transition-all duration-300", scrolled
            ? "border-brand-900/10 bg-background/70 py-2 backdrop-blur-xl"
            : "border-transparent bg-background/40 py-4 backdrop-blur-md"), children: [_jsxs(Container, { className: "flex items-center justify-between gap-4", children: [_jsxs(Link, { to: "/", className: "flex items-center gap-2 font-extrabold text-ink-900", children: [_jsx("span", { className: "glossy-top flex size-9 items-center justify-center rounded-xl bg-brand-900 text-primary-foreground", children: _jsx(FlaskConical, { className: "size-5", "aria-hidden": "true" }) }), "LabFlow"] }), _jsx("nav", { "aria-label": "Navigation principale", className: "hidden lg:block", children: _jsx("ul", { className: "flex items-center gap-1", children: anchors.map((anchor) => (_jsx("li", { children: _jsx("a", { href: `#${anchor.id}`, className: cn("rounded-full px-3.5 py-2 text-sm font-semibold transition-colors duration-200", activeId === anchor.id
                                        ? "bg-accent-lime text-ink-900"
                                        : "text-ink-900/70 hover:bg-surface-card hover:text-ink-900"), children: anchor.label }) }, anchor.id))) }) }), _jsxs("div", { className: "hidden items-center gap-2 lg:flex", children: [_jsx(Link, { to: "/login", children: _jsx(Button, { variant: "secondary", children: "Se connecter" }) }), _jsx(Link, { to: "/integration", children: _jsx(Button, { variant: "primary", children: "Int\u00E9grer mon laboratoire" }) })] }), _jsx(Button, { variant: "secondary", size: "icon", className: "lg:hidden", "aria-label": open ? "Fermer le menu" : "Ouvrir le menu", "aria-expanded": open, onClick: () => setOpen((v) => !v), children: open ? (_jsx(X, { className: "size-5", "aria-hidden": "true" })) : (_jsx(Menu, { className: "size-5", "aria-hidden": "true" })) })] }), open && (_jsx(Container, { className: "lg:hidden", children: _jsxs("nav", { "aria-label": "Navigation mobile", className: "mt-3 rounded-3xl glass-card p-4", children: [_jsx("ul", { className: "flex flex-col gap-1", children: anchors.map((anchor) => (_jsx("li", { children: _jsx("a", { href: `#${anchor.id}`, onClick: () => setOpen(false), className: "block rounded-2xl px-3 py-2.5 text-sm font-semibold text-ink-900 hover:bg-surface-card", children: anchor.label }) }, anchor.id))) }), _jsxs("div", { className: "mt-3 flex flex-col gap-2", children: [_jsx(Link, { to: "/login", onClick: () => setOpen(false), children: _jsx(Button, { variant: "secondary", className: "w-full", children: "Se connecter" }) }), _jsx(Link, { to: "/integration", onClick: () => setOpen(false), children: _jsx(Button, { className: "w-full", children: "Int\u00E9grer mon laboratoire" }) })] })] }) }))] }));
}
