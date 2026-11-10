# CMD-14 — Lecture exhaustive du frontend LIMS (Espace Technicien)

## Résumé exécutif

Le frontend est une SPA TanStack Router + React 19 + Tailwind v4. Il utilise l'alias `@/*` → `src/*`. `exactOptionalPropertyTypes: true` est activé dans `tsconfig.json`. Le Technicien tombe aujourd'hui dans `AdminShell` (branche `espaceLaboratoire` de `routes/app.tsx`) avec des entrées de navigation qui ne lui correspondent pas. Il n'existe aucune route `/app/essais`, `/app/planning`, `/app/import` ni aucun `TechnicienShell`. Tout est à créer, en suivant les patterns `ClientShell` / `AdminShell` / `EspaceShell`.

---

## A. Champs exacts de session

Fichier : `src/api/session.ts`

```typescript
export interface SessionUser {
  role: RoleUtilisateur;         // 'TECHNICIEN' | 'ADMINISTRATEUR' | ...
  tenantId: string;
  mustChangePassword: boolean;
  email: string;
  nomComplet: string;
  userId: number | null;
  permissions: string[];         // codes effectifs sans préfixe, ex. "PLANIFICATION_STATUT"
}
```

- `RoleUtilisateur` : `'SUPER_ADMINISTRATEUR' | 'ADMINISTRATEUR' | 'RESPONSABLE' | 'TECHNICIEN' | 'ACCUEIL' | 'CLIENT'`
- Fonction utilitaire disponible : `peut(code: string): boolean` → vérifie `permissions.includes(code)`
- Hook React : `useSession()` → `{ session, status }`
- Le refresh (POST `/api/auth/refresh`) renvoie aussi `permissions` et les recharge dans `renouvelerSession()`

