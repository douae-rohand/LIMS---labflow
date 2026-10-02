import { Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { ArrowRight, Check, KeyRound, ShieldCheck } from "lucide-react";
import {
  Accordion,
  Badge,
  Button,
  Card,
  Carousel,
  IconBox,
  Reveal,
  SectionHeader,
  StatCard,
  Stepper,
} from "@/components/lab";
import { Section } from "@/components/layout/Section";
import {
  ai,
  automation,
  cta,
  domains,
  faq,
  features,
  requestStatuses,
  roles,
  security,
  stats,
  validation,
  workflowSteps,
} from "@/data/landing";
import { photos } from "@/data/images";
import {
  fadeUp,
  levitate,
  slideInLeft,
  slideInRight,
  staggerContainer,
} from "@/lib/motion";
import { AiMockup, ValidationMockup } from "./Mockups";
import { Photo } from "./Photo";

const grid = (cls: string) => ({
  className: cls,
  variants: staggerContainer(0.07),
  initial: "hidden",
  whileInView: "show",
  viewport: { once: true, amount: 0.15 },
});

export function Stats() {
  return (
    <Section tone="card" className="py-16 sm:py-16">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map((s) => (
          <StatCard key={s.label} {...s} />
        ))}
      </div>
    </Section>
  );
}

