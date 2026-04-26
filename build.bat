@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo  GearMind - Generando paquete cliente...
echo ============================================
echo.

:: Comprobar Java
java -version >nul 2>&1
if errorlevel 1 (
  echo Error: Java no esta instalado o no esta en el PATH.
  echo Instala Java 21 y vuelve a intentarlo.
  pause
  exit /b 1
)

:: Buscar Maven: PATH primero, luego NetBeans bundled
set MVN_CMD=mvn
mvn -version >nul 2>&1
if errorlevel 1 (
  set MVN_CMD=C:\Program Files\NetBeans-23\netbeans\java\maven\bin\mvn.cmd
  if not exist "!MVN_CMD!" (
    echo Error: Maven no encontrado ni en el PATH ni en NetBeans.
    echo Opciones:
    echo   1. Instala Maven: https://maven.apache.org/download.cgi
    echo   2. O abre el proyecto en NetBeans y compila desde ahi.
    pause
    exit /b 1
  )
)

:: Comprobar README-CLIENTE.md
if not exist "README-CLIENTE.md" (
  echo Error: no se encontro README-CLIENTE.md en la raiz del proyecto.
  pause
  exit /b 1
)

set PACKAGE_DIR=release\client-package
set APP_DIR=%PACKAGE_DIR%\app
set LIB_DIR=%APP_DIR%\lib
set ZIP_FILE=release\GearMind-Cliente.zip

echo [1/5] Compilando proyecto con Maven...
call "!MVN_CMD!" -DskipTests clean package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependency
if errorlevel 1 (
  echo Error: fallo la compilacion de Maven.
  pause
  exit /b 1
)

echo [2/5] Preparando estructura del paquete...
if exist "%PACKAGE_DIR%" rd /s /q "%PACKAGE_DIR%"
mkdir "%LIB_DIR%"

:: Buscar el JAR principal (excluir original-*)
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

echo [3/5] Copiando scripts y plantilla...
copy /y ".env.template"  "%PACKAGE_DIR%\.env.template" >nul
copy /y "setup-db.bat"   "%PACKAGE_DIR%\setup-db.bat"  >nul
copy /y "run-app.bat"    "%PACKAGE_DIR%\run-app.bat"   >nul
copy /y "run-bot.bat"    "%PACKAGE_DIR%\run-bot.bat"   >nul
copy /y "README-CLIENTE.md" "%PACKAGE_DIR%\README-CLIENTE.md" >nul

echo [4/5] Generando ZIP...
if exist "%ZIP_FILE%" del /q "%ZIP_FILE%"
powershell -NoProfile -Command "Compress-Archive -Path '%PACKAGE_DIR%\*' -DestinationPath '%ZIP_FILE%'"
if errorlevel 1 (
  echo Error: no se pudo crear el ZIP.
  pause
  exit /b 1
)

echo [5/5] Hecho.
echo.
echo Paquete generado en: %ZIP_FILE%
echo.
pause
