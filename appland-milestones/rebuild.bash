#!/bin/bash
set -e

DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)"
cd "$DIR"

npm install
npm link -A /IslandWork/source/appland/appmap-js/packages/components/ /IslandWork/source/appland/appmap-js/packages/diagrams/ /IslandWork/source/appland/appmap-js/packages/models/
./node_modules/.bin/webpack
