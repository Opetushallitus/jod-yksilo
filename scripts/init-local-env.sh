#!/bin/bash
set -e -o pipefail

if [[ -n $AWS_SESSION_TOKEN && -n $DEV_BUCKET ]]; then
  mkdir -p ./tmp/data
  mkdir -p ./.run
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/application-local.yml .
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/jod-yksilo-bootRun.run.xml .run/
  aws s3 sync "s3://${DEV_BUCKET}/data/jod-yksilo-esco-data/" ./tmp/data/ --exclude "*" --include "*.sql"
  aws s3 sync "s3://${DEV_BUCKET}/tyomahdollisuudet/" ./tmp/data/ --exclude "*" --include "*.sql"
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

  echo 'DROP SCHEMA IF EXISTS tyomahdollisuus_data CASCADE;'\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -f - -U yksilo yksilo
  cat tyomahdollisuus_data.sql\
   | docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -f - -1 -U yksilo yksilo
)

if [[ $STARTED == true ]]; then
  docker compose stop
fi
