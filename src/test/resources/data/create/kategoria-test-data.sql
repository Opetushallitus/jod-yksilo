-- TEST YKSILO DATA
INSERT INTO yksilo (id, tunnus)
VALUES ('00000000-0000-0000-0000-000000000001', 'test_user1'),
       ('00000000-0000-0000-0000-000000000002', 'test_user2'),
       ('00000000-0000-0000-0000-000000000003', 'test_user3');

-- TEST KATEGORIA DATA
insert into koulutus_kategoria (id, yksilo_id) values ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');

insert into koulutus_kategoria_kaannos (koulutus_kategoria_id, kaannos_key, nimi, kuvaus)
values
  ( '00000000-0000-0000-0000-000000000001', 'FI', 'kategoria fi', 'kategoria 1 kuvaus fi'),
  ( '00000000-0000-0000-0000-000000000001', 'SV', 'kategoria sv', 'kategoria 1 kuvaus sv'),
  ( '00000000-0000-0000-0000-000000000001', 'EN', 'kategoria en', 'kategoria 1 kuvaus en');



