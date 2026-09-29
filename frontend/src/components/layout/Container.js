import { jsx as _jsx } from "react/jsx-runtime";
import { cn } from "@/lib/utils";
export function Container({ children, className, }) {
    return (_jsx("div", { className: cn("mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8", className), children: children }));
}
