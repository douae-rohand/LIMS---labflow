# CMD-13 — Rapport de lecture de code (Espace Technicien)

> Lecture 100 % réelle. Aucun nom inventé. Toutes les citations sont extraites des fichiers lus.

---

## SYNTHÈSE RAPIDE

| Point | Constat |
|---|---|
| Dernier numéro migration tenant | **V30** (`V30__produit_actif.sql`) |
| Prochain numéro tenant libre | **V31** |
| Dernier numéro migration central | **V36** (`V36__permissions_delegables_et_rbac.sql`) |
| Prochain numéro central libre | **V37** |
| `technicien_id` déjà posé | Oui, dans `PlanificationService.creer()` et `EssaiService.creer()` |
| Permissions TECHNICIEN existantes | `DEMANDES_SUIVI`, `ESSAIS_GERER`, `PLANIFICATION_STATUT`, `STOCK_CONSULTER`, `STOCK_GERER`, `CONFORMITE_CONSULTER`, `CONFORMITE_DECLARER`, `IA_GERER` |
| Permissions manquantes pour CMD-13 | `VALIDATION_TECHNIQUE_GERER` (nouvelle, niveau 1), `ESSAIS_IMPORT_CSV` (nouvelle) |
| Doublon principal à surveiller | `EssaiService.saisirResultat` + `EssaiController.PATCH /{id}/resultat` chevauchent la saisie manuelle |
| `ValidationController.decider` | Bug : prend `validateurId` en **param URL** (violation règle « jamais du param ») |

---

## A. Colonnes réelles de `ligne_essai` (V10) + enum `StatutEssai`

### Table `ligne_essai` (fichier : `V10__create_ligne_essai.sql`)

| Colonne | Type | Nullable | Remarque |
|---|---|---|---|
| `id_ligne_essai` | BIGINT PK AUTO_INCREMENT | NON | |
| `code` | VARCHAR(50) UNIQUE | NON | |
| `statut` | VARCHAR(50) | OUI | valeurs : `ATTENTE_ECHANTILLON`, `EN_COURS`, `TERMINE`, `NON_REALISABLE` |
| `date_attribution` | TIMESTAMP | OUI | |
| `date_debut` | TIMESTAMP | OUI | |
| `date_fin` | TIMESTAMP | OUI | |
| `duree_minutes` | INT | OUI | temps passé déclaré |
| `valeur` | VARCHAR(255) | OUI | résultat saisi |
| `date_saisie` | TIMESTAMP | OUI | |
| `conformite` | TINYINT(1) | OUI | calculé serveur |
| `score_anomalie` | DECIMAL(5,2) | OUI | calculé par IA |
| `montant` | DECIMAL(12,2) | NON | ne jamais exposer au technicien |
| `motif` | TEXT | OUI | motif non-réalisable ou commentaire |
| `demande_id` | BIGINT FK | NON | → `demande.id_demande` |
| `essai_id` | BIGINT FK | NON | → `essai.id_essai` |
| `echantillon_id` | BIGINT FK | OUI | → `echantillon.id_echantillon` |
| `technicien_id` | BIGINT | OUI | **SANS FK** (colonne logique) |

### Entité Java `LigneEssai` (fichier : `LigneEssai.java`)

