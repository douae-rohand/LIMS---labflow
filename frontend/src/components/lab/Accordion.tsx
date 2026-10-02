import * as AccordionPrimitive from "@radix-ui/react-accordion";
import { Plus } from "lucide-react";
import { cn } from "@/lib/utils";

type Item = { question: string; answer: string };

export function Accordion({
  items,
  className,
}: {
  items: Item[];
  className?: string | undefined;
}) {
  return (
    <AccordionPrimitive.Root
      type="single"
      collapsible
      className={cn("flex flex-col gap-3", className)}
    >
      {items.map((item, index) => (
        <AccordionPrimitive.Item
          key={item.question}
          value={`item-${index}`}
          className="glossy-top overflow-hidden rounded-3xl border border-brand-900/10 bg-card shadow-soft"
        >
          <AccordionPrimitive.Header>
            <AccordionPrimitive.Trigger className="group flex w-full items-center justify-between gap-4 px-6 py-5 text-left text-base font-semibold text-ink-900">
              {item.question}
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-card text-brand-900 transition-transform duration-300 group-data-[state=open]:rotate-45">
                <Plus className="size-4" aria-hidden="true" />
              </span>
            </AccordionPrimitive.Trigger>
          </AccordionPrimitive.Header>
          <AccordionPrimitive.Content className="acc-content overflow-hidden">
            <p className="px-6 pb-6 text-sm leading-relaxed text-muted-foreground">
              {item.answer}
            </p>
          </AccordionPrimitive.Content>
        </AccordionPrimitive.Item>
      ))}
    </AccordionPrimitive.Root>
  );
}
