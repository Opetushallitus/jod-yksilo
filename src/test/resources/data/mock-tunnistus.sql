-- mock tunnistus schema for tests
CREATE SCHEMA IF NOT EXISTS tunnistus
;;;
CREATE TABLE IF NOT EXISTS tunnistus.henkilo (
  yksilo_id  UUID PRIMARY KEY,
  henkilo_id VARCHAR(300) NOT NULL UNIQUE
)
;;;
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
WHERE yksilo_id = $1
RETURNING yksilo_id
$$ LANGUAGE SQL;
;;;