Champs Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`. Champ `technicienId` annoté `@Column(name = "technicien_id")`, type `Long`. `statut` converti par `@Convert(converter = StatutEssaiConverter.class)`. `echantillon` est un `@ManyToOne(fetch = FetchType.LAZY)` nullable.

### enum `StatutEssai` (fichier : `StatutEssai.java`)

Valeurs exposées : **`ATTENTE_ECHANTILLON`**, **`EN_COURS`**, **`TERMINE`**, **`NON_REALISABLE`**. Aucune autre valeur dans l'enum.

### `StatutEssaiConverter` (fichier : `StatutEssaiConverter.java`)

Relecture des noms historiques depuis la base :
- `"EN_ATTENTE"`, `"PLANIFIE"`, `"PLANIFIEE"`, `"REPORTEE"` → `ATTENTE_ECHANTILLON`
- `"A_REFAIRE"`, `"EN_COURS"` → `EN_COURS`
- `"TERMINE"`, `"TERMINEE"` → `TERMINE`
- `"ANNULE"`, `"ANNULEE"` → `NON_REALISABLE`

### `CorrespondanceStatutLigne` (fichier : `CorrespondanceStatutLigne.java`)

- `versLigne(StatutPlanification)` : `PLANIFIEE/REPORTEE` → `ATTENTE_ECHANTILLON` ; `EN_COURS` → `EN_COURS` ; `TERMINEE` → `TERMINE` ; `ANNULEE` → `NON_REALISABLE`
- `versPlanification(StatutEssai)` : inverse.

---

## B. Colonnes réelles de `essai` (V4) : limites, seuils, durée

### Table `essai` (fichier : `V4__create_essai.sql`)

| Colonne | Type | Nullable |
|---|---|---|
| `id_essai` | BIGINT PK AUTO_INCREMENT | NON |
| `code` | VARCHAR(50) UNIQUE | NON |
| `designation` | VARCHAR(255) | NON |
| `description` | TEXT | OUI |
| `methode` | VARCHAR(255) | OUI |
| `tarif` | DECIMAL(12,2) DEFAULT 0 | OUI |
| `duree_estimee` | INT | NON |
| `unite` | VARCHAR(50) | OUI |
| `limite_min` | DECIMAL(12,4) | OUI |
| `limite_max` | DECIMAL(12,4) | OUI |
| `seuil_critique_min` | DECIMAL(12,4) | OUI |
| `seuil_critique_max` | DECIMAL(12,4) | OUI |
| `actif` | TINYINT(1) DEFAULT 1 | OUI |
| `domaine_id` | BIGINT FK | OUI |

### Entité Java `Essai` (fichier : `Essai.java`)

Champs Lombok identiques. Champs Java : `limiteMin`, `limiteMax`, `seuilCritiqueMin`, `seuilCritiqueMax` (BigDecimal), `dureeEstimee` (Integer), `unite`, `designation`, `methode`, `domaine` (@ManyToOne LAZY). **`tarif` présent dans l'entité** — ne jamais exposer au technicien.

### Règle de classification (lue dans `EssaiService.saisirResultat`) — implémentation actuelle
```
double val = Double.parseDouble(dto.getResultatValeur());
ligne.setConformite(val >= dto.getValeurMin() && val <= dto.getValeurMax());
```
La classification utilise `limite_min/limite_max`. `seuil_critique_min/max` ne sont **pas encore utilisés** dans ce service. CMD-13 devra implémenter : CONFORME si `[limite_min, limite_max]`, NON_CONFORME si hors limite mais dans `[seuil_critique_min, seuil_critique_max]`, CRITIQUE si hors seuils critiques.

---

## C. Colonnes réelles de `echantillon` (V8 + V26)

### Table `echantillon` au départ (V8)

| Colonne | Type |
|---|---|
| `id_echantillon` | BIGINT PK AUTO_INCREMENT |
| `reference` | VARCHAR(50) UNIQUE |
| `nature` | VARCHAR(100) NULL → **NOT NULL** (V26) |
| `date_reception` | TIMESTAMP NULL → **DATETIME NOT NULL** (V26) |
| `conformite` | TINYINT(1) NULL |
| `motif` | TEXT NULL |
| `conditions_conservation` | TEXT NULL → **renommée `etat_conservation_constate`** (V26) |
| `demande_id` | BIGINT FK NOT NULL |
| `receptionneur_id` | BIGINT NULL → **NOT NULL** (V26) |

### Entité Java `Echantillon` (fichier : `Echantillon.java`)

```java
private String reference;          // VARCHAR(50)
private String nature;             // VARCHAR(100) NOT NULL
private Instant dateReception;
private Boolean conformite;        // null si non évalué
private String motif;              // texte non conformité
private String etatConservationConstate; // ex-conditions_conservation
private Demande demande;           // @ManyToOne LAZY
private Long receptionneurId;
```

> **Point important** : il n'existe pas de champ `statutEchantillon` ni de champ `etat_constate` séparé. L'état constaté = `etatConservationConstate` (TEXT). La conformité est un booléen.

---

## D. Colonnes réelles de `validation` (V12)

### Table `validation` (fichier : `V12__create_validation.sql`)

| Colonne | Type | Nullable |
|---|---|---|
| `id_validation` | BIGINT PK AUTO_INCREMENT | NON |
| `code` | VARCHAR(50) UNIQUE | NON |
| `niveau` | INT | NON |
| `decision` | VARCHAR(50) | OUI |
| `motif` | TEXT | OUI |
| `date_validation` | TIMESTAMP | NON |
| `signature` | VARCHAR(255) | OUI |
| `ligne_essai_id` | BIGINT FK | NON |
| `validateur_id` | BIGINT | NON |

> **Absence de colonne `action`** : le prompt CMD-13 mentionne `ActionValidation` mais aucune colonne `action` n'existe dans `validation`. Le champ `decision` porte ce rôle. L'enum `StatutValidation` vaut : **`EN_ATTENTE`**, **`VALIDEE`**, **`REJETEE`**, **`REVISION_DEMANDEE`**. Il n'y a pas d'enum `NiveauValidation` ni d'enum `ActionValidation` dans le code lu — le `niveau` est un INT brut (1 = technique, 2 = responsable d'après la seed V22 `niveau=2`).

### Entité Java `Validation` (fichier : `Validation.java`)

- `id`, `code`, `niveau` (Integer), `decision` (String), `motif`, `dateValidation`, `signature`, `ligneEssai` (@ManyToOne), `validateurId` (Long)

---

## E. Colonnes réelles de `lot` (V6) et `consommation_lot` (V11)

### Table `lot` (fichier : `V6__create_lot.sql`)

| Colonne | Type |
|---|---|
| `id_lot` | BIGINT PK AUTO_INCREMENT |
| `numero` | VARCHAR(50) UNIQUE par produit |
| `quantite` | DECIMAL(10,3) |
| `peremption` | DATE |
| `statut` | VARCHAR(50) |
| `produit_id` | BIGINT FK → `produit` |

### Table `consommation_lot` (fichier : `V11__create_consommation_lot.sql`)

| Colonne | Type |
|---|---|
| `ligne_essai_id` | BIGINT FK PK → `ligne_essai` |
| `lot_id` | BIGINT FK PK → `lot` |
| `quantite_consommee` | DECIMAL(10,3) |

> La PK est composite `(ligne_essai_id, lot_id)`. Donc **plusieurs lots peuvent être consommés par une même ligne d'essai** mais un lot donné ne peut apparaître qu'une fois par ligne.

### Table `produit` (fichier : `V5__create_produit.sql`)

| Colonne | Type |
|---|---|
| `id_produit` | BIGINT PK |
| `reference` | VARCHAR(50) UNIQUE |
| `nom` | VARCHAR(255) |
| `unite` | VARCHAR(50) |
| `seuil_minimal` | DECIMAL(10,3) |
| `conditions_stockage` | TEXT |

> `actif` ajouté dans V30 (`ALTER TABLE produit ADD COLUMN actif TINYINT(1) NOT NULL DEFAULT 1`).

---

## F. PlanificationController/Service : est-ce que `technicien_id` est posé là ?

### Réponse : OUI, `technicien_id` est posé dans `PlanificationService.creer()`

Fichier `PlanificationService.java`, méthode `creer(PlanificationDto dto)` :
```java
LigneEssai ligne = LigneEssai.builder()
    ...
    .technicienId(dto.getTechnicienId())
    ...
    .build();
