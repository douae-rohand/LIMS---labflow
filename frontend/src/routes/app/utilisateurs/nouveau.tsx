import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, UserPlus, Mail, User, Phone, Shield } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { creerUtilisateur } from "@/api/utilisateurs";
import { ApiError } from "@/api/axios";
import { useState } from "react";

export const Route = createFileRoute("/app/utilisateurs/nouveau")({
  head: () => ({
    meta: [{ title: "Créer un utilisateur — LabFlow LIMS" }],
  }),
  component: NouvelUtilisateurPage,
});

// ---------------------------------------------------------------------------
// Schéma de validation
// ---------------------------------------------------------------------------

const schema = z.object({
  prenom: z.string().min(1, "Le prénom est obligatoire.").max(100),
  nom: z.string().min(1, "Le nom est obligatoire.").max(100),
  email: z
    .string()
    .min(1, "L'email est obligatoire.")
    .email("Format d'email invalide.")
    .max(180),
  telephone: z
    .string()
    .regex(/^[+]?[0-9]{8,15}$/, "Numéro de téléphone invalide.")
    .optional()
    .or(z.literal("")),
  role: z.enum(["RESPONSABLE", "TECHNICIEN", "ACCUEIL"], {
    required_error: "Le rôle est obligatoire.",
  }),
});

type FormValues = z.infer<typeof schema>;

// ---------------------------------------------------------------------------
// Configurations des rôles
// ---------------------------------------------------------------------------

const ROLES = [
  {
    value: "RESPONSABLE" as const,
    label: "Responsable laboratoire",
    description: "Valide les résultats et supervise les analyses.",
    color: "bg-brand-900/10 text-brand-900 border-brand-900/20",
  },
  {
    value: "TECHNICIEN" as const,
    label: "Technicien",
    description: "Réalise les essais et saisit les résultats.",
    color: "bg-accent-lime/40 text-ink-900 border-accent-lime",
  },
  {
    value: "ACCUEIL" as const,
    label: "Agent d'accueil",
    description: "Réceptionne les échantillons et les demandes.",
    color: "bg-mint-200/60 text-brand-900 border-mint-200",
  },
] as const;

// ---------------------------------------------------------------------------
// Composant
// ---------------------------------------------------------------------------

