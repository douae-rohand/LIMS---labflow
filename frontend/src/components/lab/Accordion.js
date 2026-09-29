import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import * as AccordionPrimitive from "@radix-ui/react-accordion";
import { Plus } from "lucide-react";
import { cn } from "@/lib/utils";
export function Accordion({ items, className, }) {
    return (_jsx(AccordionPrimitive.Root, { type: "single", collapsible: true, className: cn("flex flex-col gap-3", className), children: items.map((item, index) => (_jsxs(AccordionPrimitive.Item, { value: `item-${index}`, className: "glossy-top overflow-hidden rounded-3xl border border-brand-900/10 bg-card shadow-soft", children: [_jsx(AccordionPrimitive.Header, { children: _jsxs(AccordionPrimitive.Trigger, { className: "group flex w-full items-center justify-between gap-4 px-6 py-5 text-left text-base font-semibold text-ink-900", children: [item.question, _jsx("span", { className: "flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-card text-brand-900 transition-transform duration-300 group-data-[state=open]:rotate-45", children: _jsx(Plus, { className: "size-4", "aria-hidden": "true" }) })] }) }), _jsx(AccordionPrimitive.Content, { className: "acc-content overflow-hidden", children: _jsx("p", { className: "px-6 pb-6 text-sm leading-relaxed text-muted-foreground", children: item.answer }) })] }, item.question))) }));
}