```
Le `technicienId` vient de `dto.getTechnicienId()`, lui-même issu du corps de la requête POST `/api/planifications`. La sécurité de ce DTO n'est PAS vérifiée (pas de contrôle que l'appelant est bien un responsable). La permission requise est `PERM_PLANIFICATION_GERER` (attribuée à RESPONSABLE et ADMINISTRATEUR par V35, pas à TECHNICIEN). Donc dans le flux normal, c'est le Responsable qui pose `technicien_id` via cet endpoint.

### Méthode `listerParTechnicien` déjà existante dans `PlanificationService`

```java
public Page<PlanificationDto> listerParTechnicien(Long technicienId, Pageable pageable) {
    return ligneEssaiRepository.findByTechnicienId(technicienId, pageable).map(this::toDto);
}
```
**Attention** : cette méthode prend `technicienId` en paramètre (violation de la règle « jamais du paramètre »). Elle n'est référencée dans aucun Controller actuellement. Elle sera réutilisable mais doit être adaptée pour CMD-13 (le technicienId doit venir du principal, pas d'un paramètre).

### `PlanificationService.changerStatut()`

Réserve le passage à TERMINEE aux rôles RESPONSABLE et ADMINISTRATEUR via :
```java
RoleUtilisateur role = SecurityUtils.principalCourant().getRole();
if (role != RoleUtilisateur.RESPONSABLE && role != RoleUtilisateur.ADMINISTRATEUR) {
    throw new BusinessRuleException(...);
}
```

> **Conclusion** : Ne pas modifier `PlanificationController`/`PlanificationService`. L'attribution est bien de la responsabilité du Responsable. CMD-13 lit `technicien_id` déjà posé en base.

---

## G. ValidationService méthodes exactes

Fichier : `ValidationService.java`

| Méthode | Signature | Ce qu'elle fait |
|---|---|---|
| `listerParEssai` | `List<ValidationDto> listerParEssai(Long essaiId)` | Lit toutes les validations d'une ligne ; filtre par propriété client si connecté |
| `valider` | `ValidationDto valider(Long id, Long validateurId, String commentaire, StatutValidation decision)` | Met à jour une validation existante (change decision, motif, validateurId, dateValidation) |
| `creer` | `ValidationDto creer(ValidationDto dto)` | Crée une nouvelle ligne `validation` avec `niveau=1`, code UUID, décision depuis le DTO |

**Problèmes observés pour CMD-13** :
1. `valider()` prend `validateurId` en paramètre → violation règle de sécurité. CMD-13 doit résoudre le validateur depuis `SecurityUtils.principalCourant()`.
2. `creer()` ne change pas le `statut` de `ligne_essai` après création (pas de passage à TERMINE). CMD-13 devra soit étendre ce service, soit créer sa propre logique.
3. `valider()` ne vérifie pas que la ligne a un résultat saisi avant de valider.
4. `creer()` accepte n'importe quel `dto.getStatut()` — le technicien pourrait envoyer `REJETEE` si on réutilise cet endpoint tel quel. CMD-13 doit créer un endpoint dédié qui force `decision = VALIDEE`.

---

## H. ConformiteService méthodes exactes

Fichier : `ConformiteService.java`

| Méthode | Signature | Ce qu'elle fait |
|---|---|---|
| `lister` | `Page<NonConformiteDto> lister(StatutNonConformite statut, Pageable pageable)` | Liste les échantillons non conformes (conformite=false) |
| `declarer` | `NonConformiteDto declarer(NonConformiteDto dto)` | Met `echantillon.conformite = false` et pose le motif |
| `traiter` | `NonConformiteDto traiter(Long id, String actionCorrective, Long responsableId)` | Met `conformite = false`, nouveau motif |
| `cloturer` | `NonConformiteDto cloturer(Long id)` | Met `conformite = true` |

**Point important** : `ConformiteService` opère sur `Echantillon`, pas sur `LigneEssai`. La conformité de l'échantillon (vérifiée au démarrage d'un essai) est `Echantillon.conformite` (Boolean). La conformité du résultat de l'essai est `LigneEssai.conformite` (Boolean). Ce sont **deux champs distincts** portés par deux entités différentes.

---

## I. DemandeService : transactions, méthodes liées au statut

Fichier : `DemandeService.java`

### Pattern de transaction
- Méthodes client : utilisent `TenantExecutor.inTenant()` et `inCentral()`, chaque bloc dans une `PROPAGATION_REQUIRES_NEW` via `TransactionTemplate`.
- Méthodes laboratoire : `@Transactional` classique sur le schéma courant.
- Audit : résolu dans le thread appelant, planifié via `TransactionSynchronizationManager.registerSynchronization` → `afterCommit()`, ou via `ApresCommit.executer()` dans `JournalAuditService`.

### Méthodes liées au statut de demande

| Méthode | Transition |
|---|---|
| `changerStatut(Long id, StatutDemande statut)` | Générique ; protège ACCEPTEE/REFUSEE/EN_VALIDATION/TERMINEE par rôle |
| `accepter(Long id)` | NOUVEAU → ACCEPTEE (RESPONSABLE requis) |
| `refuser(Long id, String motif)` | NOUVEAU → REFUSEE (RESPONSABLE requis) |
| `annuler(Long utilisateurId, Long laboratoireId, Long demandeId, String motif)` | NOUVEAU → ANNULEE (client) |

### Transition EN_COURS et EN_VALIDATION

**IMPORTANT** : Aucune méthode dans `DemandeService` ne gère les transitions `ACCEPTEE → EN_COURS` ni `EN_COURS → EN_VALIDATION`. Ces transitions sont à implémenter dans le TechnicienService de CMD-13, selon les règles :
- ACCEPTEE → EN_COURS : au premier `demarrer()` d'une ligne de la demande
- EN_COURS → EN_VALIDATION : quand toutes les lignes sont TERMINE ou NON_REALISABLE

### `StatutDemande` valeurs et mapping

```java
NOUVEAU, ACCEPTEE, EN_COURS, EN_VALIDATION, TERMINEE, ANNULEE, REFUSEE
```
La méthode `StatutDemande.depuis(String)` et `aliasesPersistés()` gèrent les anciens noms.

---

## J. Catalogue réel des permissions (V35 + V36)

### Permissions TECHNICIEN par défaut (V35, table `role_permission`)

```
DEMANDES_SUIVI        (DEMANDES / SUIVI)
ESSAIS_GERER          (ESSAIS / GERER)
PLANIFICATION_STATUT  (PLANIFICATION / STATUT)
STOCK_CONSULTER       (STOCK / CONSULTER)
STOCK_GERER           (STOCK / GERER)
CONFORMITE_CONSULTER  (CONFORMITE / CONSULTER)
CONFORMITE_DECLARER   (CONFORMITE / DECLARER)
IA_GERER              (IA / GERER)
```

### Permissions manquantes pour CMD-13 — à proposer en V37 (central)

| Code proposé | Module | Action | Libellé | Délégable | Rôle par défaut |
|---|---|---|---|---|---|
| `VALIDATION_TECHNIQUE_GERER` | `VALIDATION_TECHNIQUE` | `GERER` | Soumettre une validation technique (niveau 1) | OUI | TECHNICIEN |
| `ESSAIS_IMPORT_CSV` | `ESSAIS` | `IMPORT_CSV` | Importer des résultats par CSV | OUI | TECHNICIEN |

> Note : `VALIDATION_GERER` (V35) est attribuée à RESPONSABLE et ADMINISTRATEUR — pas au TECHNICIEN. Pour la validation technique (niveau 1), une permission distincte est nécessaire pour éviter que le technicien n'accède aux endpoints de validation responsable.

### Permissions non-délégables (V36)

Les permissions suivantes ont `deleguable = 0` : `EMPLOYES_CONSULTER`, `EMPLOYES_GERER`, `CATALOGUE_CONSULTER`, `CATALOGUE_GERER`, `FACTURATION_CONSULTER`, `FACTURATION_GERER`, `FACTURATION_PAYER`, `STOCK_GERER`, `SATISFACTION_CONSULTER`, `NOTIFICATIONS_GERER`, `PARAMETRES_LABORATOIRE_GERER`, `AUDIT_CONSULTER`, `INDICATEURS_CONSULTER`, `EXPORT_FACTURES`, `EXPORT_AUDIT`, `EXPORT_EMPLOYES`.

### Alerte V37 dans `AutorisationService`

La méthode `verifierCatalogue()` (annotée `@EventListener(ApplicationReadyEvent.class)`) vérifie que toutes les permissions de la liste `ATTENDUES` sont présentes au démarrage. La liste actuelle contient 32 codes ; elle devra être étendue à 34 après V37.

---

## K. Dernier numéro de migration tenant et central

| Schéma | Dernier fichier présent | Dernier numéro | **Prochain libre** |
|---|---|---|---|
| **tenant** | `V30__produit_actif.sql` | 30 | **V31** |
| **central** | `V36__permissions_delegables_et_rbac.sql` | 36 | **V37** |

> Note : il n'y a pas de V24 tenant (le numéro est absent de la liste — sauté entre V23 et V25). CMD-13 commence à V31 tenant et V37 central.

---

## L. SecurityUtils : méthode pour récupérer l'utilisateur courant

Fichier : `SecurityUtils.java`

```java
public static UtilisateurPrincipal principalCourant()
```

- Lit depuis `SecurityContextHolder.getContext().getAuthentication()`
- Retourne `UtilisateurPrincipal` (implements `UserDetails`)
- Lance `BusinessRuleException("UTILISATEUR_NON_AUTHENTIFIE", ...)` si non authentifié

### Champs utiles de `UtilisateurPrincipal`

| Champ | Type | Utilité CMD-13 |
|---|---|---|
| `id` | Long | → technicien_id à filtrer |
| `email` | String | audit |
| `role` | RoleUtilisateur | vérification rôle |
| `laboratoireId` | Long | lab du technicien |
| `nomSchema` | String | tenant courant |
| `permissions` | Set\<String\> | codes sans préfixe PERM_ |
| `isEmployeLaboratoire()` | boolean | vrai pour TECHNICIEN |

> `SecurityUtils.principalCourant().getId()` est la valeur à utiliser comme `technicien_id` dans **toutes** les requêtes CMD-13.

---

## M. JournalAuditService : signatures des méthodes `enregistrer`

Fichier : `JournalAuditService.java`

```java
// Surcharge 1 : à partir d'un JournalAudit pré-construit
public void enregistrer(JournalAudit entree)

