#!/bin/bash

set -e -o pipefail


# Default values
DB_NAME="yksilo"
DB_PORT="15432"
S3_BUCKET="s3://${BUCKET_NAME}/data/jod-yksilo-esco-data/"
ESCO_VERSION="1.2.0"

# Parse command-line options
while getopts "d:p:s:esco:" opt; do
  case ${opt} in
    d )
      DB_NAME=$OPTARG
      ;;
    p )
      DB_PORT=$OPTARG
      ;;
    s )
      S3_BUCKET=$OPTARG
      ;;
    esco )
      ESCO_VERSION=$OPTARG
      ;;
    \? )
      echo "Usage: cmd [-d database] [-p port] [-s s3_bucket] [-esco esco_version]"
      exit 1
      ;;
  esac
done

if [[ -n $AWS_SESSION_TOKEN && -n $BUCKET_NAME ]]; then
  # Create necessary directories
  mkdir -p ./tmp/data

  # Sync data from S3
  aws s3 sync "${S3_BUCKET}/${ESCO_VERSION}/" ./tmp/data/ --exclude "*" --include "*.csv"

  echo 'Truncate esco_data.skills...'
  # Truncate the table
  psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
      -p ${DB_PORT} \
      -c "TRUNCATE TABLE esco_data.skills"

  # Change directory to data
  cd ./tmp/data

  # Import data for each language
  echo "Importing esco  ${ESCO_VERSION} skills data..."
  for lang in "en" "fi" "sv"; do
      <skills_${lang}.csv psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
        -p ${DB_PORT} \
        -c "SET esco.lang=${lang}" \
        -c "\COPY esco_data.skills(conceptType,conceptUri,skillType,reuseLevel,preferredLabel,altLabels,hiddenLabels,status,modifiedDate,scopeNote,definition,inScheme,description) FROM STDIN (FORMAT CSV, HEADER true)"
  done

  # Truncate the table
  echo 'Truncate esco_data.occupations...'
  psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
      -p ${DB_PORT} \
        -c "TRUNCATE TABLE esco_data.occupations"

  echo "Importing esco  ${ESCO_VERSION} occupations data..."
  for lang in "en" "fi" "sv"; do
      <occupations_${lang}.csv psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
        -p ${DB_PORT} \
        -c "SET esco.lang=${lang}" \
        -c "\COPY esco_data.occupations(conceptType,conceptUri,iscoGroup,preferredLabel,altLabels,hiddenLabels,status,modifiedDate,regulatedProfessionNote,scopeNote,definition,inScheme,description,code) FROM STDIN (FORMAT CSV, HEADER true)"
    done

  echo -n "Importing esco  ${ESCO_VERSION} ISCOGroups data..."
  for lang in "en" "fi" "sv"; do
      <ISCOGroups_${lang}.csv psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
        -p ${DB_PORT} \
        -c "SET esco.lang=${lang}"\
        -c "\COPY esco_data.occupations(conceptType,conceptUri,code,preferredLabel,status,altLabels,inScheme,description) FROM STDIN (FORMAT csv, HEADER true);"
    done

  #update esco data
    for esco in "ammatti" "osaaminen"; do
      psql "postgresql://yksilo@localhost/${DB_NAME}?sslmode=require" \
        -p ${DB_PORT} \
        -c "CALL esco_data.import_${esco}();"
    done

  else
    echo 'ERROR: Missing AWS credentials or $BUCKET_NAME. Before running this SSH tunnel the database as instructed at https://wiki.eduuni.fi/display/OPHPALV/RDS
    and Run this script in AWS subshell as follows, where TARGET_DATABASE_HOST is the cloud RDS hostname.

    PGPASSWORD=$(aws rds generate-db-auth-token --region eu-west-1 \
     --hostname  ${TARGET_DATABASE_HOST}
     --username yksilo --port 5432)  bash cloud-update-esco-from-s3.sh ' >&2
fi
