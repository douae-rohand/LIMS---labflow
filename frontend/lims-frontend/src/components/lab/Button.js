import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cva } from "class-variance-authority";
import { Loader2 } from "lucide-react";
import { forwardRef } from "react";
import { cn } from "@/lib/utils";
export const buttonVariants = cva("inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-all duration-200 disabled:pointer-events-none disabled:opacity-60", {
    variants: {
        variant: {
            primary: "btn-gloss glossy-top text-primary-foreground hover:-translate-y-0.5",
            secondary: "bg-surface-card text-brand-900 border border-brand-900/10 hover:bg-mint-200/60",
            ghost: "text-brand-900 hover:bg-surface-card",
            lime: "glossy-top bg-accent-lime text-ink-900 hover:-translate-y-0.5",
            outlineLight: "border border-white/40 text-primary-foreground hover:bg-white/10",
        },
        size: {
            sm: "h-9 px-4 text-sm",
            md: "h-11 px-5 text-sm",
            lg: "h-13 px-7 text-base",
            icon: "size-11",
        },
    },
    defaultVariants: { variant: "primary", size: "md" },
});
export const Button = forwardRef(({ className, variant, size, loading, icon, iconPosition = "right", children, ...props }, ref) => (_jsxs("button", { ref: ref, className: cn(buttonVariants({ variant, size }), className), ...props, children: [loading ? (_jsx(Loader2, { className: "size-4 animate-spin", "aria-hidden": "true" })) : (icon && iconPosition === "left" && icon), children, !loading && icon && iconPosition === "right" && icon] })));
Button.displayName = "Button";
