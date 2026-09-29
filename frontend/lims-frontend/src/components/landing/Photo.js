import { jsx as _jsx } from "react/jsx-runtime";
import { cn } from "@/lib/utils";
/** Photo avec voile mint/sarcelle, coins arrondis et reflet glossy. */
export function Photo({ src, alt, className, imgClassName, eager, }) {
    return (_jsx("div", { className: cn("photo-tint glossy-top rounded-3xl border border-white/60 bg-mint-200 shadow-glossy", className), children: _jsx("img", { src: src, alt: alt, loading: eager ? "eager" : "lazy", decoding: "async", className: cn("size-full object-cover", imgClassName) }) }));
}