function NouvelUtilisateurPage() {
  const navigate = useNavigate();
  const [successEmail, setSuccessEmail] = useState<string | null>(null);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { prenom: "", nom: "", email: "", telephone: "" },
  });

  const selectedRole = form.watch("role");

  const onSubmit = async (values: FormValues) => {
    try {
      await creerUtilisateur({
        prenom: values.prenom,
        nom: values.nom,
        email: values.email,
        ...(values.telephone ? { telephone: values.telephone } : {}),
        role: values.role,
      });
      setSuccessEmail(values.email);
    } catch (e) {
      form.setError("root", {
        message: e instanceof ApiError ? e.message : "Une erreur est survenue.",
      });
    }
  };

  if (successEmail) {
    return (
      <Container className="py-16">
        <Card variant="flat" padding="lg" className="mx-auto max-w-md text-center">
          <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-emerald-100">
            <Mail className="size-8 text-emerald-600" />
          </div>
          <h1 className="text-xl font-extrabold text-ink-900">Invitation envoyée !</h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Un email d'activation a été envoyé à{" "}
            <span className="font-semibold text-ink-900">{successEmail}</span>.
            <br />
            L'utilisateur devra cliquer sur le lien pour définir son mot de passe et activer son
            compte.
          </p>
          <div className="mt-6 flex flex-col gap-2">
            <Button
              onClick={() => {
                setSuccessEmail(null);
                form.reset();
              }}
              icon={<UserPlus className="size-4" />}
              iconPosition="left"
            >
              Créer un autre utilisateur
            </Button>
            <Button
              variant="secondary"
              onClick={() => navigate({ to: "/app/utilisateurs" })}
            >
              Retour à la liste
            </Button>
          </div>
        </Card>
      </Container>
    );
  }

  return (
    <Container className="py-10">
      {/* En-tête */}
      <div className="mb-8">
        <button
          onClick={() => navigate({ to: "/app/utilisateurs" })}
          className="mb-4 flex items-center gap-2 text-sm text-muted-foreground transition hover:text-ink-900"
        >
          <ArrowLeft className="size-4" />
          Retour à la liste
        </button>
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-brand-900/10">
            <UserPlus className="size-5 text-brand-900" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-ink-900">Créer un utilisateur</h1>
            <p className="text-sm text-muted-foreground">
              Un email d'activation sera envoyé automatiquement.
            </p>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-2xl">
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-6">
          {/* Informations personnelles */}
          <Card variant="flat" padding="lg">
            <h2 className="mb-5 flex items-center gap-2 text-base font-bold text-ink-900">
              <User className="size-4 text-brand-900" />
              Informations personnelles
            </h2>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {/* Prénom */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="prenom" className="text-sm font-medium text-ink-900">
                  Prénom <span className="text-red-500">*</span>
                </label>
                <input
                  id="prenom"
                  type="text"
                  placeholder="Marie"
                  className="rounded-2xl border border-input bg-card px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-900/30"
                  {...form.register("prenom")}
                />
                {form.formState.errors.prenom && (
                  <p className="text-xs text-red-600">{form.formState.errors.prenom.message}</p>
                )}
              </div>

              {/* Nom */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="nom" className="text-sm font-medium text-ink-900">
                  Nom <span className="text-red-500">*</span>
                </label>
                <input
                  id="nom"
                  type="text"
                  placeholder="Dupont"
                  className="rounded-2xl border border-input bg-card px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-900/30"
                  {...form.register("nom")}
                />
                {form.formState.errors.nom && (
                  <p className="text-xs text-red-600">{form.formState.errors.nom.message}</p>
                )}
              </div>

              {/* Email */}
              <div className="flex flex-col gap-1.5 sm:col-span-2">
                <label htmlFor="email" className="text-sm font-medium text-ink-900">
                  <span className="flex items-center gap-1.5">
                    <Mail className="size-3.5 text-muted-foreground" />
                    Adresse email <span className="text-red-500">*</span>
                  </span>
                </label>
                <input
                  id="email"
                  type="email"
                  placeholder="marie.dupont@labo.ma"
                  className="rounded-2xl border border-input bg-card px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-900/30"
                  {...form.register("email")}
                />
                {form.formState.errors.email && (
                  <p className="text-xs text-red-600">{form.formState.errors.email.message}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Le lien d'activation sera envoyé à cette adresse.
                </p>
              </div>

              {/* Téléphone */}
              <div className="flex flex-col gap-1.5 sm:col-span-2">
                <label htmlFor="telephone" className="text-sm font-medium text-ink-900">
                  <span className="flex items-center gap-1.5">
                    <Phone className="size-3.5 text-muted-foreground" />
                    Téléphone
                    <span className="text-xs font-normal text-muted-foreground">(optionnel)</span>
                  </span>
                </label>
                <input
                  id="telephone"
                  type="tel"
                  placeholder="+212600000000"
                  className="rounded-2xl border border-input bg-card px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-900/30"
                  {...form.register("telephone")}
                />
                {form.formState.errors.telephone && (
                  <p className="text-xs text-red-600">{form.formState.errors.telephone.message}</p>
                )}
              </div>
            </div>
          </Card>

          {/* Sélection du rôle */}
          <Card variant="flat" padding="lg">
            <h2 className="mb-5 flex items-center gap-2 text-base font-bold text-ink-900">
              <Shield className="size-4 text-brand-900" />
              Rôle <span className="text-red-500">*</span>
            </h2>

            <div className="flex flex-col gap-3">
              {ROLES.map((r) => (
                <label
                  key={r.value}
                  htmlFor={`role-${r.value}`}
                  className={`flex cursor-pointer items-start gap-4 rounded-2xl border-2 p-4 transition-all ${
                    selectedRole === r.value
                      ? `border-brand-900 bg-brand-900/5`
                      : "border-input bg-card hover:border-brand-900/30 hover:bg-surface-card/40"
                  }`}
                >
                  <input
                    id={`role-${r.value}`}
                    type="radio"
                    value={r.value}
                    className="mt-0.5 accent-brand-900"
                    {...form.register("role")}
                  />
                  <div className="flex flex-1">
                    <div>
                      <p className="font-semibold text-ink-900">{r.label}</p>
                      <p className="mt-0.5 text-sm text-muted-foreground">{r.description}</p>
                    </div>
                  </div>
                </label>
              ))}
            </div>

            {form.formState.errors.role && (
              <p className="mt-2 text-xs text-red-600">{form.formState.errors.role.message}</p>
            )}
          </Card>

          {/* Erreur globale */}
          {form.formState.errors.root && (
            <div className="rounded-2xl bg-red-50 px-5 py-4 text-sm font-medium text-red-600">
              {form.formState.errors.root.message}
            </div>
          )}

          {/* Note informative */}
          <div className="flex items-start gap-3 rounded-2xl bg-mint-200/40 px-5 py-4">
            <Mail className="mt-0.5 size-4 shrink-0 text-brand-900" />
            <p className="text-sm text-brand-900">
              <span className="font-semibold">Aucun mot de passe à saisir.</span> Le compte sera
              créé sans mot de passe. L'utilisateur recevra un email avec un lien d'activation
              valable 48 h pour définir son propre mot de passe.
            </p>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate({ to: "/app/utilisateurs" })}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              loading={form.formState.isSubmitting}
              icon={<UserPlus className="size-4" />}
              iconPosition="left"
            >
              Créer et envoyer l'invitation
            </Button>
          </div>
        </form>
      </div>
    </Container>
  );
}
