# Workflows n8n

Workflows : W1 demande-soumise, W4 echantillon-recu, W5 rapport-signe, W7 relance-factures,
W8 essai-affecte, W9 enquete-satisfaction, W10 alertes-stock.

## Installation
1. Lancer n8n : `npx n8n` (http://localhost:5678).
2. Créer 2 credentials :
   - Header Auth : nom `X-Webhook-Secret`, valeur = `N8N_WEBHOOK_SECRET` du `backend/.env`.
   - SMTP (Gmail : smtp.gmail.com, port 587, SSL/TLS désactivé, mot de passe d'application).
3. Importer : `npx n8n import:workflow --separate --input=n8n/workflows/`
   (ou menu ⋯ > Import from File, workflow par workflow).
4. Dans chaque nœud Webhook, HTTP Request et Send Email : re-sélectionner ses credentials, vérifier l'expéditeur.
5. Publier chaque workflow.

## Backend (.env)
N8N_WEBHOOK_BASE_URL=http://localhost:5678/webhook
N8N_WEBHOOK_SECRET=<même valeur que le credential Header Auth>