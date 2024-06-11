INSERT INTO osaaminen(id, uri)
VALUES (1, 'urn:osaaminen1'),
       (2, 'urn:osaaminen2'),
       (3, 'urn:osaaminen3'),
       (4, 'urn:osaaminen4'),
       (5, 'urn:osaaminen5'),
       (6, 'urn:osaaminen6'),
       (7, 'urn:osaaminen7');

-- TEST YKSILO DATA
INSERT INTO yksilo (id, tunnus)
VALUES ('00000000-0000-0000-0000-000000000001', 'test_user1'),
       ('00000000-0000-0000-0000-000000000002', 'test_user2'),
       ('00000000-0000-0000-0000-000000000003', 'test_user3');

-- TEST KOULUTUS DATA FOR test_user1
insert into koulutus (id, alku_pvm, loppu_pvm, yksilo_id) values ('00000000-0000-0000-0000-000000000001', now()::date, now()::date, '00000000-0000-0000-0000-000000000001');
insert into koulutus (id, alku_pvm, loppu_pvm, yksilo_id) values ('00000000-0000-0000-0000-000000000002', now()::date, now()::date, '00000000-0000-0000-0000-000000000001');
insert into koulutus (id, alku_pvm, loppu_pvm, yksilo_id) values ('00000000-0000-0000-0000-000000000003', now()::date, now()::date, '00000000-0000-0000-0000-000000000001');


insert into koulutus_kaannos (koulutus_id, kaannos_key, nimi, kuvaus)
values
  ( '00000000-0000-0000-0000-000000000001', 'FI', 'koulutus1 fi', 'koulutus 1 kuvaus fi'),
  ( '00000000-0000-0000-0000-000000000001', 'SV', 'koulutus1 sv', 'koulutus 1 kuvaus sv'),
  ( '00000000-0000-0000-0000-000000000001', 'EN', 'koulutus1 en', 'koulutus 1 kuvaus en');



