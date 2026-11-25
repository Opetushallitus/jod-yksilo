ALTER TABLE koski_tehtava
  DROP CONSTRAINT IF EXISTS koski_tehtava_tila_check;

ALTER TABLE koski_tehtava
  ADD CONSTRAINT koski_tehtava_tila_check CHECK
    (tila IN ('ODOTTAA', 'VALMIS', 'EPAONNISTUNUT', 'POISTETTU'));

ALTER TABLE koski_tehtava
  ADD COLUMN versio bigint NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX idx_koski_tehtava_odottaa
  ON koski_tehtava (yksilo_id) WHERE tila = 'ODOTTAA';

ALTER TABLE koulutus
  DROP COLUMN osaamisen_tunnistus_status;
