ALTER TABLE tavoite_kaannos
  ADD COLUMN IF NOT EXISTS kuvaus TEXT;

ALTER TABLE tavoite
  DROP CONSTRAINT IF EXISTS paamaara_tyyppi_check;
ALTER TABLE tavoite
  DROP CONSTRAINT IF EXISTS paamaara_check;

ALTER TABLE polun_suunnitelma
  ADD COLUMN IF NOT EXISTS koulutusmahdollisuus_id UUID;
ALTER TABLE polun_suunnitelma
  ADD CONSTRAINT fk_ps_koulutusmahdollisuus
    FOREIGN KEY (koulutusmahdollisuus_id) REFERENCES koulutusmahdollisuus (id);

ALTER TABLE polun_suunnitelma_kaannos
  ADD COLUMN IF NOT EXISTS kuvaus TEXT;

DROP TABLE IF EXISTS polun_vaihe CASCADE;
DROP TABLE IF EXISTS polun_vaihe_kaannos CASCADE;
DROP TABLE IF EXISTS polun_vaihe_linkit CASCADE;
DROP TABLE IF EXISTS polun_vaihe_osaamiset CASCADE;
DROP TABLE IF EXISTS polun_suunnitelma_ignored_osaamiset;

ALTER TABLE tavoite DROP COLUMN IF EXISTS tyyppi;
