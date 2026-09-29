import { jsx as _jsx } from "react/jsx-runtime";
import { cva } from "class-variance-authority";
import { cn } from "@/lib/utils";
const iconBoxVariants = cva("inline-flex items-center justify-center shrink-0 transition-transform duration-200", {
    variants: {
        variant: {
            brand: "bg-brand-900 text-primary-foreground",
            mint: "bg-mint-200 text-brand-900",
            lime: "bg-accent-lime text-ink-900",
            soft: "bg-surface-card text-brand-900",
            glass: "glass-card text-brand-900",
        },
        shape: {
            rounded: "rounded-2xl",
            circle: "rounded-full",
        },
        size: {
            sm: "size-9",
            md: "size-11",
            lg: "size-14",
        },
    },
    defaultVariants: { variant: "mint", shape: "rounded", size: "md" },
});
export function IconBox({ icon: Icon, className, variant, shape, size, }) {
    const iconSize = size === "lg" ? "size-6" : size === "sm" ? "size-4" : "size-5";
    return (_jsx("span", { className: cn(iconBoxVariants({ variant, shape, size }), className), children: _jsx(Icon, { className: iconSize, "aria-hidden": "true" }) }));
}
