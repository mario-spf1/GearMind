@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================================
echo   GearMind - Actualizacion
echo ============================================================
echo.

if not exist "update\app\GearMind.jar" (
    echo ERROR: no se encontro update\app\GearMind.jar.
    echo Asegurate de haber descomprimido el ZIP de actualizacion
    echo dentro de la carpeta de GearMind ^(la que contiene launcher.bat^).
    echo.
    pause
    exit /b 1
)

if not exist "app\GearMind.jar" (
    echo ERROR: no se encontro la instalacion de GearMind en esta carpeta.
    echo Esta carpeta no parece ser una instalacion existente.
    echo Si es la primera vez que instalas GearMind, ejecuta setup-client.bat.
    echo.
    pause
    exit /b 1
)

echo Comprobando que GearMind no este abierto...
tasklist /FI "IMAGENAME eq java.exe" 2>nul | findstr /i "java.exe" >nul
if not errorlevel 1 (
    echo.
    echo AVISO: hay procesos Java en ejecucion. Cierra GearMind antes de actualizar.
    echo Si estas seguro de que GearMind esta cerrado, pulsa una tecla para continuar.
    pause
)

echo.
echo [1/3] Haciendo copia de seguridad de la version anterior...
if exist "app.bak" rd /s /q "app.bak"
xcopy /e /i /q "app" "app.bak" >nul
if errorlevel 1 (
    echo ERROR: no se pudo crear la copia de seguridad.
    pause & exit /b 1
)
echo       Copia guardada en app.bak\

echo [2/3] Sustituyendo archivos de la aplicacion...
del /q "app\GearMind.jar" 2>nul
if exist "app\lib" rd /s /q "app\lib"
xcopy /e /i /q "update\app\*" "app\" >nul
if errorlevel 1 (
    echo ERROR: fallo al copiar los nuevos archivos.
    echo Restaurando version anterior...
    if exist "app" rd /s /q "app"
    xcopy /e /i /q "app.bak" "app" >nul
    pause & exit /b 1
)

echo [3/3] Limpiando archivos temporales...
rd /s /q "update"

echo.
echo ============================================================
echo   Actualizacion completada
echo ============================================================
echo.
echo   La copia de seguridad de la version anterior esta en app.bak\
echo   Puedes borrarla cuando confirmes que todo funciona.
echo.
echo   Al iniciar GearMind con launcher.bat, la base de datos
echo   se actualizara automaticamente si es necesario.
echo   Tus datos ^(clientes, citas, vehiculos^) se mantienen intactos.
echo.
pause
endlocal
