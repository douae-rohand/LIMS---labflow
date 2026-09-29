import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "./Button";
import { cn } from "@/lib/utils";
/** Défilement horizontal avec snap : la carte centrée est agrandie. */
export function Carousel({ children, className, ariaLabel }) {
    const trackRef = useRef(null);
    const [active, setActive] = useState(0);
    const onScroll = useCallback(() => {
        const track = trackRef.current;
        if (!track)
            return;
        const center = track.scrollLeft + track.clientWidth / 2;
        let best = 0;
        let bestDist = Infinity;
        Array.from(track.children).forEach((child, i) => {
            const el = child;
            const dist = Math.abs(el.offsetLeft + el.offsetWidth / 2 - center);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        });
        setActive(best);
    }, []);
    useEffect(() => {
        onScroll();
    }, [onScroll]);
    const scrollBy = (dir) => {
        const track = trackRef.current;
        if (!track)
            return;
        track.scrollBy({ left: dir * track.clientWidth * 0.6, behavior: "smooth" });
    };
    return (_jsxs("div", { className: cn("relative", className), children: [_jsx("div", { ref: trackRef, onScroll: onScroll, role: "group", "aria-label": ariaLabel, className: "no-scrollbar flex snap-x snap-mandatory gap-5 overflow-x-auto px-1 pb-6", children: children.map((child, i) => (_jsx("div", { className: cn("w-[85%] shrink-0 snap-center transition-all duration-300 sm:w-[46%] lg:w-[30%]", active === i ? "scale-100 opacity-100" : "scale-95 opacity-70"), children: child }, i))) }), _jsxs("div", { className: "flex justify-center gap-3", children: [_jsx(Button, { variant: "secondary", size: "icon", "aria-label": "Carte pr\u00E9c\u00E9dente", onClick: () => scrollBy(-1), children: _jsx(ChevronLeft, { className: "size-5", "aria-hidden": "true" }) }), _jsx(Button, { variant: "secondary", size: "icon", "aria-label": "Carte suivante", onClick: () => scrollBy(1), children: _jsx(ChevronRight, { className: "size-5", "aria-hidden": "true" }) })] })] }));
}
