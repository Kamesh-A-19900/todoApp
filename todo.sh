#!/usr/bin/env bash
# ─────────────────────────────────────────────
#  todo  —  Personal Todo Calendar App launcher
# ─────────────────────────────────────────────
set -e

JAR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/target"
JAR="$JAR_DIR/todo-app.jar"

# Build if jar is missing
if [ ! -f "$JAR" ]; then
    echo "Building todo-app.jar …"
    (cd "$(dirname "${BASH_SOURCE[0]}")" && mvn package -q)
fi

exec java \
    --module-path "$JAR" \
    --add-modules javafx.controls,javafx.fxml \
    -jar "$JAR" "$@"
