CREATE INDEX idx_koulutusmahdollisuus_kaannos_otsikko_trgm ON koulutusmahdollisuus_kaannos
  USING gin (otsikko gin_trgm_ops);
CREATE INDEX idx_koulutusmahdollisuus_kaannos_tiivistelma_trgm ON koulutusmahdollisuus_kaannos
  USING gin (tiivistelma gin_trgm_ops);
CREATE INDEX idx_koulutusmahdollisuus_kaannos_kuvaus_trgm ON koulutusmahdollisuus_kaannos
  USING gin (kuvaus gin_trgm_ops);

CREATE INDEX idx_tyomahdollisuus_kaannos_otsikko_trgm ON tyomahdollisuus_kaannos
  USING gin (otsikko gin_trgm_ops);
CREATE INDEX idx_tyomahdollisuus_kaannos_tiivistelma_trgm ON tyomahdollisuus_kaannos
  USING gin (tiivistelma gin_trgm_ops);
CREATE INDEX idx_tyomahdollisuus_kaannos_kuvaus_trgm ON tyomahdollisuus_kaannos
  USING gin (kuvaus gin_trgm_ops);
