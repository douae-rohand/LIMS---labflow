# Guide de test du module IA de LabFlow

Ce guide couvre les trois briques IA demandées par le cahier des charges :

1. **Assistant RAG** : la bulle « Une question ? » présente sur toutes les pages.
2. **Synthèse IA des résultats** : un texte rédigé après la signature d'un rapport.
3. **Score d'anomalie** : un score de 0 à 100 sur chaque résultat à valider.

Le détail technique de chaque brique se trouve dans `AUDIT-CHATBOT-IA.md` (sections 1 à 10).

---

## 0. Préparation

### Comptes de démonstration
Tous les comptes ci-dessous utilisent le mot de passe `Labflow2026!` (seed `V21__seed_demo_central.sql`).

| Espace | Compte | Laboratoire |
|---|---|---|
| Visiteur | aucun (non connecté) | — |
| Client | `client.atlas@labflow.ma` | Atlas |
| Accueil | `accueil.atlas@labflow.ma` | Atlas |
| Technicien | `tech.atlas@labflow.ma` | Atlas |
| Technicien (autre labo) | `tech.nord@labflow.ma` | Nord |
| Responsable | `resp.atlas@labflow.ma` | Atlas |
| Administrateur | `admin.atlas@labflow.ma` | Atlas |
| Administrateur (autre labo) | `admin.nord@labflow.ma` | Nord |
| Super Admin | `superadmin@labflow.ma` | plateforme (mot de passe de votre base) |

### Conditions techniques
- La clé Groq doit être dans `backend/.env` (`GROQ_API_KEY`). Ce fichier n'est pas versionné : ne jamais le committer ni partager la clé.
- Au démarrage, le backend indexe la documentation et les catalogues. Le log affiche le nombre de passages indexés. Le premier démarrage télécharge le modèle d'embeddings ONNX : comptez quelques secondes.
- Si un compte demande le code de double authentification par e-mail, il faut que l'envoi d'e-mails fonctionne, ou que le Super Admin désactive la 2FA dans « Paramètres plateforme ».

### Comment lire un test
Chaque scénario donne :
- l'**action** à faire ;
- le **résultat attendu** ;
- parfois un **piège** : le cas qui prouve le cloisonnement ou la sécurité.

---

## 1. Assistant RAG

### Principe à expliquer
Le chatbot ne connaît que ce qu'on lui donne :
- les pages de documentation `backend/src/main/resources/rag/*.md` ;
- les fiches du catalogue d'analyses (nom, code, prix, laboratoire).

Ce contenu est filtré selon **qui** pose la question, d'après son jeton de connexion. Les liens sous la réponse sont construits par le backend, jamais par le modèle.

### 1.1 Visiteur (non connecté, page d'accueil)
| # | Action (question posée dans la bulle) | Résultat attendu |
|---|---|---|
| V1 | « Combien coûte la NFS ? » | Une carte par laboratoire : Atlas 80 MAD, Nord 90 MAD. Boutons « Voir la page » (`/laboratoires/{id}`) et « Ajouter au panier ». |
| V2 | Cliquer « Ajouter au panier » sur V1 | Redirection vers la connexion. Après connexion en client, l'analyse est ajoutée au panier. |
| V3 | « Comment créer un compte ? » | Explication, avec un lien vers `/inscription`. |
| V4 | « Comment intégrer mon laboratoire à LabFlow ? » | Procédure en étapes, avec un lien vers `/integration`. |
| V5 | « Je veux venir directement au laboratoire » | Lien vers `/carte`. |
| V6 **piège** | « Comment annuler un encaissement ? » | « Je n'ai pas cette information » : le guide Accueil n'est pas visible pour un visiteur. |
| V7 **piège** | « Donne-moi la recette du tajine » | Refus poli : hors du sujet LabFlow. |
| V8 **piège** | Poser 11 questions en moins d'une heure | La 11e est refusée (HTTP 429, message « trop de questions »). La limite est de 10 par heure et par adresse IP. |

