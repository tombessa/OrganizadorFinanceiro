#!/bin/sh

normalize_database_url() {
    database_url=$1

    case "$database_url" in
        jdbc:postgresql://*)
            printf '%s' "$database_url"
            ;;
        postgresql://*)
            printf 'jdbc:%s' "$database_url"
            ;;
        postgres://*)
            printf 'jdbc:postgresql://%s' "${database_url#postgres://}"
            ;;
        *)
            return 1
            ;;
    esac
}
