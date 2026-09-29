export const EASE = [0.22, 1, 0.36, 1];
export const enter = { duration: 0.5, ease: EASE };
export const fadeUp = {
    hidden: { opacity: 0, y: 24 },
    show: { opacity: 1, y: 0, transition: enter },
};
export const fadeIn = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: enter },
};
export const scaleIn = {
    hidden: { opacity: 0, scale: 0.96 },
    show: { opacity: 1, scale: 1, transition: { duration: 0.6, ease: EASE } },
};
export const slideInLeft = {
    hidden: { opacity: 0, x: -28 },
    show: { opacity: 1, x: 0, transition: enter },
};
export const slideInRight = {
    hidden: { opacity: 0, x: 28 },
    show: { opacity: 1, x: 0, transition: enter },
};
export const staggerContainer = (stagger = 0.07, delay = 0) => ({
    hidden: {},
    show: { transition: { staggerChildren: stagger, delayChildren: delay } },
});
export const levitate = (offset = 8, duration = 5) => ({
    animate: { y: [0, -offset, 0] },
    transition: { duration, repeat: Infinity, ease: "easeInOut" },
});
