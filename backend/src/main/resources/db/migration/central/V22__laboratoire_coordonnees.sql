ALTER TABLE laboratoire
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;

-- Coordonnées des laboratoires seedés, calées sur leur ville / adresse connues.
UPDATE laboratoire
SET latitude = 33.589200, longitude = -7.618600
WHERE code = 'atlas' AND latitude IS NULL;

UPDATE laboratoire
SET latitude = 35.767300, longitude = -5.803900
WHERE code = 'nord' AND latitude IS NULL;
