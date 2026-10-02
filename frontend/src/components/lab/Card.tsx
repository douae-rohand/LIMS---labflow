import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const cardVariants = cva("rounded-3xl", {
  variants: {
    variant: {
      flat: "bg-card border border-brand-900/10 shadow-soft",
      surface: "bg-surface-card border border-brand-900/5",
      glass: "glass-card glossy-top",
      glossy: "glossy-top bg-card border border-white/60 shadow-glossy",
      bento: "bg-surface-alt border border-brand-900/5",
      dark: "glossy-top bg-brand-900 text-primary-foreground border border-white/10",
      lime: "glossy-top bg-accent-lime text-ink-900",
    },
    padding: {
      none: "",
      sm: "p-4",
      md: "p-6",
      lg: "p-8",
    },
  },
  defaultVariants: { variant: "flat", padding: "md" },
});

type CardProps = HTMLAttributes<HTMLDivElement> & VariantProps<typeof cardVariants>;

export function Card({ className, variant, padding, ...props }: CardProps) {
  return (
    <div className={cn(cardVariants({ variant, padding }), className)} {...props} />
  );
}
