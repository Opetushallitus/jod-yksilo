ALTER TABLE cv_tehtava
  DROP CONSTRAINT IF EXISTS cv_tehtava_tila_check;

ALTER TABLE cv_tehtava
  ADD CONSTRAINT cv_tehtava_tila_check CHECK
    (tila IN ('ODOTTAA', 'EPAONNISTUNUT', 'VALMIS', 'POISTETTU'));
