#!/bin/bash
set -e -o pipefail

echo "NOTE: This script will DELETE all user data related to tyomahdollisuus and koulutusmahdollisuus from:
 - tyomahdollisuus_jakauma_arvot
 - paamaara
 - tyomahdollisuus_kaannos
 - tyomahdollisuus_jakauma
 - yksilon_suosikki
 - paamaara_kaannos
 - tyomahdollisuus_jakauma_arvot
 - koulutusmahdollisuus_jakauma_arvot
 - koulutus_viite_kaannos
 - koulutusmahdollisuus_kaannos
 - koulutusmahdollisuus_jakauma
 - koulutus_viite
 - paamaara
 - yksilon_suosikki
 - koulutusmahdollisuus_jakauma_arvot
 - koulutus_viite_kaannos
 - paamaara_kaannos
"
# Function to ask for user confirmation
ask_confirmation() {
    while true; do
        read -p "Do you want to continue? (y/n): " choice
        case "$choice" in
            y|Y ) echo "Continuing..."; break;;
            n|N ) echo "Operation aborted."; exit;;
            * ) echo "Please answer y or n.";;
        esac
    done
}

# Call the function to ask for confirmation
ask_confirmation

STARTED=false
DB=$(docker compose ps postgres --format '{{.Name}}')
if [[ -z $DB ]]; then
  docker compose up postgres -d --wait
  DB=$(docker compose ps postgres --format '{{.Name}}')
  docker exec "$DB" bash -c 'while ! pg_isready; do sleep 1; done'
  STARTED=true
fi

