import { Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import { Badge, Button, Card, IconBox } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { hero } from "@/data/landing";
import { photos } from "@/data/images";
import { useLandingPublic } from "@/hooks/useLandingPublic";
import { fadeUp, levitate, scaleIn, staggerContainer } from "@/lib/motion";
import { DashboardMockup } from "./Mockups";
import { Photo } from "./Photo";

export function Hero() {
  const { data, isPending } = useLandingPublic();
  const nombreLabs = data?.statistiques?.nombreLaboratoiresActifs;
  const badge =
    nombreLabs == null
      ? hero.badge
      : `${nombreLabs} laboratoire${nombreLabs > 1 ? "s" : ""} actif${nombreLabs > 1 ? "s" : ""}`;

  return (
    <section className="relative overflow-hidden pt-10 pb-20 sm:pt-16">
      <div className="halo halo-drift -top-32 -left-24 size-[28rem] bg-mint-200/70" />
      <div className="halo halo-drift top-40 -right-32 size-[26rem] bg-accent-lime/30" />
      <Container className="relative grid items-center gap-12 lg:grid-cols-[1.05fr_1fr]">
        <motion.div
          variants={staggerContainer(0.08)}
          initial="hidden"
          animate="show"
          className="flex flex-col items-start gap-6"
        >
          <motion.div variants={fadeUp}>
            <Badge variant="lime" dot>
              {badge}
            </Badge>
          </motion.div>
          <motion.h1
            variants={fadeUp}
            className="text-4xl leading-[1.05] font-extrabold text-ink-900 sm:text-5xl lg:text-6xl"
          >
            Digitalisez tout le cycle d'analyse de votre{" "}
            <span className="text-brand-600">laboratoire</span>
          </motion.h1>
          <motion.p variants={fadeUp} className="max-w-xl text-lg text-muted-foreground">
            {hero.subtitle}
          </motion.p>
          <motion.div variants={fadeUp} className="flex flex-wrap gap-3">
            <Link to="/integration">
              <Button size="lg" icon={<ArrowRight className="size-4" aria-hidden="true" />}>
                {hero.primaryCta}
              </Button>
            </Link>
            <a href="#workflow">
              <Button size="lg" variant="secondary">
                {hero.secondaryCta}
              </Button>
            </a>
          </motion.div>
          <motion.ul variants={fadeUp} className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-ink-900/80">
            {["Multi-laboratoires", "Double validation", "Audit immuable"].map((t) => (
              <li key={t} className="flex items-center gap-1.5">
                <CheckCircle2 className="size-4 text-brand-600" aria-hidden="true" />
                {t}
              </li>
            ))}
          </motion.ul>
        </motion.div>

        <motion.div variants={scaleIn} initial="hidden" animate="show" className="relative">
          <Photo
            eager
            src={photos.teamLab.src}
            alt={photos.teamLab.alt}
            className="aspect-[4/3.4] w-full"
          />
          <motion.div
            {...levitate(8, 6)}
            className="absolute -bottom-10 -left-4 w-[78%] sm:-left-10 sm:w-[62%]"
          >
            <DashboardMockup
              loading={isPending}
              nombreLaboratoires={data?.statistiques?.nombreLaboratoiresActifs}
              nombreRoles={data?.statistiques?.nombreRoles}
              laboratoires={data?.laboratoires}
              statutsDemande={data?.statutsDemande}
            />
          </motion.div>
        </motion.div>
      </Container>

      <Container className="relative mt-24 lg:mt-20">
        <motion.div
          variants={staggerContainer(0.08)}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true, amount: 0.3 }}
          className="grid gap-4 md:grid-cols-3"
        >
          {hero.bento.map((b, i) => (
            <motion.div key={b.title} variants={fadeUp}>
              <Card
                variant={i === 1 ? "dark" : "glossy"}
                className="hover-lift flex h-full items-start gap-4"
              >
                <IconBox icon={b.icon} variant={i === 1 ? "lime" : "mint"} />
                <div>
                  <h3 className="font-bold">{b.title}</h3>
                  <p className={i === 1 ? "mt-1 text-sm text-primary-foreground/75" : "mt-1 text-sm text-muted-foreground"}>
                    {b.description}
                  </p>
                </div>
              </Card>
            </motion.div>
          ))}
        </motion.div>
      </Container>
    </section>
  );
}
