import { useForm, useController } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, useRef } from "react";
import { toast } from "sonner";
import {
  UserPlus,
  Mail,
  User,
  Phone,
  Shield,
  ArrowRight,
  ArrowLeft,
  Check,
  AlertCircle,
  Sparkles,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/lab";
import {
  creerEmploye,
  messageErreurApi,
  type RoleEmploye,
  type CreerEmployePayload,
} from "@/api/employes";
import { ApiError } from "@/api/axios";
import { LIBELLE_ROLE } from "./dateUtils";
import {
  schemaCreerEmploye,
  type CreerEmployeFormValues,
} from "./employeSchemas";

interface EmployeCreationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccessCreated: (employeId: number | undefined) => void;
}

const ROLES_CONFIG = [
  {
    value: "RESPONSABLE" as const,
    label: LIBELLE_ROLE["RESPONSABLE"],
    description: "Supervise les analyses, valide les résultats et coordonne l'équipe technique.",
    badge: "Supervision & Validation",
    badgeColor: "bg-brand-900/10 text-brand-900 border-brand-900/20",
  },
  {
    value: "TECHNICIEN" as const,
    label: LIBELLE_ROLE["TECHNICIEN"],
    description: "Prend en charge les échantillons, réalise les tests et saisit les résultats bruts.",
    badge: "Analyses & Paillasse",
    badgeColor: "bg-accent-lime/40 text-ink-900 border-accent-lime",
  },
  {
    value: "ACCUEIL" as const,
    label: LIBELLE_ROLE["ACCUEIL"],
    description: "Accueille les patients, réceptionne les prélèvements et enregistre les dossiers.",
    badge: "Accueil & Enregistrement",
    badgeColor: "bg-mint-200/60 text-brand-900 border-mint-200",
  },
] as const;

