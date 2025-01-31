#!/bin/bash
set -e -o pipefail

mkdir -p ./tmp/data
mkdir -p ./.run
ESCO_VERSION="1.2.0"

# Parse command-line options
while getopts "esco:" opt; do
  case ${opt} in
    esco )
      ESCO_VERSION=$OPTARG
      ;;
    \? )
      echo "Usage: cmd [-esco 1.2.0]"
      exit 1
      ;;
  esac
done

if [[ -n $AWS_SESSION_TOKEN && -n $DEV_BUCKET ]]; then
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/application-local.yml .
  aws s3 cp s3://${DEV_BUCKET}/jod-yksilo-backend/jod-yksilo-bootRun.run.xml .run/
  aws s3 sync "s3://${DEV_BUCKET}/data/jod-yksilo-esco-data/${ESCO_VERSION}/" ./tmp/data/ --exclude "*" --include "*.csv"
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

echo -e "NOTE: Creating database schema. Errors about 'relation X does not exist' can be ignored.\n"
{ echo '\set VERBOSITY terse'; cat ./src/main/resources/schema.sql; } \
     | docker exec -i -e PGPASSWORD=yksilo -e PGOPTIONS='--client-min-messages=warning' "$DB" psql -q -f - -U yksilo yksilo
echo "NOTE: Importing data"
(
  cd ./tmp/data

  docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "TRUNCATE TABLE esco_data.skills"

  for lang in "en" "fi" "sv"; do
    <skills_${lang}.csv docker exec \
      -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "SET esco.lang=${lang}" \
      -c "\COPY esco_data.skills(conceptType,conceptUri,skillType,reuseLevel,preferredLabel,altLabels,hiddenLabels,status,modifiedDate,scopeNote,definition,inScheme,description) FROM STDIN (FORMAT CSV, HEADER true)"
  done

  docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "TRUNCATE TABLE esco_data.occupations"

  for lang in "en" "fi" "sv"; do
    <occupations_${lang}.csv docker exec \
      -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "SET esco.lang=${lang}" \
      -c "\COPY esco_data.occupations(conceptType,conceptUri,iscoGroup,preferredLabel,altLabels,hiddenLabels,status,modifiedDate,regulatedProfessionNote,scopeNote,definition,inScheme,description,code) FROM STDIN (FORMAT CSV, HEADER true)"
  done

  for lang in "en" "fi" "sv"; do
    <ISCOGroups_${lang}.csv docker exec \
      -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "SET esco.lang=${lang}"\
      -c "\COPY esco_data.occupations(conceptType,conceptUri,code,preferredLabel,status,altLabels,inScheme,description) FROM STDIN (FORMAT csv, HEADER true);"
  done

  for mahdollisuus in "tyomahdollisuus" "koulutusmahdollisuus"; do
    docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "TRUNCATE TABLE ${mahdollisuus}_data.import"

    <full_json_lines_${mahdollisuus}.json docker exec \
      -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "\COPY ${mahdollisuus}_data.import(data) FROM STDIN (FORMAT text)"
  done
)

if [[ $STARTED == true ]]; then
  docker compose stop
fi
