import { motion } from "framer-motion";
import type { LucideIcon } from "lucide-react";
import { staggerContainer, fadeUp } from "@/lib/motion";
import { cn } from "@/lib/utils";

export type Step = { title: string; description: string; icon: LucideIcon };

export function Stepper({ steps, className }: { steps: Step[]; className?: string }) {
  return (
    <div className={cn("relative", className)}>
      <motion.div
        className="absolute top-6 left-0 hidden h-px origin-left bg-brand-900/20 lg:block lg:w-full"
        initial={{ scaleX: 0 }}
        whileInView={{ scaleX: 1 }}
        viewport={{ once: true, amount: 0.3 }}
        transition={{ duration: 1.2, ease: [0.22, 1, 0.36, 1] }}
        aria-hidden="true"
      />
      <motion.ol
        className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3"
        variants={staggerContainer(0.08)}
        initial="hidden"
        whileInView="show"
        viewport={{ once: true, amount: 0.2 }}
      >
        {steps.map((step, index) => (
          <motion.li key={step.title} variants={fadeUp} className="relative">
            <div className="flex items-start gap-4">
              <span className="glossy-top flex size-12 shrink-0 items-center justify-center rounded-full bg-brand-900 text-sm font-bold text-primary-foreground">
                {String(index + 1).padStart(2, "0")}
              </span>
              <div>
                <div className="flex items-center gap-2">
                  <step.icon className="size-4 text-brand-600" aria-hidden="true" />
                  <h3 className="text-sm font-bold text-ink-900">{step.title}</h3>
                </div>
                <p className="mt-1 text-sm text-muted-foreground">
                  {step.description}
                </p>
              </div>
            </div>
          </motion.li>
        ))}
      </motion.ol>
    </div>
  );
}