// Surcharge 2 : action simple, pas de avant/après
public void enregistrer(String action, String nomEntite, Long idEntite,
                        String utilisateur, String description)

// Surcharge 3 : avec avant/après, sans laboratoireId
public void enregistrer(String action, String nomEntite, Long idEntite, String acteurEmail,
                        String valeurAvant, String valeurApres, String motif)

// Surcharge 4 : avec avant/après ET laboratoireId (recommandée pour CMD-13)
public void enregistrer(String action, String nomEntite, Long idEntite, String acteurEmail,
                        String valeurAvant, String valeurApres, String motif, Long laboratoireId)
```

**Comportement** : toutes les surcharges planifient l'écriture via `ApresCommit.executer()` qui utilise un `@Async` + `REQUIRES_NEW`. L'acteur (email) doit être résolu **dans le thread appelant** avant l'appel (le contexte de sécurité n'est pas propagé dans le thread async).

**Surcharge à privilégier pour CMD-13** : surcharge 4 avec `laboratoireId = principal.getLaboratoireId()`.

---

## N. Pattern pagination EmployeController (Pageable, taille, tri)

Fichier : `EmployeController.java` — `GET /api/employes` sans `Pageable` Spring injecté.

Fichier : `UtilisateurService.listerEmployes()` — pattern réel :

```java
// 1. Plafonnement
int taillePlafonnee = Math.min(Math.max(1, size), 50); // size ≤ 50
int pageAjustee = Math.max(0, page);

