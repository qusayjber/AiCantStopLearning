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
# Locate dependencies
# ------------------------------------------------

SQLITE_JAR=$(find "$LIB" -maxdepth 1 -type f -name "sqlite-jdbc-*.jar" | head -n 1)
SLF4J_JAR=$(find "$LIB" -maxdepth 1 -type f -name "slf4j-api-*.jar" | head -n 1)

if [ -z "$SQLITE_JAR" ]; then
    echo "ERROR: SQLite JDBC JAR not found."
    exit 1
fi

if [ -z "$SLF4J_JAR" ]; then
    echo "ERROR: SLF4J JAR not found."
    exit 1
fi

echo ""
echo "Dependencies:"
echo "  SQLite : $SQLITE_JAR"
echo "  SLF4J  : $SLF4J_JAR"

# ------------------------------------------------
# Verify module names
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
# Clean build directory
# ------------------------------------------------

rm -rf "$OUT"
mkdir -p "$OUT"

# ------------------------------------------------
# Collect sources
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
# Module path
#
# IMPORTANT:
# JavaFX + local dependency JARs must be on
# the module path because module-info.java
# uses JPMS requires declarations.
# ------------------------------------------------

MODULE_PATH="$JAVAFX:$LIB"

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
