# LabFlow : tâches IA restantes

État au 16/11/2026, après lecture du code (module `ia`, `integration/llm`, rapports client).

**Légende :** ❌ à faire · ⚠️ existe mais à corriger · ✅ déjà fait
**Taille :** S = moins d'une demi-journée · M = 1 jour · L = 2 jours ou plus

---

## 1. Ce qui existe déjà

| Élément | Fichier | État |
|---|---|---|
| Dépendance Spring AI 2.0.1 (compatible Ollama, Groq, OpenAI…) | `backend/pom.xml` | ✅ |
| Configuration `LLM_API_URL`, `LLM_API_KEY`, `LLM_MODEL` (par défaut Ollama + `llama3.2`) | `application.yaml` | ✅ |
| Client LLM `appelerLlm(prompt, system)` | `integration/llm/LlmClientService.java` | ⚠️ renvoie le texte « Erreur… » au lieu d'échouer |
| Table `synthese_ia`, entité et repository | `V15__create_synthese_ia.sql`, `ia/entity/SyntheseIa.java`, `ia/repository/SyntheseIaRepository.java` | ✅ |
| Lecture de la synthèse côté client avec la mention « non contractuelle » | `RapportClientService.java`, `RapportDetail.tsx` | ✅ |
| Synthèse de démonstration `SYN-DEMO-0001` (écrite à la main) | `V29__seed_demo_espace_client.sql` | ✅ |
| Route `/api/ia/analyser` et `IaService` | `ia/controller/IaController.java`, `ia/service/IaService.java` | ❌ coquille vide |
| Enum `TypeTacheIa` (5 types, dont 3 inutiles pour le CDC) | `ia/dto/TypeTacheIa.java` | ⚠️ |
| RAG : embeddings, base vectorielle, documents | — | ❌ rien |

---

## 2. Socle IA (à faire en premier, tout le reste en dépend)

