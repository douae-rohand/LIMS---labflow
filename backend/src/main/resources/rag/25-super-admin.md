<!-- audience: super-admin -->
# Guide du Super Administrateur

## Le menu du Super Admin
Le menu comprend : Tableau de bord, Laboratoires, Administrateurs, Journal d'audit, Paramètres, Mon compte.

## Étudier une demande d'intégration
Dans « Laboratoires », les demandes d'intégration envoyées depuis le site public sont listées. Pour chacune, vous pouvez demander des compléments (le demandeur reçoit un lien pour compléter son dossier), refuser avec un motif, ou accepter. À l'acceptation, la base de données du laboratoire est créée et son administrateur reçoit une invitation par e-mail.

## Gérer les laboratoires
Dans « Laboratoires », consultez chaque laboratoire partenaire, son statut (actif, suspendu…), ses indicateurs et ses finances.

## Commission mensuelle
L'onglet des finances d'un laboratoire calcule la facture de commission mensuelle de la plateforme (numérotée COM-AAAAMM-CODE) et permet de la télécharger en PDF.

## Comptes administrateurs
« Administrateurs » liste les comptes administrateurs des laboratoires : création, modification, désactivation et consultation des sessions.

## Double authentification par e-mail
Dans « Paramètres », choisissez les rôles pour lesquels un code de vérification à 6 chiffres est exigé à chaque connexion. Le code est envoyé par e-mail, il est valable 15 minutes et ne sert qu'une fois ; on peut le renvoyer après une minute. Après 5 codes faux, la vérification est bloquée 5 minutes.

## Journal d'audit de la plateforme
« Journal d'audit » trace les actions sensibles au niveau de la plateforme (décisions d'intégration, comptes, paramètres).

## Assistant IA
L'index de l'assistant (documentation et catalogues des laboratoires actifs) est reconstruit au démarrage et les catalogues sont relus toutes les 30 minutes. Un Super Admin peut forcer la reconstruction avec la route POST /api/ia/assistant/reindexer.

## Interroger les données de la plateforme avec l'assistant
L'assistant peut lire, en lecture seule, l'état de la plateforme : nombre de laboratoires actifs, suspendus et désactivés, et nombre de demandes d'intégration en attente, en attente de compléments, acceptées ou refusées. Il ne modifie rien : pour traiter une demande, ouvrez « Gestion des laboratoires ».
