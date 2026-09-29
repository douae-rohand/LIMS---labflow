import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Footer } from "@/components/layout/Footer";
import { Navbar } from "@/components/layout/Navbar";
/** Structure des pages publiques. Un AppLayout (espace connecté) viendra à côté. */
export function PublicLayout({ children }) {
    return (_jsxs("div", { className: "flex min-h-dvh flex-col bg-background", children: [_jsx(Navbar, {}), _jsx("main", { className: "flex-1", children: children }), _jsx(Footer, {})] }));
}
