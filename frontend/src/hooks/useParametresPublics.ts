import { useQuery } from "@tanstack/react-query";
import { lireParametresPublics, type PolitiqueMotDePasse } from "@/api/parametresPlateforme";
import { listerDocumentsRequis } from "@/api/plateforme";

/** Politique de mot de passe par défaut, appliquée tant que le serveur n'a pas répondu. */
export const POLITIQUE_PAR_DEFAUT: PolitiqueMotDePasse = { longueurMin: 10, exigerCaractereSpecial: false };

/** Paramètres publics de la plateforme (politique de mot de passe, types de laboratoire). */
export function useParametresPublics() {
  return useQuery({
    queryKey: ["parametres-publics"],
    queryFn: lireParametresPublics,
    staleTime: 5 * 60_000,
  });
}

export function usePolitiqueMotDePasse(): PolitiqueMotDePasse {
  return useParametresPublics().data?.politiqueMotDePasse ?? POLITIQUE_PAR_DEFAUT;
}

export function useTypesLaboratoire(): string[] {
  return useParametresPublics().data?.typesLaboratoire ?? [];
}

/** Documents actuellement exigés pour une demande d'intégration. */
export function useDocumentsRequis() {
  return useQuery({
    queryKey: ["documents-requis-integration"],
    queryFn: listerDocumentsRequis,
    staleTime: 5 * 60_000,
  });
}

export interface RegleMotDePasse {
  id: string;
  label: string;
  test: (valeur: string) => boolean;
}

/** Règles affichées à côté des champs « nouveau mot de passe ». */
export function reglesMotDePasse(politique: PolitiqueMotDePasse): RegleMotDePasse[] {
  const regles: RegleMotDePasse[] = [
    {
      id: "len",
      label: `${politique.longueurMin} caractères minimum`,
      test: (v) => v.length >= politique.longueurMin,
    },
    { id: "lower", label: "Une lettre minuscule", test: (v) => /[a-z]/.test(v) },
    { id: "upper", label: "Une lettre majuscule", test: (v) => /[A-Z]/.test(v) },
    { id: "digit", label: "Un chiffre", test: (v) => /[0-9]/.test(v) },
  ];
  if (politique.exigerCaractereSpecial) {
    regles.push({ id: "special", label: "Un caractère spécial", test: (v) => /[^\p{L}\p{N}]/u.test(v) });
  }
  return regles;
}

/** Premier message d'erreur de la politique, ou null si le mot de passe la respecte. */
export function erreurMotDePasse(politique: PolitiqueMotDePasse, valeur: string): string | null {
  const regle = reglesMotDePasse(politique).find((r) => !r.test(valeur));
  return regle ? `Le mot de passe doit contenir : ${regle.label.toLowerCase()}.` : null;
}
