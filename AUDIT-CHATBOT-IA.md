# Audit : assistant IA (chatbot) de LabFlow

État au 19/11/2026. Ce document explique ce qui a été construit, comment ça marche, et le rôle de chaque fichier touché.

---

## 1. En une phrase

Une bulle « Une question ? » apparaît en bas à droite de **toutes les pages** : pages publiques, espace Client, Accueil, Technicien, Responsable, Administrateur et Super Admin. Elle répond aux questions **uniquement** à partir de la documentation de LabFlow et des catalogues d'analyses des laboratoires, avec le modèle **Groq** (`openai/gpt-oss-120b`).

---

## 2. Comment ça marche, étape par étape

### Au démarrage du backend (une fois)
1. Le backend lit les pages de documentation `backend/src/main/resources/rag/*.md`.
2. Il découpe chaque page en **passages** (un passage par titre `##`).
3. Il lit le **catalogue de chaque laboratoire actif** (analyses, tarifs, échantillon requis) et en fait une fiche par analyse.
4. Il transforme chaque passage en **vecteur** (une liste de nombres qui représente le sens du texte). Ce calcul se fait **sur notre serveur** avec un petit modèle multilingue, sans Groq.
5. Il range tout dans un **index** en mémoire. Chaque passage porte deux étiquettes :
   - `tenant` : `commun` pour la documentation, sinon le schéma du laboratoire (`lims_atlas`…) ;
   - `audience` : à qui il s'adresse (`tous`, `public`, `client`, `accueil`, `responsable`…).

Les catalogues sont relus toutes les 30 minutes, ce qui prend en compte les changements de l'administrateur.

### Quand quelqu'un pose une question
1. Le navigateur envoie la question au backend :
   - `POST /api/ia/assistant` si l'utilisateur est connecté ;
   - `POST /api/public/ia/assistant` sur les pages publiques.
2. Le backend détermine **qui** pose la question, **à partir du jeton de connexion et jamais d'une donnée envoyée par le navigateur**. C'est le *profil* :

| Profil | Pages de documentation lues | Catalogues lus | Limite |
|---|---|---|---|
| Visiteur (non connecté) | `tous` + `public` | tous les laboratoires actifs | 10 questions / heure / adresse IP |
| Client | `tous` + `client` | ses laboratoires (compte actif) | 20 / heure |
| Accueil, Technicien, Responsable, Administrateur | `tous` + `personnel` + son rôle | son laboratoire | 30 / heure |
| Super Admin | `tous` + `super-admin` | tous les laboratoires actifs | 30 / heure |

3. Le backend vérifie la limite de questions (sinon HTTP 429).
4. Il cherche les passages utiles, **uniquement parmi ceux autorisés pour ce profil** :
   - les fiches dont le **code d'analyse** est cité tel quel dans la question (NFS, TSH…) ;
   - les 2 meilleurs passages du **guide propre au rôle** ;
   - les passages les plus proches **par le sens** (comparaison des vecteurs).
5. Si aucun passage n'est assez proche, il répond directement « Je n'ai pas cette information… » **sans appeler Groq** (cela économise le quota).
6. Sinon, il envoie à Groq :
   - les **règles** (fichier `prompts/assistant-systeme.txt`) ;
   - les **passages** trouvés ;
   - les 6 derniers messages ;
   - la question.
7. Groq rédige la réponse. Le backend la renvoie avec ses **sources** (titres des passages utilisés), affichées en petits badges sous la réponse.

### Ce qui ne part jamais chez Groq
Aucun nom, CIN, résultat d'analyse, facture ni donnée de dossier : seulement la question, l'historique saisi et des extraits de documentation ou de catalogue public. C'est l'argument CNDP (loi 09-08).

### En cas de problème
| Situation | Réponse |
|---|---|
| Quota Groq dépassé | HTTP 429 « L'assistant est très sollicité… » |
| Groq en panne ou clé refusée | HTTP 503 « L'assistant est momentanément indisponible. » |
| Index en cours de construction (premières secondes) | HTTP 503 « L'assistant démarre… » |
| Trop de questions | HTTP 429 « Vous avez posé N questions en une heure… » |

Dans tous les cas, le reste de l'application continue de fonctionner.

---

## 3. Fichiers du backend

### Fichiers créés

