INSERT INTO tyomahdollisuus (id, aineisto)
VALUES ('00016334-886e-4d11-93f0-872fcf671920'::uuid, 'TMT'),
       ('00143beb-0817-4e6d-9107-57d0245b57ee'::uuid, 'TMT'),
       ('0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid, 'TMT');

INSERT INTO tyomahdollisuus_jakauma (id, maara, tyhjia, tyyppi, tyomahdollisuus_id)
VALUES (1, 10, 2, 'OSAAMINEN', '00016334-886e-4d11-93f0-872fcf671920'::uuid),
       (2, 20, 3, 'OSAAMINEN', '00143beb-0817-4e6d-9107-57d0245b57ee'::uuid),
       (3, 30, 4, 'OSAAMINEN', '0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid);

INSERT INTO tyomahdollisuus_jakauma_arvot (tyomahdollisuus_jakauma_id, arvo, osuus)
VALUES (1, 'urn:osaaminen:1', 3),
       (1, 'urn:osaaminen:2', 2),
       (1, 'urn:osaaminen:3', 1),
       (1, 'urn:osaaminen:4', 1);

INSERT INTO koulutusmahdollisuus (id, tyyppi)
VALUES ('00016334-886e-4d11-93f0-872fcf671921'::uuid, 'TUTKINTO'),
       ('00143beb-0817-4e6d-9107-57d0245b57e1'::uuid, 'TUTKINTO'),
       ('0014885a-4aa6-4202-9865-2fcb4457cc51'::uuid, 'TUTKINTO');

INSERT INTO koulutusmahdollisuus_jakauma (id, maara, tyhjia, tyyppi, koulutusmahdollisuus_id)
VALUES (1, 10, 2, 'OSAAMINEN', '00016334-886e-4d11-93f0-872fcf671921'::uuid),
       (2, 20, 3, 'OSAAMINEN', '00143beb-0817-4e6d-9107-57d0245b57e1'::uuid),
       (3, 30, 4, 'OSAAMINEN', '0014885a-4aa6-4202-9865-2fcb4457cc51'::uuid);

INSERT INTO koulutusmahdollisuus_jakauma_arvot (koulutusmahdollisuus_jakauma_id, arvo, osuus)
VALUES (1, 'urn:osaaminen:1', 3),
       (1, 'urn:osaaminen:2', 2),
       (1, 'urn:osaaminen:3', 1),
       (1, 'urn:osaaminen:4', 1),
       (2, 'urn:osaaminen:5', 1),
       (2, 'urn:osaaminen:6', 1),
       (3, 'urn:osaaminen:7', 1);
