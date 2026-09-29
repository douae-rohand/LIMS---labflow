import { jsx as _jsx } from "react/jsx-runtime";
import { Container } from "./Container";
import { cn } from "@/lib/utils";
export function Section({ id, children, className, containerClassName, tone = "white", }) {
    const tones = {
        white: "bg-background",
        alt: "bg-surface-alt",
        card: "bg-surface-card",
        mint: "bg-mint-200/40",
    };
    return (_jsx("section", { id: id, className: cn("relative scroll-mt-24 overflow-hidden py-20 sm:py-24", tones[tone], className), children: _jsx(Container, { className: containerClassName, children: children }) }));
}