| # | Tâche | Ce qu'il faut faire | Fichiers | Taille | État |
|---|---|---|---|---|---|
| 1 | Fournisseur IA | Garder Ollama en local avec `llama3.2` : gratuit, aucune donnée ne sort du serveur (argument CNDP) | `application.yaml`, `.env` | S | ✅ config prête |
| 2 | Erreurs du LLM | Lever une exception au lieu de renvoyer le texte « Erreur… », sinon l'erreur est enregistrée comme synthèse et montrée au client | `LlmClientService.java` | S | ⚠️ |
| 3 | Timeout | Couper l'appel au modèle après 30 secondes | `LlmClientService.java`, `RestClientConfig.java` | S | ❌ |
| 4 | Nom du modèle | `getModeleActif()` doit renvoyer la valeur de `LLM_MODEL`, pas le nom de la classe | `LlmClientService.java` | S | ⚠️ |
| 5 | Température | 0,2 pour la synthèse, 0,3 pour l'assistant (0,7 partout aujourd'hui) | `LlmClientService.java` | S | ⚠️ |
| 6 | Prompts | Ranger les prompts dans des fichiers au lieu du code | `resources/prompts/synthese-rapport.st`, `resources/prompts/assistant.st` | S | ❌ |
| 7 | Données personnelles | Ne jamais envoyer au modèle le nom, le CIN, le téléphone ou l'adresse : seulement les analyses, valeurs, unités et normes | Services IA | S | ❌ |
| 8 | Nettoyage | Supprimer ou réécrire `/api/ia/analyser` ; ne garder que `RESUME_RAPPORT` et `QA_DOCUMENTAIRE` ; brancher ou retirer la permission `IA_GERER` | `IaController.java`, `IaService.java`, `TypeTacheIa.java`, migration | S | ❌ |
| 9 | Traçabilité | Enregistrer chaque appel IA : type, modèle, durée, succès ou échec (TODO d'`IaService`) | `AuditService` ou nouvelle table | S | ❌ |

---

## 3. Synthèse IA du rapport (CDC M07, M14)

| # | Tâche | Ce qu'il faut faire | Fichiers | Taille | État |
|---|---|---|---|---|---|
| 10 | Contexte | `SyntheseRapportService.generer(demandeId)` : pour chaque analyse, désignation, résultat, unité, norme, conforme ou hors norme | nouveau `ia/service/SyntheseRapportService.java` | M | ❌ |
| 11 | Prompt système | 5 à 8 phrases simples ; aucun diagnostic ni traitement ; signaler les valeurs hors norme sans les interpréter ; phrase finale qui renvoie au rapport officiel et au médecin | `synthese-rapport.st` | S | ❌ |
| 12 | Déclenchement | Générer après la signature, après le commit et en asynchrone (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) : la signature ne dépend jamais de l'IA | `RapportService.signerDemande` | S | ❌ |
| 13 | Enregistrement | Écrire dans `synthese_ia` avec un code `SYN-AAAA-xxxx` ; en cas d'échec, aucune ligne, seulement un log | `SyntheseIaRepository` | S | ✅ table · ❌ écriture |
| 14 | Contrôle des chiffres | Rejeter la synthèse si un chiffre cité ne figure pas dans les résultats | `SyntheseRapportService` | S | ❌ |
| 15 | Nouvelle version | Régénérer après un rapport partiel ou une nouvelle version ; le client voit la dernière | `SyntheseRapportService` | S | ❌ |
| 16 | Régénération | Route `POST /api/rapports/{demande}/synthese` pour relancer la génération | contrôleur rapport | S | ❌ |
| 17 | Écran Responsable | Aperçu de la synthèse et bouton « Régénérer la synthèse » | `PageRapports.tsx` | S | ❌ |
| 18 | Écran Client | Afficher la synthèse avec la mention « non contractuelle » | `RapportDetail.tsx` | — | ✅ |
| 19 | État vide côté client | Afficher « Synthèse en cours » ou « Synthèse indisponible » quand aucune ligne n'existe | `RapportDetail.tsx` | S | ❌ |

---

## 4. Assistant IA avec RAG (CDC M14)

| # | Tâche | Ce qu'il faut faire | Fichiers | Taille | État |
|---|---|---|---|---|---|
| 20 | Corpus | Pages Markdown : passer une demande, prélèvements et conservation, délais, paiement et facture, lire un rapport, enquête, FAQ | `resources/rag/*.md` | M | ❌ |
| 21 | Catalogue | Ajouter au corpus le catalogue de chaque laboratoire (analyses, tarifs, type d'échantillon), lu depuis sa base | nouveau `IndexationRagService.java` | S | ❌ |
| 22 | Dépendances | `spring-ai-vector-store` (`SimpleVectorStore`, car MariaDB n'a pas de recherche vectorielle) et le modèle d'embeddings `nomic-embed-text` sur Ollama | `pom.xml`, `application.yaml` | S | ❌ |
| 23 | Indexation | Découper en blocs d'environ 500 tokens (`TokenTextSplitter`), métadonnées `{source, tenant}`, sauvegarde dans `data/rag-index.json` | `IndexationRagService.java` | M | ❌ |
| 24 | Réindexation | Au démarrage, puis à chaque modification du catalogue | `IndexationRagService.java` | S | ❌ |
| 25 | Recherche | Les 4 meilleurs passages, filtrés par laboratoire (un client d'Atlas ne voit jamais le catalogue de Nord) | nouveau `AssistantService.java` | S | ❌ |
| 26 | API | `POST /api/ia/assistant` : reçoit `{question, historique}` (3 derniers messages), renvoie `{reponse, sources[]}` | `IaController.java` | S | ❌ |
| 27 | Prompt système | Répondre seulement à partir des passages fournis, sinon « je ne sais pas, contactez le laboratoire » ; aucun conseil médical ; aucune donnée d'un autre client | `assistant.st` | S | ❌ |
| 28 | Limite d'usage | 20 questions par heure et par utilisateur | sur le modèle de `TwoFactorRateLimiter` | S | ❌ |
| 29 | Écran | Bulle de chat dans l'espace Client : sources citées sous la réponse, état « réflexion… », message d'erreur clair | nouveau `AssistantIa.tsx`, `api/ia.ts` | M | ❌ |

---

## 5. Démo et livrables

| # | Tâche | Ce qu'il faut faire | Taille | État |
|---|---|---|---|---|
| 30 | Préparer Ollama | `ollama pull llama3.2` et `ollama pull nomic-embed-text`, vérifier qu'Ollama tourne avant la démo | S | ❌ |
| 31 | Plan de secours | Garder une synthèse déjà générée, au cas où le modèle serait lent en direct | S | ❌ |
| 32 | Rapport de projet | Expliquer : modèle en local (CNDP, loi 09-08), synthèse non contractuelle, RAG limité au corpus, signature indépendante de l'IA | S | ❌ |

---

## Ordre conseillé

1. **Socle** : tâches 1 à 9 (environ ½ journée).
2. **Synthèse IA** : tâches 10 à 17, puis 19 (environ 2 jours).
3. **Assistant RAG** : tâches 20 à 29 (environ 2,5 jours). Il peut avancer en même temps que la synthèse, une fois le socle fini.
4. **Démo et livrables** : tâches 30 à 32 (½ journée).

**Total restant :** 31 tâches, environ 5 jours de travail.
