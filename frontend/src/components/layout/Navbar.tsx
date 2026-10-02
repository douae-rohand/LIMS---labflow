import { Link } from "@tanstack/react-router";
import { Menu, X } from "lucide-react";
import { useState } from "react";
import { Container } from "./Container";
import { Button } from "@/components/lab";
import { anchors } from "@/data/navigation";
import { useScrollSpy, useScrolled } from "@/hooks/useScrollSpy";
import { cn } from "@/lib/utils";

export function Navbar() {
  const [open, setOpen] = useState(false);
  const scrolled = useScrolled();
  const activeId = useScrollSpy(anchors.map((a) => a.id));

  return (
    <header
      className={cn(
        "sticky top-0 z-40 border-b transition-all duration-300",
        scrolled
          ? "border-brand-900/10 bg-background/70 py-2 backdrop-blur-xl"
          : "border-transparent bg-background/40 py-4 backdrop-blur-md",
      )}
    >
      <Container className="flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center">
          <img src="/nom.png" alt="LabFlow" className="h-12" />
        </Link>

        <nav aria-label="Navigation principale" className="hidden lg:block">
          <ul className="flex items-center gap-1">
            {anchors.map((anchor) => (
              <li key={anchor.id}>
                <a
                  href={`#${anchor.id}`}
                  className={cn(
                    "rounded-full px-3.5 py-2 text-sm font-semibold transition-colors duration-200",
                    activeId === anchor.id
                      ? "bg-accent-lime text-ink-900"
                      : "text-ink-900/70 hover:bg-surface-card hover:text-ink-900",
                  )}
                >
                  {anchor.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="hidden items-center gap-2 lg:flex">
          <Link to="/login">
            <Button variant="secondary">Se connecter</Button>
          </Link>
          <Link to="/integration">
            <Button variant="primary">Intégrer mon laboratoire</Button>
          </Link>
        </div>

        <Button
          variant="secondary"
          size="icon"
          className="lg:hidden"
          aria-label={open ? "Fermer le menu" : "Ouvrir le menu"}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? (
            <X className="size-5" aria-hidden="true" />
          ) : (
            <Menu className="size-5" aria-hidden="true" />
          )}
        </Button>
      </Container>

      {open && (
        <Container className="lg:hidden">
          <nav aria-label="Navigation mobile" className="mt-3 rounded-3xl glass-card p-4">
            <ul className="flex flex-col gap-1">
              {anchors.map((anchor) => (
                <li key={anchor.id}>
                  <a
                    href={`#${anchor.id}`}
                    onClick={() => setOpen(false)}
                    className="block rounded-2xl px-3 py-2.5 text-sm font-semibold text-ink-900 hover:bg-surface-card"
                  >
                    {anchor.label}
                  </a>
                </li>
              ))}
            </ul>
            <div className="mt-3 flex flex-col gap-2">
              <Link to="/login" onClick={() => setOpen(false)}>
                <Button variant="secondary" className="w-full">
                  Se connecter
                </Button>
              </Link>
              <Link to="/integration" onClick={() => setOpen(false)}>
                <Button className="w-full">Intégrer mon laboratoire</Button>
              </Link>
            </div>
          </nav>
        </Container>
      )}
    </header>
  );
}
