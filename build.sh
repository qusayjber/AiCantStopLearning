#!/usr/bin/env bash
# build.sh
set -e
JAVAFX=${JAVAFX:-/usr/share/openjfx/lib}
mkdir -p out
find src -name "*.java" > /tmp/aicsl_sources.txt
javac -d out --module-path "$JAVAFX" --add-modules javafx.controls -cp "lib/*" @/tmp/aicsl_sources.txt
echo "Build OK"