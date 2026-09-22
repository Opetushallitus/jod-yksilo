-- OPHJOD-3583: Add KOULUTUSALA_TASO_1 to koulutusmahdollisuus_jakauma tyyppi constraint

ALTER TABLE koulutusmahdollisuus_jakauma
  DROP CONSTRAINT koulutusmahdollisuus_jakauma_tyyppi_check;

ALTER TABLE koulutusmahdollisuus_jakauma
  ADD CONSTRAINT koulutusmahdollisuus_jakauma_tyyppi_check
    CHECK (tyyppi IN
           ('OSAAMINEN', 'KOULUTUSALA', 'MAKSULLISUUS', 'OPETUSTAPA', 'AIKA', 'KUNTA', 'MAAKUNTA',
            'KOULUTUSALA_TASO_1'));
