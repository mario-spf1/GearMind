#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_DIR="$ROOT_DIR/release"
PACKAGE_DIR="$RELEASE_DIR/client-package"
APP_DIR="$PACKAGE_DIR/app"
LIB_DIR="$APP_DIR/lib"
ZIP_FILE="$RELEASE_DIR/GearMind-Cliente.zip"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: comando requerido no encontrado: $1" >&2
    exit 1
  fi
}

require_cmd mvn
require_cmd zip

cd "$ROOT_DIR"

echo "[1/5] Limpiando y compilando proyecto Maven..."
mvn -q -DskipTests clean package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency

echo "[2/5] Preparando estructura de paquete cliente..."
rm -rf "$PACKAGE_DIR"
mkdir -p "$LIB_DIR"

JAR_SOURCE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
if [[ -z "$JAR_SOURCE" ]]; then
  echo "Error: no se encontró el JAR principal en target/." >&2
  exit 1
fi

cp "$JAR_SOURCE" "$APP_DIR/GearMind.jar"
cp -R target/dependency/. "$LIB_DIR/"

echo "[3/5] Copiando assets/scripts de ejecución..."
cp "$ROOT_DIR/.env.template" "$PACKAGE_DIR/.env.template"
cp "$ROOT_DIR/setup-db.sh" "$PACKAGE_DIR/setup-db.sh"
cp "$ROOT_DIR/setup-db.bat" "$PACKAGE_DIR/setup-db.bat"
cp "$ROOT_DIR/run-app.sh" "$PACKAGE_DIR/run-app.sh"
cp "$ROOT_DIR/run-app.bat" "$PACKAGE_DIR/run-app.bat"
cp "$ROOT_DIR/run-bot.sh" "$PACKAGE_DIR/run-bot.sh"
cp "$ROOT_DIR/run-bot.bat" "$PACKAGE_DIR/run-bot.bat"
cp "$ROOT_DIR/README-CLIENTE.md" "$PACKAGE_DIR/README-CLIENTE.md"

chmod +x "$PACKAGE_DIR"/*.sh

echo "[4/5] Generando ZIP final..."
rm -f "$ZIP_FILE"
(
  cd "$RELEASE_DIR"
  zip -qr "$(basename "$ZIP_FILE")" "$(basename "$PACKAGE_DIR")"
)

echo "[5/5] Paquete cliente generado correctamente: $ZIP_FILE"