// 2. Liste blanche des champs de tri
Set<String> champsTriAutorises = Set.of(
    "nom", "prenom", "matricule", "dateCreation", "derniereConnexion"
);
if (!champsTriAutorises.contains(champTri)) {
    throw new BusinessRuleException("TRI_INVALIDE", ...);
}

// 3. Direction
Sort.Direction sortDirection = "asc".equals(dirNorm) ? ASC : DESC;
// si autre → BusinessRuleException("DIRECTION_TRI_INVALIDE", ...)

// 4. Tri secondaire stable par id
Sort sort = Sort.by(sortDirection, champTri).and(Sort.by(Sort.Direction.ASC, "id"));
Pageable pageable = PageRequest.of(pageAjustee, taillePlafonnee, sort);
```

**À reproduire dans CMD-13** : le `Pageable` est **construit manuellement** (pas le `Pageable` Spring injecté). Taille max = 50. Tri secondaire par `id` pour la stabilité. Liste blanche explicite.

---

## O. EssaiController et EssaiService : méthodes existantes (doublons avec CMD-13)

### `EssaiController` — fichier `EssaiController.java`

| Endpoint | Permission | Ce qu'il fait |
|---|---|---|
| `GET /api/essais/{id}` | aucune | Lit une LigneEssai par id (ou essai catalogue si pas client) |
| `GET /api/essais/echantillon/{echantillonId}` | aucune | Liste les lignes d'un échantillon |
| `POST /api/essais` | `PERM_ESSAIS_GERER` | Crée une LigneEssai depuis un DTO |
| `PATCH /api/essais/{id}/resultat` | `PERM_ESSAIS_GERER` | Saisit un résultat (valeur + statut TERMINE) |

### `EssaiService` — méthodes

| Méthode | Ce qu'elle fait réellement |
|---|---|
| `trouverParId(Long id)` | Si client connecté : vérifie propriété demande ; sinon : charge par id (sans filtre technicien_id) |
| `listerParEchantillon(Long echantillonId)` | Liste les lignes d'un échantillon, avec vérif propriété client |
| `creer(EssaiDto dto)` | Crée LigneEssai + pose technicienId depuis dto (pas depuis principal) |
| `saisirResultat(Long id, EssaiDto dto)` | Pose valeur + TERMINE + calcul conformité simplifié (limite_min/max depuis DTO, pas depuis l'essai) |

### Doublons critiques avec CMD-13

1. **`PATCH /api/essais/{id}/resultat`** chevauche `PUT /api/technicien/essais/{id}/resultat`. Différences nécessaires pour CMD-13 : (a) filtre par `technicien_id = principal.id`, (b) classification SERVEUR basée sur colonnes réelles de `essai` (pas sur les valeurs du DTO), (c) motif obligatoire si résultat déjà saisi, (d) refus si validé par Responsable.
2. **`GET /api/essais/{id}`** ne filtre pas par technicien. CMD-13 doit le refaire avec filtre.
3. **`POST /api/essais`** n'est pas pertinent pour CMD-13 (creation de ligne par le Responsable/planification).

> **Recommandation** : ne pas modifier les endpoints existants `/api/essais`. Créer un nouveau contrôleur `TechnicienController` sous `/api/technicien/essais` avec sa propre logique. Les deux coexistent sans conflit de mapping.

---

## P. Lien `echantillon` → `ligne_essai` : quelle colonne/entité fait le lien ?

### Lien direct via `ligne_essai.echantillon_id`

Dans `V10__create_ligne_essai.sql` :
```sql
echantillon_id BIGINT NULL,
CONSTRAINT fk_ligne_essai_echantillon
    FOREIGN KEY (echantillon_id) REFERENCES echantillon (id_echantillon)
