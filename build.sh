```bash
#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# AI Can't Stop Learning
# Java 25 + JavaFX + SQLite + SLF4J
# ============================================================

JAVAFX="${JAVAFX:-/usr/share/openjfx/lib}"
OUT="out"
LIB="lib"

echo "=============================================="
echo " AI Can't Stop Learning"
echo " CI Build"
echo "=============================================="

# ------------------------------------------------------------
# Java
# ------------------------------------------------------------

echo ""
echo "Java version:"
java -version

echo ""
echo "Javac version:"
javac -version


# ------------------------------------------------------------
# Validate JavaFX
# ------------------------------------------------------------

if [ ! -d "$JAVAFX" ]; then
    echo ""
    echo "ERROR: JavaFX directory does not exist:"
    echo "$JAVAFX"
    exit 1
fi


# ------------------------------------------------------------
# Validate lib directory
# ------------------------------------------------------------

if [ ! -d "$LIB" ]; then
    echo ""
    echo "ERROR: lib directory does not exist."
    exit 1
fi


# ------------------------------------------------------------
# Required dependencies
# ------------------------------------------------------------

SQLITE_JAR=$(find "$LIB" -maxdepth 1 -type f -name "sqlite-jdbc-*.jar" | head -n 1)
SLF4J_JAR=$(find "$LIB" -maxdepth 1 -type f -name "slf4j-api-*.jar" | head -n 1)

if [ -z "$SQLITE_JAR" ]; then
    echo ""
    echo "ERROR: SQLite JDBC dependency not found."
    echo "Expected: lib/sqlite-jdbc-*.jar"
    exit 1
fi

if [ -z "$SLF4J_JAR" ]; then
    echo ""
    echo "ERROR: SLF4J dependency not found."
    echo "Expected: lib/slf4j-api-*.jar"
    exit 1
fi


echo ""
echo "Dependencies found:"
echo "SQLite:"
echo "  $SQLITE_JAR"
echo "SLF4J:"
echo "  $SLF4J_JAR"


# ------------------------------------------------------------
# Show module names
# ------------------------------------------------------------

echo ""
echo "SQLite module:"
jar --describe-module --file "$SQLITE_JAR" || true

echo ""
echo "SLF4J module:"
jar --describe-module --file "$SLF4J_JAR" || true


# ------------------------------------------------------------
# Clean output
# ------------------------------------------------------------

rm -rf "$OUT"
mkdir -p "$OUT"


# ------------------------------------------------------------
# Collect sources
# ------------------------------------------------------------

SOURCE_LIST=$(mktemp)

trap 'rm -f "$SOURCE_LIST"' EXIT

find src -type f -name "*.java" | sort > "$SOURCE_LIST"

if [ ! -s "$SOURCE_LIST" ]; then
    echo ""
    echo "ERROR: No Java source files found."
    exit 1
fi


# ------------------------------------------------------------
# Build module path
#
# JavaFX modules:
#   /usr/share/openjfx/lib
#
# Application dependencies:
#   lib/*.jar
# ------------------------------------------------------------

MODULE_PATH="$JAVAFX:$LIB"

echo ""
echo "Module path:"
echo "$MODULE_PATH"


# ------------------------------------------------------------
# Compile
# ------------------------------------------------------------

echo ""
echo "Compiling..."

javac \
    -d "$OUT" \
    --module-path "$MODULE_PATH" \
    @"$SOURCE_LIST"


# ------------------------------------------------------------
# Success
# ------------------------------------------------------------

echo ""
echo "=============================================="
echo " BUILD SUCCESSFUL"
echo "=============================================="
```
