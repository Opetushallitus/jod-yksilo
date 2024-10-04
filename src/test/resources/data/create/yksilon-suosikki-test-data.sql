INSERT INTO tyomahdollisuus_data.tyomahdollisuus
(id, json_id)
VALUES('00016334-886e-4d11-93f0-872fcf671920'::uuid, 'a20e0454-c949-42b9-9198-4da730a6c43d'::uuid);
INSERT INTO tyomahdollisuus_data.tyomahdollisuus
(id, json_id)
VALUES('00143beb-0817-4e6d-9107-57d0245b57ee'::uuid, '51dbc87e-0261-473c-acfc-8cd20a945468'::uuid);
INSERT INTO tyomahdollisuus_data.tyomahdollisuus
(id, json_id)
VALUES('0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid, 'c8ee8856-68f1-4f28-a096-628298bd8134'::uuid);

INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00016334-886e-4d11-93f0-872fcf671920'::uuid, 'FI', 'Muoviasentaja', 'Muoviasentajan tiivistelmä', 'Muoviasentajan kuvaus.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00016334-886e-4d11-93f0-872fcf671920'::uuid, 'SV', 'Installatör av plastprodukter', 'Sammanfattning på svenska av plastprodukter.', 'Beskrivning på svenska av plastprodukter.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00016334-886e-4d11-93f0-872fcf671920'::uuid, 'EN', 'Plastic product installer', 'Summary in English of plastic product installer.', 'Description in English of plastic product installer.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00143beb-0817-4e6d-9107-57d0245b57ee'::uuid, 'FI', 'Puheterapeutti', 'Puheterapeutin suomenkielinen yhteenveto.', 'Puheterapeutin suomenkielinen kuvaus.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00143beb-0817-4e6d-9107-57d0245b57ee'::uuid, 'SV', 'Logoped', 'Logopedens sammanfattning på svenska.', 'Beskrivning på svenska av logopeden.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('00143beb-0817-4e6d-9107-57d0245b57ee'::uuid, 'EN', 'Speech therapist', 'Summary in English of the speech therapist.', 'Description in English of the speech therapist.');

INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid, 'FI', 'Automaatioasentaja', 'Automaatioasentajan suomenkielinen yhteenveto.', 'Automaatioasentajan suomenkielinen kuvaus.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid, 'SV', 'Automation installer', 'Automationsinstallatörens sammanfattning på svenska.', 'Beskrivning av automationsinstallatören på svenska.');
INSERT INTO tyomahdollisuus_data.tyomahdollisuus_kaannos
(tyomahdollisuus_id, kaannos_key, otsikko, tiivistelma, kuvaus)
VALUES('0014885a-4aa6-4202-9865-2fcb4457cc59'::uuid, 'EN', 'Automation installer', 'Summary of Automation installer in English.', 'Description of Automation installer in English.');

