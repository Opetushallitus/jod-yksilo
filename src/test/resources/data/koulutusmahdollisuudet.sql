-- Insert active Koulutusmahdollisuus 1
INSERT INTO Koulutusmahdollisuus (id, aktiivinen, tyyppi)
VALUES ('481e204a-691a-48dd-9b01-7f08d5858db9', TRUE, 'TUTKINTO');

INSERT INTO koulutusmahdollisuus_kaannos (koulutusmahdollisuus_id, kaannos_key, otsikko, kuvaus,
                                          tiivistelma)
VALUES ('481e204a-691a-48dd-9b01-7f08d5858db9', 'FI', 'Koulutusmahdollisuus 2', 'Kuvaus',
        'Tiivistelmä'),
       ('481e204a-691a-48dd-9b01-7f08d5858db9', 'EN', 'Educational opportunity 2', 'Description',
        'Summary');

INSERT INTO koulutusmahdollisuus_jakauma (koulutusmahdollisuus_id, tyyppi, maara, tyhjia)
VALUES ('481e204a-691a-48dd-9b01-7f08d5858db9', 'KOULUTUSALA', 3, 0),
       ('481e204a-691a-48dd-9b01-7f08d5858db9', 'OSAAMINEN', 5, 0);


-- Insert values for KOULUTUSALA type
INSERT INTO koulutusmahdollisuus_jakauma_arvot (koulutusmahdollisuus_jakauma_id, arvo, osuus)
SELECT id, arvo, osuus
FROM koulutusmahdollisuus_jakauma
       CROSS JOIN (VALUES ('Tietojenkäsittely', 2),
                          ('Musiikki', 1)) AS vals(arvo, osuus)
WHERE koulutusmahdollisuus_id = '481e204a-691a-48dd-9b01-7f08d5858db9'
  AND tyyppi = 'KOULUTUSALA';

-- Insert values for OSAAMINEN type
INSERT INTO koulutusmahdollisuus_jakauma_arvot (koulutusmahdollisuus_jakauma_id, arvo, osuus)
SELECT id, arvo, osuus
FROM koulutusmahdollisuus_jakauma
       CROSS JOIN (VALUES ('urn:osaaminen:1', 2),
                          ('urn:osaaminen:2', 1)) AS vals(arvo, osuus)
WHERE koulutusmahdollisuus_id = '481e204a-691a-48dd-9b01-7f08d5858db9'
  AND tyyppi = 'OSAAMINEN';


-- Insert active Koulutusmahdollisuus 2
INSERT INTO Koulutusmahdollisuus (id, aktiivinen, tyyppi)
VALUES ('c11249fd-e0a3-4b23-8de5-9dc67a157f46', TRUE, 'TUTKINTO');

INSERT INTO koulutusmahdollisuus_kaannos (koulutusmahdollisuus_id, kaannos_key, otsikko, kuvaus,
                                          tiivistelma)
VALUES ('c11249fd-e0a3-4b23-8de5-9dc67a157f46', 'FI', 'Koulutusmahdollisuus 1', 'Kuvaus',
        'Tiivistelmä'),
       ('c11249fd-e0a3-4b23-8de5-9dc67a157f46', 'EN', 'Educational opportunity 1', 'Description',
        'Summary');

INSERT INTO koulutusmahdollisuus_jakauma (koulutusmahdollisuus_id, tyyppi, maara, tyhjia)
VALUES ('c11249fd-e0a3-4b23-8de5-9dc67a157f46', 'KOULUTUSALA', 3, 0),
       ('c11249fd-e0a3-4b23-8de5-9dc67a157f46', 'OSAAMINEN', 5, 0);

-- Insert values for OSAAMINEN type
INSERT INTO koulutusmahdollisuus_jakauma_arvot (koulutusmahdollisuus_jakauma_id, arvo, osuus)
SELECT id, arvo, osuus
FROM koulutusmahdollisuus_jakauma
       CROSS JOIN (VALUES ('urn:osaaminen:1', 2),
                          ('urn:osaaminen:2', 1),
                          ('urn:osaaminen:3', 2),
                          ('urn:osaaminen:4', 3),
                          ('urn:osaaminen:5', 4),
                          ('urn:osaaminen:6', 6)) AS vals(arvo, osuus)
WHERE koulutusmahdollisuus_id = 'c11249fd-e0a3-4b23-8de5-9dc67a157f46'
  AND tyyppi = 'OSAAMINEN';


-- Insert inactive Koulutusmahdollisuus
INSERT INTO Koulutusmahdollisuus (id, aktiivinen, tyyppi)
VALUES ('c74eed41-c729-433e-8d36-4fc7527fe3df', FALSE, 'EI_TUTKINTO');

INSERT INTO koulutusmahdollisuus_kaannos (koulutusmahdollisuus_id, kaannos_key, otsikko, kuvaus,
                                          tiivistelma)
VALUES ('c74eed41-c729-433e-8d36-4fc7527fe3df', 'FI', 'Koulutusmahdollisuus 3', 'Kuvaus',
        'Tiivistelmä'),
       ('c74eed41-c729-433e-8d36-4fc7527fe3df', 'EN', 'Educational opportunity 3', 'Description',
        'Summary');
