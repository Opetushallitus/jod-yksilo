-- Insert active Tyomahdollisuus 1
INSERT INTO tyomahdollisuus (id, aktiivinen, aineisto)
VALUES ('ca466237-ce1d-4aca-9f9b-2ed566ef4f94', TRUE, 'TMT');

INSERT INTO tyomahdollisuus_kaannos (tyomahdollisuus_id, kaannos_key, otsikko, kuvaus, tiivistelma,
                                     tehtavat, yleiset_vaatimukset)
VALUES ('ca466237-ce1d-4aca-9f9b-2ed566ef4f94', 'FI', 'Työmahdollisuus', 'Kuvaus', 'Tiivistelmä',
        'Tehtävät', 'Vaatimukset'),
       ('ca466237-ce1d-4aca-9f9b-2ed566ef4f94', 'EN', 'Job opportunity', 'Description', 'Summary',
        'Tasks', 'Requirements');

INSERT INTO tyomahdollisuus_jakauma (tyomahdollisuus_id, tyyppi, maara, tyhjia)
VALUES ('ca466237-ce1d-4aca-9f9b-2ed566ef4f94', 'AMMATTI', 2, 0),
       ('ca466237-ce1d-4aca-9f9b-2ed566ef4f94', 'OSAAMINEN', 3, 0);

-- Insert values for AMMATTI type
INSERT INTO tyomahdollisuus_jakauma_arvot (tyomahdollisuus_jakauma_id, arvo, osuus)
SELECT id, arvo, osuus
FROM tyomahdollisuus_jakauma
       CROSS JOIN (VALUES ('Software Developer', 0.6),
                          ('DevOps Engineer', 0.4)) AS vals(arvo, osuus)
WHERE tyomahdollisuus_id = 'ca466237-ce1d-4aca-9f9b-2ed566ef4f94'
  AND tyyppi = 'AMMATTI';

-- Insert values for OSAAMINEN type
INSERT INTO tyomahdollisuus_jakauma_arvot (tyomahdollisuus_jakauma_id, arvo, osuus)
SELECT id, uri, osuus
FROM tyomahdollisuus_jakauma
       CROSS JOIN (VALUES ('urn:osaaminen1', 0.4),
                          ('urn:osaaminen2', 0.3),
                          ('urn:osaaminen3', 0.3)) AS vals(uri, osuus)
WHERE tyomahdollisuus_id = 'ca466237-ce1d-4aca-9f9b-2ed566ef4f94'
  AND tyyppi = 'OSAAMINEN';


-- Insert active Tyomahdollisuus 2
INSERT INTO tyomahdollisuus (id, aktiivinen, aineisto)
VALUES ('cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949', TRUE, 'TMT');

INSERT INTO tyomahdollisuus_kaannos (tyomahdollisuus_id, kaannos_key, otsikko, kuvaus, tiivistelma,
                                     tehtavat, yleiset_vaatimukset)
VALUES ('cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949', 'FI', 'Työmahdollisuus', 'Kuvaus', 'Tiivistelmä',
        'Tehtävät', 'Vaatimukset'),
       ('cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949', 'EN', 'Job opportunity', 'Description', 'Summary',
        'Tasks', 'Requirements');

INSERT INTO tyomahdollisuus_jakauma (tyomahdollisuus_id, tyyppi, maara, tyhjia)
VALUES ('cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949', 'OSAAMINEN', 3, 0);

-- Insert values for OSAAMINEN type
INSERT INTO tyomahdollisuus_jakauma_arvot (tyomahdollisuus_jakauma_id, arvo, osuus)
SELECT id, uri, osuus
FROM tyomahdollisuus_jakauma
       CROSS JOIN (VALUES ('urn:osaaminen1', 0.4),
                          ('urn:osaaminen3', 0.3)) AS vals(uri, osuus)
WHERE tyomahdollisuus_id = 'cd5bb0b2-d09d-45e0-96e5-6c0c9a37a949'
  AND tyyppi = 'OSAAMINEN';



-- Insert inactive Tyomahdollisuus
INSERT INTO tyomahdollisuus (id, aktiivinen, aineisto)
VALUES ('af34f11f-05b5-434c-963a-df6d89a2149b', FALSE, 'AMMATTITIETO');

INSERT INTO tyomahdollisuus_kaannos (tyomahdollisuus_id, kaannos_key, otsikko, kuvaus, tiivistelma,
                                     tehtavat, yleiset_vaatimukset)
VALUES ('af34f11f-05b5-434c-963a-df6d89a2149b', 'FI', 'Työmahdollisuus', 'Kuvaus', 'Tiivistelmä',
        'Tehtävät', 'Vaatimukset'),
       ('af34f11f-05b5-434c-963a-df6d89a2149b', 'EN', 'Job opportunity', 'Description', 'Summary',
        'Tasks', 'Requirements');
