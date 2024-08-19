#!/bin/bash

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    CREATE ROLE yksilo WITH LOGIN PASSWORD 'yksilo';
    GRANT CONNECT,CREATE,TEMPORARY ON DATABASE yksilo TO yksilo;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "yksilo" <<'EOSQL'
    BEGIN;
    CREATE SCHEMA IF NOT EXISTS yksilo;
    GRANT ALL PRIVILEGES ON SCHEMA yksilo TO yksilo;
    CREATE ROLE tunnistus;
    CREATE SCHEMA IF NOT EXISTS tunnistus AUTHORIZATION tunnistus;
    REVOKE ALL ON SCHEMA tunnistus FROM public;
    SET LOCAL ROLE tunnistus;
    CREATE TABLE IF NOT EXISTS henkilo(yksilo_id UUID PRIMARY KEY, henkilo_id VARCHAR(300) NOT NULL UNIQUE);

    CREATE OR REPLACE FUNCTION generate_yksilo_id(henkilo_id VARCHAR(300)) RETURNS UUID AS $$
    DECLARE
      id UUID;
    BEGIN
      INSERT INTO henkilo(yksilo_id, henkilo_id) VALUES (gen_random_uuid(), $1) ON CONFLICT DO NOTHING RETURNING yksilo_id INTO id;
      IF id IS NULL THEN
        SELECT h.yksilo_id FROM henkilo h WHERE h.henkilo_id = $1 INTO id;
      END IF;
      RETURN id;
    END $$ LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;
    REVOKE ALL ON FUNCTION generate_yksilo_id FROM public;
    GRANT EXECUTE ON FUNCTION generate_yksilo_id TO yksilo;

    CREATE OR REPLACE FUNCTION remove_yksilo_id(yksilo_id UUID) RETURNS UUID AS $$
            DELETE FROM henkilo WHERE yksilo_id = $1 RETURNING yksilo_id
            $$ LANGUAGE SQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;
    REVOKE ALL ON FUNCTION remove_yksilo_id FROM public;
    GRANT EXECUTE ON FUNCTION remove_yksilo_id TO yksilo;

    GRANT REFERENCES(yksilo_id) ON henkilo TO yksilo;
    GRANT USAGE ON SCHEMA tunnistus TO yksilo;

    -- Workaround for Hibernate ddl-auto
    ALTER TABLE henkilo ENABLE ROW LEVEL SECURITY;
    CREATE POLICY select_none_policy ON henkilo FOR SELECT TO yksilo USING (false);
    GRANT SELECT(yksilo_id) ON henkilo TO yksilo;

    RESET ROLE;
    END;
EOSQL
