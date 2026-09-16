#!/bin/sh
set -eu

. "$(dirname "$0")/database-url.sh"

assert_normalized() {
    input=$1
    expected=$2
    actual=$(normalize_database_url "$input")

    if [ "$actual" != "$expected" ]; then
        echo "Expected '$expected' but got '$actual'" >&2
        exit 1
    fi
}

assert_normalized \
    'postgres://user:password@example.com:5432/database?sslmode=require' \
    'jdbc:postgresql://user:password@example.com:5432/database?sslmode=require'
assert_normalized \
    'postgresql://user:password@example.com:5432/database?sslmode=require' \
    'jdbc:postgresql://user:password@example.com:5432/database?sslmode=require'
assert_normalized \
    'jdbc:postgresql://user:password@example.com:5432/database?sslmode=require' \
    'jdbc:postgresql://user:password@example.com:5432/database?sslmode=require'

if normalize_database_url 'https://example.com/database' >/dev/null 2>&1; then
    echo "An unsupported URL scheme was accepted" >&2
    exit 1
fi
