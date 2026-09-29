import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import * as TooltipPrimitive from "@radix-ui/react-tooltip";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
export function Modal({ trigger, title, children, }) {
    return (_jsxs(DialogPrimitive.Root, { children: [_jsx(DialogPrimitive.Trigger, { asChild: true, children: trigger }), _jsxs(DialogPrimitive.Portal, { children: [_jsx(DialogPrimitive.Overlay, { className: "fixed inset-0 z-50 bg-ink-900/40 backdrop-blur-sm" }), _jsxs(DialogPrimitive.Content, { className: "glass-card glossy-top fixed top-1/2 left-1/2 z-50 w-[90vw] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-3xl p-6", children: [_jsx(DialogPrimitive.Title, { className: "text-lg font-bold text-ink-900", children: title }), _jsx("div", { className: "mt-3 text-sm text-muted-foreground", children: children }), _jsx(DialogPrimitive.Close, { "aria-label": "Fermer la fen\u00EAtre", className: "absolute top-4 right-4 flex size-9 items-center justify-center rounded-full bg-surface-card text-brand-900", children: _jsx(X, { className: "size-4", "aria-hidden": "true" }) })] })] })] }));
}
export function Tooltip({ children, content, }) {
    return (_jsx(TooltipPrimitive.Provider, { delayDuration: 150, children: _jsxs(TooltipPrimitive.Root, { children: [_jsx(TooltipPrimitive.Trigger, { asChild: true, children: children }), _jsx(TooltipPrimitive.Portal, { children: _jsx(TooltipPrimitive.Content, { sideOffset: 8, className: "z-50 rounded-xl bg-brand-900 px-3 py-1.5 text-xs font-medium text-primary-foreground", children: content }) })] }) }));
}
export function Tabs({ items, className, }) {
    return (_jsxs(TabsPrimitive.Root, { defaultValue: items[0]?.value, className: cn(className), children: [_jsx(TabsPrimitive.List, { className: "inline-flex gap-1 rounded-full bg-surface-card p-1", children: items.map((item) => (_jsx(TabsPrimitive.Trigger, { value: item.value, className: "rounded-full px-4 py-2 text-sm font-semibold text-brand-900 transition-colors duration-200 data-[state=active]:bg-brand-900 data-[state=active]:text-primary-foreground", children: item.label }, item.value))) }), items.map((item) => (_jsx(TabsPrimitive.Content, { value: item.value, className: "mt-4 text-sm text-muted-foreground", children: item.content }, item.value)))] }));
}
