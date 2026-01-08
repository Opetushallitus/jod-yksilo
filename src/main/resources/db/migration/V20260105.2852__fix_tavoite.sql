DELETE
FROM polun_suunnitelma_osaamiset
WHERE polun_suunnitelma_id IN (SELECT id
                               FROM polun_suunnitelma
                               WHERE tavoite_id IN
                                     (SELECT id FROM tavoite WHERE tyomahdollisuus_id IS NULL));

DELETE
FROM polun_suunnitelma_kaannos
WHERE polun_suunnitelma_id IN (SELECT id
                               FROM polun_suunnitelma
                               WHERE tavoite_id IN
                                     (SELECT id FROM tavoite WHERE tyomahdollisuus_id IS NULL));

DELETE
FROM polun_suunnitelma
WHERE tavoite_id IN (SELECT id FROM tavoite WHERE tyomahdollisuus_id IS NULL);

DELETE
FROM tavoite
WHERE tyomahdollisuus_id IS NULL;

ALTER TABLE tavoite
  DROP COLUMN koulutusmahdollisuus_id;

ALTER TABLE tavoite
  ALTER COLUMN tyomahdollisuus_id SET NOT NULL;


