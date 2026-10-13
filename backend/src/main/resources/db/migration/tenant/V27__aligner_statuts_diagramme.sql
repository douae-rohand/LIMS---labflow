-- Aligne les statuts tenant sur le diagramme de classes.
-- Les migrations précédentes ne sont pas modifiées : ce script réécrit les valeurs déjà posées.

UPDATE ligne_essai SET statut = 'ATTENTE_ECHANTILLON'
WHERE statut IN ('EN_ATTENTE', 'PLANIFIE', 'PLANIFIEE', 'REPORTEE');

UPDATE ligne_essai SET statut = 'EN_COURS'
WHERE statut = 'A_REFAIRE';

UPDATE ligne_essai SET statut = 'TERMINE'
WHERE statut = 'TERMINEE';

UPDATE ligne_essai SET statut = 'NON_REALISABLE'
WHERE statut IN ('ANNULE', 'ANNULEE');

UPDATE demande SET statut = 'NOUVEAU'
WHERE statut IN ('BROUILLON', 'SOUMISE');

UPDATE demande SET statut = 'REFUSEE'
WHERE statut = 'REJETEE';

UPDATE facture SET statut = 'IMPAYEE'
WHERE statut IN ('BROUILLON', 'ANNULEE');

UPDATE facture SET statut = 'EMISE'
WHERE statut = 'ENVOYEE';

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
