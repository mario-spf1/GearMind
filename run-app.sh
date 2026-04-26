#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f ".env" ]]; then
  echo "No existe el fichero .env."
  echo "Copia .env.template como .env y configura los datos de conexión."
  exit 1
fi

set -a
source ".env"
set +a

if [[ ! -f "app/GearMind.jar" ]]; then
  echo "No se encontró app/GearMind.jar."
  echo "Revisa que el paquete cliente esté completo."
  exit 1
fi

if [[ ! -d "app/lib" ]]; then
  echo "No se encontró app/lib con dependencias runtime."
  echo "Revisa que el paquete cliente esté completo."
  exit 1
fi

java --module-path "app/lib" --add-modules javafx.controls,javafx.fxml -cp "app/GearMind.jar:app/lib/*" com.gearmind.presentation.App