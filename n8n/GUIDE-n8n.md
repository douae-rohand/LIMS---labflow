# Workflows n8n

Workflows : W1 demande-soumise, W2 demande-acceptee, W3 demande-refusee, W4 echantillon-recu,
W5 rapport-signe, W6 rappel-validation, W7 relance-factures, W8 essai-affecte,
W9 enquete-satisfaction, W10 alertes-stock.

## Installation
1. Lancer n8n : `npx n8n` (http://localhost:5678).
2. Créer 2 credentials :
   - Header Auth : nom `X-Webhook-Secret`, valeur = `N8N_WEBHOOK_SECRET` du `backend/.env`.
   - SMTP (Gmail : smtp.gmail.com, port 465 avec SSL/TLS activé, ou port 587 avec SSL/TLS désactivé ;
     mot de passe d'application de 16 lettres, sans espaces). Laisser **Client Host Name vide** :
     un nom avec un espace (ex. « LabFlow LIMS ») fait couper la connexion par Gmail
     (« Connection closed unexpectedly »). Le nom affiché de l'expéditeur se règle dans le nœud Send Email.
3. Importer : `npx n8n import:workflow --separate --input=n8n/workflows/`
   (ou menu ⋯ > Import from File, workflow par workflow).
4. Dans chaque nœud Webhook, HTTP Request et Send Email : re-sélectionner ses credentials, vérifier l'expéditeur.
5. Publier chaque workflow.

## Backend (.env)
N8N_WEBHOOK_BASE_URL=http://localhost:5678/webhook
N8N_WEBHOOK_SECRET=<même valeur que le credential Header Auth>

Quand `N8N_WEBHOOK_SECRET` est renseigné, l'e-mail d'acceptation/refus part par n8n (W2/W3)
et non plus par SendGrid ; la notification dans l'application est conservée.

## W2 demande-acceptee / W3 demande-refusee
Webhook (signature) → réponse 200 → e-mail au client.
- W2 : déclenché quand le Responsable accepte une demande en ligne. Contient la référence,
  le n° de facture, le montant, l'échéance, les échantillons à déposer, l'adresse du laboratoire
  et le lien de suivi. Une demande créée sur place à l'accueil ne déclenche pas W2.
- W3 : déclenché au refus, avec le motif et un lien vers le catalogue des laboratoires.

## W6 rappel-validation
Toutes les heures → `GET http://localhost:8081/api/n8n/resultats-en-attente?heures=24`
(credential Header Auth) → IF `total > 0` → regroupement par Responsable → un seul e-mail
récapitulatif par Responsable (résultats critiques en tête).

- `heures=X` : un résultat est rappelé quand il attend depuis X h, puis 2X h, 3X h…
  (un rappel par palier, pas un e-mail toutes les heures). Maximum 720.
- `heures=0` : renvoie tous les résultats en attente, utile pour tester (« Execute workflow »
  après avoir mis `heures=0` dans le nœud HTTP Request, puis remettre 24).
- L'endpoint est protégé par le même secret : sans l'en-tête `X-Webhook-Secret`, réponse 401.
