-- Données de démonstration du schéma central.
-- Mot de passe de tous les comptes seedés : Labflow2026!

INSERT INTO laboratoire (code, raison_sociale, ice, adresse, ville, telephone, email, nom_schema, statut)
SELECT 'atlas', 'Laboratoire Atlas', '001545678000012', '12 Boulevard Zerktouni', 'Casablanca',
       '+212522000001', 'contact@atlas.labflow.ma', 'lims_atlas', 'ACTIF'
WHERE NOT EXISTS (SELECT 1 FROM laboratoire WHERE code = 'atlas');

INSERT INTO laboratoire (code, raison_sociale, ice, adresse, ville, telephone, email, nom_schema, statut)
SELECT 'nord', 'Laboratoire du Nord', '001545678000045', '8 Avenue Mohammed VI', 'Tanger',
       '+212539000002', 'contact@nord.labflow.ma', 'lims_nord', 'ACTIF'
WHERE NOT EXISTS (SELECT 1 FROM laboratoire WHERE code = 'nord');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Bennani', 'Sara', 'admin.atlas@labflow.ma', '+212600000010',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'ADMINISTRATEUR'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'atlas')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'admin.atlas@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Amrani', 'Sofia', 'resp.atlas@labflow.ma', '+212600000011',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'RESPONSABLE'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'atlas')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'resp.atlas@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Idrissi', 'Youssef', 'tech.atlas@labflow.ma', '+212600000012',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'TECHNICIEN'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'atlas')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'tech.atlas@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Tahiri', 'Lina', 'accueil.atlas@labflow.ma', '+212600000013',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'ACCUEIL'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'atlas')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'accueil.atlas@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Mansouri', 'Karim', 'client.atlas@labflow.ma', '+212600000014',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'CLIENT'),
       NULL
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'client.atlas@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Chraibi', 'Hanae', 'admin.nord@labflow.ma', '+212600000020',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'ADMINISTRATEUR'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'nord')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'admin.nord@labflow.ma');

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, mot_de_passe_hash, must_change_password, double_authentification, actif, role_id, laboratoire_id)
SELECT NULL, 'Alaoui', 'Mehdi', 'tech.nord@labflow.ma', '+212600000021',
       '$2a$10$.kzwyrGQxJnjnsSsbpk8w.7JwybNZDiqb1fGZwcfWgZhNgJOITEN2', 0, 0, 1,
       (SELECT id_role FROM role WHERE code = 'TECHNICIEN'),
       (SELECT id_laboratoire FROM laboratoire WHERE code = 'nord')
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE email = 'tech.nord@labflow.ma');

INSERT INTO utilisateur_domaine (utilisateur_id, code_domaine)
SELECT u.id_utilisateur, 'BIOCHIMIE'
FROM utilisateur u
WHERE u.email IN ('tech.atlas@labflow.ma', 'resp.atlas@labflow.ma', 'tech.nord@labflow.ma')
  AND NOT EXISTS (
      SELECT 1 FROM utilisateur_domaine ud
      WHERE ud.utilisateur_id = u.id_utilisateur AND ud.code_domaine = 'BIOCHIMIE'
  );

INSERT INTO client_profil (utilisateur_id, raison_sociale, ice, adresse, consentement_cndp, date_creation)
SELECT u.id_utilisateur, 'Clinique Al Amal', '001888777000019', '45 Rue Ibn Sina, Casablanca', 1, NOW()
FROM utilisateur u
WHERE u.email = 'client.atlas@labflow.ma'
  AND NOT EXISTS (SELECT 1 FROM client_profil WHERE utilisateur_id = u.id_utilisateur);

INSERT INTO client_laboratoire (utilisateur_id, laboratoire_id, client_local_id, statut, date_premier_contact)
SELECT u.id_utilisateur, l.id_laboratoire, 1, 'ACTIF', NOW()
FROM utilisateur u
JOIN laboratoire l ON l.code = 'atlas'
WHERE u.email = 'client.atlas@labflow.ma'
  AND NOT EXISTS (
      SELECT 1 FROM client_laboratoire cl
      WHERE cl.utilisateur_id = u.id_utilisateur AND cl.laboratoire_id = l.id_laboratoire
  );

INSERT INTO demande_integration (
    numero, date_demande, statut, raison_sociale, ice, adresse,
    contact_nom, contact_email, contact_telephone, laboratoire_id
)
SELECT 'INT-ATLAS01', NOW(), 'APPROUVEE', 'Laboratoire Atlas', '001545678000012',
       '12 Boulevard Zerktouni, Casablanca', 'Sara Bennani', 'admin.atlas@labflow.ma',
       '+212600000010', l.id_laboratoire
FROM laboratoire l
WHERE l.code = 'atlas'
  AND NOT EXISTS (SELECT 1 FROM demande_integration WHERE numero = 'INT-ATLAS01');

INSERT INTO demande_integration (
    numero, date_demande, statut, raison_sociale, ice, adresse,
    contact_nom, contact_email, contact_telephone, laboratoire_id
)
SELECT 'INT-ATTENTE', NOW(), 'EN_ATTENTE', 'Laboratoire Oasis', '001999000000033',
       '3 Rue des Palmiers, Marrakech', 'Noura Kadiri', 'noura.kadiri@oasis.ma',
       '+212600000099', NULL
WHERE NOT EXISTS (SELECT 1 FROM demande_integration WHERE numero = 'INT-ATTENTE');

INSERT INTO journal_audit (code, date_action, action, objet, valeur_apres, motif, utilisateur_id)
SELECT 'AUD-SEED-001', NOW(), 'CREATION', 'LABORATOIRE',
       '{"code":"atlas","statut":"ACTIF"}', 'Seed de démonstration',
       u.id_utilisateur
FROM utilisateur u
WHERE u.email = 'superadmin@labflow.ma'
  AND NOT EXISTS (SELECT 1 FROM journal_audit WHERE code = 'AUD-SEED-001');

INSERT INTO evenement_metier (code, type, date_heure, donnees, envoye, nb_tentatives, laboratoire_id)
SELECT 'EVT-SEED-001', 'LABORATOIRE_CREE', NOW(), '{"code":"atlas"}', 1, 1, l.id_laboratoire
FROM laboratoire l
WHERE l.code = 'atlas'
  AND NOT EXISTS (SELECT 1 FROM evenement_metier WHERE code = 'EVT-SEED-001');
