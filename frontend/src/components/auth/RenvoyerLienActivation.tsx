import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Mail, CheckCircle, AlertCircle, Clock } from "lucide-react";
import { Button, Input, Label } from "@/components/lab";
import { renvoyerActivation } from "@/api/auth";
import { ApiError } from "@/api/axios";

const schema = z.object({
  email: z
    .string()
    .min(1, "L'email est obligatoire.")
    .email("Format d'email invalide."),
});

type FormValues = z.infer<typeof schema>;

interface RenvoyerLienActivationProps {
  emailInitial?: string;
}

export function RenvoyerLienActivation({ emailInitial }: RenvoyerLienActivationProps) {
  const [etat, setEtat] = useState<"succes" | "429" | "erreur" | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState<number>(0);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: emailInitial ?? "",
    },
  });

  useEffect(() => {
    if (emailInitial) {
      setValue("email", emailInitial);
    }
  }, [emailInitial, setValue]);

  // Décompte du cooldown (60 secondes) avec nettoyage au démontage
  useEffect(() => {
    if (cooldown <= 0) return;

    const timer = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [cooldown]);

  const onSubmit = async (values: FormValues) => {
    if (cooldown > 0 || isSubmitting) return;

    setEtat(null);
    setMessage(null);

    try {
      await renvoyerActivation(values.email);
      setEtat("succes");
      setMessage("Si un compte non activé existe pour cette adresse, un nouveau lien d'activation a été envoyé.");
      setCooldown(60);
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setEtat("429");
        setMessage("Trop de tentatives. Réessayez dans quelques instants.");
        setCooldown(60);
      } else {
        setEtat("erreur");
        setMessage("Serveur indisponible. Réessayez.");
      }
    }
  };

  return (
    <div className="mt-6 flex flex-col gap-4 text-left">
      <div className="rounded-2xl border border-input bg-card p-4">
        <h3 className="text-sm font-semibold text-ink-900 flex items-center gap-2">
          <Mail className="size-4 text-brand-900" aria-hidden="true" />
          Renvoyer le lien d'activation
        </h3>
        <p className="mt-1 text-xs text-muted-foreground">
          Saisissez votre e-mail pour recevoir un nouveau lien d'activation.
        </p>

        {/* Alerte message d'état */}
        {etat && message && (
          <div
            role="alert"
            aria-live="polite"
            className={`mt-3 flex items-start gap-2.5 rounded-xl p-3 text-xs font-medium ${
              etat === "succes"
                ? "bg-green-50 text-green-800 border border-green-200"
                : etat === "429"
                ? "bg-amber-50 text-amber-800 border border-amber-200"
                : "bg-red-50 text-red-800 border border-red-200"
            }`}
          >
            {etat === "succes" ? (
              <CheckCircle className="size-4 shrink-0 text-green-600 mt-0.5" aria-hidden="true" />
            ) : etat === "429" ? (
              <Clock className="size-4 shrink-0 text-amber-600 mt-0.5" aria-hidden="true" />
            ) : (
              <AlertCircle className="size-4 shrink-0 text-red-600 mt-0.5" aria-hidden="true" />
            )}
            <span>{message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="mt-3 flex flex-col gap-3" noValidate>
          <div className="flex flex-col gap-1">
            <Label htmlFor="renvoyer-email" className="sr-only">
              Adresse e-mail
            </Label>
            <Input
              id="renvoyer-email"
              type="email"
              placeholder="nom@exemple.ma"
              autoComplete="email"
              aria-describedby={errors.email ? "renvoyer-email-error" : undefined}
              aria-invalid={!!errors.email}
              {...register("email")}
            />
            {errors.email && (
              <p id="renvoyer-email-error" role="alert" aria-live="polite" className="text-xs text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>

          <Button
            type="submit"
            variant="secondary"
            className="w-full"
            disabled={isSubmitting || cooldown > 0}
            loading={isSubmitting}
          >
            {cooldown > 0
              ? `Renvoyer le lien (${cooldown} s)`
              : isSubmitting
              ? "Envoi en cours…"
              : "Renvoyer le lien"}
          </Button>
        </form>
      </div>
    </div>
  );
}
