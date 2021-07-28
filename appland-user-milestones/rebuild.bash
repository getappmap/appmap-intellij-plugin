#!/bin/bash
set -e

DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)"
cd "$DIR"

yarn install
./node_modules/.bin/webpack