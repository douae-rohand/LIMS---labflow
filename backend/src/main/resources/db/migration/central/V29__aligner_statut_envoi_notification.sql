-- ============================================================
-- V29 : Aligne statut_envoi sur StatutNotification.ENVOYEE
--       (l'application écrivait ENVOYE, absent de l'énumération)
-- ============================================================

UPDATE lims_central.notification_reception
SET statut_envoi = 'ENVOYEE'
WHERE statut_envoi = 'ENVOYE';
