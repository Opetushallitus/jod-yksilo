INSERT INTO osaaminen_versio(versio)
VALUES (0)
ON CONFLICT DO NOTHING;

INSERT INTO ammatti_versio(versio)
VALUES (0)
ON CONFLICT DO NOTHING;

--- temporary recovery for failed competence detection tasks
UPDATE koulutus
SET osaamisen_tunnistus_status = 'FAIL'
WHERE osaamisen_tunnistus_status = 'WAIT';

DO
$$
  BEGIN

    IF NOT exists(SELECT 1 from osaaminen) THEN
      CALL esco_data.import_osaaminen();
    END IF;

    IF NOT exists(SELECT 1 from ammatti) THEN
      CALL esco_data.import_ammatti();
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
