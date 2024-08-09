#!/bin/bash

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    CREATE ROLE yksilo WITH LOGIN PASSWORD 'yksilo';
    GRANT CONNECT,CREATE,TEMPORARY ON DATABASE yksilo TO yksilo;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "yksilo" <<'EOSQL'
    BEGIN;
    CREATE SCHEMA IF NOT EXISTS yksilo;
    GRANT ALL PRIVILEGES ON SCHEMA yksilo TO yksilo;
    CREATE ROLE auth;
    CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION auth;
    REVOKE ALL ON SCHEMA auth FROM public;
    SET LOCAL ROLE auth;
    CREATE TABLE IF NOT EXISTS person_id (id UUID PRIMARY KEY, person_id VARCHAR(300) NOT NULL UNIQUE);

    CREATE OR REPLACE FUNCTION generate_yksilo_id(person_id VARCHAR(300)) RETURNS UUID AS $$
        WITH new_id AS (
          INSERT INTO person_id(id, person_id) VALUES (gen_random_uuid(), $1) ON CONFLICT DO NOTHING RETURNING id
        )
        SELECT id from new_id
        UNION ALL
        SELECT id FROM person_id WHERE person_id = $1
        $$ LANGUAGE SQL SECURITY DEFINER SET search_path = auth, pg_temp;
    REVOKE ALL ON FUNCTION generate_yksilo_id FROM public;
    GRANT EXECUTE ON FUNCTION generate_yksilo_id TO yksilo;

    CREATE OR REPLACE FUNCTION remove_yksilo_id(id UUID) RETURNS UUID AS $$
            DELETE FROM person_id WHERE id = $1 RETURNING id
            $$ LANGUAGE SQL SECURITY DEFINER SET search_path = auth, pg_temp;
    REVOKE ALL ON FUNCTION generate_yksilo_id FROM public;
    GRANT EXECUTE ON FUNCTION generate_yksilo_id TO yksilo;

    GRANT REFERENCES(id) ON person_id TO yksilo;
    GRANT USAGE ON SCHEMA auth TO yksilo;

    -- Workaround for Hibernate ddl-auto
    ALTER TABLE person_id ENABLE ROW LEVEL SECURITY;
    CREATE POLICY select_none_policy ON person_id FOR SELECT TO yksilo USING (false);
    GRANT SELECT(id) ON person_id TO yksilo;

    RESET ROLE;
    END;
EOSQL