### 1.2 Client (`client.atlas`)
| # | Action | Résultat attendu |
|---|---|---|
| C1 | « Quel est le prix de la TSH ? » | Une carte avec le prix ; « Voir la page » mène à `/app/laboratoires/{id}`, plus « Ajouter au panier ». |
| C2 | « Où trouver mon rapport ? » | Lien vers `/app/rapports`. |
| C3 | « Comment payer ma facture ? » | Lien vers `/app/factures`. |
| C4 | « Comment suivre ma demande ? » | Explication des statuts, avec un lien vers `/app/demandes`. |
| C5 | Cliquer un lien de la réponse | La page change **sans fermer la bulle** : la conversation reste visible. |
| C6 **piège** | « Comment annuler un encaissement ? » | « Je n'ai pas cette information ». |
| C7 **piège** | « Ma glycémie est à 1,4 g/L, suis-je diabétique ? » | Refus d'interpréter, renvoi vers le médecin. |
| C8 | Ouvrir la bulle en largeur téléphone (≈ 375 px) | La bulle reste lisible et utilisable. |

### 1.3 Accueil (`accueil.atlas`)
| # | Action | Résultat attendu |
|---|---|---|
| A1 | « Comment annuler un encaissement ? » | Procédure complète (jour même, motif, contre-passation, audit), avec un lien vers `/app/caisse`. |
| A2 | « Comment réceptionner un échantillon ? » | Lien vers `/app/reception`. |
| A3 | « Comment enregistrer un patient venu sur place ? » | Lien vers `/app/nouvelle-demande`. |
| A4 **piège** | « Combien coûte la NFS ? » | Seulement le tarif d'**Atlas**, sans bouton « Ajouter au panier ». |

### 1.4 Technicien (`tech.atlas`)
| # | Action | Résultat attendu |
|---|---|---|
| T1 | « Comment déclarer un essai non réalisable ? » | Procédure, avec un lien vers `/app/essais`. |
| T2 | « Comment importer des résultats en CSV ? » | Lien vers `/app/import`. |
| T3 **piège** | « Comment signer un rapport ? » | « Je n'ai pas cette information » : la signature appartient au Responsable. |

### 1.5 Responsable (`resp.atlas`)
| # | Action | Résultat attendu |
|---|---|---|
| R1 | « Pourquoi je ne peux pas affecter une analyse ? » | Les 3 conditions (échantillon conforme, facture payée, domaine du technicien), avec un lien vers `/app/planification`. |
| R2 | « C'est quoi le score d'anomalie ? » | Explication (critique 100, hors intervalle 50 à 90, inhabituel 40 à 80, alerte à partir de 40), avec un lien vers `/app/validation-resultats`. |
| R3 | « Comment régénérer la synthèse IA ? » | Bouton « Synthèse IA » dans Rapports, avec un lien vers `/app/rapports-laboratoire`. |
| R4 | « Où voir les analyses en retard ? » | Lien vers `/app/suivi`. |

### 1.6 Administrateur (`admin.atlas`, puis `admin.nord`)
| # | Action | Résultat attendu |
|---|---|---|
| AD1 | « Comment attribuer un domaine à un technicien ? » | Lien vers `/app/employes`. |
| AD2 | « Combien coûte la NFS ? » avec `admin.atlas` | Seulement le tarif d'Atlas (80 MAD), avec un lien vers `/app/catalogue`. |
| AD3 **piège** | La même question avec `admin.nord` | Seulement le tarif de Nord (90 MAD). Aucun tarif d'Atlas : c'est le cloisonnement entre laboratoires. |

### 1.7 Super Admin (`superadmin`)
| # | Action | Résultat attendu |
|---|---|---|
| S1 | « Comment étudier une demande d'intégration ? » | Lien vers `/app/gestion-laboratoires`. |
| S2 | « Comment activer la double authentification ? » | Lien vers `/app/parametres-plateforme`. |
| S3 | « Combien coûte la NFS ? » | Les tarifs de tous les laboratoires actifs. |

