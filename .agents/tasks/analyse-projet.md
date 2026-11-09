# Rapport d'analyse du projet LIMS

> **Date** : Analyse effectuée en lecture seule — aucune modification apportée au code.  
> **Périmètre** : `c:\Projet_GI3\LIMS\` (backend + frontend)

---

## Résumé exécutif

Le projet LIMS est une **plateforme SaaS multi-tenant** destinée à la gestion complète du cycle d'analyse d'un laboratoire scientifique (médical, industriel, environnemental, pharmaceutique). Il couvre toute la chaîne de valeur : réception des demandes clients, réalisation et validation des essais, génération de rapports PDF, facturation conforme aux normes marocaines (ICE/TVA), et suivi de la satisfaction. Le backend est un **monolithe modulaire Spring Boot 4 / Java 21** ; le frontend est une **SPA React 19 / TypeScript** avec routage file-based via TanStack Router. L'isolation des données entre laboratoires est assurée par un **schéma MySQL dédié par tenant**, provisionné à la volée par Flyway. Cinq intégrations externes sont présentes : LLM (Spring AI / Ollama-compatible), stockage fichiers (MinIO / disque local), automatisation de workflows (n8n via webhooks), e-mails transactionnels (SendGrid), et notifications push navigateur (Web Push — stub). Le niveau de maturité global est **élevé** pour un projet académique : patterns d'entreprise bien appliqués, sécurité JWT + 2FA, RBAC granulaire avec délégation de permissions. Plusieurs intégrations (WebPush, RAG IA, génération PDF avancée) restent des stubs commentés.

---

## 1. Vue d'ensemble du projet

| Critère | Valeur |
|---|---|
| Nom | LIMS — Système de Gestion de Laboratoire (SGL) |
| Objectif | Digitaliser le cycle complet d'analyse : demande → essai → rapport → facturation |
| Domaines cibles | Médical, industriel, environnemental, pharmaceutique/cosmétique |
| Modèle d'hébergement | Multi-tenant SaaS (un schéma MySQL par laboratoire) |
| Conformité locale | Facturation conforme normes marocaines (champ ICE, TVA) |
| Langue du code | Français (noms de méthodes, DTOs, messages d'erreur) |

### Stack technique

| Couche | Technologie | Version |
|---|---|---|
| Backend framework | Spring Boot | 4.1.1 |
| Langage backend | Java | 21 |
| ORM | Spring Data JPA / Hibernate | inclus Spring Boot |
| Sécurité | Spring Security, JWT (jjwt 0.12.6), TOTP 2FA (googleauth 1.5.0) | — |
| Base de données | MySQL 8 (dialecte MariaDB dans config) | — |
| Migrations | Flyway + flyway-mysql | inclus Spring Boot |
| Mapping objet | MapStruct 1.6.3 + Lombok | — |
| Documentation API | SpringDoc OpenAPI / Swagger UI 2.8.9 | — |
| Export PDF | OpenPDF 2.0.3 (ex LibrePDF) | — |
| Export Excel | Apache POI 5.4.1 | — |
| IA | Spring AI 2.0.1 (BOM), starter OpenAI-compatible | — |
| Frontend framework | React 19 + TypeScript | — |
| Bundler | Vite 6.3.5 | — |
| Routage frontend | TanStack Router 1.170.18 (file-based) | — |
| Requêtes HTTP frontend | TanStack Query 5.101.1 + Axios 1.7.9 | — |
| UI components | Radix UI + shadcn/ui + Tailwind CSS v4 | — |
| Graphiques | Recharts 2.15.4 | — |
| Cartes | Leaflet 1.9.4 + react-leaflet | — |
| Animations | Framer Motion 13 | — |
| Formulaires | react-hook-form 7 + Zod 3 | — |
| Notifications toast | Sonner 2 | — |

---

## 2. Architecture backend

### 2.1 Organisation des packages

```
com.backend/
├── BackendApplication.java          Point d'entrée Spring Boot
├── config/                          Beans de configuration transverse
│   ├── AsyncConfig.java             Pool de threads asynchrones (core=4, max=16)
│   ├── CorsConfig.java              CORS configurable par env (CORS_ALLOWED_ORIGINS)
│   ├── JacksonConfig.java           Sérialisation JSON personnalisée
│   ├── JwtConfig.java               Propriétés JWT (secret, expirations)
│   ├── MultiTenantConfig.java       DataSource routant par tenant (ThreadLocal)
│   ├── OpenApiConfig.java           Configuration Swagger/OpenAPI
│   ├── OriginValidationFilter.java  Vérification defense-in-depth de l'Origin
│   ├── RestClientConfig.java        Clients RestClient (SendGrid, n8n, LLM)
│   ├── SecurityConfig.java          Chaîne de filtres Spring Security
│   ├── SuperAdminInitializer.java   Création du super-admin au démarrage
│   ├── TotpConfig.java              Configuration Google Authenticator
│   ├── WebMvcConfig.java            Intercepteurs (TenantInterceptor)
│   └── WebSocketConfig.java         STOMP over SockJS
├── common/                          Utilitaires partagés entre modules
│   ├── audit/                       Journal d'audit (JournalAudit, service, repo)
│   ├── config/SchemaConstants.java  Constante "lims_central"
│   ├── dto/                         ApiResponse<T>, PageResponse<T>
│   ├── exception/                   GlobalExceptionHandler, BusinessRuleException, etc.
│   ├── security/                    SecurityUtils, ProprieteClient, handlers 401/403
│   ├── tenant/                      TenantContext (ThreadLocal), TenantProvisioner,
│   │                                TenantDataSourceRegistry, TenantInterceptor
│   └── util/                        DateUtils, ApresCommit (post-commit callback)
├── export/                          Export Excel/PDF (controllers + services)
├── integration/                     Connecteurs externes
│   ├── llm/LlmClientService.java    Appels LLM via Spring AI ChatClient
│   ├── minio/StockageFichierService  Stockage fichiers (disque local, MinIO prévu)
│   ├── n8n/NotificationEventPublisher  Publication d'événements vers n8n
│   ├── sendgrid/                    Envoi e-mails via API v3 SendGrid
│   └── webpush/WebPushService.java  Notifications push navigateur (stub VAPID)
└── modules/                         Un sous-package par domaine métier (voir §3)
```

### 2.2 Patterns architecturaux

| Pattern | Description | Exemple |
|---|---|---|
| Layered (Controller → Service → Repository) | Respecté dans chaque module | `DemandeController` → `DemandeService` → `DemandeRepository` |
| DTO / MapStruct | DTOs distincts des entités JPA ; MapStruct génère les mappeurs | `UtilisateurMapper.java` |
| Repository (Spring Data JPA) | Un `JpaRepository` par agrégat | `EssaiRepository`, `LignePanier Repository` |
| BusinessRuleException | Exception métier avec `code` String (pas de code HTTP dans la logique) | `throw new BusinessRuleException("PANIER_VIDE", "...")` |
| GlobalExceptionHandler | Centralisé dans `common/exception/` | Traduit en `ApiResponse` JSON |
| ApresCommit | Exécution post-commit pour audit et automatisations | `ApresCommit.executer(() → ...)` |
| TenantExecutor | Wrapper `inTenant(schema, Callable)` / `inCentral(Callable)` pour changer de datasource dans les services | `DemandeService.creerDepuisPanier` |
| Événement → webhook | Découplage n8n : Spring Boot publie, n8n orchestre | `NotificationEventPublisher.publier(...)` |

### 2.3 Sécurité

La chaîne de filtres (déclarée dans `SecurityConfig.java`) est :

```
OriginValidationFilter → JwtAuthenticationFilter → MustChangePasswordFilter → @PreAuthorize
```

- **Stateless** : pas de session HTTP (`SessionCreationPolicy.STATELESS`).
- **JWT HS512** : access token (15 min), refresh token (7 jours, HttpOnly cookie), token 2FA (5 min).
- **2FA obligatoire** pour les rôles `RESPONSABLE` et `ADMINISTRATEUR` (TOTP / Google Authenticator).
- **RBAC granulaire** : rôles (`SUPER_ADMINISTRATEUR`, `ADMINISTRATEUR`, `RESPONSABLE`, `TECHNICIEN`, `ACCUEIL`, `CLIENT`) + permissions individuelles déléguables par l'administrateur (tables `role_permission`, `utilisateur_permission`).
- **MustChangePasswordFilter** : bloque les routes si `must_change_password = true`.
- **Endpoints publics** : `/api/auth/**`, `/api/public/**` (GET), `/swagger-ui/**`, `/ws/**`, `POST /api/plateforme/demandes`.
- **BCrypt** pour les mots de passe.
- Le frontend stocke l'access token **exclusivement en mémoire vive** (jamais en localStorage), le refresh token voyage uniquement via cookie HttpOnly. (Source : `axios.ts`, `session.ts`)

### 2.4 Multi-tenancy

| Élément | Mécanisme |
|---|---|
| Stratégie | Schema-per-tenant (une base MySQL `lims_<code>` par laboratoire) |
| Routage | `AbstractRoutingDataSource` → lit `TenantContext.getCurrentTenant()` (ThreadLocal) |
| Résolution du tenant | `TenantInterceptor` lit l'header `X-Tenant-ID` de chaque requête HTTP |
| Provisionnement | `TenantProvisioner.provisionner(Laboratoire)` : CREATE DATABASE + Flyway tenant |
| Déprovisionnement | `TenantProvisioner.deprovisionner(nomSchema)` : ferme le pool, DROP DATABASE |
| Schéma central | `lims_central` : données de plateforme (laboratoires, utilisateurs, permissions, audit) |
| Schéma tenant | `lims_<code>` : données métier (demandes, essais, rapports, facturation…) |
| Sécurité | Un nom de schéma doit correspondre à `lims_[a-z0-9_]+` ; le schéma central est protégé contre le déprovisionnement |

---

## 3. Modules métier

Le package `modules/` contient 22 sous-modules :

| Module | Package | Rôle |
|---|---|---|
| **auth** | `modules/auth` | Authentification (login, refresh, 2FA TOTP, inscription client, activation de compte, réinitialisation mot de passe) |
| **utilisateur** | `modules/utilisateur` | CRUD utilisateurs, rôles, domaines d'intervention, profils (entités : `Utilisateur`, `Role`, `Permission`, `UtilisateurDomaine`, `HistoriqueStatutLaboratoire`) |
| **plateforme** | `modules/plateforme` | Gestion de la plateforme : laboratoires, demandes d'intégration (onboarding d'un nouveau labo), statistiques publiques, paramétrage |
| **demande** | `modules/demande` | Cycle de vie des demandes d'analyse (NOUVEAU → ACCEPTEE/REFUSEE → EN_COURS → EN_VALIDATION → TERMINEE / ANNULEE), pièces jointes |
| **catalogue** | `modules/catalogue` | Catalogue des essais paramétrable par domaine (entité `Essai` avec tarif, durée estimée, limites min/max, seuils critiques) |
| **domaine** | `modules/domaine` | Référentiel des domaines d'analyse (médical, industriel, environnemental, pharmaceutique/cosmétique) |
| **echantillon** | `modules/echantillon` | Réception et identification des échantillons avec traçabilité |
| **essai** | `modules/essai` | Exécution des essais, lignes d'essai, saisie et contrôle automatique des résultats, statuts d'exécution |
| **planification** | `modules/planification` | Attribution et planification des essais aux techniciens |
| **validation** | `modules/validation` | Validation à deux niveaux (technicien puis responsable) avec signature électronique |
| **rapport** | `modules/rapport` | Génération et diffusion des rapports PDF (statuts : GENERE, SIGNE, ENVOYE) ; stockage de la clé PDF (MinIO/disque) |
| **facturation** | `modules/facturation` | Facturation conforme (ICE, TVA), factures, lignes de facturation, paiements ; émission automatique à l'acceptation de la demande |
| **stock** | `modules/stock` | Gestion des articles de stock (réactifs), catégories, mouvements, types |
| **produit** | `modules/produit` | Catalogue produits avec lots, gestion des lots (péremption, activation), consommation |
| **client** | `modules/client` | Fiches clients, profils clients par laboratoire, espace client agrégé multi-laboratoires |
| **patient** | `modules/patient` | Entité patient pour les laboratoires médicaux (liée aux demandes) |
| **panier** | `modules/panier` | Panier d'essais pour la création de demandes par le client (panier central par laboratoire) |
| **satisfaction** | `modules/satisfaction` | Enquêtes de satisfaction client post-rapport |
| **conformite** | `modules/conformite` | Non-conformités qualité : déclaration, gravité (MINEURE/MAJEURE/CRITIQUE), suivi, actions correctives |
| **notification** | `modules/notification` | Notifications in-app (WebSocket STOMP) avec réception par destinataire, canaux (IN_APP, EMAIL…) |
| **evenement** | `modules/evenement` | Entité événement métier pour la traçabilité des transitions |
| **ia** | `modules/ia` | Assistant IA : analyse LLM via Spring AI, synthèse de résultats, scoring d'anomalies (stub partiel) |

---

## 4. Base de données

### 4.1 Schéma central (`lims_central`)

Tables créées par les migrations `db/migration/central/` (36 migrations à ce jour) :

| Table | Contenu |
|---|---|
| `laboratoire` | Référentiel des laboratoires (code, raison_sociale, ICE, nom_schema, statut) |
| `utilisateur` | Comptes utilisateurs toutes entités (matricule, email, rôle, laboratoire_id, must_change_password, 2FA) |
| `role` | Rôles système (SUPER_ADMINISTRATEUR, ADMINISTRATEUR, RESPONSABLE, TECHNICIEN, ACCUEIL, CLIENT) |
| `permission` | Permissions granulaires (code, module, action, libelle, deleguable) |
| `role_permission` | Association rôle ↔ permission |
| `utilisateur_permission` | Surcharges individuelles (GRANT/DENY) |
| `utilisateur_domaine` | Domaines d'expertise par technicien |
| `token_activation` | Tokens d'activation de compte (email) |
| `token_reinitialisation` | Tokens de réinitialisation de mot de passe |
| `refresh_token` | Refresh tokens JWT (rotation) |
| `journal_audit` | Journal d'audit centralisé (action, objet, valeur avant/après, acteur, laboratoire_id) |
| `demande_integration` | Demandes d'onboarding d'un nouveau laboratoire |
| `demande_integration_message` | Messagerie liée aux demandes d'intégration |
| `historique_statut_laboratoire` | Historique des changements de statut d'un laboratoire |
| `client` | Fiches clients (mutualisées entre laboratoires via `client_laboratoire`) |
| `client_laboratoire` | Liaison client ↔ laboratoire |
| `client_profil` | Profil enrichi du client |

### 4.2 Schéma tenant (`lims_<code>`)

Tables créées par les migrations `db/migration/tenant/` (36 migrations) :

| Table | Contenu clé |
|---|---|
| `domaine` | Domaines d'analyse du laboratoire |
| `essai` | Catalogue des essais (code, tarif, durée, limites, seuils, domaine_id) |
| `produit` / `lot` | Catalogue produits et lots (péremption, quantité) |
| `demande` | Demandes d'analyse (référence unique, statut, client ou patient, décideur) |
| `piece_jointe` | Fichiers attachés aux demandes |
| `echantillon` | Échantillons associés aux demandes |
| `ligne_essai` | Lignes d'essai (association demande ↔ essai, statut, montant, résultat) |
| `exigence_echantillon` | Exigences de préparation par essai |
| `validation` | Validations à deux niveaux (niveau 1 = technicien, niveau 2 = responsable, signature) |
| `rapport` | Rapports PDF (numéro, statut, cle_pdf MinIO, diffuse, signataire_id) |
| `rapport_ligne` | Lignes de rapport détaillées |
| `synthese_ia` | Synthèses IA générées (code, contenu, date_generation, demande_id) |
| `facture` | Factures (numéro, type, montant_ht, date_echeance, statut, demande_id) |
| `facture_ligne` | Lignes de facture |
| `paiement` | Paiements enregistrés |
| `notification` | Notifications créées |
| `notification_reception` | Réceptions par utilisateur (lu, canal, statut envoi) |
| `evenement_metier` | Log des transitions d'état métier |
| `enquete_satisfaction` | Enquêtes de satisfaction client |
| `article` | Articles de stock (réactifs) |
| `categorie` | Catégories de stock |
| `mouvement_stock` | Entrées/sorties de stock |
| `type_stock` | Types d'articles |
| `consommation_lot` | Consommation de lots lors des essais |
| `panier` / `ligne_panier` | Paniers clients pour création de demandes |
| `patient` | Patients (laboratoires médicaux) |
| `matricule_sequence` | Séquence de génération des matricules |

### 4.3 Conventions de schéma

- Toutes les tables en `utf8mb4 / utf8mb4_unicode_ci`.
- Clés primaires : `id_<table>` BIGINT AUTO_INCREMENT.
- Noms de contraintes explicites (`uk_`, `fk_`, `chk_`).
- Statuts stockés en `VARCHAR(50)` (pas d'enum MySQL) pour flexibilité des migrations.
- Flyway configuré avec `out-of-order: true` pour les merges de branches.

---

## 5. Intégrations externes

### 5.1 LLM (Spring AI)

- **Fichier** : `integration/llm/LlmClientService.java`
- Utilise `spring-ai-starter-model-openai` avec BOM `spring-ai-bom:2.0.1`.
- Compatible OpenAI, **Ollama** (par défaut `http://localhost:11434/v1`), vLLM, LM Studio.
- Modèle par défaut : `llama3.2` (configurable via `LLM_MODEL`).
- `IaService` (`modules/ia/service/IaService.java`) délègue à `LlmClientService.appelerLlm()`.
- Trois fonctionnalités métier **TODO** : scoring d'anomalies, synthèse automatique, assistant RAG.
- La table `synthese_ia` est créée (migration V15) et l'entité `SyntheseIa` existe, mais la persistance des analyses n'est pas encore câblée dans `IaService`.

### 5.2 MinIO / Stockage fichiers

- **Fichier** : `integration/minio/StockageFichierService.java`
- Implémentation actuelle : **stockage local sur disque** (`${user.home}/lims-documents`), path configurable via `APP_STORAGE_PATH`.
- Configuration MinIO (`minio.*`) présente dans `application.yaml` mais le SDK MinIO n'est **pas dans `pom.xml`** ; la migration vers MinIO est prévue mais non implémentée.
- La méthode `genererUrlPresignee()` retourne une URL simulée (non signée).
- Protection path-traversal implémentée : `cible.startsWith(racine)`.

### 5.3 n8n (automatisation des workflows)

- **Fichier** : `integration/n8n/NotificationEventPublisher.java`
- Pattern : Spring Boot publie un événement JSON en POST sur `{n8n_base_url}/{eventType}` ; n8n orchestre les actions (emails conditionnels, rappels, intégrations tierces).
- Découplage total : une erreur n8n est loggée mais **ne fait jamais échouer le flux Spring**.
- La signature HMAC sortante est **TODO** (signalée dans le Javadoc).
- URL configurable : `N8N_WEBHOOK_BASE_URL` (défaut `http://localhost:5678/webhook`).

### 5.4 SendGrid (e-mails transactionnels)

- **Fichiers** : `integration/sendgrid/SendGridMailClient.java`, `SendGridConfig.java`, `SendGridProperties.java`, `EmailService.java`, `EmailTemplates.java`
- Appelle l'**API SendGrid v3** (`/mail/send`) via `RestClient` avec autorisation Bearer.
- Logique robuste : validation des destinataires (regex), désactivation gracieuse (`SENDGRID_ENABLED=false`), absence de clé, réponses d'erreur classifiées (temporaire vs définitive).
- Un SMTP de repli (`spring.mail.*`) est aussi configuré mais l'envoi applicatif passe par l'API HTTP.
- `EmailTemplates.java` centralise les templates HTML d'e-mails.
- Méthode `sonder()` pour les health checks sans envoi.

### 5.5 Web Push

- **Fichier** : `integration/webpush/WebPushService.java`
- **Stub complet** : les méthodes `envoyer()`, `enregistrerAbonnement()`, `supprimerAbonnement()` ne font que logger.
- La dépendance `nl.martijndwars:web-push` est commentée dans le Javadoc mais absente du `pom.xml`.
- La table `push_subscription` n'existe pas encore en base.

### 5.6 WebSocket (temps réel in-app)

- **Fichier** : `config/WebSocketConfig.java`
- STOMP over SockJS sur `/ws`.
- `NotificationService` utilise `SimpMessagingTemplate.convertAndSendToUser()` pour livrer les notifications in-app.
- Endpoint de handshake dans les routes publiques de `SecurityConfig`.

---

## 6. Frontend

### 6.1 Framework et outillage

| Outil | Rôle |
|---|---|
| React 19 | UI déclarative |
| TypeScript 5.8 | Typage statique |
| Vite 6.3.5 | Build + HMR |
| TanStack Router 1.170 | Routage **file-based** (arbre généré dans `routeTree.gen.ts`) |
| TanStack Query 5.101 | Cache et synchronisation des données serveur |
| Axios 1.7.9 | Client HTTP (via `ApiClient` centralisé) |
| Tailwind CSS v4 | Utilitaires CSS |
| Radix UI | Primitives accessibles (Dialog, Select, Dropdown, Tabs…) |
| shadcn/ui | Composants UI construits sur Radix + Tailwind |
| Lucide React | Icônes |
| react-hook-form + Zod | Formulaires avec validation schéma |
| Recharts | Graphiques (tableaux de bord) |
| Leaflet | Cartes géographiques |
| Framer Motion | Animations |
| Sonner | Toasts / notifications |

### 6.2 Structure de `src/`

```
src/
├── api/                  Couche d'accès données (un fichier par domaine)
│   ├── axios.ts          ApiClient (Axios + intercepteurs, refresh silencieux)
│   ├── session.ts        État de session en mémoire (accessToken, user, permissions)
│   ├── auth.ts           Endpoints d'authentification
│   ├── demandes.ts       CRUD demandes labo
│   ├── demandesClient.ts Demandes côté client
│   ├── employes.ts       Gestion des employés
│   ├── facturesAdmin.ts  Facturation admin
│   ├── exports.ts        Export Excel/PDF
│   └── ...               (31 fichiers au total)
├── components/           Composants partagés et spécialisés par domaine
│   ├── ui/               Composants shadcn/ui de base
│   ├── layout/           Layouts (sidebars, headers)
│   ├── admin/            Composants admin (catalogue, employés, facturation, stock…)
│   ├── demandes/         Composants gestion des demandes
│   ├── client/           Composants espace client
│   └── ...
├── features/             Shells et composants de haut niveau par rôle
│   ├── admin/            Shell admin (AdminShell.tsx)
│   ├── client/           Shell client (ClientShell.tsx)
│   ├── shell/            Guards de routes (garde.ts → exigerPermission())
│   └── super-admin/      Shell super-admin + pages audit, labo, utilisateurs
├── layouts/              Layouts de page (auth, app, client…)
├── hooks/                Hooks React custom
├── lib/                  Utilitaires (map, cn…)
├── data/                 Données statiques (rôles, libellés…)
└── routes/               **Arbre de routes TanStack Router (file-based)**
    ├── __root.tsx         Route racine (restauration de session)
    ├── login.tsx          Page de connexion
    ├── inscription.tsx    Inscription client
    ├── activation.tsx     Activation de compte
    └── app/               Routes authentifiées
        ├── tableau-de-bord.tsx
        ├── demandes/       Gestion des demandes labo
        ├── demandes-clients/ Espace demandes client
        ├── catalogue.tsx   Catalogue des essais
        ├── employes.tsx    Gestion des employés
        ├── facturation.tsx Facturation admin
        ├── factures/       Détail factures
        ├── rapports/       Rapports
        ├── enquetes/       Enquêtes de satisfaction
        ├── stock.tsx       Gestion du stock
        ├── panier.tsx      Panier client
        ├── laboratoires/   Vue labo (par ID)
        ├── parametres.tsx  Paramètres du laboratoire
        ├── exports.tsx     Export Excel/PDF
        ├── audit.tsx       Journal d'audit (admin)
        ├── journal-audit.tsx Journal d'audit (super-admin)
        ├── profil.tsx      Profil utilisateur
        └── ...
```

### 6.3 Sécurité frontend

- **Access token en mémoire vive uniquement** (`session.ts`) — perdu au rechargement, restauré silencieusement via cookie HttpOnly (`POST /api/auth/refresh`).
- **Refresh token transparent** : le cookie HttpOnly voyage automatiquement, jamais accessible en JS.
- **Guards de routes** : `exigerPermission(code)` dans `garde.ts` redirige vers `/app` si la permission manque.
- **Gardes de rôle** dans les `beforeLoad` des routes.
- **Multi-tenant** : le `tenantId` est extrait du JWT (claim) et porté dans le header `X-Tenant-ID` via `ApiClient.setTenant()`.
- Intercepteur `doRefresh()` partagé entre requêtes concurrentes (une seule promesse de refresh en vol).
- **403 PASSWORD_CHANGE_REQUIRED** : redirige automatiquement vers `/changer-mot-de-passe`.

### 6.4 Pages principales

| Route | Public/Auth | Rôles | Description |
|---|---|---|---|
| `/` | Public | — | Landing page avec cartes des laboratoires (Leaflet) |
| `/login` | Public | — | Connexion (email + mot de passe + 2FA) |
| `/inscription` | Public | — | Inscription client |
| `/app/tableau-de-bord` | Auth | Tous | Dashboard adapté au rôle |
| `/app/demandes` | Auth | Labo | Gestion des demandes du laboratoire |
| `/app/demandes-clients` | Auth | CLIENT | Mes demandes (multi-laboratoires) |
| `/app/catalogue` | Auth | Admin/Responsable | Catalogue des essais |
| `/app/employes` | Auth | Admin | Gestion des employés et permissions |
| `/app/facturation` | Auth | Admin/Accueil | Facturation et paiements |
| `/app/rapports` | Auth | Technicien/Responsable | Rapports PDF |
| `/app/stock` | Auth | Admin/Technicien | Stock de réactifs |
| `/app/panier` | Auth | CLIENT | Panier d'essais |
| `/app/enquetes` | Auth | CLIENT/Admin | Enquêtes de satisfaction |
| `/app/exports` | Auth | Admin | Exports Excel/PDF |
| `/app/audit` | Auth | Admin | Journal d'audit du laboratoire |
| `/app/journal-audit` | Auth | Super-Admin | Journal d'audit plateforme |
| `/app/laboratoires` | Auth | Super-Admin | Gestion des laboratoires |
| `/carte` | Public | — | Carte des laboratoires |

---

## 7. Points notables

### 7.1 Forces architecturales

1. **Isolation multi-tenant robuste** : `TenantRoutingDataSource` + `TenantProvisioner` garantissent qu'aucune donnée d'un laboratoire ne peut fuir dans un autre. La validation du nom de schéma (`lims_[a-z0-9_]+`) prévient l'injection SQL sur les `CREATE DATABASE`.

2. **Sécurité frontend exemplaire** : access token en mémoire vive (pas de XSS localStorage), refresh cookie HttpOnly (pas de CSRF car `withCredentials` + validation Origin), intercepteur de refresh partagé pour éviter les race conditions.

3. **RBAC à deux niveaux** : rôles prédéfinis + permissions déléguables individuellement (`utilisateur_permission` avec effet GRANT/DENY). La migration V36 différencie permissions déléguables des permissions structurelles (non déléguables).

4. **Découplage événement/notification** : le pattern `événement → n8n → action` évite de coupler la logique métier Spring Boot aux détails des notifications. Un changement d'e-mail ne nécessite pas de toucher le service Java.

5. **Audit post-commit fiable** : `ApresCommit.executer()` garantit que le journal d'audit n'est écrit que si la transaction principale aboutit (pas de faux positifs).

6. **Gestion d'erreur SendGrid mature** : classification temporaire/définitive, désactivation gracieuse, validation des destinataires, sonde de health check.

7. **Export Excel/PDF** : module `export/` indépendant (Apache POI + OpenPDF) sans couplage aux modules métier.

### 7.2 Potentielles améliorations

| Priorité | Problème | Recommandation |
|---|---|---|
| **Haute** | `WebPushService` est un stub complet | Ajouter la dépendance `nl.martijndwars:web-push`, générer les clés VAPID, créer la table `push_subscription`, implémenter l'envoi VAPID chiffré. |
| **Haute** | `IaService` ne persiste pas les synthèses | Câbler `SyntheseIaRepository` dans `IaService.analyser()` pour exploiter la table `synthese_ia` déjà créée. |
| **Haute** | `StockageFichierService` utilise le disque local en prod | Ajouter le SDK MinIO (`io.minio:minio`) dans `pom.xml` et implémenter le stockage objet réel (pré-signé URLs). |
| **Haute** | La génération PDF de `RapportService` ne produit pas de vrai PDF | `rapport.setClePdf()` n'est jamais appelé ; brancher OpenPDF pour la génération et `StockageFichierService` pour le stockage. |
| **Moyenne** | Webhook n8n sans signature HMAC | Implémenter la signature HMAC (ex. `X-Hub-Signature-256`) pour authentifier les requêtes sortantes et valider les webhooks entrants. |
| **Moyenne** | `LlmClientService` sans RAG | Implémenter les méthodes `calculerScoreAnomalie`, `genererSynthese`, `repondreQuestion` avec chunks vectoriels (Spring AI VectorStore). |
| **Moyenne** | Pas de rate limiting sur les endpoints critiques | Ajouter un filtre Bucket4j ou Spring Cloud Gateway pour les endpoints `/api/auth/login` et `/api/plateforme/demandes`. |
| **Faible** | `NonConformite` n'a pas d'annotation `@Entity` | L'entité dans `modules/conformite/entity/NonConformite.java` a bien les annotations JPA mais **manque de `@Table`**. Vérifier que la table `non_conformite` existe dans les migrations tenant (non trouvée dans les 36 migrations). |
| **Faible** | Dialecte Hibernate `MariaDBDialect` pour MySQL 8 | Préférer `MySQLDialect` ou `MySQL8Dialect` pour éviter des incompatibilités potentielles sur les fonctions MySQL-spécifiques. |
| **Faible** | `tenantId` porté à la fois dans le JWT et en header `X-Tenant-ID` | Choisir une seule source de vérité : si le JWT porte déjà le tenant, `TenantInterceptor` peut lire le claim JWT plutôt que le header (réduire la surface d'attaque). |
| **Documentation** | `spring.ai.openai.base-url` pointe sur Ollama par défaut | Documenter la configuration LLM dans `demarrage.md` avec les étapes pour lancer Ollama ou pointer vers OpenAI. |

---

## 8. Annexe — Migrations par ordre chronologique (résumé)

### Schéma central
`V1` laboratoire → `V2` demande_integration → `V3` role → `V4` permission → `V5` role_permission → `V6` utilisateur → `V7` utilisateur_domaine → `V8` token_activation → `V9` token_reinitialisation → `V10` refresh_token → `V11` journal_audit → … → `V21` seed demo central → `V22` coordonnées laboratoire → `V23` secret 2FA → `V24` intégration laboratoire complète → `V25` inscription client → … → `V35` permissions effectives → `V36` permissions déléguables + RBAC

### Schéma tenant
`V1` client → `V2` patient → `V3` domaine → `V4` essai → `V5` produit → `V6` lot → `V7` demande → `V8` echantillon → `V9` piece_jointe → `V10` ligne_essai → `V11` consommation_lot → `V12` validation → `V13` rapport → `V14` rapport_ligne → `V15` synthese_ia → `V16` enquete_satisfaction → `V17` facture → `V18` facture_ligne → `V19` paiement → `V20` evenement_metier → … → `V25` exigence_echantillon → … → `V31` panier → … → `V36` permissions déléguables

---

*Rapport généré en lecture seule — aucune modification apportée au code source.*