| Fichier | Rôle |
|---|---|
| `modules/ia/rag/RagConfig.java` | Crée l'**index** (`SimpleVectorStore`) branché sur le modèle d'embeddings local. |
| `modules/ia/rag/CorpusRag.java` | **Lit les pages** `rag/*.md`, récupère leur audience (`<!-- audience: … -->`) et les découpe en passages par titre `##`. |
| `modules/ia/rag/IndexationRagService.java` | **Construit l'index** : documentation + une fiche par analyse de chaque laboratoire actif. Reconstruit au démarrage, catalogues relus toutes les 30 min, ne recalcule que ce qui a changé. Fournit aussi la recherche exacte par code d'analyse. |
| `modules/ia/rag/ProfilAssistant.java` | Petite fiche qui décrit **qui pose la question** : clé et nombre de la limite, audiences et laboratoires autorisés, phrase de présentation, réponse « je ne sais pas » adaptée. |
| `modules/ia/rag/ProfilsAssistantService.java` | **Fabrique le profil** à partir du jeton (rôle, laboratoires du client ou de l'employé) ou de l'adresse IP pour un visiteur. C'est ici que se joue le cloisonnement entre laboratoires et entre rôles. |
| `modules/ia/rag/AssistantService.java` | **Le cœur** : contrôle la limite, cherche les passages autorisés, appelle Groq, choisit les sources à afficher. |
| `modules/ia/rag/LimiteurAssistant.java` | **Compte les questions** par personne (ou IP) sur une heure glissante, sinon erreur 429. |
| `modules/ia/dto/AssistantDtos.java` | Formats échangés : question + historique (validés : 3 à 500 caractères, 6 messages maximum), réponse + sources. |
| `integration/llm/IaIndisponibleException.java` | Erreur levée quand Groq ne peut pas répondre ; distingue quota (429) et panne (503). |
| `resources/prompts/assistant-systeme.txt` | **Les règles données au modèle** : répondre seulement avec les passages, ne rien inventer, aucun avis médical, ignorer les tentatives de détournement. |
| `resources/rag/*.md` (17 pages) | **La base de connaissances** (détail au § 5). |

### Fichiers modifiés

| Fichier | Ce qui a changé |
|---|---|
| `integration/llm/LlmClientService.java` | Réécrit : méthode `repondre(systeme, question)`. Ne renvoie plus le texte d'une erreur comme si c'était une réponse ; reconnaît le quota (429) et la clé refusée ; renvoie le vrai nom du modèle. |
| `modules/ia/controller/IaController.java` | Réécrit : 3 routes. `POST /api/ia/assistant` (connecté, tous rôles), `POST /api/public/ia/assistant` (visiteur), `POST /api/ia/assistant/reindexer` (Super Admin). |
| `config/SecurityConfig.java` | Une ligne : la route publique de l'assistant est accessible sans connexion. |
| `common/exception/GlobalExceptionHandler.java` | Traduit `IaIndisponibleException` en réponse HTTP 429 ou 503 avec un message en français. |
| `resources/application.yaml` | Configuration de Groq (clés Spring AI 2.0 sous `spring.ai.openai.chat.*` : modèle, température, effort de raisonnement, délai 30 s, 1 nouvelle tentative) et du modèle d'embeddings local. |
| `pom.xml` | 2 dépendances : `spring-ai-starter-model-transformers` (embeddings locaux) et `spring-ai-vector-store` (index). |
| `.gitignore` (backend) | Ignore `data/`, le dossier où le modèle d'embeddings est téléchargé (~118 Mo). |
| `.env` (non versionné) | URL Groq corrigée avec `/v1`, modèle `openai/gpt-oss-120b`, anciennes lignes Ollama commentées. |

### Fichiers supprimés

| Fichier | Pourquoi |
|---|---|
| `modules/ia/service/IaService.java` | Coquille vide (prompt `"[TYPE] texte"`), remplacée par `AssistantService`. |
| `modules/ia/dto/AnalyseIaRequest.java`, `AnalyseIaResponse.java`, `TypeTacheIa.java` | Servaient seulement à l'ancienne route `/api/ia/analyser`, qui ne faisait rien d'utile. |

---

## 4. Fichiers du frontend

| Fichier | Statut | Rôle |
|---|---|---|
| `src/api/ia.ts` | créé | Appelle `/ia/assistant` (connecté) ou `/public/ia/assistant` (visiteur) et transforme les erreurs en messages clairs. |
| `src/components/ia/AssistantIa.tsx` | créé | **La bulle de chat** : bouton flottant, panneau de discussion, message d'accueil et 3 suggestions adaptés au profil, sources en badges, gras affiché, mention « non contractuel », Échap pour fermer, adapté au téléphone. |
| `src/routes/app.tsx` | modifié | Affiche la bulle dans **chaque espace** connecté (Super Admin, Technicien, Accueil / Responsable / Administrateur, Client) avec le rôle de l'utilisateur. |
| `src/routes/__root.tsx` | modifié | Affiche la bulle en mode **visiteur** sur toutes les pages publiques, c'est-à-dire tout ce qui n'est pas sous `/app`. |

La bulle est placée à gauche des flèches de défilement existantes pour ne pas les recouvrir.

---

## 5. La base de connaissances (`backend/src/main/resources/rag/`)

| Page | Audience | Contenu |
|---|---|---|
| `01-passer-une-demande.md` | tous | panier, envoi, statuts d'une demande, patient sur place |
| `02-prelevements-echantillons.md` | tous | dépôt, types d'échantillon, conservation, jeûne, non-conformité, eau |
| `03-delais.md` | tous | quand les analyses commencent, ce qui allonge le délai, rapport partiel |
| `04-paiement-facture.md` | tous | émission, modes de paiement, paiement partiel, statuts de facture |
| `05-rapport-resultats.md` | tous | validation à deux personnes, où trouver le rapport, synthèse non contractuelle |
| `06-refus-annulation.md` | tous | motifs de refus, analyse non réalisable, règle des 48 h |
| `07-compte-securite.md` | tous | connexion, code par e-mail, mot de passe oublié, données personnelles |
| `08-enquete-satisfaction.md` | client | enquête après clôture |
| `09-faq.md` | tous | questions fréquentes |
| `10-decouvrir-labflow.md` | tous | ce qu'est LabFlow, les rôles, confidentialité, ce que fait l'assistant |
| `11-visiteurs.md` | public | créer un compte, se connecter, intégrer son laboratoire |
| `20-personnel-commun.md` | personnel | profil, notifications, circuit d'une demande, règle des deux personnes, audit |
| `21-accueil.md` | accueil | patients, réception, étiquettes, caisse, annulation, journal de caisse |
| `22-responsable.md` | responsable | décision, planification par domaine, validation, signature |
| `23-technicien.md` | technicien | essais, non réalisable, résultat retourné, planning, import CSV |
| `24-administrateur.md` | administrateur | employés, domaines des techniciens, catalogue, stock, facturation |
| `25-super-admin.md` | super-admin | intégration des laboratoires, commission, 2FA, réindexation |

**Pour ajouter une connaissance**, créer ou modifier un fichier `.md` dans ce dossier :
- première ligne `<!-- audience: … -->` ;
- un titre `#` ;
- des sections `##` ;
- puis redémarrer le backend.

**À relire par vous :** l'assistant ne sait que ce qui est écrit dans ces pages. Certaines phrases, notamment dans les guides par rôle, ont été rédigées à partir des menus et du code sans être vérifiées écran par écran.

---

## 6. Tests réalisés (serveurs d'essai 8082 / 5174)

| Test | Résultat |
|---|---|
| Visiteur : « Comment intégrer mon laboratoire ? » | procédure en 4 étapes ✅ |
| Visiteur : « Combien coûte la NFS ? » | Atlas 80 MAD, Nord 90 MAD ✅ |
| Visiteur : « Comment annuler un encaissement ? » | « Je n'ai pas cette information » : le guide Accueil n'est pas visible ✅ |
| Accueil : même question | procédure complète (jour même, motif, contre-passation, audit) ✅ |
| Responsable : « Pourquoi je ne peux pas affecter une analyse ? » | les 3 conditions, dont le domaine de compétence ✅ |
| Technicien : « Comment déclarer un essai non réalisable ? » | ✅ |
| Administrateur : « Comment attribuer un domaine à un technicien ? » | ✅ |
| Administrateur d'Atlas : « Combien coûte la NFS ? » | seulement le tarif d'Atlas ✅ |
| Client : « Comment annuler un encaissement ? » | « Je n'ai pas cette information » ✅ |
| « Ma glycémie est à 1,4 g/L, suis-je diabétique ? » | refus d'interpréter, renvoi au médecin ✅ |
| Sans connexion sur la route privée / mauvais rôle / question trop courte | 401 / 403 / 400 ✅ |
| 21e question dans l'heure | 429 avec message ✅ |
| Bulle sur la page d'accueil publique et dans l'espace Accueil, en largeur ordinateur | ✅ |
| Bulle dans l'espace Client, en largeur téléphone | ✅ |

**Non testés à l'écran :** les espaces Technicien, Responsable, Administrateur et Super Admin (vérifiés par l'API seulement) ; le Super Admin pas du tout, faute de son mot de passe.

