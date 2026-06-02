@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo  GearMind - Generando paquete cliente...
echo ============================================
echo.

:: -------------------------------------------------------
:: Cargar configuracion de despliegue
:: -------------------------------------------------------
if not exist "build-config.bat" (
  echo Error: no se encontro build-config.bat.
  echo Crea ese fichero con las credenciales del cliente antes de continuar.
  pause
  exit /b 1
)
call build-config.bat

if "!CLIENT_NAME!"=="" set CLIENT_NAME=Cliente
echo Cliente: !CLIENT_NAME!
echo.

:: -------------------------------------------------------
:: Comprobar herramientas
:: -------------------------------------------------------
java -version >nul 2>&1
if errorlevel 1 (
  echo Error: Java no esta instalado o no esta en el PATH.
  echo Instala Java 21 y vuelve a intentarlo.
  pause
  exit /b 1
)

:: Maven necesita un JDK (javac), no solo un JRE. Apuntamos JAVA_HOME al JDK
:: derivando su ruta desde la ubicacion de javac, para no depender de una
:: variable JAVA_HOME global que podria apuntar a un JRE.
set "JAVAC_BIN="
for /f "delims=" %%i in ('where javac 2^>nul') do if not defined JAVAC_BIN set "JAVAC_BIN=%%~dpi"
if not defined JAVAC_BIN (
  echo Error: no se encontro javac. Necesitas un JDK 21 ^(no solo un JRE^).
  echo Instala el JDK de Eclipse Adoptium / Temurin y vuelve a intentarlo.
  pause
  exit /b 1
)
for %%j in ("!JAVAC_BIN!..") do set "JAVA_HOME=%%~fj"
echo Usando JDK: !JAVA_HOME!

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

if not exist "setup-client.template.bat" (
  echo Error: no se encontro setup-client.template.bat en la raiz del proyecto.
  pause
  exit /b 1
)

if not exist "README-CLIENTE.md" (
  echo Error: no se encontro README-CLIENTE.md en la raiz del proyecto.
  pause
  exit /b 1
)

set PACKAGE_DIR=release\client-package
set APP_DIR=%PACKAGE_DIR%\app
set LIB_DIR=%APP_DIR%\lib
set ZIP_FILE=release\GearMind-!CLIENT_NAME!.zip

:: -------------------------------------------------------
:: [1/6] Compilar con Maven
:: -------------------------------------------------------
echo [1/6] Compilando proyecto con Maven...
call "!MVN_CMD!" -DskipTests clean package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependency
if errorlevel 1 (
  echo Error: fallo la compilacion de Maven.
  pause
  exit /b 1
)

:: -------------------------------------------------------
:: [2/6] Preparar estructura del paquete
:: -------------------------------------------------------
echo [2/6] Preparando estructura del paquete...
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
:: [3/6] Generar setup-client.bat con credenciales
:: -------------------------------------------------------
echo [3/6] Generando script de instalacion del cliente...

powershell -NoProfile -Command ^
  "$c = Get-Content 'setup-client.template.bat' -Raw -Encoding UTF8;" ^
  "$c = $c -replace '\{\{MYSQL_ROOT_PASS\}\}', '!MYSQL_ROOT_PASS!';" ^
  "$c = $c -replace '\{\{APP_DB_USER\}\}', '!APP_DB_USER!';" ^
  "$c = $c -replace '\{\{APP_DB_PASS\}\}', '!APP_DB_PASS!';" ^
  "$c = $c -replace '\{\{SUPPORT_USER\}\}', '!SUPPORT_USER!';" ^
  "$c = $c -replace '\{\{SUPPORT_PASS\}\}', '!SUPPORT_PASS!';" ^
  "$enc = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText((Resolve-Path '.').Path + '\%PACKAGE_DIR%\setup-client.bat', $c, $enc)"

if errorlevel 1 (
  echo Error: no se pudo generar setup-client.bat.
  pause
  exit /b 1
)

:: -------------------------------------------------------
:: [4/6] Copiar scripts de arranque
:: -------------------------------------------------------
echo [4/6] Copiando scripts y ficheros...
copy /y "launcher.bat" "%PACKAGE_DIR%\launcher.bat">nul
copy /y "run-app.bat" "%PACKAGE_DIR%\run-app.bat">nul
copy /y "run-bot.bat" "%PACKAGE_DIR%\run-bot.bat">nul
copy /y "mostrar-ip.bat" "%PACKAGE_DIR%\mostrar-ip.bat">nul
copy /y "update.bat" "%PACKAGE_DIR%\update.bat">nul
copy /y "README-CLIENTE.md" "%PACKAGE_DIR%\README-CLIENTE.md">nul

:: -------------------------------------------------------
:: [5/6] Generar ZIP
:: -------------------------------------------------------
echo [5/6] Generando ZIP...
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

:: -------------------------------------------------------
:: [6/6] Hecho
:: -------------------------------------------------------
echo [6/6] Hecho.
echo.
echo Paquete generado en: !ZIP_FILE!
echo.
echo Contenido del paquete:
echo setup-client.bat ^<-- ejecutar primero (como admin) y elegir SERVIDOR o CLIENTE
echo launcher.bat ^<-- para arrancar GearMind cada dia
echo mostrar-ip.bat ^<-- el SERVIDOR lo ejecuta para ver su IP
echo run-app.bat / run-bot.bat
echo app\GearMind.jar + lib\
echo.
echo Credenciales horneadas:
echo MySQL root: !MYSQL_ROOT_PASS!
echo App DB: !APP_DB_USER! / !APP_DB_PASS!
echo Soporte: !SUPPORT_USER! / !SUPPORT_PASS!
echo.
pause
endlocal
 