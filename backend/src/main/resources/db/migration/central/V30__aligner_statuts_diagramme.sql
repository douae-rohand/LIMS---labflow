-- Aligne les statuts du schéma central sur le diagramme de classes.
-- V29 a écrit ENVOYEE ; le diagramme retient ENVOYE.

UPDATE laboratoire SET statut = 'DESACTIVE'
WHERE statut = 'INACTIF';

UPDATE demande_integration SET statut = 'EN_ATTENTE'
WHERE statut IN ('EN_COURS_VALIDATION', 'SUSPENDUE');

UPDATE demande_integration SET statut = 'ACCEPTEE'
WHERE statut = 'APPROUVEE';

UPDATE demande_integration SET statut = 'REFUSEE'
WHERE statut = 'REJETEE';

UPDATE notification_reception SET statut_envoi = 'EN_ATTENTE'
WHERE statut_envoi = 'CREEE';

UPDATE notification_reception SET statut_envoi = 'ENVOYE'
WHERE statut_envoi IN ('ENVOYEE', 'ENVOYE', 'LIVRE', 'PARTIELLEMENT_ENVOYEE');

UPDATE notification_reception SET canal = 'PUSH'
WHERE canal = 'WEB_PUSH';

UPDATE notification_reception SET canal = 'EMAIL'
WHERE canal = 'N8N_WEBHOOK';

UPDATE evenement_metier SET type = 'NOUVELLE_DEMANDE'
WHERE type IN ('DEMANDE_SOUMISE', 'LABORATOIRE_CREE', 'COMPTE_CREE', 'COMPTE_DESACTIVE');

UPDATE evenement_metier SET type = 'DEMANDE_REFUSEE'
WHERE type = 'DEMANDE_REJETEE';

UPDATE evenement_metier SET type = 'DEMANDE_CLOTUREE'
WHERE type = 'DEMANDE_TERMINEE';

UPDATE evenement_metier SET type = 'ECHANTILLON_RECU'
WHERE type IN ('ECHANTILLON_RECEPTIONNE', 'ECHANTILLON_NON_CONFORME');

UPDATE evenement_metier SET type = 'RESULTAT_EN_ATTENTE'
WHERE type IN ('ESSAI_TERMINE', 'RESULTAT_REJETE');

UPDATE evenement_metier SET type = 'RAPPORT_VALIDE'
WHERE type IN ('RESULTAT_VALIDE', 'RAPPORT_GENERE', 'RAPPORT_ENVOYE');

UPDATE evenement_metier SET type = 'FACTURE_ECHUE'
WHERE type IN ('FACTURE_EMISE', 'FACTURE_EN_RETARD');

UPDATE evenement_metier SET type = 'ALERTE_STOCK'
WHERE type IN ('STOCK_RUPTURE_IMMINENTE', 'NON_CONFORMITE_DECLAREE', 'NON_CONFORMITE_CLOTUREE');

UPDATE evenement_metier SET type = 'DEMANDE_ACCEPTEE'
WHERE type = 'DEMANDE_INTEGRATION_TRAITEE';
