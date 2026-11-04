-- Statistics Finland education classification code (koodisto "koulutus"), e.g. 351301
ALTER TABLE koulutus
  ADD COLUMN koulutuskoodi VARCHAR(6);