```

Dans `LigneEssai.java` :
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "echantillon_id")
private Echantillon echantillon;
```

### Lien inverse via `demande`

`Echantillon.demande` → `LigneEssai.demande` (même `demande_id`). Un échantillon et les lignes d'essai partagent la même demande, mais le lien direct est `ligne_essai.echantillon_id`.

### Exigences d'échantillon (table `exigence_echantillon`, V25)

```java
public class ExigenceEchantillon {
    private Essai essai;
    private String nature;              // nature de l'échantillon requis
    private String etatConservation;    // condition de conservation attendue
    private BigDecimal quantiteMinimale;
    private String unite;
}
```
Lien : `exigence_echantillon.essai_id` → `essai.id_essai`. Repository : `ExigenceEchantillonRepository.findByEssai_Id(Long essaiId)`.

> **Conclusion** : Pour vérifier qu'une ligne a un échantillon attaché avant `demarrer()`, il suffit de tester `ligne.getEchantillon() != null`. Pour exposer les exigences d'échantillon au technicien, utiliser `ExigenceEchantillonRepository.findByEssai_Id(ligne.getEssai().getId())`.

---

## CONCLUSIONS ET POINTS À CONFIRMER POUR LE PLAN

### Points techniques confirmés

1. **`technicien_id` sans FK** en base — correctement annoté `@Column(name = "technicien_id")` sans `@ManyToOne` dans `LigneEssai.java`. Le filtre sera `WHERE le.technicien_id = :id`.

