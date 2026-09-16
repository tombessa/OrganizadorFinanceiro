#!/bin/sh
set -eu

. /app/database-url.sh

if [ -n "${DATABASE_URL:-}" ]; then
    if ! normalized_database_url=$(normalize_database_url "$DATABASE_URL"); then
        echo "DATABASE_URL must use postgres://, postgresql://, or jdbc:postgresql://" >&2
        exit 78
    fi
    export DATABASE_URL=$normalized_database_url
fi

exec java -XX:MaxRAMPercentage=75 "-Dserver.port=${PORT:-80}" -jar /app/app.jar
