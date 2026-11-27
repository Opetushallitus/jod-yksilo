#!/bin/bash
# Converts ESCO <concepts>.json and <concepts>_descriptions.json into a single JSON lines file
# escaped for PostgreSQL COPY text format. Each line is a concept with its description merged in
# as a "description" language map.
#
# Usage: esco-to-jsonl.sh <concepts.json> <descriptions.json> > esco_<occupations|skills>.json
#
# The output is loaded into esco_data.<occupations|skills> by the jod-infra mahdollisuudet lambda
# (upload as esco_occupations.json / esco_skills.json) or by init-local-env.sh.
set -e -o pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <concepts.json> <descriptions.json>" >&2
  exit 1
fi

# COPY text format treats backslash as an escape character; jq -c escapes all other control
# characters inside JSON strings, so doubling backslashes is sufficient.
jq -c --slurpfile descriptions "$2" '.[] | .description = ($descriptions[0][.uri] // null)' "$1" |
  sed 's/\\/\\\\/g'
