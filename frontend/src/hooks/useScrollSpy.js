import { useEffect, useState } from "react";
/** Renvoie l'identifiant de la section actuellement visible. */
export function useScrollSpy(ids, offset = 120) {
    const [activeId, setActiveId] = useState(null);
    useEffect(() => {
        const onScroll = () => {
            let current = null;
            for (const id of ids) {
                const el = document.getElementById(id);
                if (el && el.getBoundingClientRect().top - offset <= 0)
                    current = id;
            }
            setActiveId(current);
        };
        onScroll();
        window.addEventListener("scroll", onScroll, { passive: true });
        return () => window.removeEventListener("scroll", onScroll);
    }, [ids, offset]);
    return activeId;
}
/** Vrai dès que la page a été défilée au-delà du seuil. */
export function useScrolled(threshold = 24) {
    const [scrolled, setScrolled] = useState(false);
    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > threshold);
        onScroll();
        window.addEventListener("scroll", onScroll, { passive: true });
        return () => window.removeEventListener("scroll", onScroll);
    }, [threshold]);
    return scrolled;
}
