#!/bin/bash
set -e -o pipefail

if [[ -n $AWS_SESSION_TOKEN && -n $DEV_BUCKET ]]; then
  mkdir -p ./tmp/data
  mkdir -p ./.run
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/application-local.yml .
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/jod-yksilo-bootRun.run.xml .run/
  aws s3 sync "s3://${DEV_BUCKET}/data/jod-yksilo-esco-data/" ./tmp/data/ --exclude "*" --include "*.sql"
  aws s3 sync "s3://${DEV_BUCKET}/tyomahdollisuudet/" ./tmp/data/ --exclude "*" --include "full_json_lines_tyomahdollisuus.json"
  aws s3 sync "s3://${DEV_BUCKET}/koulutusmahdollisuudet/" ./tmp/data/ --exclude "*" --include "full_json_lines_koulutusmahdollisuus.json"
else
  echo "WARN: Skipping data and configuration download, missing AWS credentials or DEV_BUCKET" >&2
fi

STARTED=false
DB=$(docker compose ps postgres --format '{{.Name}}')
if [[ -z $DB ]]; then
  docker compose up postgres -d --wait
  DB=$(docker compose ps postgres --format '{{.Name}}')
  docker exec "$DB" bash -c 'while ! pg_isready; do sleep 1; done'
  STARTED=true
fi

(
  cd ./tmp/data
  echo 'DROP SCHEMA IF EXISTS esco_data CASCADE;'\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -f - -U yksilo yksilo
  cat esco_data_scheme.sql\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -f - -U yksilo yksilo
  cat skills_*1_1_2.sql\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -f - -1 -U yksilo yksilo

  echo "\
  CREATE SCHEMA IF NOT EXISTS tyomahdollisuus_data;
  CREATE TABLE IF NOT EXISTS tyomahdollisuus_data.import(
    data jsonb not null,
    id uuid generated always as ( (data ->> 'id')::uuid ) stored primary key);
  TRUNCATE TABLE tyomahdollisuus_data.import;"\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -f - -U yksilo yksilo
  <full_json_lines_tyomahdollisuus.json docker exec \
    -i -e PGPASSWORD=yksilo "$DB" psql -q -U yksilo yksilo \
    -c "\COPY tyomahdollisuus_data.import(data) FROM STDIN (FORMAT text)"

  echo "\
    CREATE SCHEMA IF NOT EXISTS koulutusmahdollisuus_data;
    CREATE TABLE IF NOT EXISTS koulutusmahdollisuus_data.import(
      data jsonb not null,
      id uuid generated always as ( (data ->> 'id')::uuid ) stored primary key);
    TRUNCATE TABLE koulutusmahdollisuus_data.import;"\
    | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -f - -U yksilo yksilo
  <full_json_lines_koulutusmahdollisuus.json docker exec \
    -i -e PGPASSWORD=yksilo "$DB" psql -q -U yksilo yksilo \
    -c "\COPY koulutusmahdollisuus_data.import(data) FROM STDIN (FORMAT text)"
)

if [[ $STARTED == true ]]; then
  docker compose stop
fi
