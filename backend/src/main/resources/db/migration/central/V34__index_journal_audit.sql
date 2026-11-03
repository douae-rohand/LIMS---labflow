CREATE INDEX idx_journal_audit_laboratoire_date ON journal_audit (laboratoire_id, date_action);
CREATE INDEX idx_journal_audit_acteur ON journal_audit (utilisateur_id);
