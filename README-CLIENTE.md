# GearMind Cliente

Este paquete está preparado para ejecutar GearMind sin código fuente.

## Contenido del paquete

- `app/GearMind.jar`
- `app/lib/` (dependencias runtime)
- `.env.template`
- `setup-db.bat` y `setup-db.sh`
- `run-app.bat` y `run-app.sh`
- `run-bot.bat` y `run-bot.sh`

## Requisitos

- Java 21 instalado y disponible en `PATH`.
- MySQL 8+ accesible desde este equipo.

## Configuración inicial

1. Copia `.env.template` como `.env`.
2. Edita `.env` con los datos reales del cliente:
   - Base de datos: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`.
   - Telegram: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_EMPRESA_ID`.

## Inicializar base de datos (Flyway)

- Linux/macOS:
  ```bash
  ./setup-db.sh
  ```
- Windows:
  ```bat
  setup-db.bat
  ```

Esto ejecuta las migraciones Flyway embebidas en el JAR (`classpath:db/migration`).

## Ejecutar aplicación de escritorio

- Linux/macOS:
  ```bash
  ./run-app.sh
  ```
- Windows:
  ```bat
  run-app.bat
  ```

## Ejecutar bot de Telegram (long polling)

El bot se conecta a la API de Telegram en modo long polling: solo necesita salida a internet, no requiere IP pública, port forwarding ni ngrok.


- Linux/macOS:
  ```bash
  ./run-bot.sh
  ```
- Windows:
  ```bat
  run-bot.bat
  ```

## Notas

- El mismo paquete sirve para todos los clientes.
- Lo único que cambia entre clientes es el archivo `.env`.