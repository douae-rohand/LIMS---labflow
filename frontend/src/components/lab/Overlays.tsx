import * as DialogPrimitive from "@radix-ui/react-dialog";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import * as TooltipPrimitive from "@radix-ui/react-tooltip";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Modal({
  trigger,
  title,
  children,
}: {
  trigger: ReactNode;
  title: string;
  children: ReactNode;
}) {
  return (
    <DialogPrimitive.Root>
      <DialogPrimitive.Trigger asChild>{trigger}</DialogPrimitive.Trigger>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-ink-900/40 backdrop-blur-sm" />
        <DialogPrimitive.Content className="glass-card glossy-top fixed top-1/2 left-1/2 z-50 w-[90vw] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-3xl p-6">
          <DialogPrimitive.Title className="text-lg font-bold text-ink-900">
            {title}
          </DialogPrimitive.Title>
          <div className="mt-3 text-sm text-muted-foreground">{children}</div>
          <DialogPrimitive.Close
            aria-label="Fermer la fenêtre"
            className="absolute top-4 right-4 flex size-9 items-center justify-center rounded-full bg-surface-card text-brand-900"
          >
            <X className="size-4" aria-hidden="true" />
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

export function Tooltip({
  children,
  content,
}: {
  children: ReactNode;
  content: string;
}) {
  return (
    <TooltipPrimitive.Provider delayDuration={150}>
      <TooltipPrimitive.Root>
        <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
        <TooltipPrimitive.Portal>
          <TooltipPrimitive.Content
            sideOffset={8}
            className="z-50 rounded-xl bg-brand-900 px-3 py-1.5 text-xs font-medium text-primary-foreground"
          >
            {content}
          </TooltipPrimitive.Content>
        </TooltipPrimitive.Portal>
      </TooltipPrimitive.Root>
    </TooltipPrimitive.Provider>
  );
}

export function Tabs({
  items,
  className,
}: {
  items: { value: string; label: string; content: ReactNode }[];
  className?: string | undefined;
}) {
  return (
    <TabsPrimitive.Root defaultValue={items[0]?.value ?? ""} className={cn(className)}>
      <TabsPrimitive.List className="inline-flex gap-1 rounded-full bg-surface-card p-1">
        {items.map((item) => (
          <TabsPrimitive.Trigger
            key={item.value}
            value={item.value}
            className="rounded-full px-4 py-2 text-sm font-semibold text-brand-900 transition-colors duration-200 data-[state=active]:bg-brand-900 data-[state=active]:text-primary-foreground"
          >
            {item.label}
          </TabsPrimitive.Trigger>
        ))}
      </TabsPrimitive.List>
      {items.map((item) => (
        <TabsPrimitive.Content
          key={item.value}
          value={item.value}
          className="mt-4 text-sm text-muted-foreground"
        >
          {item.content}
        </TabsPrimitive.Content>
      ))}
    </TabsPrimitive.Root>
  );
}
