@echo off
setlocal
cd /d "%~dp0"

if not exist ".env" (
  echo Error: no existe el fichero .env.
  echo Copia .env.template como .env y configura los datos de conexion.
  pause
  exit /b 1
)

if not exist "app\GearMind.jar" (
  echo Error: no se encontro app\GearMind.jar.
  echo Revisa que el paquete cliente este completo.
  pause
  exit /b 1
)

if not exist "app\lib\" (
  echo Error: no se encontro app\lib con dependencias runtime.
  echo Revisa que el paquete cliente este completo.
  pause
  exit /b 1
)

java -cp "app/GearMind.jar;app/lib/*" com.gearmind.presentation.App
if errorlevel 1 pause
endlocal