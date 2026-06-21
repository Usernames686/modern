#!/bin/sh
set -e

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

cd "$BACKEND_DIR"

if [ ! -x "./mvnw" ]; then
  echo "modern/backend/mvnw is missing or not executable" >&2
  exit 1
fi

if [ ! -f "./pom.xml" ]; then
  echo "modern/backend/pom.xml not found" >&2
  exit 1
fi

sh scripts/check-release-copy.sh
./mvnw -pl crmeb-modern-admin-api -am -DskipTests package
./mvnw -pl crmeb-modern-front-api -am -DskipTests package

echo "backend delivery check passed: admin-api and front-api packaged"
