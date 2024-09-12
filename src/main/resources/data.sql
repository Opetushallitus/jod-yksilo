ALTER TABLE yksilo
  DROP CONSTRAINT IF EXISTS fk_yksilo_id,
  ADD CONSTRAINT fk_yksilo_id FOREIGN KEY (id) REFERENCES tunnistus.henkilo(yksilo_id);
