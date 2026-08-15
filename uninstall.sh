#!/usr/bin/env bash
# ─────────────────────────────────────────────
#  uninstall.sh  —  removes Todo system install
# ─────────────────────────────────────────────
set -e

APP_DIR="$HOME/.local/share/todo-app"
BIN_LINK="$HOME/.local/bin/todo"
DESKTOP_FILE="$HOME/.local/share/applications/todo-app.desktop"

rm -rf  "$APP_DIR"      && echo "Removed $APP_DIR"
rm -f   "$BIN_LINK"     && echo "Removed $BIN_LINK"
rm -f   "$DESKTOP_FILE" && echo "Removed $DESKTOP_FILE"
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true

echo "Uninstall complete."
