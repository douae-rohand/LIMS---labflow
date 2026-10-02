import type { ReactNode } from "react";
import { Badge } from "./Badge";
import { Reveal } from "./Reveal";
import { cn } from "@/lib/utils";

type SectionHeaderProps = {
  eyebrow?: string;
  title: ReactNode;
  description?: string;
  align?: "left" | "center";
  className?: string | undefined;
};

export function SectionHeader({
  eyebrow,
  title,
  description,
  align = "center",
  className,
}: SectionHeaderProps) {
  return (
    <Reveal
      className={cn(
        "flex flex-col gap-4",
        align === "center" ? "items-center text-center" : "items-start",
        className,
      )}
    >
      {eyebrow && (
        <Badge variant="lime" dot>
          {eyebrow}
        </Badge>
      )}
      <h2 className="max-w-3xl text-3xl font-extrabold text-ink-900 sm:text-4xl">
        {title}
      </h2>
      {description && (
        <p className="max-w-2xl text-base text-muted-foreground">{description}</p>
      )}
    </Reveal>
  );
}
