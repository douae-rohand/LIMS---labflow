# CMD-02 — Panier client en base centrale (backend)

## Recherche sur le dépôt (EXÉCUTÉ)

### Numéro de migration central libre
Liste de `db/migration/central` : V1 à V30 toutes présentes. **V31 est le prochain numéro libre.**

### Tables centrales référencées
- `utilisateur` (`schema = lims_central`), PK `id_utilisateur` — [Utilisateur.java#L19-L28](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/utilisateur/entity/Utilisateur.java#L19-L28)
- `laboratoire` (`schema = lims_central`), PK `id_laboratoire` — [Laboratoire.java#L15-L24](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/plateforme/entity/Laboratoire.java#L15-L24)

### Pattern moteur/charset/collation des migrations centrales
Extrait de [V19__create_client_laboratoire.sql](file:///c:/Projet_GI3/LIMS/backend/src/main/resources/db/migration/central/V19__create_client_laboratoire.sql) :
```sql
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### `TenantExecutor` — comportement transactionnel et exceptions
[TenantExecutor.java#L29-L47](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/tenant/TenantExecutor.java#L29-L47) :
```java
private <T> T executer(String nomSchema, Supplier<T> action) {
    String precedent = TenantContext.getCurrentTenant();
    // ... positionne le contexte ...
    try {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());   // <-- transaction REQUIRES_NEW
    } finally {
        // restaure le contexte précédent
    }
}
```
- Chaque appel `inCentral`/`inTenant` ouvre sa **propre transaction `REQUIRES_NEW`** (commit/rollback indépendant).
- **Comportement sur exception** : `TransactionTemplate.execute()` intercepte toute `RuntimeException` (et sous-types, y compris `DataIntegrityViolationException`) levée dans le `Supplier`, marque la transaction en rollback, et **repropage l'exception** au code appelant. Le `finally` restaure quand même le `TenantContext`. L'exception n'est **pas** encapsulée. Aucune vérification de `rollbackOnly` n'est possible car la transaction est déjà terminée (commit ou rollback) à la sortie de `execute()`.
- **Conséquence** : une fois qu'un `inCentral` a levé `DataIntegrityViolationException`, sa transaction est rollbackée. La relecture de l'état doit donc se faire dans un **NOUVEL** `inCentral` (nouvelle transaction).

### `SecurityUtils.principalCourant()`
[SecurityUtils.java#L13-L20](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/security/SecurityUtils.java#L13-L20) : retourne `UtilisateurPrincipal`, lance `BusinessRuleException("UTILISATEUR_NON_AUTHENTIFIE")` si non authentifié.

### `UtilisateurPrincipal` pour un CLIENT
[UtilisateurPrincipal.java#L17-L26](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/auth/security/UtilisateurPrincipal.java#L17-L26) : `nomSchema` et `laboratoireId` sont **null** pour un client.

### `tenantId` dans le JWT et la réponse de login
- [JwtTokenProvider.java#L33-L35](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/auth/security/JwtTokenProvider.java#L33-L35) : claim `tenantId` ajouté uniquement si `principal.getNomSchema() != null`. Pas de claim pour un client.
- [AuthService.java#L277](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/auth/service/AuthService.java#L277) : `LoginResponse.tenantId = principal.getNomSchema()` → null pour un client.
- Frontend [auth.ts#L318](file:///c:/Projet_GI3/LIMS/frontend/src/api/auth.ts#L318) : `tenantId: r.tenantId ?? ''` → chaîne vide pour un client.
- [axios.ts#L149-L151](file:///c:/Projet_GI3/LIMS/frontend/src/api/axios.ts#L149-L151) + [#L310-L312](file:///c:/Projet_GI3/LIMS/frontend/src/api/axios.ts#L310-L312) : `setTenant('')` → `currentTenant = ''`. L'intercepteur envoie `X-Tenant-ID: ''` car la condition est `this.currentTenant !== null`.

### `TenantInterceptor` — routes `/api/client/paniers/**` sans `X-Tenant-ID`
[TenantInterceptor.java#L58-L81](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/tenant/TenantInterceptor.java#L58-L81) :
```java
if (principal.isClient()) {
    if (!StringUtils.hasText(header)) {            // '' → false
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/client/demandes") || uri.startsWith("/api/client/factures")) {
            throw new TenantHeaderRequiredException(...);
        }
        TenantContext.clear();                     // contexte central
        return true;
    }
    Laboratoire laboratoire = tenantResolver.exigerActif(header);
    Optional<ClientLaboratoire> clientLab = clientLaboratoireRepository
            .findByUtilisateur_IdAndLaboratoire_Id(principal.getId(), laboratoire.getId());
    if (clientLab.isEmpty() || !"ACTIF".equalsIgnoreCase(clientLab.get().getStatut())) {
        throw new UnauthorizedTenantException(...);
    }
    TenantContext.setCurrentTenant(laboratoire.getNomSchema());
    return true;
}
```
- **`X-Tenant-ID: ''` fonctionne par hasard** : `StringUtils.hasText('')` retourne `false`, ce qui fait entrer dans la branche "pas d'en-tête". La liste des URI exigeant l'en-tête ne contient que `/api/client/demandes` et `/api/client/factures`, donc `/api/client/paniers/**` passe en contexte central. Si la condition était `header != null` au lieu de `StringUtils.hasText(header)`, ça échouerait.
- Si le frontend envoie un `X-Tenant-ID` non vide : `exigerActif` trouve le labo, puis `clientLaboratoireRepository` cherche une liaison `client_laboratoire`. **Comme le panier ne crée pas de `client_laboratoire`, la liaison est absente → `UnauthorizedTenantException` (403).**
- **Recommandation (non appliquée dans ce lot)** : ajouter `/api/client/paniers` à la liste blanche en commentaire de l'intercepteur pour documenter que ces routes sont censées rester en contexte central. À ce stade, on ne modifie pas l'intercepteur.

### `MustChangePasswordFilter` et `SecurityConfig`
- [SecurityConfig.java#L105](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/config/SecurityConfig.java#L105) : `.requestMatchers("/api/client/**").hasRole("CLIENT")` → les routes panier nécessitent le rôle CLIENT. ✅
- [MustChangePasswordFilter.java#L48-L95](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/auth/security/MustChangePasswordFilter.java#L48-L95) : bloque les routes panier si `mustChangePassword == true` (seuls les endpoints auth sont exemptés). Comportement voulu.

### `Laboratoire.statut` — type réel
[Laboratoire.java#L71-L74](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/plateforme/entity/Laboratoire.java#L71-L74) : `statut` est un **`String`** (pas un enum), `@Builder.Default private String statut = "ACTIF"`. La colonne stocke le nom de `StatutLabo` (`ACTIF`, `SUSPENDU`, `DESACTIVE`). → `findByIdAndStatut(Long id, String statut)` prend un `String`.

### `Essai.actif` — type réel et nullabilité
[Essai.java#L59-L60](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/essai/entity/Essai.java#L59-L60) : `private Boolean actif = true;` (objet `Boolean`, nullable). Colonne [V4__create_essai.sql#L14](file:///c:/Projet_GI3/LIMS/backend/src/main/resources/db/migration/tenant/V4__create_essai.sql#L14) : `actif TINYINT(1) NULL DEFAULT 1`.
→ **`actif` peut être `null` en base**. La vérification `Boolean.TRUE.equals(essai.getActif())` traite `null` comme indisponible sans NPE.

### `exigerLaboratoireActif` — réutilisabilité
[PlateformeService.java#L100-L107](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/plateforme/service/PlateformeService.java#L100-L107) : `exigerLaboratoireActif(Long id)` est **`private`** dans `PlateformeService`. Non réutilisable.

**Choix retenu (option 2)** : ajouter `findByIdAndStatut(Long id, String statut)` à `LaboratoireRepository` et faire la vérification dans `PanierService`.
- Laboratoire introuvable → `ResourceNotFoundException` (404 générique).
- Laboratoire trouvé mais `statut != "ACTIF"` → `BusinessRuleException("LABORATOIRE_INDISPONIBLE", ...)` (422). **Uniquement pour l'ajout d'essai (POST).**
- Les `GET` et `DELETE` fonctionnent aussi sur un laboratoire inactif (le client doit pouvoir consulter et vider son panier).

### `EssaiRepository` — chargement en masse
[EssaiRepository.java](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/modules/essai/repository/EssaiRepository.java) actuel : `findByCode`, `existsByCode`, `findByActif`, `findFirstByActifTrue`, `findActifsAvecDomaine`.

**Méthode à ajouter** :
```java
@Query("SELECT e FROM Essai e LEFT JOIN FETCH e.domaine WHERE e.id IN :ids")
List<Essai> findByIdsAvecDomaine(@Param("ids") Collection<Long> ids);
```
⚠️ Jamais appelée avec une collection vide (le service vérifie `ids.isEmpty()` avant).

### `BusinessRuleException` — mapping HTTP
[BusinessRuleException.java](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/exception/BusinessRuleException.java) : `@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)` (422). [GlobalExceptionHandler.java#L45-L59](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/exception/GlobalExceptionHandler.java#L45-L59) → `ApiResponse.error("[" + code + "] " + message)`. Codes : `LABORATOIRE_INDISPONIBLE`, `ESSAI_INDISPONIBLE`, `PANIER_PLEIN`.

### `ResourceNotFoundException` — 404
[ResourceNotFoundException.java](file:///c:/Projet_GI3/LIMS/backend/src/main/java/com/backend/common/exception/ResourceNotFoundException.java) : 404, message générique.

---

## Fichiers et modules à créer / modifier

### Créés
- `backend/src/main/java/com/backend/modules/panier/entity/Panier.java`
- `backend/src/main/java/com/backend/modules/panier/entity/LignePanier.java`
- `backend/src/main/java/com/backend/modules/panier/repository/PanierRepository.java`
- `backend/src/main/java/com/backend/modules/panier/repository/LignePanierRepository.java`
- `backend/src/main/java/com/backend/modules/panier/dto/PanierResumeDto.java`
- `backend/src/main/java/com/backend/modules/panier/dto/PanierDetailDto.java`
- `backend/src/main/java/com/backend/modules/panier/dto/LignePanierDto.java`
- `backend/src/main/java/com/backend/modules/panier/dto/AjouterEssaiRequest.java`
- `backend/src/main/java/com/backend/modules/panier/service/PanierService.java`
- `backend/src/main/java/com/backend/modules/panier/controller/PanierController.java`
- `backend/src/main/resources/db/migration/central/V31__create_panier.sql`

### Modifiés
- `backend/src/main/java/com/backend/modules/essai/repository/EssaiRepository.java` — ajouter `findByIdsAvecDomaine`.
- `backend/src/main/java/com/backend/modules/plateforme/repository/LaboratoireRepository.java` — ajouter `findByIdAndStatut`.

### Justification du module `panier` vs `client`
Le panier est une ressource **transversale** qui n'appartient pas au domaine "client" (profil, facturation) ni au domaine "demande". Il est temporaire, stocké en central, et sert de tremplin vers la demande. Un module dédié `com.backend.modules.panier` évite de surcharger `modules/client` et garde une frontière claire : `panier` dépend de `essai`, `plateforme`, `common`, mais pas de `demande` ni `client`.

---

## Étapes d'implémentation (ordonnées)

### 1. Migration centrale `V31__create_panier.sql`
```sql
CREATE TABLE panier (
    id_panier BIGINT NOT NULL AUTO_INCREMENT,
    utilisateur_id BIGINT NOT NULL,
    laboratoire_id BIGINT NOT NULL,
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NOT NULL,
    PRIMARY KEY (id_panier),
    UNIQUE KEY uk_panier_utilisateur_laboratoire (utilisateur_id, laboratoire_id),
    KEY idx_panier_utilisateur (utilisateur_id),
    KEY idx_panier_laboratoire (laboratoire_id),
    CONSTRAINT fk_panier_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur),
    CONSTRAINT fk_panier_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ligne_panier (
    id_ligne_panier BIGINT NOT NULL AUTO_INCREMENT,
    panier_id BIGINT NOT NULL,
    essai_id BIGINT NOT NULL,
    date_ajout DATETIME NOT NULL,
    PRIMARY KEY (id_ligne_panier),
    UNIQUE KEY uk_ligne_panier_essai (panier_id, essai_id),
    KEY idx_ligne_panier_panier (panier_id),
    CONSTRAINT fk_ligne_panier_panier
        FOREIGN KEY (panier_id) REFERENCES panier (id_panier) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
⚠️ `essai_id` est une colonne logique **sans clé étrangère** (l'essai vit dans le schéma du laboratoire, pas dans `lims_central`).

### 2. Entités JPA
**`Panier`** — sans `@OneToMany` (les lignes se lisent par `LignePanierRepository`) :
```java
@Entity
@Table(name = "panier", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
    @UniqueConstraint(name = "uk_panier_utilisateur_laboratoire", columnNames = {"utilisateur_id", "laboratoire_id"})
})
public class Panier {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id_panier")
    private Long id;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;          // Long simple, pas @ManyToOne

    @Column(name = "laboratoire_id", nullable = false)
    private Long laboratoireId;          // Long simple, pas @ManyToOne

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", nullable = false)
    private Instant dateModification;
    // PAS de @OneToMany vers LignePanier
}
```

**`LignePanier`** — garde `@ManyToOne(LAZY)` vers `Panier` :
```java
@Entity
@Table(name = "ligne_panier", schema = SchemaConstants.CENTRAL_SCHEMA, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ligne_panier_essai", columnNames = {"panier_id", "essai_id"})
})
public class LignePanier {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id_ligne_panier")
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "panier_id", nullable = false)
    private Panier panier;

    @Column(name = "essai_id", nullable = false)
    private Long essaiId;                // Long simple (essai dans schéma tenant)

    @Column(name = "date_ajout", nullable = false)
    private Instant dateAjout;
}
```

### 3. Repositories
- `PanierRepository` : `findByUtilisateurIdAndLaboratoireId(Long, Long)`, `findByUtilisateurId(Long)`.
- `LignePanierRepository` : `findByPanierId(Long)`, `deleteByPanierIdAndEssaiId(Long, Long)`, `countByPanierId(Long)`.
- `EssaiRepository` : ajouter `findByIdsAvecDomaine(Collection<Long> ids)`.
- `LaboratoireRepository` : ajouter `findByIdAndStatut(Long id, String statut)`.

### 4. DTOs
- **`AjouterEssaiRequest`** : `@NotNull @Positive Long essaiId` (seul champ).
- **`LignePanierDto`** : `essaiId`, `code`, `designation`, `domaineLibelle`, `tarif` (BigDecimal), `dureeEstimee`, `disponible` (boolean). Champs descriptifs null si `disponible = false`.
- **`PanierResumeDto`** (pour `GET /paniers`) : `laboratoireId`, `laboratoireNom`, `nombreEssais`, `sousTotal` (BigDecimal, somme des tarifs des essais disponibles uniquement), `dateModification`.
- **`PanierDetailDto`** (pour `GET /paniers/{laboratoireId}` et POST/DELETE) : `laboratoireId`, `laboratoireNom`, `lignes` (List<LignePanierDto>), `sousTotal`, `dateModification`. Quand le panier est vide/supprimé : `lignes = []`, `sousTotal = 0`.

### 5. `PanierService` — détail transactionnel méthode par méthode

L'utilisateur vient **toujours** du principal (`SecurityUtils.principalCourant().getId()`). Aucune vérification d'utilisateur supplémentaire : les repositories filtrent par `(utilisateurId, laboratoireId)`, donc un panier d'un autre utilisateur n'est jamais retourné (considéré comme inexistant → 404 ou panier vide selon le contrat).

#### `listerPaniers(Long utilisateurId)`
1. **`inCentral`** : `panierRepository.findByUtilisateurId(utilisateurId)` → tous les paniers du client.
2. **`inCentral`** (même appel possible en regroupant) : `lignePanierRepository.findByPanierIdIn(panierIds)` → **toutes les lignes en une requête** (pas de N+1). Si aucun panier, cette requête est sautée.
3. **`inCentral`** : `laboratoireRepository.findAllById(laboratoireIds)` → tous les laboratoires en une requête.
4. Pour chaque laboratoire : **`inTenant(nomSchema)`** : `essaiRepository.findByIdsAvecDomaine(essaiIdsDuPanier)` → une requête par laboratoire. Si `essaiIds` est vide, l'appel est sauté (panier vide).
5. Construction des `PanierResumeDto` : `sousTotal` = somme des tarifs des essais `disponible` (présents ET `Boolean.TRUE.equals(actif)`).

**Nombre de requêtes SQL pour N laboratoires** : 1 (paniers) + 1 (lignes tous paniers) + 1 (laboratoires) + N (essais par tenant) = **N + 3 requêtes maximum**. Sans N+1 sur les lignes.

#### `obtenirPanier(Long utilisateurId, Long laboratoireId)`
1. **`inCentral`** : `laboratoireRepository.findById(laboratoireId)` → si absent → `ResourceNotFoundException` (404).
2. **`inCentral`** : `panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId)`.
   - Si absent → retourner un `PanierDetailDto` vide (`lignes = []`, `sousTotal = 0`) avec le nom du laboratoire. **HTTP 200.**
3. **`inCentral`** : `lignePanierRepository.findByPanierId(panier.id)` → lignes.
4. **`inTenant(nomSchema)`** : si `essaiIds` non vide → `findByIdsAvecDomaine(essaiIds)`. Sinon sauté.
5. Construction du `PanierDetailDto`. Essais absents ou inactifs → `disponible = false`, champs descriptifs null, exclus du `sousTotal`.

#### `ajouterEssai(Long utilisateurId, Long laboratoireId, Long essaiId)`
1. **`inCentral`** : `laboratoireRepository.findById(laboratoireId)` → si absent → `ResourceNotFoundException` (404). Si `statut != "ACTIF"` → `BusinessRuleException("LABORATOIRE_INDISPONIBLE")`.
2. **`inTenant(nomSchema)`** : `essaiRepository.findById(essaiId)` → si absent OU `!Boolean.TRUE.equals(actif)` → `BusinessRuleException("ESSAI_INDISPONIBLE")`.
3. **`inCentral` (UN SEUL appel pour tout le bloc d'écriture)** :
   - `panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId)` → création si absent (`dateCreation = dateModification = now`).
   - `lignePanierRepository.countByPanierId(panier.id) >= 50` → `BusinessRuleException("PANIER_PLEIN")`.
   - `lignePanierRepository.findByPanierIdAndEssaiId(panier.id, essaiId)` → si déjà présente, retourner l'état actuel (idempotent, 200).
   - Créer `LignePanier` ; `panier.dateModification = now` ; `panierRepository.save(panier)` ; `lignePanierRepository.save(ligne)`.
   - **Ce bloc entier est dans la même transaction `REQUIRES_NEW`** → atomicité entre le check des 50 lignes, l'insertion de la ligne et la mise à jour de `date_modification`.
4. **`catch (DataIntegrityViolationException e)` autour de l'appel `inCentral` de l'étape 3** : la transaction est rollbackée. La relecture se fait dans un **NOUVEL `inCentral`** : recharger le panier + ses lignes, puis retourner le `PanierDetailDto` (200). Pas de 500.
5. Retourner le `PanierDetailDto` (HTTP 200).

**Pourquoi le `catch` entoure `inCentral` et pas l'intérieur** : une transaction qui a levé une exception est rollbackée et terminée. On ne peut pas relire dedans. Le `Supplier` ayant propagé l'exception, le `catch` est positionné au niveau du code appelant, puis un nouveau `inCentral` ouvre une transaction fraîche pour la relecture.

#### `retirerEssai(Long utilisateurId, Long laboratoireId, Long essaiId)`
1. **`inCentral`** : `laboratoireRepository.findById(laboratoireId)` → si absent → `ResourceNotFoundException` (404).
2. **`inCentral`** : `panierRepository.findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId)`.
   - Si absent → retourner `PanierDetailDto` vide (200).
3. **`inCentral` (UN SEUL appel)** :
   - `lignePanierRepository.deleteByPanierIdAndEssaiId(panier.id, essaiId)`.
   - `long restantes = lignePanierRepository.countByPanierId(panier.id)`.
   - Si `restantes == 0` → `panierRepository.delete(panier)`. La FK `ON DELETE CASCADE` supprime les lignes (déjà supprimées dans ce cas).
   - Sinon → `panier.dateModification = now` ; `panierRepository.save(panier)`.
4. Retourner `PanierDetailDto` (HTTP 200). Si le panier a été supprimé : `lignes = []`, `sousTotal = 0`.

### 6. `PanierController`
`@RequestMapping("/api/client/paniers")`, `@PreAuthorize("hasRole('CLIENT')")` au niveau classe. L'utilisateur vient de `SecurityUtils.principalCourant().getId()`.

| Méthode | Path | Corps | Réponse HTTP | Corps réponse |
|---|---|---|---|---|
| GET | `/api/client/paniers` | — | 200 | `ApiResponse<List<PanierResumeDto>>` |
| GET | `/api/client/paniers/{laboratoireId}` | — | 200 (panier vide si inexistant) / 404 (labo inconnu) | `ApiResponse<PanierDetailDto>` |
| POST | `/api/client/paniers/{laboratoireId}/essais` | `AjouterEssaiRequest` | 200 | `ApiResponse<PanierDetailDto>` |
| DELETE | `/api/client/paniers/{laboratoireId}/essais/{essaiId}` | — | 200 | `ApiResponse<PanierDetailDto>` |

Contrats :
- **POST** : toujours 200 (même si l'essai était déjà présent — idempotent).
- **DELETE** : toujours 200 avec `PanierDetailDto` (`lignes = []`, `sousTotal = 0` si le panier a été supprimé).
- **GET /paniers/{laboratoireId}** : sans panier existant → 200 avec panier vide. Laboratoire inconnu → 404.

---

## Dépendances et considérations

- **Transaction `REQUIRES_NEW`** : chaque `inCentral`/`inTenant` est une transaction séparée. Pour `ajouterEssai`, la validation de l'essai (lecture tenant) précède l'écriture centrale. Si l'écriture échoue, la lecture n'a pas d'effet de bord. Si la lecture échoue (essai inactif), aucune écriture n'est tentée. ✅
- **Pas de `@OneToMany` sur `Panier`** : les lignes sont chargées explicitement par `LignePanierRepository.findByPanierId(...)`. La suppression du panier (`delete(panier)`) déclenche la suppression des lignes via la FK `ON DELETE CASCADE` en base.
- **Pas de `@ManyToOne` vers `Utilisateur`/`Laboratoire`** : `Long` simples, pas de jointure JPA.
- **`LaboratoireRepository` (module `plateforme`) et `EssaiRepository` (module `essai`)** injectés directement dans `PanierService` (Spring gère l'injection inter-modules).
- **`essai_id` sans FK** : une suppression d'essai dans le tenant ne casse pas le panier ; le service marque l'essai comme `disponible = false`.
- **`findByIdsAvecDomaine` jamais appelée avec collection vide** : le service vérifie `ids.isEmpty()` avant chaque appel.

---

## Validation (après implémentation)

1. `./mvnw clean compile` — doit réussir sans erreur.
2. `git status` — doit montrer uniquement les fichiers listés.
3. Contenu complet de chaque fichier créé.
4. `git diff` complet des fichiers modifiés (`EssaiRepository`, `LaboratoireRepository`).
5. Ne pas démarrer l'application ni toucher à une base.

---

## Risques

1. **Conflit unique concurrent** (`panier(utilisateur_id, laboratoire_id)` ou `ligne_panier(panier_id, essai_id)`) : catch `DataIntegrityViolationException` autour de `inCentral`, relecture dans un nouvel `inCentral`, retour 200. **Géré.**
2. **`findByIdsAvecDomaine` avec collection vide** → SQL invalide. → vérifier `isEmpty()` côté service. **Géré.**
3. **`X-Tenant-ID: ''` fonctionne par hasard** via `StringUtils.hasText`. Si la condition change, les routes panier cassent. → recommander d'ajouter `/api/client/paniers` en commentaire de `TenantInterceptor` (hors périmètre de ce lot).
4. **Frontend envoie `X-Tenant-ID` non vide** → `UnauthorizedTenantException`. → hors périmètre (frontend), à corriger dans le lot frontend.
5. **Essai supprimé/inactivé** → `disponible = false`, exclu du sous-total. **Géré.**
6. **Limite 50 lignes** : check + insertion dans la même transaction. Risque résiduel de dépassement en cas de haute concurrence (deux transactions lisent 49 puis insèrent chacune → 51). La contrainte UNIQUE ne protège pas contre le dépassement. Acceptable pour ce lot (le CDC ne demande pas de verrou pessimiste).
