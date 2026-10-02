# LabFlow LIMS — Frontend

Interface web du système de gestion de laboratoire (LIMS) multi-tenant LabFlow.

Construit avec React 19, TanStack Router, TanStack Query, Tailwind CSS v4 et Vite.  
Le backend est une API REST Spring Boot séparée.

## Prérequis

- Node.js ≥ 20
- npm ≥ 10

## Installation

```sh
npm install
```

## Développement

```sh
npm run dev
```

Le serveur démarre sur `http://localhost:5173`.  
Les requêtes vers `/api` et `/actuator` sont proxifiées vers le backend (par défaut `http://localhost:8081`).  
Pour changer la cible du proxy, créez un fichier `.env.local` :

```env
VITE_BACKEND_URL=http://localhost:8081
```

## Build de production

```sh
npm run build
```

Les fichiers compilés sont générés dans `dist/`.

## Prévisualiser le build

```sh
npm run preview
```

## Lint / Formatage

```sh
npm run lint
npm run format
```

## Composants UI

Les pages et composants métier importent depuis **`@/components/lab`** (design system LabFlow).

`@/components/ui` (généré par shadcn/ui) est réservé aux composants qui n'ont **pas** d'équivalent dans `lab/`. Ne pas créer dans `ui/` un composant déjà présent dans `lab/` (Button, Badge, Card, Accordion, Carousel, Tabs, Tooltip, Modal, Input, Textarea, Select, Checkbox, Avatar, Skeleton, Table, …).

Règle : avant d'ajouter un composant shadcn, vérifier qu'il n'existe pas déjà dans `lab/`.

## Structure du projet

```
src/
  components/
    lab/        Design system LabFlow (Button, Card, Badge, …)
    landing/    Sections de la page d'accueil
    layout/     Navbar, Footer, Container, Section
    ui/         Composants shadcn/ui (Radix UI)
  data/         Données statiques (landing, navigation, images)
  hooks/        Hooks React utilitaires
  layouts/      Layouts de pages (PublicLayout, …)
  lib/          Utilitaires (cn, motion, …)
  routes/       Routes TanStack Router (file-based)
  main.tsx      Point d'entrée React
  router.tsx    Création du routeur
  styles.css    Design tokens et utilitaires Tailwind
```
