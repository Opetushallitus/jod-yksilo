#!/bin/bash
set -e -o pipefail

mkdir -p ./tmp/data
mkdir -p ./.run
ESCO_VERSION="FINESCO-1.2.0-R8"

if [[ -n $AWS_SESSION_TOKEN && -n $DEV_BUCKET ]]; then
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/application-local.yml .
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/jod-yksilo-bootRun.run.xml .run/
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/Dockerfile.osaamissuosittelija .
  aws s3 sync "s3://${DEV_BUCKET}/data/${ESCO_VERSION}/" ./tmp/data/ --exclude "*" --include "*.json"
  aws s3 sync "s3://${DEV_BUCKET}/tyomahdollisuudet/" ./tmp/data/ --exclude "*" --include "full_json_lines_tyomahdollisuus.json"
  aws s3 sync "s3://${DEV_BUCKET}/koulutusmahdollisuudet/" ./tmp/data/ --exclude "*" --include "full_json_lines_koulutusmahdollisuus.json"
  aws s3 sync "s3://${DEV_BUCKET}/ammattiryhma/" ./tmp/data/ --exclude "*" --include "ammattiryhma.csv"
  aws s3 sync "s3://${DEV_BUCKET}/jod-yksilo-backend/data/" ./tmp/data/
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

echo -e "Creating database schema"
host=$(docker compose ps postgres --format '{{.Ports}}')
host=${host%-\>*}
./gradlew -q flywayMigrate -Pflyway.user=yksilo -Pflyway.password=yksilo \
  -P"flyway.url=jdbc:postgresql://$host/yksilo"

echo "Importing data"
# shellcheck disable=SC1009
(
  DEVDATA="$(dirname "$(realpath "$0")")/devdata"
  cd ./tmp/data

  for esco in "skills" "skill_descriptions" "occupations" "occupation_descriptions"; do

    if [[ $esco == *"_descriptions" ]]; then
      filter='to_entries | .[]'
    else
      filter='.[]'
    fi

    jq -c "$filter" "$esco.json" | sed 's/\\/\\\\/g' |
      docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
        -c "TRUNCATE TABLE esco_data.$esco" \
        -c "\COPY esco_data.${esco}(data) FROM STDIN (FORMAT text)"

  done

  for mahdollisuus in "tyomahdollisuus" "koulutusmahdollisuus"; do
    docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "TRUNCATE TABLE ${mahdollisuus}_data.import"

    docker <full_json_lines_${mahdollisuus}.json exec \
      -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "\COPY ${mahdollisuus}_data.import(data) FROM STDIN (FORMAT text)"
  done

  docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "CALL esco_data.import_osaaminen();" \
    -c "CALL esco_data.import_ammatti();" \
    -c "CALL tyomahdollisuus_data.import();" \
    -c "CALL koulutusmahdollisuus_data.import();"

  docker <"$DEVDATA/yksilot.sql" exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo

  docker <ammattiryhma.csv exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "$(cat "$DEVDATA/ammattiryhma.sql")"

  sed 's/\\/\\\\/g' koulutuskoodi.jsonl | docker exec \
    -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "$(cat "$DEVDATA/koulutuskoodi.sql")"
)
if [[ $STARTED == true ]]; then
  docker compose stop
fi
