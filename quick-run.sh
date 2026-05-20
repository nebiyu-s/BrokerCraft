#!/usr/bin/env bash
set -euo pipefail

# BrokerCraft quick compile & run (client or server)
# Usage:
#   ./quick-run.sh              # start JavaFX client
#   ./quick-run.sh server       # start RMI server
#   ./quick-run.sh client       # same as default

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

MODE="${1:-client}"

JFX_VER="17.0.17"
JFX_SYSTEM="C:/java/javafx-sdk-${JFX_VER}"
if [ -d "$JFX_SYSTEM/lib" ]; then
  JFX="$JFX_SYSTEM"
else
  JFX="$ROOT/.javafx/javafx-sdk-${JFX_VER}"
fi

if [ ! -d "$JFX/lib" ]; then
  echo "JavaFX SDK not found at $JFX"
  echo "Install to C:/java/javafx-sdk-${JFX_VER} or run ./run.sh once to download."
  exit 1
fi

LIBS_DIR="$ROOT/.javafx/libs"
mkdir -p "$LIBS_DIR"

GSON="$LIBS_DIR/gson-2.10.1.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
if [ ! -f "$GSON" ]; then
  echo "Downloading Gson..."
  curl -L -o "$GSON" "$GSON_URL" 2>/dev/null || \
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '$GSON_URL' -OutFile '$GSON'"
fi

MYSQL_VER="8.3.0"
MYSQL_JAR="$LIBS_DIR/mysql-connector-j-${MYSQL_VER}.jar"
MYSQL_URL="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_VER}/mysql-connector-j-${MYSQL_VER}.jar"
if [ ! -f "$MYSQL_JAR" ]; then
  echo "Downloading MySQL JDBC driver..."
  curl -L -o "$MYSQL_JAR" "$MYSQL_URL" 2>/dev/null || \
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '$MYSQL_URL' -OutFile '$MYSQL_JAR'"
fi

DEPS_CP="$GSON;$MYSQL_JAR"
JFX_MODULES_PATH="$JFX/lib"

OUT="$ROOT/out"
mkdir -p "$OUT"
SOURCES_TXT="$OUT/sources.txt"

find src/main/java -name '*.java' -print0 | xargs -0 -I{} printf '"%s"\n' "{}" > "$SOURCES_TXT"
if [ ! -s "$SOURCES_TXT" ]; then
  echo "No Java sources under src/main/java"
  exit 1
fi

echo "Compiling BrokerCraft..."
javac -cp "$DEPS_CP" -d "$OUT" \
  --module-path "$JFX_MODULES_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  @"$SOURCES_TXT"

if [ -d "src/main/resources" ]; then
  cp -r src/main/resources/. "$OUT/"
fi

JFX_WIN="$ROOT/.javafx/win-jars"
mkdir -p "$JFX_WIN"
BASE_MVN="https://repo1.maven.org/maven2/org/openjfx"
for mod in base graphics controls fxml; do
  jar="$JFX_WIN/javafx-${mod}-${JFX_VER}-win.jar"
  if [ ! -f "$jar" ]; then
    curl -sL -o "$jar" "$BASE_MVN/javafx-${mod}/${JFX_VER}/javafx-${mod}-${JFX_VER}-win.jar" || true
  fi
done

MPATH="$JFX/lib;$JFX_WIN"
CP="$DEPS_CP;$OUT"

if command -v cygpath >/dev/null 2>&1; then
  WIN_CP="$(cygpath -w "$GSON");$(cygpath -w "$MYSQL_JAR");$(cygpath -w "$OUT")"
  WIN_MPATH="$(cygpath -w "$JFX/lib");$(cygpath -w "$JFX_WIN")"
  WIN_JFX_BIN="$(cygpath -w "$JFX/bin")"
else
  WIN_CP="$CP"
  WIN_MPATH="$MPATH"
  WIN_JFX_BIN="$JFX/bin"
fi

if [ "$MODE" = "server" ]; then
  MAIN="brokercraft.main.ServerMain"
  echo "Starting BrokerCraft Server..."
  java -cp "$WIN_CP" "$MAIN"
else
  MAIN="brokercraft.main.Main"
  echo "Starting BrokerCraft Client..."
  java \
    --module-path "$WIN_MPATH" \
    --add-modules javafx.graphics,javafx.controls,javafx.fxml \
    -Djava.library.path="$WIN_JFX_BIN" \
    -cp "$WIN_CP" \
    "$MAIN"
fi
