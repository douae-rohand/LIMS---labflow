import { motion } from "framer-motion";
import { ArrowDown, ArrowLeft, ArrowRight, type LucideIcon } from "lucide-react";
import { staggerContainer, fadeUp } from "@/lib/motion";
import { cn } from "@/lib/utils";

export type Step = { title: string; description: string; icon: LucideIcon };

const COLS = 3;

function snakeRows<T>(items: T[]): T[][] {
  const rows: T[][] = [];
  for (let i = 0; i < items.length; i += COLS) {
    rows.push(items.slice(i, i + COLS));
  }
  return rows;
}

function StepCard({
  step,
  index,
}: {
  step: Step;
  index: number;
}) {
  return (
    <div className="hover-lift glossy-top h-full rounded-3xl border border-white/60 bg-card p-5 shadow-glossy">
      <div className="flex items-start gap-3">
        <span className="glossy-top flex size-12 shrink-0 items-center justify-center rounded-full bg-brand-900 text-sm font-bold text-primary-foreground">
          {String(index + 1).padStart(2, "0")}
        </span>
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <step.icon className="size-4 shrink-0 text-brand-600" aria-hidden="true" />
            <h3 className="text-sm font-bold text-ink-900">{step.title}</h3>
          </div>
          <p className="mt-1.5 text-sm text-muted-foreground">{step.description}</p>
        </div>
      </div>
    </div>
  );
}

function FlowArrow({
  direction,
  className,
}: {
  direction: "right" | "left" | "down";
  className?: string;
}) {
  const Icon = direction === "down" ? ArrowDown : direction === "left" ? ArrowLeft : ArrowRight;
  return (
    <span
      className={cn(
        "flex items-center justify-center text-brand-600",
        className,
      )}
      aria-hidden="true"
    >
      <span className="flex size-8 items-center justify-center rounded-full bg-mint-200">
        <Icon className="size-4" />
      </span>
    </span>
  );
}

export function Stepper({ steps, className }: { steps: Step[]; className?: string }) {
  const rows = snakeRows(steps.map((step, index) => ({ step, index })));

  return (
    <div className={cn("relative", className)}>
      <ol className="flex flex-col gap-4 lg:hidden">
        {steps.map((step, index) => (
          <li key={step.title}>
            <StepCard step={step} index={index} />
            {index < steps.length - 1 ? (
              <FlowArrow direction="down" className="mx-auto my-2" />
            ) : null}
          </li>
        ))}
      </ol>

      <motion.ol
        className="hidden lg:flex lg:flex-col lg:gap-3"
        variants={staggerContainer(0.08)}
        initial="hidden"
        whileInView="show"
        viewport={{ once: true, amount: 0.15 }}
        aria-label="Parcours de la demande à la clôture"
      >
        {rows.map((row, rowIndex) => {
          const reversed = rowIndex % 2 === 1;
          const visual = reversed ? [...row].reverse() : row;
          const turnDownOnLeft = reversed;
          const isLastRow = rowIndex === rows.length - 1;

          return (
            <li key={rowIndex} className="contents">
              <div className="grid grid-cols-[1fr_auto_1fr_auto_1fr] items-stretch gap-y-0">
                {visual.map((cell, visualIndex) => (
                  <motion.div
                    key={cell.step.title}
                    variants={fadeUp}
                    className="contents"
                  >
                    <div className="min-w-0">
                      <StepCard step={cell.step} index={cell.index} />
                    </div>
                    {visualIndex < visual.length - 1 ? (
                      <FlowArrow
                        direction={reversed ? "left" : "right"}
                        className="mx-2 self-center"
                      />
                    ) : null}
                  </motion.div>
                ))}
              </div>

              {!isLastRow ? (
                <div
                  className="grid grid-cols-[1fr_auto_1fr_auto_1fr]"
                  aria-hidden="true"
                >
                  <div
                    className={cn(
                      "flex justify-center py-1",
                      turnDownOnLeft ? "col-start-1" : "col-start-5",
                    )}
                  >
                    <FlowArrow direction="down" />
                  </div>
                </div>
              ) : null}
            </li>
          );
        })}
      </motion.ol>
    </div>
  );
}
