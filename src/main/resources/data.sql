DO
$$
  BEGIN

    IF NOT exists(SELECT 1 from osaaminen) THEN
      CALL esco_data.import();
    END IF;

    IF NOT exists(SELECT 1 FROM tyomahdollisuus) THEN
      CALL tyomahdollisuus_data.import();
    END IF;

    IF NOT exists(SELECT 1 FROM koulutusmahdollisuus) THEN
      CALL koulutusmahdollisuus_data.import();
    END IF;

  END
$$
;;;