2. **`duree_minutes` stocke UNE valeur** (INT, pas une liste). Il n'existe pas de table de sessions de temps séparée. Donc **plusieurs déclarations de temps ne sont pas possibles** — la dernière valeur remplace. À documenter dans le plan.

3. **`score_anomalie`** : colonne `DECIMAL(5,2)` présente dans `ligne_essai`, champ `scoreAnomalie` dans l'entité. Pas de calcul implémenté (aucun service ne le peuple actuellement). CMD-13 ne le calcule pas mais peut le lire/exposer.

4. **Niveau de validation** : dans la seed V22, `niveau=2` pour une validation responsable. CMD-13 doit donc écrire `niveau=1` pour la validation technique.

5. **`ValidationController.decider()`** prend `validateurId` en `@RequestParam` — violation des règles du projet. Ne pas imiter ce pattern. Dans CMD-13, le validateur = `SecurityUtils.principalCourant().getId()`.

6. **La demande n'a pas de méthode `passerEnCours()` ni `passerEnValidation()`**. Ces transitions doivent être implémentées en SQL conditionnel dans le `TechnicienService` (UPDATE atomique).

7. **`exigence_echantillon`** est la seule table exprimant les contraintes sur l'échantillon. Colonnes : `nature`, `etat_conservation`, `quantite_minimale`, `unite`.

