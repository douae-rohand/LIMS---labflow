import type { ReactNode } from "react";
import { Container } from "./Container";
import { cn } from "@/lib/utils";

export function Section({
  id,
  children,
  className,
  containerClassName,
  tone = "white",
}: {
  id?: string;
  children: ReactNode;
  className?: string | undefined;
  containerClassName?: string;
  tone?: "white" | "alt" | "card" | "mint";
}) {
  const tones = {
    white: "bg-background",
    alt: "bg-surface-alt",
    card: "bg-surface-card",
    mint: "bg-mint-200/40",
  } as const;

  return (
    <section
      id={id}
      className={cn(
        "relative scroll-mt-24 overflow-hidden py-20 sm:py-24",
        tones[tone],
        className,
      )}
    >
      <Container className={containerClassName}>{children}</Container>
    </section>
  );
}
