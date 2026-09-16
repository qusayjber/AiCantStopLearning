```bash
#!/usr/bin/env bash
set -euo pipefail

JAVAFX="${JAVAFX:-/usr/share/openjfx/lib}"
OUT="out"
LIB="lib"

echo "=============================================="
echo " AI Can't Stop Learning"
echo " GitHub Actions Build"
echo "=============================================="

echo ""
echo "Java:"
java -version

echo ""
echo "Javac:"
javac -version

# ------------------------------------------------
# Validate directories
# ------------------------------------------------

if [ ! -d "$JAVAFX" ]; then
    echo "ERROR: JavaFX directory not found:"
    echo "$JAVAFX"
    exit 1
fi

if [ ! -d "$LIB" ]; then
    echo "ERROR: lib directory not found."
    exit 1
fi

# ------------------------------------------------
# Dependencies
# ------------------------------------------------

SQLITE_JAR="$LIB/sqlite-jdbc-3.42.0.0.jar"
SLF4J_JAR="$LIB/slf4j-api-1.7.36.jar"

if [ ! -f "$SQLITE_JAR" ]; then
    echo "ERROR: SQLite JDBC JAR not found:"
    echo "$SQLITE_JAR"
    exit 1
fi

if [ ! -f "$SLF4J_JAR" ]; then
    echo "ERROR: SLF4J JAR not found:"
    echo "$SLF4J_JAR"
    exit 1
fi

echo ""
echo "Dependencies:"
echo "  SQLite : $SQLITE_JAR"
echo "  SLF4J  : $SLF4J_JAR"

# ------------------------------------------------
# Verify modules
# ------------------------------------------------

echo ""
echo "SQLite module:"
jar --describe-module \
    --file "$SQLITE_JAR" \
    --release 9

echo ""
echo "SLF4J module:"
jar --describe-module \
    --file "$SLF4J_JAR"

# ------------------------------------------------
# Clean output
# ------------------------------------------------

rm -rf "$OUT"
mkdir -p "$OUT"

# ------------------------------------------------
# Collect Java sources
# ------------------------------------------------

SOURCE_LIST=$(mktemp)
trap 'rm -f "$SOURCE_LIST"' EXIT

find src -type f -name "*.java" | sort > "$SOURCE_LIST"

if [ ! -s "$SOURCE_LIST" ]; then
    echo "ERROR: No Java source files found."
    exit 1
fi

echo ""
echo "Source files:"
wc -l "$SOURCE_LIST"

# ------------------------------------------------
# Explicit module path
#
# IMPORTANT:
# Do NOT use:
#
#   --module-path "$JAVAFX:$LIB"
#
# because scanning the whole lib directory can
# cause duplicate JPMS module discovery with the
# multi-release SQLite JDBC JAR.
# ------------------------------------------------

MODULE_PATH="$JAVAFX:$SQLITE_JAR:$SLF4J_JAR"

echo ""
echo "Module path:"
echo "$MODULE_PATH"

# ------------------------------------------------
# Compile
# ------------------------------------------------

echo ""
echo "Compiling..."

javac \
    -d "$OUT" \
    --module-path "$MODULE_PATH" \
    @"$SOURCE_LIST"

echo ""
echo "=============================================="
echo " BUILD SUCCESSFUL"
echo "=============================================="
```