echo "BACKING UP, CLEARING, and UPDATING tyomahdollisuus, koulutusmahdollisuus and esco"
(
  # First create and call backup procedures then continue with the clear and import procedures.
  docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "
      -- Create backup schema if it doesn't exist yet
      CREATE SCHEMA IF NOT EXISTS yksilo_backup_mahdollisuudet;

      -- Create backup procedures
      CREATE OR REPLACE PROCEDURE tyomahdollisuus_data.backup()
      LANGUAGE plpgsql
      AS \$\$
      BEGIN
        -- Create backup tables if they don't exist
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.tyomahdollisuus (LIKE tyomahdollisuus INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.tyomahdollisuus_kaannos (LIKE tyomahdollisuus_kaannos INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma (LIKE tyomahdollisuus_jakauma INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma_arvot (LIKE tyomahdollisuus_jakauma_arvot INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.paamaara_tyo (LIKE paamaara INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.paamaara_kaannos_tyo (LIKE paamaara_kaannos INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.yksilon_suosikki_tyo (LIKE yksilon_suosikki INCLUDING ALL);

        -- Truncate existing backup tables
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.tyomahdollisuus CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.tyomahdollisuus_kaannos CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma_arvot CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.paamaara_tyo CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.paamaara_kaannos_tyo CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.yksilon_suosikki_tyo CASCADE;

        -- Backup data
        INSERT INTO yksilo_backup_mahdollisuudet.tyomahdollisuus SELECT * FROM tyomahdollisuus;
        INSERT INTO yksilo_backup_mahdollisuudet.tyomahdollisuus_kaannos SELECT * FROM tyomahdollisuus_kaannos;
        INSERT INTO yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma SELECT * FROM tyomahdollisuus_jakauma;
        INSERT INTO yksilo_backup_mahdollisuudet.tyomahdollisuus_jakauma_arvot SELECT * FROM tyomahdollisuus_jakauma_arvot;
        INSERT INTO yksilo_backup_mahdollisuudet.paamaara_tyo SELECT * FROM paamaara WHERE tyomahdollisuus_id IS NOT NULL;
        INSERT INTO yksilo_backup_mahdollisuudet.paamaara_kaannos_tyo
          SELECT pk.* FROM paamaara_kaannos pk
          JOIN paamaara p ON pk.paamaara_id = p.id
          WHERE p.tyomahdollisuus_id IS NOT NULL;
        INSERT INTO yksilo_backup_mahdollisuudet.yksilon_suosikki_tyo
          SELECT * FROM yksilon_suosikki WHERE tyomahdollisuus_id IS NOT NULL;

        RAISE NOTICE 'Backed up tyomahdollisuus data';
      END;
      \$\$;

      CREATE OR REPLACE PROCEDURE koulutusmahdollisuus_data.backup()
      LANGUAGE plpgsql
      AS \$\$
      BEGIN
        -- Create backup tables if they don't exist
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutusmahdollisuus (LIKE koulutusmahdollisuus INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutusmahdollisuus_kaannos (LIKE koulutusmahdollisuus_kaannos INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma (LIKE koulutusmahdollisuus_jakauma INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma_arvot (LIKE koulutusmahdollisuus_jakauma_arvot INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutus_viite (LIKE koulutus_viite INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.koulutus_viite_kaannos (LIKE koulutus_viite_kaannos INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.paamaara_koulutus (LIKE paamaara INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.paamaara_kaannos_koulutus (LIKE paamaara_kaannos INCLUDING ALL);
        CREATE TABLE IF NOT EXISTS yksilo_backup_mahdollisuudet.yksilon_suosikki_koulutus (LIKE yksilon_suosikki INCLUDING ALL);

        -- Truncate existing backup tables
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutusmahdollisuus CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutusmahdollisuus_kaannos CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma_arvot CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutus_viite CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.koulutus_viite_kaannos CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.paamaara_koulutus CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.paamaara_kaannos_koulutus CASCADE;
        TRUNCATE TABLE yksilo_backup_mahdollisuudet.yksilon_suosikki_koulutus CASCADE;

        -- Backup data
        INSERT INTO yksilo_backup_mahdollisuudet.koulutusmahdollisuus SELECT * FROM koulutusmahdollisuus;
        INSERT INTO yksilo_backup_mahdollisuudet.koulutusmahdollisuus_kaannos SELECT * FROM koulutusmahdollisuus_kaannos;
        INSERT INTO yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma SELECT * FROM koulutusmahdollisuus_jakauma;
        INSERT INTO yksilo_backup_mahdollisuudet.koulutusmahdollisuus_jakauma_arvot SELECT * FROM koulutusmahdollisuus_jakauma_arvot;
        INSERT INTO yksilo_backup_mahdollisuudet.koulutus_viite SELECT * FROM koulutus_viite;
        INSERT INTO yksilo_backup_mahdollisuudet.koulutus_viite_kaannos SELECT * FROM koulutus_viite_kaannos;
        INSERT INTO yksilo_backup_mahdollisuudet.paamaara_koulutus SELECT * FROM paamaara WHERE koulutusmahdollisuus_id IS NOT NULL;
        INSERT INTO yksilo_backup_mahdollisuudet.paamaara_kaannos_koulutus
          SELECT pk.* FROM paamaara_kaannos pk
          JOIN paamaara p ON pk.paamaara_id = p.id
          WHERE p.koulutusmahdollisuus_id IS NOT NULL;
        INSERT INTO yksilo_backup_mahdollisuudet.yksilon_suosikki_koulutus
          SELECT * FROM yksilon_suosikki WHERE koulutusmahdollisuus_id IS NOT NULL;

        RAISE NOTICE 'Backed up koulutusmahdollisuus data';
      END;
      \$\$;

      -- Call the backup procedures
      CALL tyomahdollisuus_data.backup();
      CALL koulutusmahdollisuus_data.backup();

      -- Then continue with the clear and import procedures
      CALL tyomahdollisuus_data.clear();
      CALL koulutusmahdollisuus_data.clear();
      CALL tyomahdollisuus_data.import();
      CALL koulutusmahdollisuus_data.import();
    "

  #update esco data
  for esco in "ammatti" "osaaminen"; do
    docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "CALL esco_data.import_${esco}();"
  done
)

if [[ $STARTED == true ]]; then
  docker compose stop
fi
