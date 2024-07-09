-- TEST DATA FOR EARLY DEVELOPMENT
INSERT INTO yksilo (id, tunnus)
VALUES ('495ae98d-593d-4a0f-8d36-daf5cceafdfd', 'user1'),
       ('e5ff74e0-5eaa-466d-8989-326536c19763', 'user2'),
       ('9b110e0c-297f-4b0a-8cf1-0c0a812b760b', 'user3')
ON CONFLICT DO NOTHING;

INSERT INTO osaaminen(id, uri)
VALUES (1, 'urn:osaaminen1'),
       (2, 'urn:osaaminen2'),
       (3, 'urn:osaaminen3')
ON CONFLICT DO NOTHING;

INSERT INTO osaaminen_kaannos(osaaminen_id, kaannos_key, nimi, kuvaus)
VALUES (1, 'FI', 'Osaaminen 1', 'Kuvaus 1'),
       (2, 'FI', 'Osaaminen 2', 'Kuvaus 2'),
       (3, 'FI', 'Osaaminen 3', 'Kuvaus 3')
ON CONFLICT DO NOTHING;

INSERT INTO tyomahdollisuus(id)
VALUES ('495ae98d-593d-4a0f-8d36-daf5cceafdfd'),
       ('e5ff74e0-5eaa-466d-8989-326536c19763'),
       ('9b110e0c-297f-4b0a-8cf1-0c0a812b760b')
ON CONFLICT DO NOTHING;

INSERT INTO tyomahdollisuus_kaannos(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma)
VALUES ('495ae98d-593d-4a0f-8d36-daf5cceafdfd', 'FI', 'Laborantti',
        'Laborantti analysoi näytteitä.'),
       ('e5ff74e0-5eaa-466d-8989-326536c19763', 'FI', 'Tarjoilija',
        'Tarjoilija tarjoilee asiakkaille ruokaa ja juomaa.'),
       ('9b110e0c-297f-4b0a-8cf1-0c0a812b760b', 'FI', 'Kuorma-autonkuljettaja',
        'Kuorma-autonkuljettaja kuljettaa tavaraa.')
ON CONFLICT DO NOTHING;
