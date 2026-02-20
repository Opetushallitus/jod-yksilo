#!/bin/bash

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    CREATE ROLE yksilo WITH LOGIN PASSWORD 'yksilo';
    GRANT CONNECT,CREATE,TEMPORARY ON DATABASE yksilo TO yksilo;
    CREATE ROLE dataloader WITH LOGIN PASSWORD 'dataloader';
    GRANT CONNECT,TEMPORARY ON DATABASE yksilo TO dataloader;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "yksilo" <<'EOSQL'
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";

    BEGIN;
    CREATE SCHEMA IF NOT EXISTS yksilo;
    GRANT USAGE ON SCHEMA yksilo TO dataloader;
    GRANT ALL PRIVILEGES ON SCHEMA yksilo TO yksilo;
    CREATE ROLE tunnistus;
    CREATE SCHEMA IF NOT EXISTS tunnistus AUTHORIZATION tunnistus;
    REVOKE ALL ON SCHEMA tunnistus FROM public;
    SET LOCAL ROLE tunnistus;
    CREATE TABLE IF NOT EXISTS henkilo(
      yksilo_id UUID PRIMARY KEY,
      henkilo_id VARCHAR(300) NOT NULL UNIQUE,
      email VARCHAR(254),
      etunimi TEXT,
      sukunimi TEXT,
      luotu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      muokattu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS jakolinkki(
      jakolinkki_id UUID PRIMARY KEY,
      ulkoinen_id UUID NOT NULL UNIQUE,
      henkilo_id VARCHAR(300) NOT NULL REFERENCES henkilo(henkilo_id) ON DELETE CASCADE,
      voimassa_asti TIMESTAMPTZ NOT NULL,
      nimi_jaettu BOOLEAN NOT NULL DEFAULT FALSE,
      email_jaettu BOOLEAN NOT NULL DEFAULT FALSE,
      luotu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      muokattu TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

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

    CREATE OR REPLACE FUNCTION read_yksilo_email(henkilo_id VARCHAR(300)) RETURNS VARCHAR(254) AS $$
      DECLARE
        email VARCHAR(254);
      BEGIN
        SELECT h.email INTO email FROM henkilo h WHERE h.henkilo_id = $1;
        RETURN email;
      END $$ LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = "tunnistus", pg_temp;

    CREATE OR REPLACE FUNCTION update_yksilo_email(henkilo_id VARCHAR(300), email VARCHAR(254)) RETURNS VOID AS $$
      BEGIN
        UPDATE henkilo SET email = $2, muokattu = CURRENT_TIMESTAMP WHERE henkilo.henkilo_id = $1;
      END $$ LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = "tunnistus", pg_temp;

    CREATE OR REPLACE FUNCTION read_yksilo_name(henkilo_id VARCHAR(300))
      RETURNS TABLE(etunimi TEXT, sukunimi TEXT) AS $$
      BEGIN
        RETURN QUERY
          SELECT h.etunimi, h.sukunimi
          FROM henkilo h
          WHERE h.henkilo_id = $1;
      END $$ LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = "tunnistus", pg_temp;

    CREATE OR REPLACE FUNCTION update_yksilo_name(henkilo_id VARCHAR(300), etunimi TEXT, sukunimi TEXT) RETURNS VOID AS $$
      BEGIN
        UPDATE henkilo SET etunimi = $2, sukunimi = $3, muokattu = CURRENT_TIMESTAMP WHERE henkilo.henkilo_id = $1;
      END $$ LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = "tunnistus", pg_temp;

    CREATE OR REPLACE FUNCTION remove_yksilo_id(yksilo_id UUID) RETURNS UUID AS $$
            DELETE FROM henkilo WHERE yksilo_id = $1 RETURNING yksilo_id
            $$ LANGUAGE SQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION create_jakolinkki(henkilo_id VARCHAR(300), voimassa_asti TIMESTAMPTZ, nimi_jaettu BOOLEAN DEFAULT FALSE, email_jaettu BOOLEAN DEFAULT FALSE)
    RETURNS UUID AS $$
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
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION update_jakolinkki(
      in_henkilo_id VARCHAR(300),
      in_jakolinkki_id UUID,
      in_voimassa_asti TIMESTAMPTZ,
      in_nimi_jaettu BOOLEAN,
      in_email_jaettu BOOLEAN
    ) RETURNS BOOLEAN AS $$
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
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION delete_jakolinkki(
      in_henkilo_id VARCHAR(300),
      in_jakolinkki_id UUID
    ) RETURNS BOOLEAN AS $$
    DECLARE
      rows_deleted INTEGER;
    BEGIN
      DELETE FROM jakolinkki
      WHERE henkilo_id = in_henkilo_id
      AND jakolinkki_id = in_jakolinkki_id;

      GET DIAGNOSTICS rows_deleted = ROW_COUNT;
      RETURN rows_deleted > 0;
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION get_jakolinkit(in_henkilo_id VARCHAR(300))
    RETURNS TABLE (
      jakolinkki_id UUID,
      ulkoinen_id UUID,
      voimassa_asti TIMESTAMPTZ,
      nimi_jaettu BOOLEAN,
      email_jaettu BOOLEAN
    ) AS $$
    BEGIN
      RETURN QUERY
      SELECT
        j.jakolinkki_id,
        j.ulkoinen_id,
        j.voimassa_asti,
        j.nimi_jaettu,
        j.email_jaettu
      FROM jakolinkki j
      WHERE j.henkilo_id = in_henkilo_id;
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION get_jakolinkki(in_henkilo_id VARCHAR(300), in_jakolinkki_id UUID)
    RETURNS TABLE (
      jakolinkki_id UUID,
      ulkoinen_id UUID,
      voimassa_asti TIMESTAMPTZ,
      nimi_jaettu BOOLEAN,
      email_jaettu BOOLEAN
    ) AS $$
     BEGIN
      RETURN QUERY
      SELECT
        j.jakolinkki_id,
        j.ulkoinen_id,
        j.voimassa_asti,
        j.nimi_jaettu,
        j.email_jaettu
      FROM jakolinkki j
      WHERE j.jakolinkki_id = in_jakolinkki_id
      AND j.henkilo_id = in_henkilo_id;
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    CREATE OR REPLACE FUNCTION get_jakolinkki_by_ulkoinen_id(in_ulkoinen_id UUID)
    RETURNS TABLE (
      jakolinkki_id UUID,
      email VARCHAR(254),
      etunimi TEXT,
      sukunimi TEXT,
      voimassa_asti TIMESTAMPTZ,
      nimi_jaettu BOOLEAN,
      email_jaettu BOOLEAN
    ) AS $$
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
      JOIN henkilo h ON h.henkilo_id = j.henkilo_id
      WHERE j.ulkoinen_id = in_ulkoinen_id
      AND j.voimassa_asti > CURRENT_TIMESTAMP;
    END $$
    LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = tunnistus, pg_temp;

    REVOKE ALL ON FUNCTION generate_yksilo_id, create_jakolinkki, update_jakolinkki, update_yksilo_email, update_yksilo_name, delete_jakolinkki, get_jakolinkit, get_jakolinkki_by_ulkoinen_id, remove_yksilo_id, read_yksilo_email, read_yksilo_name FROM public;
    GRANT EXECUTE ON FUNCTION generate_yksilo_id, create_jakolinkki, update_jakolinkki, update_yksilo_email, update_yksilo_name, delete_jakolinkki, get_jakolinkit, get_jakolinkki_by_ulkoinen_id, remove_yksilo_id, read_yksilo_email, read_yksilo_name TO yksilo;
    GRANT REFERENCES(yksilo_id) ON henkilo TO yksilo;
    GRANT REFERENCES(jakolinkki_id) ON jakolinkki TO yksilo;
    GRANT USAGE ON SCHEMA tunnistus TO yksilo;

    -- Workaround for Hibernate ddl-auto
    ALTER TABLE henkilo ENABLE ROW LEVEL SECURITY;
    CREATE POLICY select_none_policy ON henkilo FOR SELECT TO yksilo USING (false);
    GRANT SELECT(yksilo_id) ON henkilo TO yksilo;

    ALTER TABLE jakolinkki ENABLE ROW LEVEL SECURITY;
    CREATE POLICY select_none_policy ON jakolinkki FOR SELECT TO yksilo USING (false);
    GRANT SELECT(jakolinkki_id) ON jakolinkki TO yksilo;

    RESET ROLE;
    END;
EOSQL
