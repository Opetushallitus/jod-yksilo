INSERT INTO osaaminen(id, uri)
SELECT id, 'urn:osaaminen:' || id
FROM generate_series(1, 9) AS id;

INSERT INTO osaaminen_kaannos
  (osaaminen_id, kaannos_key, nimi, kuvaus)
SELECT id,
       lang,
       lang || ' skill ' || id,
       lang || ' description ' || id
FROM osaaminen
       cross join (values ('EN'), ('FI'), ('SV')) as lang(lang);

INSERT INTO osaaminen_versio(versio) values (1);

INSERT INTO ammatti(id, koodi, uri)
SELECT id, id, 'urn:ammatti:' || id
FROM generate_series(1, 9) AS id;

INSERT INTO ammatti_kaannos
  (ammatti_id, kaannos_key, nimi, kuvaus)
SELECT id,
       lang,
       lang || ' occupation ' || id,
       lang || ' description ' || id
FROM ammatti
       cross join (values ('EN'), ('FI'), ('SV')) as lang(lang);

INSERT INTO ammatti_versio(versio) values (1);
