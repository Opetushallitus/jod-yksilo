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

CREATE TABLE IF NOT EXISTS tunnistus.jakolinkki(
  jakolinkki_id UUID PRIMARY KEY,
  ulkoinen_id UUID NOT NULL UNIQUE,
  henkilo_id VARCHAR(300) NOT NULL REFERENCES tunnistus.henkilo(henkilo_id) ON DELETE CASCADE,
  voimassa_asti TIMESTAMPTZ NOT NULL,
  nimi_jaettu BOOLEAN NOT NULL DEFAULT FALSE,
  email_jaettu BOOLEAN NOT NULL DEFAULT FALSE,
  luotu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  muokattu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE OR REPLACE FUNCTION tunnistus.create_jakolinkki(henkilo_id VARCHAR(300), voimassa_asti TIMESTAMPTZ, nimi_jaettu BOOLEAN DEFAULT FALSE, email_jaettu BOOLEAN DEFAULT FALSE)
  RETURNS UUID AS
$$
DECLARE
  linkki_id UUID;
BEGIN
  BEGIN
    INSERT INTO jakolinkki(
      jakolinkki_id,
      ulkoinen_id,
      henkilo_id,
      voimassa_asti,
      nimi_jaettu,
      email_jaettu
    )
    VALUES (
   gen_random_uuid(),
   gen_random_uuid(),
   $1,
   $2,
    $3,
    $4
   )
    RETURNING jakolinkki_id INTO linkki_id;

    RETURN linkki_id;
  EXCEPTION WHEN unique_violation THEN
    RETURN NULL;
  WHEN foreign_key_violation THEN
    RETURN NULL;
  END;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.update_jakolinkki(
  in_henkilo_id VARCHAR(300),
  in_jakolinkki_id UUID,
  in_voimassa_asti TIMESTAMPTZ,
  in_nimi_jaettu BOOLEAN DEFAULT FALSE,
  in_email_jaettu BOOLEAN DEFAULT FALSE
) RETURNS BOOLEAN AS
$$
DECLARE
  rows_updated INTEGER;
BEGIN
  UPDATE jakolinkki
  SET voimassa_asti = in_voimassa_asti, muokattu = CURRENT_TIMESTAMP,
      nimi_jaettu = in_nimi_jaettu, email_jaettu = in_email_jaettu
  WHERE henkilo_id = in_henkilo_id
  AND jakolinkki_id = in_jakolinkki_id;

  GET DIAGNOSTICS rows_updated = ROW_COUNT;
  RETURN rows_updated > 0;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.delete_jakolinkki(
  in_henkilo_id VARCHAR(300),
  in_jakolinkki_id UUID
) RETURNS BOOLEAN AS
$$
DECLARE
  rows_deleted INTEGER;
BEGIN
  DELETE FROM jakolinkki
  WHERE henkilo_id = in_henkilo_id
  AND jakolinkki_id = in_jakolinkki_id;

  GET DIAGNOSTICS rows_deleted = ROW_COUNT;
  RETURN rows_deleted > 0;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.get_jakolinkit(in_henkilo_id VARCHAR(300))
  RETURNS TABLE (
    jakolinkki_id UUID,
    ulkoinen_id UUID,
    voimassa_asti TIMESTAMPTZ
  ) AS
$$
BEGIN
  RETURN QUERY
    SELECT j.jakolinkki_id, j.ulkoinen_id, j.voimassa_asti
    FROM jakolinkki j
    WHERE j.henkilo_id = in_henkilo_id;
END
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION tunnistus.get_jakolinkki_by_ulkoinen_id(in_ulkoinen_id UUID)
  RETURNS TABLE (
    jakolinkki_id UUID,
    email VARCHAR(254),
    etunimi VARCHAR(100),
    sukunimi VARCHAR(100),
    voimassa_asti TIMESTAMPTZ,
    nimi_jaettu BOOLEAN,
    email_jaettu BOOLEAN
  ) AS
$$
BEGIN
  RETURN QUERY
    SELECT
      j.jakolinkki_id,
      CASE WHEN j.email_jaettu THEN h.email ELSE NULL END,
      CASE WHEN j.nimi_jaettu THEN h.etunimi ELSE NULL END,
      CASE WHEN j.nimi_jaettu THEN h.sukunimi ELSE NULL END,
      j.voimassa_asti,
      j.nimi_jaettu,
      j.email_jaettu
    FROM jakolinkki j
    JOIN tunnistus.henkilo h ON h.henkilo_id = j.henkilo_id
    WHERE j.ulkoinen_id = in_ulkoinen_id
      AND j.voimassa_asti > CURRENT_TIMESTAMP;
END
$$ LANGUAGE PLPGSQL;

