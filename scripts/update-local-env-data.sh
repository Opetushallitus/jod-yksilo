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

echo "CLEAR and UPDATE tyomahdollisuus, koulutusmahdollisuus and esco"
(
  docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
    -c "CALL tyomahdollisuus_data.clear(); CALL koulutusmahdollisuus_data.clear(); CALL tyomahdollisuus_data.import(); CALL koulutusmahdollisuus_data.import();"

  #update esco data
  for esco in "ammatti" "osaaminen"; do
    docker exec -i -e PGPASSWORD=yksilo "$DB" psql -q -1 -U yksilo yksilo \
      -c "CALL esco_data.import_${esco}();"
  done
)

if [[ $STARTED == true ]]; then
  docker compose stop
fi
