import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cn } from "@/lib/utils";
export function Avatar({ initials, src, alt, className, }) {
    return (_jsx("span", { className: cn("inline-flex size-11 items-center justify-center overflow-hidden rounded-full bg-mint-200 text-sm font-bold text-brand-900", className), children: src ? (_jsx("img", { src: src, alt: alt ?? "", loading: "lazy", className: "size-full object-cover" })) : (initials) }));
}
export function Skeleton({ className }) {
    return (_jsx("span", { className: cn("block animate-pulse rounded-xl bg-surface-card", className), "aria-hidden": "true" }));
}
export function Table({ columns, rows, }) {
    return (_jsx("div", { className: "overflow-hidden rounded-3xl border border-brand-900/10", children: _jsxs("table", { className: "w-full text-left text-sm", children: [_jsx("thead", { className: "bg-surface-card text-brand-900", children: _jsx("tr", { children: columns.map((c) => (_jsx("th", { className: "px-4 py-3 font-semibold", children: c }, c))) }) }), _jsx("tbody", { children: rows.map((row, i) => (_jsx("tr", { className: "border-t border-brand-900/10", children: row.map((cell, j) => (_jsx("td", { className: "px-4 py-3 text-muted-foreground", children: cell }, j))) }, i))) })] }) }));
}
