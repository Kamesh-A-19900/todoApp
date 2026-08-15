#!/usr/bin/env bash
# ─────────────────────────────────────────────
#  install.sh  —  installs Todo as a system app
#  Usage: bash install.sh
# ─────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$HOME/.local/share/todo-app"
BIN_LINK="$HOME/.local/bin/todo"
DESKTOP_FILE="$HOME/.local/share/applications/todo-app.desktop"

echo "==> Building …"
(cd "$SCRIPT_DIR" && mvn package -q)

echo "==> Installing to $APP_DIR …"
mkdir -p "$APP_DIR"

# Copy app jar and all dependency jars
cp "$SCRIPT_DIR/target/todo-app.jar" "$APP_DIR/todo-app.jar"
rm -rf "$APP_DIR/libs"
cp -r "$SCRIPT_DIR/target/libs" "$APP_DIR/libs"

# Write launcher
# --module-path  : JavaFX platform jars (includes native -linux.jar)
# -cp            : app jar + sqlite (non-module deps)
cat > "$APP_DIR/todo" <<EOF
#!/usr/bin/env bash
exec java \\
  --module-path "$APP_DIR/libs" \\
  --add-modules javafx.controls,javafx.fxml \\
  -cp "$APP_DIR/todo-app.jar:$APP_DIR/libs/sqlite-jdbc-3.50.3.0.jar" \\
  com.kamesh.todo.Main "\$@"
EOF
chmod +x "$APP_DIR/todo"

# Symlink into PATH
mkdir -p "$HOME/.local/bin"
ln -sf "$APP_DIR/todo" "$BIN_LINK"
echo "==> Launcher installed: $BIN_LINK"

# .desktop entry
mkdir -p "$(dirname "$DESKTOP_FILE")"
cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Todo
Comment=Personal daily task calendar tracker
Exec=$APP_DIR/todo
Icon=accessories-text-editor
Terminal=false
Categories=Utility;Office;
StartupNotify=true
EOF
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
echo "==> Desktop entry installed"

if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
    echo ""
    echo "  NOTE: Add this to your ~/.bashrc:"
    echo "        export PATH=\"\$HOME/.local/bin:\$PATH\""
    echo "  Then: source ~/.bashrc"
fi

echo ""
echo "  Done! Run:  todo"
