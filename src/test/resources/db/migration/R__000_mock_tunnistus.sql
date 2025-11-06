-- mock tunnistus schema for tests
CREATE SCHEMA IF NOT EXISTS tunnistus;

CREATE TABLE IF NOT EXISTS tunnistus.henkilo (
  yksilo_id  UUID PRIMARY KEY,
  henkilo_id VARCHAR(300) NOT NULL UNIQUE,
  email      VARCHAR(254),
  etunimi    TEXT,
  sukunimi   TEXT,
  luotu      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  muokattu   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION tunnistus.generate_yksilo_id(henkilo_id VARCHAR(300)) RETURNS UUID AS
$$
DECLARE
  id UUID;
BEGIN
  INSERT INTO tunnistus.henkilo(yksilo_id, henkilo_id)
  VALUES (gen_random_uuid(), $1)
  ON CONFLICT DO NOTHING
  RETURNING yksilo_id INTO id;
  IF id IS NULL THEN
    SELECT h.yksilo_id FROM tunnistus.henkilo h WHERE h.henkilo_id = $1 INTO id;
  END IF;
  RETURN id;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.remove_yksilo_id(yksilo_id UUID) RETURNS UUID AS
$$
DELETE
FROM tunnistus.henkilo
WHERE henkilo.yksilo_id = $1
RETURNING yksilo_id
$$ LANGUAGE SQL;

CREATE OR REPLACE FUNCTION tunnistus.read_yksilo_email(henkilo_id VARCHAR(300)) RETURNS VARCHAR(254) AS
$$
DECLARE
  email VARCHAR(254);
BEGIN
  SELECT h.email INTO email FROM tunnistus.henkilo h WHERE h.henkilo_id = $1;
  RETURN email;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.update_yksilo_email(henkilo_id VARCHAR(300), email VARCHAR(254)) RETURNS VOID AS
$$
BEGIN
  UPDATE tunnistus.henkilo
  SET email    = $2,
      muokattu = CURRENT_TIMESTAMP
  WHERE henkilo.henkilo_id = $1;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.read_yksilo_name(henkilo_id VARCHAR(300))
      RETURNS TABLE(etunimi TEXT, sukunimi TEXT) AS $$
BEGIN
  RETURN QUERY
    SELECT h.etunimi, h.sukunimi
    FROM tunnistus.henkilo h
    WHERE h.henkilo_id = $1;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.update_yksilo_name(henkilo_id VARCHAR(300), etunimi TEXT, sukunimi TEXT) RETURNS VOID AS
$$
BEGIN
UPDATE tunnistus.henkilo SET etunimi = $2, sukunimi = $3, muokattu = CURRENT_TIMESTAMP WHERE henkilo.henkilo_id = $1;
END
$$ LANGUAGE PLPGSQL;
