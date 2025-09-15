DO
$$
  DECLARE
    yid UUID;
    kid UUID;
    aid UUID;
    osaamis_kiinnostus_id BIGINT;
    ammatti_kiinnostus_id BIGINT;
    suosikki_id UUID;
    osaamis_id BIGINT;
    yksiloiden_maara BIGINT = 10;
    osaamis_kiinnostusten_maara BIGINT = 7;
    ammatti_kiinnostusten_maara BIGINT = 8;
    tm_suosikkien_maara BIGINT = 15;
    km_suosikkien_maara BIGINT = 4;
    yksilon_osaamisten_maara BIGINT = 10;
    paamaara_id UUID;
    paamaarien_maara BIGINT = 6;

  BEGIN
    FOR yksilo_index IN 1..yksiloiden_maara
      LOOP
        SELECT tunnistus.generate_yksilo_id(concat('MOCK:user_testdata', yksilo_index::text)) INTO yid;


        INSERT INTO yksilo(id) VALUES (yid) ON CONFLICT DO NOTHING;
        -- yksilon osaamiset
        FOR i IN 1..yksilon_osaamisten_maara
          LOOP
            SELECT id INTO osaamis_id from (select id, row_number() OVER (ORDER BY id) as rivi from osaaminen) as ir where rivi = i;
            INSERT INTO yksilon_osaaminen(id, lahde, osaaminen_id, yksilo_id) VALUES (gen_random_uuid(), 'MUU_OSAAMINEN', osaamis_id, yid);
          END LOOP;

        -- yksilon osaamis kiinnostukset
        FOR i IN 1..osaamis_kiinnostusten_maara
          LOOP
            SELECT id INTO osaamis_kiinnostus_id from (select id, row_number() OVER (ORDER BY id) as rivi from osaaminen) as ir where rivi = i+20;
            INSERT INTO yksilo_osaamis_kiinnostukset(osaamis_kiinnostukset_id, yksilo_id) VALUES (osaamis_kiinnostus_id, yid);
          END LOOP;

        -- yksilon ammatti kiinnostukset
        FOR i IN 1..ammatti_kiinnostusten_maara
          LOOP
            SELECT id INTO ammatti_kiinnostus_id from (select id, row_number() OVER (ORDER BY id) as rivi from ammatti) as ir where rivi = i;
            INSERT INTO yksilo_ammatti_kiinnostukset(ammatti_kiinnostukset_id, yksilo_id) VALUES (ammatti_kiinnostus_id, yid);
          END LOOP;

        -- yksilon työ suosikit
        FOR i IN 1..tm_suosikkien_maara
          LOOP
            SELECT id INTO suosikki_id from (select id, row_number() OVER (ORDER BY id) as rivi from tyomahdollisuus) as ir where rivi = i;
            INSERT INTO yksilon_suosikki(id, luotu, tyomahdollisuus_id, tyyppi, yksilo_id) VALUES (gen_random_uuid(), now(), suosikki_id, 'TYOMAHDOLLISUUS', yid);
          END LOOP;

        -- yksilon koulutus suosikit
        FOR i IN 1..km_suosikkien_maara
          LOOP
            SELECT id INTO suosikki_id from (select id, row_number() OVER (ORDER BY id) as rivi from koulutusmahdollisuus) as ir where rivi = (i+1);
            INSERT INTO yksilon_suosikki(id, luotu, koulutusmahdollisuus_id, tyyppi, yksilo_id) VALUES (gen_random_uuid(), now(), suosikki_id, 'KOULUTUSMAHDOLLISUUS', yid);
          END LOOP;

        -- yksilon päämäärät
        FOR i IN 1..paamaarien_maara
          LOOP
            SELECT id INTO paamaara_id from (select id, row_number() OVER (ORDER BY id) as rivi from koulutusmahdollisuus) as ir where rivi = (i+20);
            INSERT INTO paamaara(id, luotu, koulutusmahdollisuus_id, yksilo_id) VALUES (gen_random_uuid(), now(), paamaara_id, yid);
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

              END LOOP;
          END LOOP;
      END LOOP;

  END
$$ LANGUAGE plpgsql;
