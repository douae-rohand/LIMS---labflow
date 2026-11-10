import { ArrowDown, ArrowUp } from "lucide-react";
import { useEffect, useState } from "react";

/** Distance (px) à partir de laquelle on considère que la page a « défilé ». */
const SEUIL = 300;

/**
 * Flèches flottantes pour les pages longues : une pour remonter au début,
 * une pour descendre à la fin. Elles n'apparaissent que si la page dépasse
 * nettement la hauteur de l'écran, et chacune seulement quand elle est utile.
 */
export function FlechesDefilement() {
  const [haut, setHaut] = useState(false);
  const [bas, setBas] = useState(false);

  useEffect(() => {
    let image = 0;
    const mesurer = () => {
      image = 0;
      const total = document.documentElement.scrollHeight;
      const ecran = window.innerHeight;
      const position = window.scrollY;
      const longue = total > ecran * 1.5;
      setHaut(longue && position > SEUIL);
      setBas(longue && position + ecran < total - SEUIL);
    };
    const planifier = () => {
      if (!image) image = requestAnimationFrame(mesurer);
    };
    mesurer();
    window.addEventListener("scroll", planifier, { passive: true });
    window.addEventListener("resize", planifier);
    // Le contenu change de hauteur sans défilement (données chargées, panneaux ouverts…).
    const observateur = new ResizeObserver(planifier);
    observateur.observe(document.body);
    return () => {
      window.removeEventListener("scroll", planifier);
      window.removeEventListener("resize", planifier);
      observateur.disconnect();
      if (image) cancelAnimationFrame(image);
    };
  }, []);

  if (!haut && !bas) return null;

  const bouton =
    "flex size-11 items-center justify-center rounded-full border border-brand-900/15 bg-card text-brand-900 shadow-lg " +
    "transition hover:bg-brand-900 hover:text-primary-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900";

  return (
    <div className="fixed right-4 bottom-4 z-40 flex flex-col gap-2 print:hidden">
      {haut && (
        <button
          type="button"
          className={bouton}
          onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          aria-label="Revenir en haut de la page"
          title="Haut de la page"
        >
          <ArrowUp className="size-5" aria-hidden="true" />
        </button>
      )}
      {bas && (
        <button
          type="button"
          className={bouton}
          onClick={() => window.scrollTo({ top: document.documentElement.scrollHeight, behavior: "smooth" })}
          aria-label="Aller en bas de la page"
          title="Bas de la page"
        >
          <ArrowDown className="size-5" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}
