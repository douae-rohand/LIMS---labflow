import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Badge } from "./Badge";
import { Reveal } from "./Reveal";
import { cn } from "@/lib/utils";
export function SectionHeader({ eyebrow, title, description, align = "center", className, }) {
    return (_jsxs(Reveal, { className: cn("flex flex-col gap-4", align === "center" ? "items-center text-center" : "items-start", className), children: [eyebrow && (_jsx(Badge, { variant: "lime", dot: true, children: eyebrow })), _jsx("h2", { className: "max-w-3xl text-3xl font-extrabold text-ink-900 sm:text-4xl", children: title }), description && (_jsx("p", { className: "max-w-2xl text-base text-muted-foreground", children: description }))] }));
}
