# LIMS - Système de Gestion de Laboratoire (SGL)

Plateforme web multi-laboratoires permettant de digitaliser et de fiabiliser le cycle complet d'analyse d'un laboratoire, de la demande client à la diffusion du rapport validé.

> Projet académique - Module *Frameworks Technologie Web* - Génie Informatique, filière SIAD
> Année universitaire 2026-2027 - ENSA Tétouan

---

## Sommaire

- [Présentation](#présentation)
- [Fonctionnalités principales](#fonctionnalités-principales)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Structure du projet](#structure-du-projet)
- [Prérequis](#prérequis)
- [Installation et démarrage](#installation-et-démarrage)
- [Variables d'environnement](#variables-denvironnement)
- [Migrations de base de données](#migrations-de-base-de-données)
- [Rôles et accès](#rôles-et-accès)
- [Documentation API](#documentation-api)
- [Équipe](#équipe)

---

## Présentation

Le SGL (LIMS - *Laboratory Information Management System*) centralise l'ensemble du processus d'analyse d'un laboratoire : réception des demandes, planification des essais, réception des échantillons, saisie et validation des résultats, génération des rapports, facturation et suivi de la satisfaction client.

La plateforme est **multi-tenant** : plusieurs laboratoires peuvent être hébergés indépendamment, chacun disposant d'un schéma de base de données dédié garantissant l'isolation totale de ses données.

## Fonctionnalités principales

- Gestion des demandes d'analyse (création, décision, annulation, suivi)
- Catalogue d'essais paramétrable par domaine (médical, industriel, environnemental, pharmaceutique/cosmétique)
- Attribution et planification des essais aux techniciens
- Réception et identification des échantillons avec traçabilité
- Saisie des résultats avec contrôle automatique de conformité
- Validation à deux niveaux (technicien puis responsable) avec signature électronique
- Génération et diffusion automatisée des rapports (PDF)
- Facturation conforme aux normes marocaines (ICE, TVA)
- Gestion du stock de réactifs et des lots
- Tableaux de bord adaptés à chaque rôle
- Assistant IA (scoring d'anomalies, synthèse de résultats, RAG documentaire)
- Architecture multi-tenant avec isolation stricte des données

## Architecture

Le système repose sur une **architecture modulaire**, backend et frontend suivant le même découpage fonctionnel (M01 à M15), afin de faciliter la maintenance et l'ajout de nouveaux modules sans impact sur l'existant.

```
Client (navigateur)
      │
      ▼
Frontend SPA (React)
      │  REST API / WebSocket
      ▼
Backend (Spring Boot)
      │
      ├── Base de données centrale (MySQL) - tenants, demandes d'intégration
      ├── Schémas dédiés par tenant (MySQL) - données métier par laboratoire
      ├── MinIO - stockage des fichiers (rapports, pièces jointes)
      └── n8n - orchestration des notifications et automatisations
```

Le pattern **événement → webhook → workflow** découple la logique métier (Spring Boot) de l'orchestration des notifications (n8n), permettant de faire évoluer les automatisations sans modifier le backend.

## Stack technique

| Couche | Technologie |
|---|---|
| Backend | Spring Boot 4, Java 21, API REST |
| Frontend | React (JavaScript), SPA |
| Base de données | MySQL 8, schéma par tenant |
| Migrations | Flyway |
| Automatisation | n8n |
| Intelligence artificielle | Spring AI (scoring, RAG, synthèse) |
| Stockage fichiers | MinIO |
| Génération PDF | OpenPDF / iText |
| Authentification | Spring Security, JWT, 2FA |
| Notifications e-mail | SendGrid |
| Notifications push | Web Push |
| Temps réel | WebSocket |

## Structure du projet

```
backend/
├── src/main/java/com/backend/
│   ├── config/           configuration transverse (sécurité, multi-tenant, WebSocket...)
│   ├── common/            éléments partagés (exceptions, audit, contexte tenant)
│   ├── modules/           un module par domaine métier 
│   │   ├── auth/
│   │   ├── utilisateur/
│   │   ├── plateforme/
│   │   ├── demande/
│   │   ├── planification/
│   │   ├── echantillon/
│   │   ├── catalogue/
│   │   ├── essai/
│   │   ├── validation/
│   │   ├── rapport/
│   │   ├── stock/
│   │   ├── satisfaction/
│   │   ├── facturation/
│   │   ├── conformite/
│   │   ├── ia/
│   │   └── notification/
│   └── integration/       connecteurs externes (n8n, MinIO, SendGrid, LLM, Web Push)
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/
│       ├── central/       migrations du schéma central
│       └── tenant/        migrations du schéma tenant
└── src/test/java/com/backend/
```

## Prérequis

- Java 21+
- Maven 3.9+
- MySQL 8+
- Node.js 18+ et npm (pour le frontend)
- Une instance MinIO (locale ou distante)
- Une instance n8n (locale ou distante)

## Installation et démarrage

### Backend

```bash
cd backend
cp .env.example .env
# Renseigner les variables dans .env

./mvnw clean install
./mvnw spring-boot:run
```

L'API est accessible par défaut sur `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

L'application est accessible par défaut sur `http://localhost:5173`.

## Variables d'environnement

Le fichier `.env.example` liste toutes les variables requises. Ne jamais versionner le fichier `.env`.

| Variable | Description |
|---|---|
| `DB_CENTRAL_URL` | URL de connexion au schéma central MySQL |
| `DB_CENTRAL_USERNAME` | Utilisateur de la base centrale |
| `DB_CENTRAL_PASSWORD` | Mot de passe de la base centrale |
| `JWT_SECRET` | Clé secrète de signature des tokens JWT |
| `JWT_EXPIRATION` | Durée de validité du token d'accès |
| `JWT_REFRESH_EXPIRATION` | Durée de validité du refresh token |
| `MAIL_HOST` / `MAIL_PORT` | Configuration SMTP (SendGrid) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Identifiants SendGrid |
| `MINIO_ENDPOINT` | URL du serveur MinIO |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | Identifiants MinIO |
| `MINIO_BUCKET` | Nom du bucket de stockage |
| `N8N_WEBHOOK_BASE_URL` | URL de base des webhooks n8n |
| `LLM_API_URL` / `LLM_API_KEY` | Accès au service LLM externe |

## Migrations de base de données

Les migrations sont gérées par **Flyway**, séparées en deux ensembles :

- `db/migration/central/` - schéma central (laboratoires, demandes d'intégration)
- `db/migration/tenant/` - schéma type appliqué à chaque nouveau tenant (demandes, essais, résultats, rapports, facturation...)

## Rôles et accès

| Rôle | Périmètre |
|---|---|
| Client | Ses demandes, rapports, enquêtes de satisfaction |
| Personnel d'accueil | Enregistrement client, réception des échantillons, encaissement |
| Technicien | Planning, essais attribués, saisie des résultats, validation technique |
| Responsable de laboratoire | Décision sur les demandes, validation des résultats, signature des rapports |
| Administrateur | Paramétrage du laboratoire, catalogue, utilisateurs, facturation |
| Super Administrateur | Gestion de la plateforme et des laboratoires, demandes d'intégration |

L'authentification à deux facteurs (2FA) est obligatoire pour les rôles Responsable et Administrateur.

## Documentation API

La documentation interactive de l'API (Swagger / OpenAPI) est disponible une fois le backend démarré :

```
http://localhost:8080/swagger-ui.html
```

## Équipe

| Rôle | Nom |
|---|---|
| Développement | Douae Rohan |
| Développement | Sanae Tafraouti |
| Encadrement | Oussama El Hajjamy |

---