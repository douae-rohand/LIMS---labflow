import { jsx as _jsx } from "react/jsx-runtime";
import { motion } from "framer-motion";
import { fadeUp } from "@/lib/motion";
export function Reveal({ children, variants = fadeUp, delay = 0, className, as = "div", }) {
    const Comp = motion[as];
    return (_jsx(Comp, { className: className, variants: variants, initial: "hidden", whileInView: "show", viewport: { once: true, amount: 0.25 }, transition: { delay }, children: children }));
}