### Points à confirmer avant d'écrire le code

1. **Plafond de durée configurable** (POST `/{id}/temps`) : quelle propriété Spring à utiliser ? Suggéré : `lims.technicien.duree-max-minutes=480` dans `application.properties`.

2. **Taille max du fichier CSV** (import) : `spring.servlet.multipart.max-file-size` existe déjà ? Ou propriété custom ?

3. **Calcul `enRetard`** : quelle est la règle exacte ? Suggéré : `date_attribution + essai.duree_estimee (minutes) < now()` et statut != TERMINE.

4. **Plafond de la vue planning** : 62 jours confirmé dans le prompt.

5. **La modification de résultat après validation technique** remet la ligne en quel statut ? Suggéré : `EN_COURS` (résultat saisi mais pas encore re-validé) avec `conformite = null` et une nouvelle entrée audit.

6. **Le format CSV** : les colonnes `code_ligne, valeur, commentaire` sont précisées dans le prompt. Le séparateur `;` ou `,` auto-détecté.

### Recommandations

- Créer `TechnicienController` sous `/api/technicien/essais` (nouveau fichier, pas de modification des controllers existants).
- Créer `TechnicienService` injectable par `LigneEssaiRepository`, `ValidationRepository`, `EchantillonRepository`, `ExigenceEchantillonRepository`, `DemandeRepository`, `JournalAuditService`.
- La méthode `PlanificationService.listerParTechnicien(Long, Pageable)` existe mais prend un paramètre — ne pas la réutiliser telle quelle ; créer une méthode `listerMesEssais(Long technicienId, filtres...)` dans `TechnicienService` qui résout le technicienId depuis `SecurityUtils`.
- La validation technique (niveau 1) nécessite une permission nouvelle `VALIDATION_TECHNIQUE_GERER` + migration V37 central.
- Le seed de test pour `technicien_id` (migration V31 tenant) doit UPDATE les lignes existantes de la seed V22 en posant le technicien du lab (premier utilisateur TECHNICIEN du schéma).
- Ne jamais exposer `montant`, `tarif`, `client`, `decideurId` dans les DTOs du TechnicienController.