**Codes de permission Technicien** (tels qu'exposés dans `session.permissions`, SANS le préfixe `PERM_`) :
- Lecture : `PLANIFICATION_STATUT`
- Saisie : `ESSAIS_GERER`
- Validation technique : `VALIDATION_TECHNIQUE_GERER`
- Import CSV : `ESSAIS_IMPORT_CSV`

---

## B. Pattern apiClient : méthodes disponibles, gestion multipart

Fichier : `src/api/axios.ts`

| Méthode | Signature | Remarque |
|---------|-----------|----------|
| `get<T>` | `(url, config?)` | retourne `T` directement |
| `post<T>` | `(url, body?, config?)` | retourne `T` |
| `put<T>` | `(url, body?, config?)` | retourne `T` |
| `patch<T>` | `(url, body?, config?)` | retourne `T` |
| `delete<T>` | `(url, config?)` | retourne `T` |
| `upload<T>` | `(url, formData, onProgress?, timeout?)` | multipart/form-data — c'est cette méthode pour l'import CSV |
| `getBlob` | `(url, timeout?)` | retourne `Blob` |
| `download` | `(url, nomFichierParDefaut?, timeout?)` | déclenche sauvegarde navigateur |

**Multipart** : `apiClient.upload<T>(url, formData, onProgress?, timeout?)` injecte automatiquement `Content-Type: multipart/form-data`. Pour l'import CSV, le champ s'appelle `fichier` côté backend (`@RequestParam("fichier") MultipartFile fichier`).

**Enveloppe backend** : toutes les réponses sont `ApiResponse<T> { success, message, data, timestamp }`. Le `data` est ce qu'on type en `T`. Les modules existants (ex. `employes.ts`) font `apiClient.get<ApiResponse<PageResponse<...>>>(...).then(r => r.data)`.

**Erreurs** : `ApiError` avec `.status: number`, `.message: string`, `.code: string | undefined`. Le pattern `messageErreurApi()` de `employes.ts` retire le préfixe `[CODE]` avec `.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim()`.

---

## C. Champs réels de chaque DTO backend

### `LigneEssaiTechnicienDto` (liste + retour des mutations)
```
ligneEssaiId: Long
code: String
essai: EssaiRefDto
demandeNumero: String
demandeTitre: String
echantillon: EchantillonRefDto | null    (null si pas encore rattaché)
statut: String                           // "ATTENTE_ECHANTILLON" | "EN_COURS" | "TERMINE" | "NON_REALISABLE"
dateAttribution: Instant
echeanceEstimee: Instant | null
enRetard: boolean
valeur: String | null
niveauConformite: String                 // "CONFORME" | "NON_CONFORME" | "CRITIQUE" | "NON_EVALUE"
```

### `LigneEssaiDetailTechnicienDto` (détail)
```
ligneEssaiId: Long
code: String
essai: EssaiRefDto
demandeNumero: String
demandeTitre: String
echantillon: EchantillonRefDto | null
statut: String
dateAttribution: Instant
dateDebut: Instant | null
dateFin: Instant | null
dateSaisie: Instant | null
echeanceEstimee: Instant | null
enRetard: boolean
valeur: String | null
niveauConformite: String
dureeMinutes: Integer | null
limiteMin: BigDecimal | null
limiteMax: BigDecimal | null
seuilCritiqueMin: BigDecimal | null
seuilCritiqueMax: BigDecimal | null
exigencesEchantillon: ExigenceEchantillonRefDto[]
historiqueValidations: ValidationTechniqueDto[]
```

### `EssaiRefDto`
```
code: String
designation: String
methode: String
unite: String              // unité de mesure (ex. "g/L")
dureeEstimee: Integer | null
domaine: DomaineRefDto
```

### `DomaineRefDto`
```
code: String
libelle: String
```

### `EchantillonRefDto`
```
reference: String
nature: String
conformite: Boolean | null   // true=conforme, false=non conforme, null=non évalué
etatConservationConstate: String
```

### `ExigenceEchantillonRefDto`
```
nature: String
etatConservation: String
quantiteMinimale: BigDecimal | null
unite: String
```

### `ValidationTechniqueDto`
```
id: Long
niveau: Integer              // 1=technique, 2=responsable
decision: String             // "VALIDEE", "REJETEE"
motif: String | null
dateValidation: Instant
```

### `SyntheseTechnicienDto`
```
essaisAujourdhuiCount: long
enCoursCount: long
enRetardCount: long
enAttenteEchantillonCount: long
terminesSemaineCount: long
validationsTechniquesAttenteCount: long
chargeSemaine: ChargeSemaineDto[]
  └─ ChargeSemaineDto { date: LocalDate, count: long }
```

### `PlanningJourDto`
```
date: LocalDate
essais: LigneEssaiTechnicienDto[]
```

### `ImportApercuResponse`
```
lignesLues: int
lignesOk: int
lignesErreur: int
pret: boolean              // true = toutes valides, confirmation autorisée
erreurs: ImportLigneErreurDto[]
apercu: ImportApercuLigneDto[]
```

### `ImportApercuLigneDto`
```
numeroLigne: int
codeLigne: String
valeur: String
commentaire: String | null
niveauConformite: String
```

### `ImportLigneErreurDto`
```
numeroLigne: int
colonne: String              // "code_ligne" | "valeur" | "commentaire"
code: String                 // voir section D
message: String
```

### `ImportConfirmerResponse`
```
lignesImportees: int
erreurs: ImportLigneErreurDto[]
```

### Requests (body entrant)
- `DemarrerRequest` : corps vide (record vide, corps peut être absent)
- `ResultatSaisieRequest` : `{ valeur: String @NotBlank @Size(max=255), commentaire?: String @Size(max=2000) }`
- `NonRealisableRequest` : `{ motif: String @NotBlank @Size(max=1000) }`
- `TempsRequest` : `{ debut?: Instant, fin?: Instant, dureeMinutes?: Integer @Positive }` — deux formes : debut+fin OU dureeMinutes seul
- `ValidationTechniqueRequest` : `{ commentaire?: String @Size(max=2000) }`

---

## D. Codes d'erreur réels de TechnicienController + TechnicienService

### Erreurs métier (422) — TechnicienService
| Code | Action | Message à afficher |
|------|--------|--------------------|
| `ESSAI_SANS_ECHANTILLON` | demarrer | Aucun échantillon n'est encore rattaché à cet essai |
| `ECHANTILLON_NON_CONFORME` | demarrer | L'échantillon est non conforme, l'essai ne peut pas démarrer |
| `ESSAI_STATUT_INVALIDE` | demarrer, saisirResultat, declareNonRealisable, validerTechniquement | Statut de l'essai incompatible avec cette action |
| `ESSAI_NON_DEMARRE` | saisirResultat | L'essai doit être EN_COURS pour saisir un résultat |
| `RESULTAT_DEJA_VALIDE_RESPONSABLE` | saisirResultat | Ce résultat a déjà été validé par le Responsable, la modification est interdite |
| `MOTIF_MODIFICATION_REQUIS` | saisirResultat | Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi |
| `ESSAI_NON_EN_COURS` | validerTechniquement | L'essai doit être EN_COURS pour être validé techniquement |
| `RESULTAT_ABSENT` | validerTechniquement | Un résultat doit être saisi avant la validation technique |
| `TEMPS_FIN_AVANT_DEBUT` | declarerTemps | La date de fin doit être postérieure à la date de début |
| `TEMPS_FORMAT_INVALIDE` | declarerTemps | Fournissez soit une durée en minutes, soit une plage début/fin |
| `DUREE_MAX_DEPASSEE` | declarerTemps | La durée déclarée dépasse le plafond de 480 minutes |
| `PLANNING_DATES_REQUISES` | planning | Les dates début et fin sont obligatoires |
| `PLANNING_FIN_AVANT_DEBUT` | planning | La date de fin doit être postérieure à la date de début |
| `PLANNING_PLAGE_TROP_GRANDE` | planning | La plage de dates ne peut pas dépasser 62 jours |

### Erreurs métier (422) — TechnicienImportService (fichier-niveau)
| Code | Message à afficher |
|------|--------------------|
| `IMPORT_FICHIER_VIDE` | Le fichier CSV est obligatoire et ne peut pas être vide |
| `IMPORT_FICHIER_TROP_GRAND` | Le fichier dépasse la taille maximale (1 Mo par défaut) |
| `IMPORT_FORMAT_INVALIDE` | Le fichier doit être un CSV (.csv) |
| `IMPORT_ENCODAGE_INVALIDE` | Le fichier doit être encodé en UTF-8 |
| `IMPORT_LECTURE_IMPOSSIBLE` | Impossible de lire le fichier CSV |
| `IMPORT_TROP_DE_LIGNES` | Le fichier contient trop de lignes (max 500) |
| `IMPORT_CONFLICT` | Conflit de concurrent lors de l'import, réessayez |

### Erreurs par ligne (dans `ImportLigneErreurDto.code`)
| Code | Colonne concernée |
|------|------------------|
| `IMPORT_LIGNE_MAL_FORMEE` | — |
| `IMPORT_FORMULE_INTERDITE` | valeur |
| `IMPORT_CODE_VIDE` | code_ligne |
| `IMPORT_VALEUR_VIDE` | valeur |
| `IMPORT_LIGNE_INTROUVABLE` | code_ligne |
| `IMPORT_ESSAI_NON_EN_COURS` | code_ligne |
| `IMPORT_DEJA_VALIDE_RESPONSABLE` | code_ligne |
| `IMPORT_MOTIF_REQUIS` | commentaire |

### Erreurs HTTP standard
- `404` : essai introuvable ou n'appartient pas au technicien courant
- `403` : permission manquante (le frontend doit masquer l'action et afficher un message clair)
- `400` : validation Bean Validation échouée (message du backend)

---

## E. Pattern exact de route (validateSearch, head, beforeLoad, loader)

Fichier de référence : `src/routes/app/employes/index.tsx`

### Pattern layout parent (`app/employes.tsx`)
```typescript
export const Route = createFileRoute("/app/employes")({
  beforeLoad: () => {
    exigerPermission("EMPLOYES_CONSULTER");   // ou exigerRole("TECHNICIEN")
  },
  component: EmployesLayout,
});
function EmployesLayout() { return <Outlet />; }
```

### Pattern page (`app/employes/index.tsx`)
```typescript
export const Route = createFileRoute("/app/employes/")({
  head: () => ({ meta: [{ title: "Titre — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (!session.user.permissions.includes("MA_PERMISSION")) throw redirect({ to: "/app" });
  },
  validateSearch: (search: Record<string, unknown>): MonSearch => {
    // valeurs invalides ignorées → valeurs par défaut
    const page = parsePositiveInt(search["page"], 1, 10_000) ?? 1;
    const q = typeof search["q"] === "string" ? search["q"] : undefined;
    return { page, q };
  },
  component: MaPage,
});
```

### Garde utilitaire disponible
`exigerPermission(code: string)` dans `src/features/shell/garde.ts` — redirige vers `/app` si `peut(code)` est faux.

### Pattern de redirection login
`sanitizeRedirect` dans `src/lib/redirect.ts` — valide les URLs relatives internes.

### Règle fichiers : TanStack Router file-based
- `app/essais.tsx` → layout parent (`/app/essais`)
- `app/essais/index.tsx` → `/app/essais`
- `app/essais/$ligneEssaiId.tsx` → `/app/essais/:ligneEssaiId`
- `app/planning.tsx` → `/app/planning`
- `app/import.tsx` → `/app/import`
- `routeTree.gen.ts` auto-généré, ne pas éditer

---

## F. EspaceShell : props exactes, type NavItem

Fichier : `src/features/shell/EspaceShell.tsx`

### Type `CheminEspace` (union littérale — À ÉTENDRE)
```typescript
export type CheminEspace =
  | "/app" | "/app/tableau-de-bord" | "/app/demandes" | ...
  | "/app/stock" | "/app/satisfaction" | "/app/audit" | "/app/exports" | "/app/parametres" | "/app/profil";
  // Manquant : "/app/essais" | "/app/planning" | "/app/import"
  // → Il faut ajouter ces 3 chemins à CheminEspace pour que TechnicienShell compile
```

### Type `EntreeNavigation`
```typescript
export type EntreeNavigation = {
  to: CheminEspace;
  label: string;
  icon: LucideIcon;
  exact?: boolean | undefined;
  badge?: number | undefined;
};
```

### Props de `EspaceShell`
```typescript
{
  children: ReactNode;
  utilisateur: SessionUser;
  libelleRole: string;
  accueil: CheminEspace;
  titreSection: string;
  navigation: EntreeNavigation[];
  onLogout: () => void;
}
```

### Pattern shell (calqué sur `ClientShell.tsx`)
```typescript
export function TechnicienShell({ children, utilisateur, onLogout }) {
  const syntheseQuery = useTechnicienSynthese();
  const enRetardCount = syntheseQuery.data?.enRetardCount ?? 0;
  const navigation: EntreeNavigation[] = [
    { to: "/app", label: "Tableau de bord", icon: LayoutDashboard, exact: true },
    { to: "/app/essais", label: "Mes essais", icon: FlaskConical, badge: enRetardCount },
    { to: "/app/planning", label: "Planning", icon: CalendarDays },
    { to: "/app/import", label: "Import CSV", icon: Upload },
  ];
  return (
    <EspaceShell utilisateur={utilisateur} libelleRole="Technicien"
      accueil="/app" titreSection="Mon espace" navigation={navigation} onLogout={onLogout}>
      {children}
    </EspaceShell>
  );
}
```

---

## G. Composants `lab/` disponibles (noms exacts et usage)

Tous exportés depuis `src/components/lab/index.ts` :

| Export | Fichier | Usage / Variantes |
|--------|---------|-------------------|
| `Button` | `Button.tsx` | `variant`: primary, secondary, ghost, lime, outlineLight ; `size`: sm, md, lg, icon ; props `loading`, `icon`, `iconPosition` |
| `Badge` | `Badge.tsx` | `variant`: lime, mint, soft, dark, outline ; `size`: sm, md ; props `dot`, `icon` |
| `Card` | `Card.tsx` | `variant`: flat, surface, glass, glossy, bento, dark, lime ; `padding`: none, sm, md, lg |
| `IconBox` | `IconBox.tsx` | `variant`: brand, mint, lime, soft, glass ; `shape`: rounded, circle ; `size`: sm, md, lg |
| `Reveal` | `Reveal.tsx` | animation d'apparition |
| `StatCard` | `StatCard.tsx` | props: `value`, `suffix`, `label`, `icon`, `className` — compteur animé avec framer-motion |
| `SectionHeader` | `SectionHeader.tsx` | en-tête de section avec titre/description |
| `Accordion` | `Accordion.tsx` | accordéon basé Radix |
| `Stepper` | `Stepper.tsx` | frise d'étapes en snake-layout ; `Step = { title, description, icon }` — **conçu pour landing**, peu adapté pour une frise d'essai inline |
| `Carousel` | `Carousel.tsx` | carousel horizontal |
| `Input` | `Form.tsx` | champ input stylisé (`field` CSS) |
| `Textarea` | `Form.tsx` | textarea stylisé |
| `Select` | `Form.tsx` | select natif stylisé avec flèche |
| `Checkbox` | `Form.tsx` | checkbox Radix avec label |
| `Label` | `Form.tsx` | label `<label>` stylisé |
| `Modal` | `Overlays.tsx` | dialogue Radix Dialog ; props `trigger`, `title`, `children` |
| `Tooltip` | `Overlays.tsx` | tooltip Radix ; props `children`, `content: string` |
| `Tabs` | `Overlays.tsx` | onglets Radix ; `items: { value, label, content }[]` |
| `Avatar` | `Data.tsx` | avatar cercle avec initiales ou image |
| `Skeleton` | `Data.tsx` | placeholder animé (`animate-pulse`) |
| `Table` | `Data.tsx` | tableau simple avec colonnes/lignes — peu flexible, préférer un tableau natif pour les essais |

**Note pour la frise d'essai** : Le `Stepper` existant est une frise de landing page (snake multi-colonnes, framer-motion). Pour la frise Démarré→Résultat→Validé dans le détail d'essai, il faut créer une mini-frise inline dédiée dans `components/technicien/`.

---

## H. Composants `ui/` disponibles (Radix / shadcn)

Tous dans `src/components/ui/` :

`alert-dialog.tsx`, `alert.tsx`, `aspect-ratio.tsx`, `breadcrumb.tsx`, `button.tsx`, `calendar.tsx`, `chart.tsx`, `collapsible.tsx`, `command.tsx`, `context-menu.tsx`, `dialog.tsx`, `drawer.tsx`, `dropdown-menu.tsx`, `form.tsx`, `hover-card.tsx`, `input-otp.tsx`, `input.tsx`, `label.tsx`, `menubar.tsx`, `navigation-menu.tsx`, `pagination.tsx`, `popover.tsx`, `progress.tsx`, `radio-group.tsx`, `resizable.tsx`, `scroll-area.tsx`, `select.tsx`, `separator.tsx`, `sheet.tsx`, `sidebar.tsx`, `skeleton.tsx`, `slider.tsx`, `sonner.tsx`, `switch.tsx`, `toggle-group.tsx`, `toggle.tsx`, `tooltip.tsx`

**Composants ui/ clés pour CMD-14** :
- `alert-dialog.tsx` → confirmation avant validation technique (Radix AlertDialog)
- `dialog.tsx` → dialogue « Non réalisable » avec motif
- `skeleton.tsx` → squelettes de chargement
- `alert.tsx` → alerte échantillon manquant/non conforme
- `progress.tsx` → barre de progression (si besoin)
- `separator.tsx` → séparateurs visuels

---

## I. Pattern hook TanStack Query existant (clés, options, staleTime)

Fichier de référence : `src/hooks/useDemandesClient.ts` et `src/hooks/useStockAdmin.ts`

### Pattern query (lecture)
```typescript
export function useDemandesClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: ["demande-client", "liste"] as const,
    queryFn: listerDemandesClient,
    enabled: role === "CLIENT",       // enabled conditionnel sur le rôle
    staleTime: 15_000,
    refetchOnWindowFocus: false,      // exigé par CMD-14 (30s staleTime)
    placeholderData: (prev) => prev,  // pour les listes paginées
  });
}
```

### Pattern mutation avec invalidation
```typescript
return useMutation({
  mutationFn: (vars) => monApi(vars),
  onSuccess: (detail, variables) => {
    queryClient.setQueryData(maClé(variables.id), detail);   // mise à jour cache directe
    void queryClient.invalidateQueries({ queryKey: ["technicien", "essais"] });
    void queryClient.invalidateQueries({ queryKey: ["technicien", "synthese"] });
    toast.success("Action réussie.");
  },
  onError: (error) => {
    toast.error(messageErreurTechnicien(error));
  },
});
```

### Clés à utiliser pour CMD-14 (exigées par le brief)
```typescript
["technicien", "essais", filtres]          // liste paginée
["technicien", "essai", id]                // détail
["technicien", "planning", plage]          // { debut, fin }
["technicien", "synthese"]                 // synthèse/tableau de bord
```

### Options à respecter (brief CMD-14)
```typescript
staleTime: 30_000
refetchOnWindowFocus: false
placeholderData: (prev) => prev   // pour les listes
enabled: getSession()?.user.role === "TECHNICIEN"
```

---

## J. Pattern libellés (libelles.ts)

Fichier : `src/components/demandes/client/libelles.ts`

### Libellés de statut d'essai existants (déjà définis !)
```typescript
export const LIBELLE_STATUT_ESSAI: Record<string, string> = {
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  EN_COURS: "En cours",
  TERMINE: "Terminé",
  NON_REALISABLE: "Non réalisable",
};
export function libelleStatutEssai(statut: string | null | undefined): string { ... }
export function libelleConformite(conformite: boolean | null | undefined): string { ... }
export function formatDate(valeur: string | null | undefined): string { ... }
```

**Pour CMD-14**, créer `src/components/technicien/libelles.ts` avec :
```typescript
// Libellés de niveauConformite
export const LIBELLE_CONFORMITE_CALCUL: Record<string, string> = {
  CONFORME: "Conforme",
  NON_CONFORME: "Non conforme",
  CRITIQUE: "Critique",
  NON_EVALUE: "Non évalué",
};
// Couleurs de conformité (classes Tailwind)
export const COULEUR_CONFORMITE: Record<string, string> = {
  CONFORME: "text-green-700 bg-green-50",
  NON_CONFORME: "text-amber-700 bg-amber-50",
  CRITIQUE: "text-red-700 bg-red-50",
  NON_EVALUE: "text-muted-foreground bg-surface-card",
};
```

---

## K. Composants admin existants réutilisables

Dossier `src/components/admin/` — fichiers directs :
- `TableauDeBordAdmin.tsx`
- `TableauDeBordActivite.tsx`
- `TableauDeBordAlertes.tsx`
- `TableauDeBordGraphiques.tsx`
- `TableauDeBordKpi.tsx`

Ces composants sont spécifiques à l'admin (compteurs de demandes, factures, stock). Ils ne sont **pas** réutilisables directement pour le Technicien. En revanche :

- `src/components/admin/employes/EmployesPageContent.tsx` → modèle de page-contenu avec useQuery + filtres + pagination + debounce
- `src/components/employes/EmployesFiltres.tsx`, `EmployesTable.tsx`, `EmployesPagination.tsx` → sous-composants à imiter pour la liste d'essais

---

## L. Route actuelle du Technicien dans routes/app.tsx

Fichier : `src/routes/app.tsx`

```typescript
const espaceLaboratoire = user?.role === "ADMINISTRATEUR"
  || user?.role === "RESPONSABLE"
  || user?.role === "TECHNICIEN"   // ← le Technicien tombe ici
  || user?.role === "ACCUEIL";

if (espaceLaboratoire) {
  return (
    <AdminShell utilisateur={user} onLogout={...}>   // ← AdminShell pour tous !
      <Outlet />
    </AdminShell>
  );
}
```

**État actuel** : le Technicien est rendu dans `AdminShell`. La navigation affichée est celle de `NAV_ITEMS_RESPONSABLE` filtrée par permissions (Employés, Demandes clients, Catalogue, Exports). Aucune route Technicien dédiée n'existe.

**Route `/app` (index)** (`src/routes/app/index.tsx`) : les rôles `SUPER_ADMINISTRATEUR`, `ADMINISTRATEUR`, `RESPONSABLE` ont des redirects dédiés, mais `TECHNICIEN` et `ACCUEIL` tombent dans le composant `AppHomePage` qui affiche une carte minimaliste avec nom/email/badge rôle. Pas de tableau de bord Technicien.

**Modification requise dans `app.tsx`** : extraire le Technicien de la branche `espaceLaboratoire` et ajouter une branche `TechnicienShell` dédiée, ou mieux, passer à un tableau de dispatch par rôle pour éviter les branches if/if.

---

## M. tsconfig paths (alias @)

Fichier : `tsconfig.json`
```json
"paths": { "@/*": ["./src/*"] }
```
Exemple d'usage : `import { Button } from "@/components/lab"` → `src/components/lab/index.ts`.

**Options strictes activées** :
- `exactOptionalPropertyTypes: true` → un champ `foo?: string` n'accepte pas `foo: undefined` explicite ; il faut omettre le champ ou utiliser `foo?: string | undefined`
- `noUncheckedIndexedAccess: true` → tout accès tableau/record retourne `T | undefined`
- `strict: true`, `noImplicitReturns: true`, `noFallthroughCasesInSwitch: true`

---

## N. Dépendances installées pertinentes

Fichier : `package.json`

| Dépendance | Version | Usage CMD-14 |
|-----------|---------|--------------|
| `@tanstack/react-query` | ^5.101.1 | hooks de données |
| `@tanstack/react-router` | 1.170.18 | routing file-based |
| `date-fns` | ^4.1.0 | formatage des dates (Instant ISO → affichage fr) |
| `zod` | ^3.25.76 | validation des formulaires (ResultatSaisieRequest, etc.) |
| `react-hook-form` | ^7.71.2 | formulaires (saisie résultat, temps, motif) |
| `@hookform/resolvers` | ^5.2.2 | intégration zod + react-hook-form |
| `sonner` | ^2.0.7 | toasts (déjà utilisé partout, `toast.success()` / `toast.error()`) |
| `lucide-react` | ^0.575.0 | icônes (FlaskConical, CalendarDays, Upload, Clock, CheckCircle, etc.) |
| `@radix-ui/react-alert-dialog` | ^1.1.15 | confirmation validation technique |
| `@radix-ui/react-dialog` | ^1.1.15 | dialogue motif non réalisable |
| `framer-motion` | ^13.4.4 | déjà utilisé dans StatCard/Stepper (garder les usages existants) |
| `recharts` | ^2.15.4 | graphiques (barres pour charge semaine du tableau de bord) |
| `class-variance-authority` | ^0.7.1 | CVA pour les variants de composants |
| `tailwindcss` | ^4.2.1 | Tailwind v4 (pas de `tailwind.config.js`, config dans CSS) |

---

## Conclusions et recommandations pour le plan d'implémentation

### Problèmes à résoudre obligatoirement

1. **`CheminEspace` à étendre** : `EspaceShell.tsx` a une union littérale qui n'inclut pas `/app/essais`, `/app/planning`, `/app/import`. Il faut les ajouter **avant** de créer `TechnicienShell`.

2. **`app.tsx` à modifier** : extraire le Technicien de la branche `espaceLaboratoire` pour le router vers `TechnicienShell`. Attention : `RESPONSABLE` et `ACCUEIL` doivent rester dans `AdminShell`. Seulement `TECHNICIEN` est séparé.

3. **`app/index.tsx` à modifier** : ajouter un redirect `/app` → `/app` (sans redirect, laisser le tableau de bord Technicien s'afficher) OU implémenter le tableau de bord directement dans `/app/index.tsx` conditionnel sur le rôle `TECHNICIEN`.

4. **`PageResponse<T>` à importer** : type déjà défini dans `src/api/utilisateurs.ts` → `{ content: T[], totalElements: number, totalPages: number, size: number, number: number }`. Réutiliser.

5. **`ApiResponse<T>`** : défini dans `src/api/auth.ts` → `{ success: boolean, message: string | null, data: T, timestamp: string }`.

### Découpage en sous-lots recommandé

**Lot 1** — Socle API + hooks (sans composants UI)
- `src/api/technicien/types.ts`
- `src/api/technicien/index.ts` (toutes les fonctions + `messageErreurTechnicien`)
- `src/hooks/useTechnicienSynthese.ts`, `useTechnicienEssais.ts`, `useTechnicienEssai.ts`, `useTechnicienPlanning.ts`
- Vérification : `npx tsc --noEmit`

**Lot 2** — Shell + routes (squelettes sans logique)
- Modifier `EspaceShell.tsx` (CheminEspace)
- `src/features/technicien/TechnicienShell.tsx`
- Modifier `src/routes/app.tsx` (branching rôle TECHNICIEN)
- `src/routes/app/essais.tsx` (layout)
- `src/routes/app/essais/index.tsx`, `$ligneEssaiId.tsx`, planning.tsx, import.tsx
- Modifier `src/routes/app/index.tsx` (pas de redirect pour TECHNICIEN)
- Vérification : `npm run build` puis `npx tsc --noEmit`

**Lot 3** — Tableau de bord
- `src/components/technicien/dashboard/` (cartes, liste prochains essais, charge semaine)

**Lot 4** — Liste des essais
- `src/components/technicien/essais/` (liste, filtres, pagination)

**Lot 5** — Détail + saisie
- `src/components/technicien/detail/` (en-tête, échantillon, frise, saisie, temps, validation)

**Lot 6** — Import CSV
- `src/components/technicien/import/`

### Points de vigilance

- `exactOptionalPropertyTypes: true` → ne pas passer `prop={undefined}` explicitement sur les champs optionnels des DTOs
- `noUncheckedIndexedAccess` → tous les accès `tableau[i]` retournent `T | undefined`, typer en conséquence
- `Instant` backend → `string` ISO-8601 en TypeScript (pas `Date`), utiliser `date-fns` pour l'affichage
- `LocalDate` backend (`PlanningJourDto.date`, `ChargeSemaineDto.date`) → `string` format `"YYYY-MM-DD"`
- `BigDecimal` backend → `number | null` en TypeScript (pas `string`)
- `Long` backend → `number` en TypeScript (attention aux grands entiers > 2^53, mais les IDs restent dans la plage sûre)
- Le badge « Mes essais » = `enRetardCount` de la synthèse, masqué si 0 (déjà supporté par `EntreeNavigation.badge`)
- Toute mutation sur un essai invalide : `["technicien", "essai", id]`, `["technicien", "essais"]`, `["technicien", "synthese"]`, `["technicien", "planning"]`
- Import confirmé invalide en plus : tout ce qui précède
- Anti-double-clic : désactiver le bouton pendant la requête avec `isPending` de `useMutation`
- POST `/demarrer` n'a pas de corps (record vide), envoyer `{}` ou corps vide
