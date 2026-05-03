@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo  GearMind - Generando paquete de ACTUALIZACION...
echo ============================================
echo.

:: -------------------------------------------------------
:: Leer version del pom.xml
:: -------------------------------------------------------
set APP_VERSION=
for /f "tokens=*" %%v in ('powershell -NoProfile -Command "([xml](Get-Content pom.xml)).project.version"') do set APP_VERSION=%%v
if "!APP_VERSION!"=="" (
    echo Error: no se pudo leer la version del pom.xml.
    pause & exit /b 1
)
echo Version: !APP_VERSION!
echo.

:: -------------------------------------------------------
:: Comprobar Maven
:: -------------------------------------------------------
set MVN_CMD=mvn
mvn -version >nul 2>&1
if errorlevel 1 (
  set MVN_CMD=C:\Program Files\NetBeans-23\netbeans\java\maven\bin\mvn.cmd
  if not exist "!MVN_CMD!" (
    echo Error: Maven no encontrado ni en el PATH ni en NetBeans.
    pause
    exit /b 1
  )
)

set PACKAGE_DIR=release\update-package
set APP_DIR=%PACKAGE_DIR%\update\app
set LIB_DIR=%APP_DIR%\lib
set ZIP_FILE=release\GearMind-update-!APP_VERSION!.zip

:: -------------------------------------------------------
:: [1/4] Compilar con Maven
:: -------------------------------------------------------
echo [1/4] Compilando proyecto con Maven...
call "!MVN_CMD!" -DskipTests clean package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependency
if errorlevel 1 (
  echo Error: fallo la compilacion de Maven.
  pause
  exit /b 1
)

:: -------------------------------------------------------
:: [2/4] Preparar estructura del paquete
:: -------------------------------------------------------
echo [2/4] Preparando estructura del paquete de actualizacion...
if exist "%PACKAGE_DIR%" rd /s /q "%PACKAGE_DIR%"
mkdir "%LIB_DIR%"

set JAR_SOURCE=
for %%f in (target\*.jar) do (
  echo %%f | findstr /i "original" >nul || set JAR_SOURCE=%%f
)
if "!JAR_SOURCE!"=="" (
  echo Error: no se encontro el JAR en target\.
  pause
  exit /b 1
)

copy /y "!JAR_SOURCE!" "%APP_DIR%\GearMind.jar" >nul
xcopy /s /q "target\dependency\*" "%LIB_DIR%\" >nul

:: -------------------------------------------------------
:: [3/4] Copiar update.bat y README
:: -------------------------------------------------------
echo [3/4] Copiando script de actualizacion...
copy /y "update.bat" "%PACKAGE_DIR%\update.bat" >nul

(
echo GearMind - Actualizacion v!APP_VERSION!
echo ============================================
echo.
echo COMO ACTUALIZAR:
echo.
echo   1. Cierra GearMind si lo tienes abierto.
echo   2. Descomprime este ZIP DENTRO de la carpeta donde tienes
echo      instalado GearMind ^(la que contiene launcher.bat^).
echo      Debe quedar asi:
echo        GearMind\
echo          launcher.bat
echo          app\
echo          update\         ^<-- nuevo, contiene la version nueva
echo          update.bat      ^<-- nuevo, lo ejecutaras
echo   3. Haz doble clic en update.bat.
echo   4. Cuando termine, abre GearMind con launcher.bat normal.
echo.
echo Tus datos ^(clientes, citas, vehiculos^) se mantienen.
echo La actualizacion crea una copia de seguridad en app.bak\
) > "%PACKAGE_DIR%\LEEME-ACTUALIZACION.txt"

:: -------------------------------------------------------
:: [4/4] Generar ZIP
:: -------------------------------------------------------
echo [4/4] Generando ZIP...
if exist "!ZIP_FILE!" del /q "!ZIP_FILE!"
powershell -NoProfile -Command ^
  "Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
  "$src = (Resolve-Path '%PACKAGE_DIR%').Path;" ^
  "$dst = Join-Path (Resolve-Path '.').Path '!ZIP_FILE!';" ^
  "for ($i=0; $i -lt 5; $i++) {" ^
  "  try { [System.IO.Compression.ZipFile]::CreateFromDirectory($src, $dst, 'Optimal', $false); break }" ^
  "  catch { if ($i -eq 4) { throw } ; Start-Sleep -Seconds 2 ; if (Test-Path $dst) { Remove-Item $dst -Force } }" ^
  "}"
if errorlevel 1 (
  echo Error: no se pudo crear el ZIP.
  pause
  exit /b 1
)

echo.
echo ============================================
echo   Paquete de actualizacion generado:
echo     !ZIP_FILE!
echo ============================================
echo.
echo   Envia este ZIP al cliente. Contiene instrucciones dentro.
echo   No incluye setup-client.bat ni .env: solo reemplaza el binario.
echo.
pause
endlocal
