import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";
const field = "w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm text-ink-900 placeholder:text-muted-foreground transition-colors duration-200 focus:border-brand-600";
export function Label({ children, htmlFor, }) {
    return (_jsx("label", { htmlFor: htmlFor, className: "text-sm font-semibold text-ink-900", children: children }));
}
export function Input({ className, ...props }) {
    return _jsx("input", { className: cn(field, className), ...props });
}
export function Textarea({ className, ...props }) {
    return _jsx("textarea", { className: cn(field, "min-h-28", className), ...props });
}
export function Select({ className, ...props }) {
    return _jsx("select", { className: cn(field, className), ...props });
}
export function Checkbox({ label, id, }) {
    return (_jsxs("div", { className: "flex items-center gap-3", children: [_jsx(CheckboxPrimitive.Root, { id: id, className: "flex size-5 items-center justify-center rounded-md border border-input bg-card data-[state=checked]:bg-brand-900", children: _jsx(CheckboxPrimitive.Indicator, { children: _jsx(Check, { className: "size-3.5 text-primary-foreground", "aria-hidden": "true" }) }) }), _jsx(Label, { htmlFor: id, children: label })] }));
}
