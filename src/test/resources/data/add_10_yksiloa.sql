DO
$$
  DECLARE
    yid UUID;
  BEGIN

    INSERT INTO tyomahdollisuus (id)
    SELECT gen_random_uuid()
    FROM generate_series(1, 8);

    INSERT INTO koulutusmahdollisuus (id, tyyppi)
    SELECT gen_random_uuid(), 'TUTKINTO'
    FROM generate_series(1, 8);

    FOR yksilo_index IN 1..10
      LOOP
        SELECT tunnistus.generate_yksilo_id(CONCAT('MOCK:user_testdata:', yksilo_index::TEXT))
        INTO yid;
        INSERT INTO yksilo(id) VALUES (yid) ON CONFLICT DO NOTHING;

        -- osaamis kiinnostukset
        INSERT INTO yksilon_osaaminen(id, lahde, osaaminen_id, yksilo_id)
        SELECT gen_random_uuid(), 'MUU_OSAAMINEN', o.id, yid
        FROM osaaminen o
        LIMIT 4;

        -- ammatti kiinnostukset
        INSERT INTO yksilo_osaamis_kiinnostukset(yksilo_id, osaamis_kiinnostukset_id)
        SELECT yid, o.id
        FROM osaaminen o
        LIMIT 4 OFFSET 2;

        -- ammatti kiinnostukset
        INSERT INTO yksilo_ammatti_kiinnostukset(yksilo_id, ammatti_kiinnostukset_id)
        SELECT yid, a.id
        FROM ammatti a
        LIMIT 5;

        -- ammattisuosikit
        INSERT INTO yksilon_suosikki(id, luotu, yksilo_id, tyomahdollisuus_id)
        SELECT gen_random_uuid(), NOW(), yid, t.id
        FROM tyomahdollisuus t
        LIMIT 6;

        -- koulutussuosikit
        INSERT INTO yksilon_suosikki(id, luotu, yksilo_id, koulutusmahdollisuus_id)
        SELECT gen_random_uuid(), NOW(), yid, k.id
        FROM koulutusmahdollisuus k
        LIMIT 7;

        -- päämäärät
        INSERT INTO tavoite(id, luotu, tyomahdollisuus_id, yksilo_id)
        SELECT gen_random_uuid(), NOW(), k.id, yid
        FROM tyomahdollisuus k
        LIMIT 8;
      END LOOP;
  END
$$ LANGUAGE plpgsql
;;;
