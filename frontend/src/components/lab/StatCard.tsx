import { motion, useInView } from "framer-motion";
import type { LucideIcon } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Card } from "./Card";
import { IconBox } from "./IconBox";
import { cn } from "@/lib/utils";

type StatCardProps = {
  value: number;
  suffix?: string;
  label: string;
  icon: LucideIcon;
  className?: string | undefined;
};

function useCounter(target: number, active: boolean, duration = 1200) {
  const [value, setValue] = useState(0);
  useEffect(() => {
    if (!active) return;
    if (
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      setValue(target);
      return;
    }
    let raf = 0;
    const start = performance.now();
    const tick = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      setValue(Math.round(target * (1 - Math.pow(1 - p, 3))));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [active, target, duration]);
  return value;
}

export function StatCard({
  value,
  suffix,
  label,
  icon,
  className,
}: StatCardProps) {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, amount: 0.5 });
  const shown = useCounter(value, inView);

  return (
    <motion.div ref={ref} whileHover={{ y: -4 }}>
      <Card variant="glass" padding="md" className={cn("h-full", className)}>
        <IconBox icon={icon} variant="lime" shape="circle" />
        <p className="mt-4 text-4xl font-extrabold text-brand-900">
          {shown}
          {suffix}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">{label}</p>
      </Card>
    </motion.div>
  );
}
