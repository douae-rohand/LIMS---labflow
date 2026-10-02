import { ChevronLeft, ChevronRight } from "lucide-react";
import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { Button } from "./Button";
import { cn } from "@/lib/utils";

type CarouselProps = {
  children: ReactNode[];
  className?: string | undefined;
  ariaLabel: string;
};

/** Défilement horizontal avec snap : la carte centrée est agrandie. */
export function Carousel({ children, className, ariaLabel }: CarouselProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);

  const onScroll = useCallback(() => {
    const track = trackRef.current;
    if (!track) return;
    const center = track.scrollLeft + track.clientWidth / 2;
    let best = 0;
    let bestDist = Infinity;
    Array.from(track.children).forEach((child, i) => {
      const el = child as HTMLElement;
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

  const scrollBy = (dir: 1 | -1) => {
    const track = trackRef.current;
    if (!track) return;
    track.scrollBy({ left: dir * track.clientWidth * 0.6, behavior: "smooth" });
  };

  return (
    <div className={cn("relative", className)}>
      <div
        ref={trackRef}
        onScroll={onScroll}
        role="group"
        aria-label={ariaLabel}
        className="no-scrollbar flex snap-x snap-mandatory gap-5 overflow-x-auto px-1 pb-6"
      >
        {children.map((child, i) => (
          <div
            key={i}
            className={cn(
              "w-[85%] shrink-0 snap-center transition-all duration-300 sm:w-[46%] lg:w-[30%]",
              active === i ? "scale-100 opacity-100" : "scale-95 opacity-70",
            )}
          >
            {child}
          </div>
        ))}
      </div>
      <div className="flex justify-center gap-3">
        <Button
          variant="secondary"
          size="icon"
          aria-label="Carte précédente"
          onClick={() => scrollBy(-1)}
        >
          <ChevronLeft className="size-5" aria-hidden="true" />
        </Button>
        <Button
          variant="secondary"
          size="icon"
          aria-label="Carte suivante"
          onClick={() => scrollBy(1)}
        >
          <ChevronRight className="size-5" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