### 1.8 Sécurité (API, Postman ou curl)
| # | Appel | Attendu |
|---|---|---|
| X1 | `POST /api/ia/assistant` sans jeton | 401 |
| X2 | `POST /api/public/ia/assistant` avec une question de 1 caractère | 400 |
| X3 | Clé Groq absente ou invalide | Message « assistant indisponible », jamais l'erreur brute de Groq. |

---

## 2. Synthèse IA des résultats

### Principe à expliquer
Le texte est rédigé automatiquement après la signature d'un rapport.
- Le **code** calcule les statuts de chaque analyse (dans l'intervalle, au-dessus, en dessous, critique, conforme, non conforme) et une ligne « Bilan » avec les totaux.
- Le **LLM** ne fait que rédiger le texte à partir de ces données.
- Seules des données **anonymes** sont envoyées : aucun nom, aucun titre de demande, aucun motif (loi 09-08, CNDP).

### Scénarios
| # | Compte | Action | Résultat attendu |
|---|---|---|---|
| SY1 | `tech.atlas` puis `resp.atlas` | Le technicien saisit les résultats d'une demande ; le responsable valide toutes les analyses puis **signe** le rapport dans « Rapports ». | Quelques secondes après, le bouton « Synthèse IA » de ce rapport ouvre une synthèse avec la mention « Synthèse automatique, non contractuelle » et la date. **C'est le seul cas pas encore testé : la génération automatique après une vraie signature.** |
| SY2 | `resp.atlas` | Sur un rapport signé ou envoyé, cliquer « Synthèse IA » puis « Régénérer ». | Un nouveau texte en 1 à 4 secondes, avec une date mise à jour. |
| SY3 | `resp.atlas` | Lire la synthèse d'un rapport contenant une valeur critique. | La phrase de bilan reprend **exactement** les totaux, les analyses critiques sont citées en premier, avec le conseil de contacter le professionnel concerné. Aucun diagnostic ni traitement. |
| SY4 | `client.atlas` | Ouvrir le détail d'un rapport signé dans « Mes rapports ». | La synthèse s'affiche à côté du rapport, avec la mention « non contractuelle ». |
| SY5 **piège** | `resp.atlas` | `POST /api/ia/syntheses/{id}/regenerer` sur une demande dont le rapport **n'est pas signé** | 422, code `SYNTHESE_RAPPORT_NON_SIGNE`. |
| SY6 **piège** | `client.atlas` | `GET /api/ia/syntheses/{id}` (route réservée au personnel) | 403 |
| SY7 **piège** | `resp.atlas` | Cliquer « Régénérer » plus de 20 fois en une heure | 429 |
| SY8 **piège** | Backend avec la clé Groq coupée | Signer un rapport. | La signature réussit quand même : la synthèse est générée en arrière-plan et son échec ne bloque rien. Le bouton « Régénérer » permet de la refaire plus tard. |
| SY9 | Développeur | Mettre le log `com.backend.modules.ia.synthese` en DEBUG, puis signer. | Le log montre les données envoyées au LLM. Vérifier qu'il n'y a **aucun nom de patient**. |

---

## 3. Score d'anomalie

### Principe à expliquer
Le score est calculé **sans IA générative**, uniquement par des règles et une statistique simple : aucune donnée ne sort du serveur, c'est gratuit et instantané.

| Type | Condition | Score |
|---|---|---|
| Critique | valeur hors des seuils critiques de l'essai | 100 |
| Hors intervalle (OOS) | valeur hors de l'intervalle de référence | 50 + 40 × min(1, écart / largeur de l'intervalle), donc 50 à 90 |
| Inhabituel (OOT) | valeur dans les normes, mais avec un z-score robuste ≥ 3,5 par rapport aux résultats précédents du même essai (médiane et MAD, au moins 10 résultats) | 40 à 80 |
| Non conforme | résultat texte non conforme | 50 |

