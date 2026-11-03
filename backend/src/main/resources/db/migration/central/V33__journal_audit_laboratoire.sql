-- Rattache les nouvelles lignes d'audit au laboratoire concerné.
-- Les lignes déjà présentes restent sans laboratoire_id.

ALTER TABLE journal_audit
    ADD COLUMN laboratoire_id BIGINT NULL,
    ADD KEY idx_journal_audit_laboratoire (laboratoire_id),
    ADD CONSTRAINT fk_journal_audit_laboratoire
        FOREIGN KEY (laboratoire_id) REFERENCES laboratoire (id_laboratoire);
