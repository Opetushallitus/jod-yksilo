ALTER TABLE yksilo
  DROP CONSTRAINT IF EXISTS fk_yksilo_id,
  ADD CONSTRAINT fk_yksilo_id FOREIGN KEY (id) REFERENCES tunnistus.henkilo(yksilo_id);

-- TEST DATA FOR EARLY DEVELOPMENT
INSERT INTO osaaminen(id, uri)
VALUES (1, 'urn:osaaminen1'),
       (2, 'urn:osaaminen2'),
       (3, 'urn:osaaminen3')
ON CONFLICT DO NOTHING
;;;

INSERT INTO osaaminen_kaannos(osaaminen_id, kaannos_key, nimi, kuvaus)
VALUES (1, 'FI', 'Osaaminen 1', 'Kuvaus 1'),
       (2, 'FI', 'Osaaminen 2', 'Kuvaus 2'),
       (3, 'FI', 'Osaaminen 3', 'Kuvaus 3')
ON CONFLICT DO NOTHING
;;;

INSERT INTO tyomahdollisuus(id)
VALUES ('495ae98d-593d-4a0f-8d36-daf5cceafdfd'),
       ('e5ff74e0-5eaa-466d-8989-326536c19763'),
       ('9b110e0c-297f-4b0a-8cf1-0c0a812b760b')
ON CONFLICT DO NOTHING
;;;

INSERT INTO tyomahdollisuus_kaannos(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma)
VALUES ('495ae98d-593d-4a0f-8d36-daf5cceafdfd', 'FI', 'Laborantti',
        'Laborantti analysoi näytteitä.'),
       ('e5ff74e0-5eaa-466d-8989-326536c19763', 'FI', 'Tarjoilija',
        'Tarjoilija tarjoilee asiakkaille ruokaa ja juomaa.'),
       ('9b110e0c-297f-4b0a-8cf1-0c0a812b760b', 'FI', 'Kuorma-autonkuljettaja',
        'Kuorma-autonkuljettaja kuljettaa tavaraa.')
ON CONFLICT DO NOTHING
;;;

-- RLS POLICIES
ALTER TABLE yksilo
  ENABLE ROW LEVEL SECURITY;;;
-- Use the force for now, eventually the tables should not be owned by the application user
ALTER TABLE yksilo
  FORCE ROW LEVEL SECURITY;;;
DROP POLICY IF EXISTS yksilo_policy ON yksilo;;;
CREATE POLICY yksilo_policy ON yksilo FOR ALL
  USING (id = current_setting('jod.yksilo_id', true)::uuid)
;;;

DO
$$
  DECLARE
    t  VARCHAR;
    tk VARCHAR;
  BEGIN
    FOREACH t IN ARRAY ARRAY ['koulutus', 'tyopaikka', 'toiminto', 'yksilon_osaaminen', 'koulutus_kategoria']
      LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY;', t);
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I;', t || '_policy', t);
        EXECUTE
          format(
              'CREATE POLICY %I ON %I FOR ALL ' ||
              'USING (yksilo_id = current_setting(''jod.yksilo_id'', true)::uuid)' ||
              'WITH CHECK (yksilo_id = current_setting(''jod.yksilo_id'', true)::uuid);',
              t || '_policy', t);
      END LOOP;

    FOREACH t IN ARRAY ARRAY ['koulutus', 'tyopaikka', 'toiminto', 'koulutus_kategoria']
      LOOP
        tk := t || '_kaannos';
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', tk);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY;', tk);
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I;', tk || '_policy', tk);
        EXECUTE format(
            'CREATE POLICY %I ON %I FOR ALL ' ||
            'USING (current_setting(''jod.yksilo_id'', true)::uuid = ' ||
            '(SELECT p.yksilo_id FROM %I p WHERE p.id = %I))' ||
            'WITH CHECK (current_setting(''jod.yksilo_id'', true)::uuid = ' ||
            '(SELECT p.yksilo_id FROM %I p WHERE p.id = %I));',
            tk || '_policy', tk,
            t, t || '_id',
            t, t || '_id');
      END LOOP;
  END
$$
;;;
