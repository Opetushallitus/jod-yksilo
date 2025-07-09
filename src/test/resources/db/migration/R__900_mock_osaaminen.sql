INSERT INTO osaaminen(id, uri)
VALUES (1, 'urn:osaaminen1'),
       (2, 'urn:osaaminen2'),
       (3, 'urn:osaaminen3'),
       (4, 'urn:osaaminen4'),
       (5, 'urn:osaaminen5'),
       (6, 'urn:osaaminen6'),
       (7, 'urn:osaaminen7');
INSERT INTO osaaminen_kaannos
  (osaaminen_id, kaannos_key, kuvaus, nimi)
VALUES (1, 'EN',
        'Assign and manage staff tasks in areas such as scoring, arranging, copying music and vocal coaching.',
        'manage musical staff');
INSERT INTO osaaminen_kaannos
  (osaaminen_id, kaannos_key, kuvaus, nimi)
VALUES (1, 'FI',
        'Henkilöstön tehtävien osoittaminen ja johtaminen sellaisilla aloilla kuin pisteytys, järjestäminen, musiikin kopioiminen ja ääniohjaus.',
        'johtaa musiikillista henkilökuntaa');
INSERT INTO osaaminen_kaannos
  (osaaminen_id, kaannos_key, kuvaus, nimi)
VALUES (1, 'SV',
        'Fördela och hantera arbetsuppgifter på olika områden såsom partitur, arrangering och kopiering av musik samt röstcoachning.',
        'ansvara för musikpersonal');
