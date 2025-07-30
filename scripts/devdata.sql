DO
$$
  DECLARE
    yid UUID;
    kid UUID;
    aid UUID;
    osaamis_kiinnostus_id BIGINT;
    yksiloiden_maara BIGINT = 1;
    osaamis_kiinnostusten_maara BIGINT = 1;

  BEGIN
    FOR i IN 1..yksiloiden_maara
      LOOP
        SELECT tunnistus.generate_yksilo_id(concat('MOCK:use56r18756456764433', i::text)) INTO yid;
        --IF EXISTS(SELECT yid FROM YKSILO) THEN
        --    RETURN;
        --END IF;

        INSERT INTO yksilo(id) VALUES (yid) ON CONFLICT DO NOTHING;
        -- yksilon kiinnostukset
        FOR i IN 1..osaamis_kiinnostusten_maara
          LOOP
            SELECT id INTO osaamis_kiinnostus_id from (select id, row_number() OVER (ORDER BY id) as rivi from osaaminen) as ir where rivi = i ;
            INSERT INTO yksilo_osaamis_kiinnostukset(osaamis_kiinnostukset_id, yksilo_id) VALUES (osaamis_kiinnostus_id, yid);
          END LOOP;

        FOR i IN 1..5
          LOOP
            SELECT gen_random_uuid() INTO kid;
            INSERT INTO tyopaikka(id, yksilo_id) VALUES (kid, yid);
            INSERT INTO tyopaikka_kaannos(tyopaikka_id, kaannos_key, nimi) VALUES (kid, 'FI', 'Testityöpaikka ' || i);

            FOR j IN 1..3
              LOOP
                SELECT gen_random_uuid() INTO aid;

                INSERT INTO toimenkuva(id, tyopaikka_id, alku_pvm) VALUES (aid, kid, NOW());
                INSERT INTO toimenkuva_kaannos(toimenkuva_id, kaannos_key, nimi)
                VALUES (aid, 'FI', 'Testitoimenkuva ' || i || j);

                INSERT INTO yksilon_osaaminen (id, yksilo_id, osaaminen_id, lahde, toimenkuva_id)
                SELECT gen_random_uuid(), yid, o.id, 'TOIMENKUVA', aid
                FROM (SELECT id FROM OSAAMINEN TABLESAMPLE bernoulli(i / 20.0)) o;
              END LOOP;
          END LOOP;

        FOR i IN 1..5
          LOOP
            SELECT gen_random_uuid() INTO kid;
            INSERT INTO tyopaikka(id, yksilo_id) VALUES (kid, yid);
            INSERT INTO tyopaikka_kaannos(tyopaikka_id, kaannos_key, nimi)
            VALUES (kid, 'FI', 'Testityöpaikka ' || i);

            FOR j IN 1..3
              LOOP
                SELECT gen_random_uuid() INTO aid;

                INSERT INTO toimenkuva(id, tyopaikka_id, alku_pvm) VALUES (aid, kid, NOW());
                INSERT INTO toimenkuva_kaannos(toimenkuva_id, kaannos_key, nimi)
                VALUES (aid, 'FI', 'Testitoimenkuva ' || i || j);

                INSERT INTO yksilon_osaaminen (id, yksilo_id, osaaminen_id, lahde, toimenkuva_id)
                SELECT gen_random_uuid(), yid, o.id, 'TOIMENKUVA', aid
                FROM (SELECT id FROM OSAAMINEN TABLESAMPLE bernoulli(i / 20.0)) o;
              END LOOP;
          END LOOP;

        FOR i IN 1..5
          LOOP
            SELECT gen_random_uuid() INTO kid;
            INSERT INTO koulutus_kokonaisuus(id, yksilo_id) VALUES (kid, yid);
            INSERT INTO koulutus_kokonaisuus_kaannos(koulutus_kokonaisuus_id, kaannos_key, nimi)
            VALUES (kid, 'FI', 'Testikoulutus ' || i);

            FOR j IN 1..2
              LOOP
                SELECT gen_random_uuid() INTO aid;

                INSERT INTO koulutus(id, kokonaisuus_id, alku_pvm) VALUES (aid, kid, NOW());
                INSERT INTO koulutus_kaannos(koulutus_id, kaannos_key, nimi)
                VALUES (aid, 'FI', 'Testitutkinto ' || i || j);

                INSERT INTO yksilon_osaaminen (id, yksilo_id, osaaminen_id, lahde, koulutus_id)
                SELECT gen_random_uuid(), yid, o.id, 'KOULUTUS', aid
                FROM (SELECT id FROM OSAAMINEN TABLESAMPLE bernoulli(i * j / 20.0)) o;
              END LOOP;
          END LOOP;

        FOR i IN 1..3
          LOOP
            SELECT gen_random_uuid() INTO kid;
            INSERT INTO toiminto(id, yksilo_id) VALUES (kid, yid);
            INSERT INTO yksilo.toiminto_kaannos(toiminto_id, kaannos_key, nimi)
            VALUES (kid, 'FI', 'Testitoiminto ' || i);

            FOR j IN 1..2
              LOOP
                SELECT gen_random_uuid() INTO aid;

                INSERT INTO patevyys(id, toiminto_id, alku_pvm) VALUES (aid, kid, NOW());
                INSERT INTO patevyys_kaannos(patevyys_id, kaannos_key, nimi)
                VALUES (aid, 'FI', 'Testipatevyys ' || i || j);

                INSERT INTO yksilon_osaaminen (id, yksilo_id, osaaminen_id, lahde, patevyys_id)
                SELECT gen_random_uuid(), yid, o.id, 'PATEVYYS', aid
                FROM (SELECT id FROM OSAAMINEN TABLESAMPLE bernoulli(j / 20.0)) o;
              END LOOP;
          END LOOP;

        INSERT INTO yksilon_osaaminen (id, yksilo_id, osaaminen_id, lahde)
        SELECT gen_random_uuid(), yid, o.id, 'MUU_OSAAMINEN'
        FROM (SELECT id FROM OSAAMINEN TABLESAMPLE bernoulli(0.02)) o;

      END LOOP;

  END
$$ LANGUAGE plpgsql;
