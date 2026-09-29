import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion, useInView } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { Card } from "./Card";
import { IconBox } from "./IconBox";
import { cn } from "@/lib/utils";
function useCounter(target, active, duration = 1200) {
    const [value, setValue] = useState(0);
    useEffect(() => {
        if (!active)
            return;
        if (typeof window !== "undefined" &&
            window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            setValue(target);
            return;
        }
        let raf = 0;
        const start = performance.now();
        const tick = (now) => {
            const p = Math.min((now - start) / duration, 1);
            setValue(Math.round(target * (1 - Math.pow(1 - p, 3))));
            if (p < 1)
                raf = requestAnimationFrame(tick);
        };
        raf = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(raf);
    }, [active, target, duration]);
    return value;
}
export function StatCard({ value, suffix, label, icon, className, }) {
    const ref = useRef(null);
    const inView = useInView(ref, { once: true, amount: 0.5 });
    const shown = useCounter(value, inView);
    return (_jsx(motion.div, { ref: ref, whileHover: { y: -4 }, children: _jsxs(Card, { variant: "glass", padding: "md", className: cn("h-full", className), children: [_jsx(IconBox, { icon: icon, variant: "lime", shape: "circle" }), _jsxs("p", { className: "mt-4 text-4xl font-extrabold text-brand-900", children: [shown, suffix] }), _jsx("p", { className: "mt-1 text-sm text-muted-foreground", children: label })] }) }));
}
