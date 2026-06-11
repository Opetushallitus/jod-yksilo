ALTER TABLE patevyys RENAME TO toiminto;
ALTER TABLE patevyys_kaannos RENAME TO toiminto_kaannos;

ALTER TABLE toiminto_kaannos RENAME COLUMN patevyys_id TO toiminto_id;
ALTER TABLE yksilon_osaaminen RENAME COLUMN patevyys_id TO toiminto_id;

ALTER TABLE toiminto RENAME CONSTRAINT patevyys_pkey TO toiminto_pkey;
ALTER TABLE toiminto_kaannos RENAME CONSTRAINT patevyys_kaannos_pkey TO toiminto_kaannos_pkey;
ALTER TABLE toiminto_kaannos RENAME CONSTRAINT patevyys_kaannos_kaannos_key_check TO toiminto_kaannos_kaannos_key_check;

ALTER TABLE yksilon_osaaminen DROP CONSTRAINT yksilon_osaaminen_lahde_check;
UPDATE yksilon_osaaminen SET lahde = 'TOIMINTO' WHERE lahde = 'PATEVYYS';
ALTER TABLE yksilon_osaaminen
  ADD CONSTRAINT yksilon_osaaminen_lahde_check
    CHECK (lahde IN ('TOIMENKUVA', 'KOULUTUS', 'TOIMINTO', 'MUU_OSAAMINEN'));