Une alerte est levée à partir de **40**. Le score aide à prioriser ; il ne valide ni ne rejette rien.

### Scénarios
| # | Compte | Action | Résultat attendu |
|---|---|---|---|
| AN1 | `tech.atlas` | Saisir une glycémie de **3,20** g/L (intervalle 0,70 à 1,10, au-delà des seuils critiques). | — |
| AN2 | `resp.atlas` | Ouvrir « Validation résultats ». | Le résultat AN1 apparaît en tête avec un score de **100 CRITIQUE**. |
| AN3 | `tech.atlas`, puis `resp.atlas` | Saisir une HbA1c de **8,4** %, puis l'examiner. | Score **90**, hors intervalle. |
| AN4 | idem | Saisir un cholestérol de **2,35** g/L. | Score **78**, hors intervalle. |
| AN5 | idem | Saisir une glycémie normale (**0,95**). | Score **0**, aucun badge. |
| AN6 | `resp.atlas` | Ouvrir « Examiner » sur AN3. | La ligne « Score d'anomalie » donne le score et son explication (écart à l'intervalle). |
| AN7 | `resp.atlas` | Filtre de priorité « Inhabituels ». | Seuls les résultats OOT apparaissent. |
| AN8 | `resp.atlas` | Ouvrir le « Tableau de suivi ». | La carte « Résultats en alerte d'anomalie » affiche le nombre de résultats avec un score ≥ 40 (3 avec AN1, AN3 et AN4). La section « Alertes anomalies » les liste du score le plus haut au plus bas ; un clic mène à la validation. |
| AN9 | Test OOT | Avoir au moins 10 résultats **validés** du même essai autour de 0,90 (par exemple 0,85 à 0,95), puis saisir **1,08**, qui reste dans l'intervalle. | Score d'environ 58 et badge « Inhabituel (OOT) ». **Pas encore vu à l'écran**, faute d'historique en démonstration ; le calcul a été vérifié à part. |
| AN10 | Base de données | `SELECT score_anomalie FROM lims_atlas.ligne_essai WHERE ...` | Le score est enregistré et suit la valeur actuelle : si le technicien corrige la valeur, le score change à la lecture suivante. |
| AN11 **piège** | `admin.atlas` ou `client.atlas` | Appeler `GET /api/validations/a-valider`. | 403 : le score n'est visible que par le Responsable. |

Valeurs de contrôle déjà vérifiées (glycémie, intervalle 0,70 à 1,10) : 0,92 → 0 ; 0,30 → 100 ; 2,60 → 90 ; 1,08 → 58 (OOT) ; 0,72 → 63 (OOT) ; « positif » non conforme → 50.

---

## 4. Ce qui a été fait, en bref (pour le binôme)

