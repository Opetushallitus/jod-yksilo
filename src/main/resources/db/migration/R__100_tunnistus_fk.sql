DO
$$
  BEGIN
    ALTER TABLE ONLY yksilo.yksilo
      ADD CONSTRAINT fk_yksilo_id FOREIGN KEY (id) REFERENCES tunnistus.henkilo (yksilo_id);
  EXCEPTION
    WHEN duplicate_object THEN
      RAISE NOTICE 'Foreign key constraint fk_yksilo_id already exists, ignoring.';
  END
$$ LANGUAGE plpgsql;
