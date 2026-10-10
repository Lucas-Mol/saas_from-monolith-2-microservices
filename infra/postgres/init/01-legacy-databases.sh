#!/bin/bash
# Creates one database and one login role per legacy instance (single-tenant: one DB per customer).
set -euo pipefail

create_database() {
  local db="$1" role="$2" password="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v db="$db" -v role="$role" -v password="$password" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'password';
CREATE DATABASE :"db" OWNER :"role";
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
SQL
}

create_database legacy_acme legacy_acme "${LEGACY_ACME_DB_PASSWORD:?LEGACY_ACME_DB_PASSWORD is required}"
create_database legacy_bella legacy_bella "${LEGACY_BELLA_DB_PASSWORD:?LEGACY_BELLA_DB_PASSWORD is required}"