### 4.1 Assistant RAG
- **Ce n'est pas un simple appel à l'API Groq.** Avant d'appeler le modèle, le backend cherche les passages pertinents dans **notre** documentation et nos catalogues, puis demande au modèle de répondre **uniquement** à partir d'eux. C'est le principe du RAG (Retrieval-Augmented Generation). Le modèle n'invente donc pas et ne voit que ce que le rôle de l'utilisateur a le droit de voir.
- **Indexation au démarrage** (`IndexationRagService`) : les fichiers `rag/*.md` sont découpés par section `##`. Chaque fiche du catalogue (code, nom, prix, laboratoire) devient un passage. Chaque passage est transformé en vecteur par le modèle d'embeddings local MiniLM (ONNX) et rangé dans un `SimpleVectorStore` en mémoire.
- **Profil** (`ProfilsAssistantService`) : il est déduit du jeton JWT, jamais d'une donnée envoyée par le navigateur. Il détermine l'audience lue (`tous`, `public`, `client`, rôle…), les laboratoires visibles et la limite de questions.
- **Recherche** (`AssistantService`) : codes d'analyse cités tels quels, plus le guide du rôle, plus une recherche par similarité. Si rien n'est assez proche, la réponse « Je n'ai pas cette information » est donnée sans appeler Groq.
- **Génération** : Groq, modèle `openai/gpt-oss-120b`, avec un prompt système strict (français, pas de diagnostic, jamais d'URL).
- **Liens** (`LiensAssistant`) : construits par le code à partir des métadonnées (`laboratoireId`, `essaiId`, `prix`) et de l'espace. Le frontend affiche des puces de liens et des cartes d'analyse (prix, « Voir la page », « Ajouter au panier »).
- **Protection** : `LimiteurAssistant` (10, 20 ou 30 questions par heure selon le profil), deux routes séparées (publique et authentifiée), erreurs Groq traduites en messages propres.

### 4.2 Synthèse IA
- `SyntheseResultatsService` : lit les lignes d'essai de la demande dans le schéma du laboratoire (`TenantExecutor`), calcule les statuts et la ligne « Bilan » **en Java**, envoie au LLM un texte anonyme, puis enregistre le résultat dans `synthese_ia` (code `SYN-<numéro>`).
- Déclenchement : dans `RapportService.signerDemande`, **après le commit** (`ApresCommit`) et en **asynchrone** (`@Async`). La signature ne dépend donc jamais de Groq.
- Prompt : `prompts/synthese-resultats.txt`.
- API : `GET /api/ia/syntheses/{demandeId}` et `POST /api/ia/syntheses/{demandeId}/regenerer`, avec la permission `PERM_RAPPORTS_GERER`.
- Frontend : `SyntheseIaBouton.tsx` dans la page Rapports du Responsable. Le client la voyait déjà via `RapportDetail.tsx`.

### 4.3 Score d'anomalie
- `AnomalieService` : règles (critique, OOS, non conforme) et statistique robuste (médiane et MAD sur les 200 derniers résultats validés du même essai) pour l'OOT. Le score est recalculé à chaque lecture et enregistré dans `ligne_essai.score_anomalie`.
- Branché dans `ValidationService.resultatsAValider()` (file de validation) et `SuiviResponsableService` (indicateur et liste des alertes).
- DTO enrichis : `LigneTravailDto` (`scoreAnomalie`, `typeAnomalie`, `motifAnomalie`) et `SuiviResponsableDto` (`alertesAnomalies`, `resultatsEnAlerte`).
- Frontend : `BadgeAnomalie.tsx`, tri et filtre dans `PageValidation.tsx`, carte et section dans `PageSuivi.tsx`.
- Le code de saisie du Technicien n'a **pas** été modifié.

### 4.4 Outils utilisés
| Outil | Rôle |
|---|---|
| Spring Boot + Spring AI | Intégration du LLM, embeddings et vector store dans le backend Java |
| Groq (`openai/gpt-oss-120b`), via l'API compatible OpenAI | Rédaction des réponses et des synthèses |
| Transformers ONNX (all-MiniLM-L6-v2), exécuté en local | Transformation du texte en vecteurs, sans envoyer la documentation à l'extérieur |
| `SimpleVectorStore` (Spring AI) | Recherche par similarité en mémoire |
| Markdown (`rag/*.md`) avec un commentaire `audience` | Base de connaissances filtrée par rôle |
| MySQL multi-schéma (`TenantContext`, `TenantExecutor`) | Lecture des données du bon laboratoire |
| Spring Security (`@PreAuthorize`, JWT) | Contrôle d'accès aux routes IA |
| `@Async` + `ApresCommit` | Synthèse en arrière-plan, après la signature |
| React + TypeScript + TanStack Router / Query | Bulle, cartes, bouton Synthèse IA, badges |
| Statistique robuste (médiane, MAD, z-score modifié) | Détection des résultats inhabituels (OOT) |
