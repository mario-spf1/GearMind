#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f "app/GearMind.jar" ]]; then
  echo "Error: no se encontró app/GearMind.jar."
  echo "Revisa que el paquete cliente esté completo."
  exit 1
fi

if [[ ! -d "app/lib" ]]; then
  echo "Error: no se encontró app/lib con dependencias runtime."
  echo "Revisa que el paquete cliente esté completo."
  exit 1
fi

java --module-path "app/lib" --add-modules javafx.controls,javafx.fxml -cp "app/GearMind.jar:app/lib/*" com.gearmind.launcher.LauncherApp
