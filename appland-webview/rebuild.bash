#!/bin/bash
set -e

DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)"
cd "$DIR"

# https://stackoverflow.com/questions/69692842/error-message-error0308010cdigital-envelope-routinesunsupported
export NODE_OPTIONS=--openssl-legacy-provider

rm -rf dist

yarn install
yarn run bundle
