-- Imports data that is assumed to be pre-loaded into the database
-- This is a temporary solution until database migrations are implemented

-- Osaaminen (ESCO 1.2)
TRUNCATE osaaminen CASCADE;

CREATE SEQUENCE IF NOT EXISTS osaaminen_seq;
SELECT setval('osaaminen_seq', (SELECT COALESCE(MAX(id), 1) FROM osaaminen));

INSERT INTO osaaminen (id, uri) SELECT nextval('osaaminen_seq'), "conceptUri"  AS uri FROM esco_data.skills_fi;
INSERT INTO osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'FI' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_fi e JOIN osaaminen o ON o.uri = e."conceptUri";
INSERT INTO osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'SV' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_sv e JOIN osaaminen o ON o.uri = e."conceptUri";
INSERT INTO osaaminen_kaannos (osaaminen_id, kaannos_key,  kuvaus, nimi) SELECT o.id AS osaaminen_id, 'EN' AS kaannos_key, description AS kuvaus, "preferredLabel"  AS nimi FROM esco_data.skills_en e JOIN osaaminen o ON o.uri = e."conceptUri";

