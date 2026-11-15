import { Mail, MapPin, Phone } from "lucide-react";
import { Container } from "./Container";
import { footerLinks } from "@/data/navigation";
import { useParametresPublics } from "@/hooks/useParametresPublics";

export function Footer() {
  const identite = useParametresPublics().data?.identite;
  const nom = identite?.nom?.trim() || "LabFlow";

  return (
    <footer className="border-t border-brand-900/10 bg-surface-alt py-14">
      <Container>
        <div className="grid gap-10 lg:grid-cols-[1.4fr_repeat(3,1fr)]">
          <div>
            <a href="/" className="inline-block">
              <img src="/nom.png" alt={nom} className="h-10" />
            </a>
            <p className="mt-4 max-w-xs text-sm text-muted-foreground">
              Système de gestion de laboratoire multi-tenant : de la demande client
              au rapport validé, avec traçabilité complète.
            </p>
            {(identite?.emailContact || identite?.telephone || identite?.adresse) && (
              <address className="mt-5 flex flex-col gap-2 text-sm not-italic text-muted-foreground">
                {identite.emailContact && (
                  <a href={`mailto:${identite.emailContact}`} className="inline-flex items-center gap-2 hover:text-brand-900">
                    <Mail className="size-4 shrink-0" aria-hidden="true" /> {identite.emailContact}
                  </a>
                )}
                {identite.telephone && (
                  <a href={`tel:${identite.telephone.replace(/\s+/g, "")}`} className="inline-flex items-center gap-2 hover:text-brand-900">
                    <Phone className="size-4 shrink-0" aria-hidden="true" /> {identite.telephone}
                  </a>
                )}
                {identite.adresse && (
                  <span className="inline-flex items-start gap-2">
                    <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {identite.adresse}
                  </span>
                )}
              </address>
            )}
          </div>
          {footerLinks.map((group) => (
            <div key={group.title}>
              <h3 className="text-sm font-bold text-ink-900">{group.title}</h3>
              <ul className="mt-4 flex flex-col gap-2.5">
                {group.links.map((link) => (
                  <li key={link.label}>
                    <a
                      href={link.href}
                      className="text-sm text-muted-foreground transition-colors duration-200 hover:text-brand-900"
                    >
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-10 flex flex-col gap-2 border-t border-brand-900/10 pt-6 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} {nom} — conformité ISO/IEC 17025 et loi 09-08 (CNDP).</p>
        </div>
      </Container>
    </footer>
  );
}