export function EmployeCreationModal({
  open,
  onOpenChange,
  onSuccessCreated,
}: EmployeCreationModalProps) {
  const queryClient = useQueryClient();
  const isSubmittingRef = useRef(false);

  // Étape courante : 1 = Identité & Contact, 2 = Rôle & Invitation
  const [etape, setEtape] = useState<1 | 2>(1);

  const form = useForm<CreerEmployeFormValues>({
    resolver: zodResolver(schemaCreerEmploye),
    defaultValues: {
      prenom: "",
      nom: "",
      email: "",
      telephone: "",
      role: "" as unknown as RoleEmploye,
    },
    mode: "onBlur",
  });

  const {
    control,
    handleSubmit,
    trigger,
    setFocus,
    setError,
    reset,
    formState: { errors },
  } = form;

  const prenomField = useController({ name: "prenom", control });
  const nomField = useController({ name: "nom", control });
  const emailField = useController({ name: "email", control });
  const telephoneField = useController({ name: "telephone", control });
  const roleField = useController({ name: "role", control });

  const mutation = useMutation({
    mutationFn: (payload: CreerEmployePayload) => creerEmploye(payload),
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ["employes"] });
      toast.success("Invitation envoyée avec succès.");
      reset();
      setEtape(1);
      onOpenChange(false);
      onSuccessCreated(result?.id);
    },
    onError: (err: unknown) => {
      isSubmittingRef.current = false;
      if (err instanceof ApiError) {
        const status = err.status;
        const msg = String(err.message || "");

        // Conflit email déjà utilisé
        if (
          status === 409 ||
          msg.includes("[EMAIL_DEJA_UTILISE]") ||
          msg.includes("email déjà utilisé")
        ) {
          setEtape(1); // Retour à l'étape 1 pour afficher l'erreur sous le champ email
          setError("email", {
            type: "server",
            message: "Cette adresse email est déjà associée à un compte utilisateur.",
          });
          setTimeout(() => setFocus("email"), 50);
          return;
        }

        const errData = err.data as { erreurs?: Record<string, string> } | undefined;
        if (status === 400 && errData?.erreurs) {
          const champs = errData.erreurs;
          let premierChamp: keyof CreerEmployeFormValues | null = null;
          for (const [champ, message] of Object.entries(champs)) {
            if (champ in form.getValues()) {
              const c = champ as keyof CreerEmployeFormValues;
              setError(c, { type: "server", message });
              if (!premierChamp) premierChamp = c;
            }
          }
          if (premierChamp) {
            if (["prenom", "nom", "email", "telephone"].includes(premierChamp)) {
              setEtape(1);
            } else {
              setEtape(2);
            }
            setTimeout(() => setFocus(premierChamp!), 50);
            return;
          }
        }
      }
      toast.error(messageErreurApi(err));
    },
    onSettled: () => {
      isSubmittingRef.current = false;
    },
  });

  const handleSuivant = async () => {
    // Valider uniquement les champs de l'étape 1
    const etape1Valide = await trigger(["prenom", "nom", "email", "telephone"]);
    if (etape1Valide) {
      setEtape(2);
    } else {
      if (errors.prenom) setFocus("prenom");
      else if (errors.nom) setFocus("nom");
      else if (errors.email) setFocus("email");
      else if (errors.telephone) setFocus("telephone");
    }
  };

  const handlePrecedent = () => {
    if (mutation.isPending) return;
    setEtape(1);
  };

  const onSubmit = (data: CreerEmployeFormValues) => {
    if (isSubmittingRef.current || mutation.isPending) {
      return;
    }
    isSubmittingRef.current = true;

    const payload: CreerEmployePayload = {
      prenom: data.prenom.trim(),
      nom: data.nom.trim(),
      email: data.email.trim().toLowerCase(),
      role: data.role as RoleEmploye,
      ...(data.telephone && data.telephone.trim().length > 0
        ? { telephone: data.telephone.trim() }
        : {}),
    };

    mutation.mutate(payload);
  };

  const handleClose = () => {
    if (mutation.isPending) return;
    reset();
    setEtape(1);
    onOpenChange(false);
  };

  const roleSelectionne = ROLES_CONFIG.find((r) => r.value === roleField.field.value);

  return (
    <Dialog open={open} onOpenChange={(val) => (!val ? handleClose() : onOpenChange(true))}>
      <DialogContent className="fixed top-1/2 left-1/2 z-50 w-[94vw] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-3xl border border-brand-900/10 bg-surface-card p-6 shadow-2xl backdrop-blur-md sm:p-7">
        {/* En-tête avec titre et progression par étapes */}
        <DialogHeader className="space-y-3 text-left">
          <div className="flex items-center gap-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900 shadow-sm">
              <UserPlus className="size-5" aria-hidden="true" />
            </div>
            <div>
              <DialogTitle className="text-xl font-bold tracking-tight text-ink-900">
                Ajouter un collaborateur
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
                Invitation par e-mail avec lien d'activation sécurisé.
              </DialogDescription>
            </div>
          </div>

          {/* Indicateur visuel d'étapes (Stepper moderne et compact) */}
          <div className="flex items-center gap-2 pt-1">
            {/* Étape 1 */}
            <div
              className={`flex flex-1 items-center gap-2 rounded-xl p-2 transition-colors ${
                etape === 1
                  ? "bg-brand-900/10 text-brand-900"
                  : "bg-surface-card text-muted-foreground opacity-75"
              }`}
            >
              <span
                className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                  etape === 2
                    ? "bg-emerald-600 text-white"
                    : etape === 1
                    ? "bg-brand-900 text-white"
                    : "bg-muted-foreground/20 text-muted-foreground"
                }`}
              >
                {etape === 2 ? <Check className="size-3.5" /> : "1"}
              </span>
              <div className="min-w-0">
                <p className="truncate text-xs font-bold">1. Coordonnées</p>
                <p className="hidden text-[10px] text-muted-foreground sm:block">Identité & contact</p>
              </div>
            </div>

            <div className="h-0.5 w-4 bg-brand-900/20" />

            {/* Étape 2 */}
            <div
              className={`flex flex-1 items-center gap-2 rounded-xl p-2 transition-colors ${
                etape === 2
                  ? "bg-brand-900/10 text-brand-900"
                  : "bg-surface-card text-muted-foreground opacity-75"
              }`}
            >
              <span
                className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                  etape === 2
                    ? "bg-brand-900 text-white"
                    : "bg-muted-foreground/20 text-muted-foreground"
                }`}
              >
                2
              </span>
              <div className="min-w-0">
                <p className="truncate text-xs font-bold">2. Fonction</p>
                <p className="hidden text-[10px] text-muted-foreground sm:block">Rôle & attribution</p>
              </div>
            </div>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-4 flex flex-col gap-4">
          {/* ================================================================= */}
          {/* ÉTAPE 1 : Identité & Coordonnées                                   */}
          {/* ================================================================= */}
          {etape === 1 && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {/* Prénom */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="modal-prenom" className="text-xs font-semibold text-ink-900">
                    Prénom <span className="text-rose-500">*</span>
                  </label>
                  <input
                    id="modal-prenom"
                    type="text"
                    placeholder="Ex : Karim"
                    value={prenomField.field.value || ""}
                    onChange={(e) => prenomField.field.onChange(e.target.value)}
                    onBlur={prenomField.field.onBlur}
                    disabled={mutation.isPending}
                    className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
                  />
                  {errors.prenom && (
                    <p role="alert" className="text-xs font-medium text-rose-600">
                      {errors.prenom.message}
                    </p>
                  )}
                </div>

                {/* Nom */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="modal-nom" className="text-xs font-semibold text-ink-900">
                    Nom <span className="text-rose-500">*</span>
                  </label>
                  <input
                    id="modal-nom"
                    type="text"
                    placeholder="Ex : Benali"
                    value={nomField.field.value || ""}
                    onChange={(e) => nomField.field.onChange(e.target.value)}
                    onBlur={nomField.field.onBlur}
                    disabled={mutation.isPending}
                    className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
                  />
                  {errors.nom && (
                    <p role="alert" className="text-xs font-medium text-rose-600">
                      {errors.nom.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Email */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="modal-email" className="text-xs font-semibold text-ink-900">
                  <span className="flex items-center gap-1.5">
                    <Mail className="size-3.5 text-muted-foreground" aria-hidden="true" />
                    Adresse email professionnelle <span className="text-rose-500">*</span>
                  </span>
                </label>
                <input
                  id="modal-email"
                  type="email"
                  placeholder="karim.benali@labo.ma"
                  value={emailField.field.value || ""}
                  onChange={(e) => emailField.field.onChange(e.target.value)}
                  onBlur={emailField.field.onBlur}
                  disabled={mutation.isPending}
                  className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
                />
                {errors.email ? (
                  <p role="alert" className="text-xs font-medium text-rose-600">
                    {errors.email.message}
                  </p>
                ) : (
                  <p className="text-[11px] text-muted-foreground">
                    Le lien d'activation sécurisé sera transmis à cette adresse.
                  </p>
                )}
              </div>

              {/* Téléphone */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="modal-telephone" className="text-xs font-semibold text-ink-900">
                  <span className="flex items-center gap-1.5">
                    <Phone className="size-3.5 text-muted-foreground" aria-hidden="true" />
                    Téléphone portable
                    <span className="text-xs font-normal text-muted-foreground">(optionnel)</span>
                  </span>
                </label>
                <input
                  id="modal-telephone"
                  type="tel"
                  placeholder="0612345678 ou +212612345678"
                  value={telephoneField.field.value || ""}
                  onChange={(e) => telephoneField.field.onChange(e.target.value)}
                  onBlur={telephoneField.field.onBlur}
                  disabled={mutation.isPending}
                  className="rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-muted-foreground/60 focus:border-brand-900 focus:outline-none focus:ring-2 focus:ring-brand-900/20"
                />
                {errors.telephone && (
                  <p role="alert" className="text-xs font-medium text-rose-600">
                    {errors.telephone.message}
                  </p>
                )}
              </div>

              {/* Boutons Étape 1 */}
              <div className="flex items-center justify-end gap-3 border-t border-brand-900/10 pt-4">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleClose}
                  disabled={mutation.isPending}
                >
                  Annuler
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  onClick={handleSuivant}
                  icon={<ArrowRight className="size-4" />}
                  iconPosition="right"
                >
                  Continuer (Rôle)
                </Button>
              </div>
            </div>
          )}

          {/* ================================================================= */}
          {/* ÉTAPE 2 : Choix du Rôle & Validation                               */}
          {/* ================================================================= */}
          {etape === 2 && (
            <div className="space-y-4">
              {/* Récapitulatif rapide de l'employé en haut */}
              <div className="flex items-center justify-between rounded-xl border border-brand-900/10 bg-brand-900/5 px-3.5 py-2 text-xs">
                <div className="flex items-center gap-2 text-ink-900 font-medium">
                  <User className="size-3.5 text-brand-900" />
                  <span>
                    {prenomField.field.value} {nomField.field.value}
                  </span>
                  <span className="text-muted-foreground">({emailField.field.value})</span>
                </div>
                <button
                  type="button"
                  onClick={handlePrecedent}
                  className="text-xs font-semibold text-brand-900 hover:underline"
                >
                  Modifier
                </button>
              </div>

              {/* Sélection du rôle (cartes claires UI/UX friendly) */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-ink-900">
                    Attribuer une fonction au laboratoire <span className="text-rose-500">*</span>
                  </label>
                  {roleSelectionne && (
                    <span className="text-[11px] font-semibold text-brand-900">
                      Sélectionné : {roleSelectionne.label}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-1 gap-2.5">
                  {ROLES_CONFIG.map((r) => {
                    const estSelectionne = roleField.field.value === r.value;
                    return (
                      <button
                        type="button"
                        key={r.value}
                        onClick={() => {
                          roleField.field.onChange(r.value);
                          if (errors.role) {
                            void trigger("role");
                          }
                        }}
                        disabled={mutation.isPending}
                        className={`group relative flex cursor-pointer items-start gap-3 rounded-2xl border-2 p-3 text-left transition-all duration-200 ${
                          estSelectionne
                            ? "border-brand-900 bg-brand-900/5 shadow-sm"
                            : "border-brand-900/10 bg-card hover:border-brand-900/30 hover:bg-surface-card/60"
                        }`}
                      >
                        <div className="mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border border-brand-900/40 bg-card group-hover:border-brand-900">
                          {estSelectionne && (
                            <div className="size-2 rounded-full bg-brand-900" />
                          )}
                        </div>

                        <div className="flex-1 space-y-0.5">
                          <div className="flex flex-wrap items-center justify-between gap-1">
                            <span className="text-sm font-bold text-ink-900">
                              {r.label}
                            </span>
                            <span
                              className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold ${r.badgeColor}`}
                            >
                              {r.badge}
                            </span>
                          </div>
                          <p className="text-xs leading-relaxed text-muted-foreground">
                            {r.description}
                          </p>
                        </div>
                      </button>
                    );
                  })}
                </div>

                {errors.role && (
                  <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
                    {errors.role.message}
                  </p>
                )}
              </div>

              {/* Erreur API globale */}
              {mutation.isError && (
                <div
                  role="alert"
                  className="flex items-center gap-2 rounded-xl border border-rose-500/20 bg-rose-500/10 p-3 text-xs text-rose-900"
                >
                  <AlertCircle className="size-4 shrink-0 text-rose-700" aria-hidden="true" />
                  <span>Une erreur est survenue lors de l'enregistrement de l'employé.</span>
                </div>
              )}

              {/* Boutons Étape 2 */}
              <div className="flex items-center justify-between border-t border-brand-900/10 pt-4">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handlePrecedent}
                  disabled={mutation.isPending}
                  icon={<ArrowLeft className="size-4" />}
                  iconPosition="left"
                >
                  Retour
                </Button>

                <Button
                  type="submit"
                  variant="primary"
                  disabled={mutation.isPending || isSubmittingRef.current}
                  loading={mutation.isPending}
                >
                  Envoyer l'invitation
                </Button>
              </div>
            </div>
          )}
        </form>
      </DialogContent>
    </Dialog>
  );
}
