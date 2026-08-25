ALTER TABLE cv_tehtava
  ADD COLUMN sisalto_hash VARCHAR(64);

-- Used to short-circuit re-uploads of an identical CV that was already
-- successfully extracted for the same user and language.
CREATE INDEX idx_cv_tehtava_dedup
  ON cv_tehtava (yksilo_id, kieli, sisalto_hash)
  WHERE tila = 'VALMIS' AND sisalto_hash IS NOT NULL;