export function Features() {
  return (
    <Section id="fonctionnalites">
      <SectionHeader
        eyebrow="Fonctionnalités"
        title="Tous les modules d'un LIMS moderne"
        description="Chaque module est pensé pour les contraintes réelles d'un laboratoire accrédité."
      />
      <div className="mt-14 grid items-stretch gap-6 lg:grid-cols-[1fr_2fr]">
        <Reveal className="hidden lg:block">
          <Photo
            src={photos.scientistLab.src}
            alt={photos.scientistLab.alt}
            className="h-full min-h-96"
          />
        </Reveal>
        <motion.div {...grid("grid gap-4 sm:grid-cols-2 lg:grid-cols-3")}>
          {features.map((f) => (
            <motion.div key={f.title} variants={fadeUp}>
              <Card variant="flat" className="group hover-lift h-full">
                <IconBox icon={f.icon} className="transition-transform group-hover:scale-110" />
                <h3 className="mt-4 font-bold text-ink-900">{f.title}</h3>
                <p className="mt-1.5 text-sm text-muted-foreground">{f.description}</p>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </Section>
  );
}

export function WorkflowSection() {
  return (
    <Section id="workflow" tone="alt">
      <SectionHeader
        eyebrow="Workflow"
        title="Neuf étapes, de la demande à la clôture"
        description="Un parcours linéaire et contrôlé : chaque étape est horodatée et attribuée."
      />
      <Stepper steps={workflowSteps} className="mt-14" />
      <Reveal className="mt-14">
        <Card variant="glass" className="flex flex-col gap-4">
          <p className="text-sm font-bold text-ink-900">Statuts d'une demande</p>
          <div className="flex flex-wrap gap-2">
            {requestStatuses.map((s, i) => (
              <Badge
                key={s}
                size="sm"
                variant={i < 5 ? (i === 4 ? "lime" : "mint") : "outline"}
              >
                {s}
              </Badge>
            ))}
          </div>
        </Card>
      </Reveal>
    </Section>
  );
}

export function Roles() {
  return (
    <Section id="roles">
      <SectionHeader
        eyebrow="Espaces par rôle"
        title="Six rôles, six espaces dédiés"
        description="Chacun ne voit que ce dont il a besoin, avec les droits strictement nécessaires."
      />
      <Carousel ariaLabel="Rôles utilisateurs" className="mt-12">
        {roles.map((r) => (
          <Card key={r.role} variant="glossy" padding="lg" className="h-full">
            <div className="flex items-center justify-between">
              <IconBox icon={r.icon} variant="brand" size="lg" />
              {r.twoFactor && (
                <Badge size="sm" variant="lime" icon={<KeyRound className="size-3" aria-hidden="true" />}>
                  2FA
                </Badge>
              )}
            </div>
            <h3 className="mt-5 text-xl font-bold text-ink-900">{r.role}</h3>
            <ul className="mt-4 flex flex-col gap-2.5">
              {r.capabilities.map((c) => (
                <li key={c} className="flex gap-2 text-sm text-muted-foreground">
                  <Check className="mt-0.5 size-4 shrink-0 text-brand-600" aria-hidden="true" />
                  {c}
                </li>
              ))}
            </ul>
          </Card>
        ))}
      </Carousel>
    </Section>
  );
}

export function Validation() {
  return (
    <Section tone="mint">
      <div className="grid items-center gap-14 lg:grid-cols-2">
        <Reveal variants={slideInLeft} className="flex flex-col gap-5">
          <Badge variant="lime" dot className="self-start">
            {validation.eyebrow}
          </Badge>
          <h2 className="text-3xl font-extrabold text-ink-900 sm:text-4xl">{validation.title}</h2>
          <p className="text-muted-foreground">{validation.description}</p>
          <ul className="flex flex-col gap-3">
            {validation.points.map((p) => (
              <li key={p} className="flex gap-3 text-sm text-ink-900">
                <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-accent-lime">
                  <Check className="size-3.5" aria-hidden="true" />
                </span>
                {p}
              </li>
            ))}
          </ul>
        </Reveal>
        <Reveal variants={slideInRight} className="relative">
          <Photo
            src={validation.photo.src}
            alt={validation.photo.alt}
            className="aspect-[4/3] w-full"
          />
          <motion.div {...levitate(6, 5)} className="absolute -bottom-10 -right-2 w-[75%] sm:-right-8 sm:w-[60%]">
            <ValidationMockup />
          </motion.div>
          {validation.floating.map((f, i) => (
            <motion.div
              key={f.label}
              {...levitate(5, 4 + i)}
              className={
                i === 0
                  ? "glass-card absolute top-4 -left-3 flex items-center gap-2 rounded-2xl px-3 py-2 text-xs font-semibold text-brand-900 sm:-left-8"
                  : "glass-card absolute top-24 -left-3 hidden items-center gap-2 rounded-2xl px-3 py-2 text-xs font-semibold text-brand-900 sm:-left-12 sm:flex"
              }
            >
              <f.icon className="size-4 text-brand-600" aria-hidden="true" />
              {f.label}
            </motion.div>
          ))}
        </Reveal>
      </div>
    </Section>
  );
}

export function AiSection() {
  return (
    <Section id="ia">
      <div className="grid items-center gap-14 lg:grid-cols-[1fr_1.1fr]">
        <div>
          <SectionHeader align="left" eyebrow={ai.eyebrow} title={ai.title} description={ai.description} />
          <motion.div {...grid("mt-8 flex flex-col gap-4")}>
            {ai.cards.map((c) => (
              <motion.div key={c.title} variants={fadeUp}>
                <Card variant="flat" padding="md" className="hover-lift flex gap-4">
                  <IconBox icon={c.icon} variant="lime" shape="circle" />
                  <div>
                    <h3 className="font-bold text-ink-900">{c.title}</h3>
                    <p className="mt-1 text-sm text-muted-foreground">{c.description}</p>
                  </div>
                </Card>
              </motion.div>
            ))}
          </motion.div>
          <Reveal className="mt-5">
            <p className="flex items-start gap-2 rounded-2xl bg-surface-card p-4 text-sm font-semibold text-brand-900">
              <ShieldCheck className="size-5 shrink-0" aria-hidden="true" />
              {ai.notice}
            </p>
          </Reveal>
        </div>
        <Reveal variants={slideInRight} className="relative">
          <Photo src={photos.cells.src} alt={photos.cells.alt} className="aspect-square w-full" />
          <motion.div {...levitate(7, 6)} className="absolute inset-x-6 bottom-6 sm:inset-x-auto sm:right-6 sm:w-[70%]">
            <AiMockup />
          </motion.div>
        </Reveal>
      </div>
    </Section>
  );
}

export function Automation() {
  return (
    <Section tone="alt">
      <SectionHeader eyebrow={automation.eyebrow} title={automation.title} description={automation.description} />
      <motion.div {...grid("mt-14 grid items-center gap-4 lg:grid-cols-[1fr_auto_1fr_auto_1fr_auto_1fr]")}>
        {automation.chain.map((c) => (
          <motion.div key={c.title} variants={fadeUp} className="contents">
            <Card variant="glossy" className="text-center">
              <IconBox icon={c.icon} variant="brand" className="mx-auto" />
              <h3 className="mt-3 font-bold text-ink-900">{c.title}</h3>
              <p className="text-sm text-muted-foreground">{c.description}</p>
            </Card>
            <ArrowRight className="mx-auto size-5 rotate-90 text-brand-600 lg:rotate-0" aria-hidden="true" />
          </motion.div>
        ))}
        <motion.div variants={fadeUp}>
          <Card variant="dark" className="flex flex-col gap-2">
            <p className="text-sm font-bold">Notifications</p>
            {automation.channels.map((ch) => (
              <span key={ch.label} className="flex items-center gap-2 rounded-xl bg-primary-foreground/10 px-3 py-2 text-sm">
                <ch.icon className="size-4 text-accent-lime" aria-hidden="true" />
                {ch.label}
              </span>
            ))}
          </Card>
        </motion.div>
      </motion.div>
    </Section>
  );
}

export function Security() {
  return (
    <Section id="securite">
      <SectionHeader eyebrow={security.eyebrow} title={security.title} description={security.description} />
      <div className="mt-14 grid gap-6 lg:grid-cols-[1fr_1.6fr]">
        <Reveal>
          <Photo src={security.photo.src} alt={security.photo.alt} className="h-full min-h-72" />
        </Reveal>
        <motion.div {...grid("grid gap-4 sm:grid-cols-2")}>
          {security.cards.map((c) => (
            <motion.div key={c.title} variants={fadeUp}>
              <Card variant="bento" className="hover-lift h-full">
                <IconBox icon={c.icon} variant="mint" />
                <h3 className="mt-3 font-bold text-ink-900">{c.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{c.description}</p>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </Section>
  );
}

export function Domains() {
  return (
    <Section id="domaines" tone="card">
      <SectionHeader eyebrow="Domaines" title="Un LIMS pour chaque type de laboratoire" />
      <motion.div {...grid("mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4")}>
        {domains.map((d) => (
          <motion.article key={d.title} variants={fadeUp} className="group hover-lift overflow-hidden rounded-3xl bg-card shadow-soft">
            <Photo
              src={d.photo.src}
              alt={d.photo.alt}
              className="aspect-[4/3] rounded-b-none border-0 shadow-none"
              imgClassName="transition-transform duration-500 group-hover:scale-105"
            />
            <div className="p-5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-extrabold text-brand-600">{d.number}</span>
                <IconBox icon={d.icon} size="sm" variant="lime" shape="circle" />
              </div>
              <h3 className="mt-2 text-lg font-bold text-ink-900">{d.title}</h3>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {d.examples.map((e) => (
                  <Badge key={e} size="sm" variant="soft">{e}</Badge>
                ))}
              </div>
            </div>
          </motion.article>
        ))}
      </motion.div>
    </Section>
  );
}

export function Faq() {
  return (
    <Section id="faq">
      <div className="grid gap-12 lg:grid-cols-[1fr_1.4fr]">
        <div className="flex flex-col gap-6">
          <SectionHeader align="left" eyebrow="FAQ" title="Questions fréquentes" />
          <Reveal className="hidden lg:block">
            <Photo src={photos.microscopes.src} alt={photos.microscopes.alt} className="aspect-[4/3]" />
          </Reveal>
        </div>
        <Reveal>
          <Accordion items={faq} />
        </Reveal>
      </div>
    </Section>
  );
}

export function FinalCta() {
  return (
    <Section className="pt-0">
      <Reveal>
        <div className="glossy-top relative overflow-hidden rounded-[2rem] bg-brand-900 px-6 py-16 text-center sm:px-12">
          <div className="halo halo-drift -top-20 -left-10 size-72 bg-brand-600/60" />
          <div className="halo halo-drift -right-10 -bottom-24 size-72 bg-accent-lime/30" />
          <div className="relative mx-auto flex max-w-2xl flex-col items-center gap-5">
            <h2 className="text-3xl font-extrabold text-primary-foreground sm:text-4xl">{cta.title}</h2>
            <p className="text-primary-foreground/75">{cta.description}</p>
            <Link to="/integration">
              <Button variant="lime" size="lg" icon={<ArrowRight className="size-4" aria-hidden="true" />}>
                {cta.button}
              </Button>
            </Link>
          </div>
        </div>
      </Reveal>
    </Section>
  );
}
