DO '
DECLARE
  yid UUID;
BEGIN
INSERT INTO osaaminen(id, uri) VALUES
  (10, ''45425435''),
  (12, ''regfdg''),
  (13, ''454254eg35''),
  (14, ''34234324''),
  (101, ''454254334325'') ,
  (121, ''regfddfgdfgg'') ,
  (131, ''454254eggfdgdf35'') ,
  (141, ''342343v bvcb24''),
  (1011, ''454254354325425'') ,
  (1211, ''regfdfdgdfgg'') ,
  (1311, ''454254egdfgfdgdfg4335'') ,
  (1411, ''342343fdgfdgfdgd24'');

INSERT INTO ammatti(id, uri, koodi)
VALUES (1, ''urn:ammatti1'', ''koodi1''),
  (2, ''urn:ammatti2'', ''koodi2''),
  (3, ''urn:ammatti3'', ''koodi3''),
  (4, ''urn:ammatti4'', ''koodi4''),
  (5, ''urn:ammatti5'', ''koodi5''),
  (6, ''urn:ammatti6'', ''koodi6''),
  (7, ''urn:ammatti7'', ''koodi7'');

INSERT INTO tyomahdollisuus (id, ammattiryhma)
VALUES (gen_random_uuid(), ''jotain''),
  (gen_random_uuid(), ''fdgfdg''),
  (gen_random_uuid(), ''gfdgfd''),
  (gen_random_uuid(), ''fgddgfdbvc''),
  (gen_random_uuid(), ''54654u''),
  (gen_random_uuid(), ''4y54y''),
  (gen_random_uuid(), ''54jhnbg''),
  (gen_random_uuid(), ''btrb54yy'');


INSERT INTO koulutusmahdollisuus (id, tyyppi)
VALUES (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO''),
  (gen_random_uuid(), ''TUTKINTO'');

FOR yksilo_index IN 1..10 LOOP
  SELECT tunnistus.generate_yksilo_id(CONCAT(''MOCKuser_testdata'', yksilo_index::TEXT)) INTO yid;
  INSERT INTO yksilo(id) VALUES (yid) ON CONFLICT DO NOTHING;

  -- osaamis kiinnostukset
  INSERT INTO yksilon_osaaminen(id, lahde, osaaminen_id, yksilo_id)
  SELECT gen_random_uuid(), ''MUU_OSAAMINEN'', o.id, yid
  FROM osaaminen o
  LIMIT 4;

  -- ammatti kiinnostukset
  INSERT INTO yksilo_osaamis_kiinnostukset(yksilo_id, osaamis_kiinnostukset_id)
  SELECT yid, o.id FROM osaaminen o LIMIT 4 OFFSET 2;

  -- ammatti kiinnostukset
  INSERT INTO yksilo_ammatti_kiinnostukset(yksilo_id, ammatti_kiinnostukset_id)
  SELECT yid, a.id FROM ammatti a LIMIT 5;

  -- ammattisuosikit
  INSERT INTO yksilon_suosikki(id, luotu, yksilo_id, tyomahdollisuus_id)
  SELECT gen_random_uuid(), NOW(), yid, t.id FROM tyomahdollisuus t LIMIT 6;

  -- koulutussuosikit
  INSERT INTO yksilon_suosikki(id, luotu, yksilo_id, koulutusmahdollisuus_id)
  SELECT gen_random_uuid(), NOW(), yid, k.id FROM koulutusmahdollisuus k LIMIT 7;

  -- päämäärät
  INSERT INTO tavoite(id, luotu, tyyppi, koulutusmahdollisuus_id, yksilo_id)
  SELECT gen_random_uuid(), NOW(), ''MUU'', k.id, yid FROM koulutusmahdollisuus k LIMIT 8;
  END LOOP;
END
' LANGUAGE plpgsql;