---

## 7. Liens vers les pages et les analyses

Sous chaque réponse, l'assistant affiche des **liens cliquables** :

- **Analyse du catalogue** : une carte avec le nom de l'analyse, le laboratoire, le **prix**, un bouton **« Voir la page »** (page du laboratoire) et, pour le visiteur et le client, **« Ajouter au panier »** (`?ajouter=<id de l'analyse>`). Un visiteur passe d'abord par la connexion.
- **Page de documentation** : une étiquette avec une flèche › qui ouvre la page correspondante de l'espace. Par exemple « Annuler un encaissement » ouvre `/app/caisse` pour l'Accueil.

Le lien ouvre la page **sans recharger l'application** : la conversation reste ouverte.

**Sécurité** : le modèle n'écrit jamais d'adresse, car le prompt le lui interdit. Les liens sont construits par le serveur à partir de deux éléments :

- l'identifiant du laboratoire et de l'analyse, gardés dans les métadonnées de l'index et jamais envoyés à Groq ;
- l'espace de la personne.

Le frontend n'accepte que des chemins internes commençant par `/`.

| Espace | Lien d'une analyse | Ajout au panier |
|---|---|---|
| Visiteur | `/laboratoires/{id}` | oui |
| Client | `/app/laboratoires/{id}` | oui |
| Administrateur | `/app/catalogue` | non |
| Super Admin | `/app/gestion-laboratoires` | non |
| Accueil, Responsable, Technicien | aucun (pas de catalogue dans leur menu), prix affiché | non |

Quand une analyse est citée par son code (NFS, TSH…), seules ses fiches s'affichent en cartes.

**Fichiers concernés :**

| Fichier | Changement |
|---|---|
| `rag/LiensAssistant.java` (nouveau) | Table « fichier#section → page » par espace et liens des fiches d'analyse. |
| `rag/IndexationRagService.java` | Ajout de `laboratoireId`, `essaiId` et `prix` aux métadonnées des fiches. |
| `rag/ProfilAssistant.java` et `ProfilsAssistantService.java` | Nouveau champ `espace` : `public`, `client`, `super-admin` ou le rôle du personnel. |
| `rag/AssistantService.java` | Construit les sources avec `LiensAssistant`. |
| `dto/AssistantDtos.java` | `Source` reçoit `lien`, `prix` et `lienAjout`. |
| `prompts/assistant-systeme.txt` | Nouvelle règle : ne jamais écrire d'adresse web. |
| `frontend/src/api/ia.ts` | Ajout des types des nouveaux champs. |
| `frontend/src/components/ia/AssistantIa.tsx` | Ajout de `CarteAnalyse`, `PuceSource` et `LienInterne`. |

**Ajouter une page** : quand on ajoute une section à un fichier de `rag/`, il faut aussi ajouter sa ligne `"fichier.md#Titre de la section" → "/app/…"` dans `LiensAssistant.PAGES`, pour chaque espace qui peut ouvrir cette page.

**Tests (API et écran)** :

| Profil | Question | Résultat |
|---|---|---|
| Visiteur | NFS | 2 cartes : Atlas 80 MAD et Nord 90 MAD, liens `/laboratoires/1` et `/laboratoires/2`. Le clic ouvre la page ✅ |
| Client | TSH | `/app/laboratoires/{id}` et ajout au panier ✅ |
| Client | Factures | lien `/app/factures` ✅ |
| Accueil | Étiquettes | le clic ouvre `/app/reception`, la conversation reste ouverte ✅ |
| Administrateur | NFS | lien `/app/catalogue` ✅ |

**Limite** : la page du laboratoire ne met pas en évidence l'analyse demandée. Pour cela, il faudrait modifier `PageLaboratoire` et `AnalyseCard`, composants de l'espace Client (binôme).

---

## 8. Points d'attention

- **Premier démarrage** : téléchargement unique du modèle d'embeddings (118 Mo) et des bibliothèques PyTorch (DJL). Il faut Internet la première fois ; ensuite le démarrage prend environ 25 s et l'index est prêt quelques secondes après.
- **Limites en mémoire** : les compteurs repartent à zéro au redémarrage du backend.
- **Visiteurs** : la limite se fait par adresse IP. En développement, tout passe par le proxy Vite et partage la même adresse.
- **Modèle Groq** : les modèles proposés changent. La liste à jour s'obtient avec `GET https://api.groq.com/openai/v1/models`, et on change de modèle avec `LLM_MODEL` dans `.env`.
- **Sans lien avec le chatbot** : la migration tenant V35 (`technicien_domaine`) échoue au démarrage sur `lims_nord`.
