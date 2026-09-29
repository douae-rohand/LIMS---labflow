import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cva } from "class-variance-authority";
import { cn } from "@/lib/utils";
const badgeVariants = cva("inline-flex items-center gap-2 rounded-full font-semibold tracking-wide", {
    variants: {
        variant: {
            lime: "bg-accent-lime text-ink-900",
            mint: "bg-mint-200 text-brand-900",
            soft: "bg-surface-card text-brand-900 border border-brand-900/10",
            dark: "bg-brand-900 text-primary-foreground",
            outline: "border border-brand-900/20 text-brand-900",
        },
        size: {
            sm: "px-2.5 py-1 text-[11px]",
            md: "px-3.5 py-1.5 text-xs",
        },
    },
    defaultVariants: { variant: "lime", size: "md" },
});
export function Badge({ children, className, variant, size, dot, icon, }) {
    return (_jsxs("span", { className: cn(badgeVariants({ variant, size }), className), children: [dot && (_jsx("span", { className: "size-1.5 rounded-full bg-brand-900", "aria-hidden": "true" })), icon, children] }));
}
