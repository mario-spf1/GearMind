@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

:: -------------------------------------------------------
:: Credenciales horneadas por build.bat - no editar
:: -------------------------------------------------------
set MYSQL_ROOT_PASS={{MYSQL_ROOT_PASS}}
set APP_DB_USER={{APP_DB_USER}}
set APP_DB_PASS={{APP_DB_PASS}}
set SUPPORT_USER={{SUPPORT_USER}}
set SUPPORT_PASS={{SUPPORT_PASS}}

set MYSQL_DIR=C:\MySQL91
set MYSQL_BIN=!MYSQL_DIR!\bin
set MYSQL_CMD=!MYSQL_BIN!\mysql.exe
set MYSQL_SVC=MySQL91
set JAVA_CMD=java

echo.
echo ============================================================
echo   GearMind - Instalacion y configuracion inicial
echo ============================================================
echo.

:: Verificar permisos de administrador
net session >nul 2>&1
if errorlevel 1 (
    echo ERROR: Este script necesita permisos de Administrador.
    echo Haz clic derecho en setup-client.bat y selecciona
    echo "Ejecutar como administrador".
    echo.
    pause
    exit /b 1
)

:: -------------------------------------------------------
:: [1/5] Verificar Java 21
:: -------------------------------------------------------
echo [1/5] Verificando Java 21...

java -version >nul 2>&1
if not errorlevel 1 (
    java -version 2>&1 | findstr /r "\"21\." >nul
    if not errorlevel 1 (
        echo       Java 21 encontrado. Continuando...
        goto :check_mysql
    )
    echo       Se encontro Java pero no es version 21. Instalando Java 21...
) else (
    echo       Java no encontrado. Instalando Java 21...
)

:: Intentar con winget primero
winget --version >nul 2>&1
if not errorlevel 1 (
    echo       Instalando mediante winget...
    winget install EclipseAdoptium.Temurin.21.JRE --silent --accept-source-agreements --accept-package-agreements
    if not errorlevel 1 goto :buscar_java_adoptium
)

:: Si winget no esta disponible, descargar MSI de Adoptium
echo       Descargando Java 21 JRE ^(puede tardar unos minutos^)...
set JAVA_MSI=%TEMP%\java21-jre.msi
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '!JAVA_MSI!' -UseBasicParsing"
if errorlevel 1 (
    echo ERROR: No se pudo descargar Java 21. Comprueba la conexion a internet.
    pause & exit /b 1
)
echo       Instalando Java 21...
msiexec /i "!JAVA_MSI!" /quiet /norestart
timeout /t 10 /nobreak >nul

:buscar_java_adoptium
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jre-21*") do (
    if exist "%%d\bin\java.exe" set JAVA_CMD=%%d\bin\java.exe
)
if "!JAVA_CMD!"=="java" (
    echo ERROR: Java 21 no se pudo instalar correctamente.
    echo Instala Java 21 manualmente desde https://adoptium.net y vuelve a ejecutar este script.
    pause & exit /b 1
)
echo       Java 21 instalado correctamente.
echo.

:check_mysql
:: -------------------------------------------------------
:: [2/5] Detectar puerto e instalar MySQL si es necesario
:: -------------------------------------------------------
echo [2/5] Comprobando MySQL...

set MYSQL_PORT=3306

:: Si nuestro servicio ya existe, reutilizar y leer su puerto del my.ini
sc query !MYSQL_SVC! >nul 2>&1
if not errorlevel 1 (
    echo       Servicio !MYSQL_SVC! ya instalado.
    if exist "!MYSQL_DIR!\my.ini" (
        for /f "tokens=2 delims==" %%p in ('findstr /b "port=" "!MYSQL_DIR!\my.ini"') do set MYSQL_PORT=%%p
    )
    if exist "!MYSQL_BIN!\mysql.exe" set MYSQL_CMD=!MYSQL_BIN!\mysql.exe
    echo       Puerto detectado: !MYSQL_PORT!
    goto :configurar_db
)

:: Comprobar si el puerto 3306 esta ocupado
netstat -ano -p tcp | findstr "LISTENING" | findstr ":3306 " >nul 2>&1
if errorlevel 1 goto :install_mysql

echo       Puerto 3306 ocupado. Comprobando si es un MySQL compatible...

:: Intentar conectar con la contrasena horneada
for /f "delims=" %%i in ('where mysql.exe 2^>nul') do (
    "%%i" -u root -p"!MYSQL_ROOT_PASS!" -P 3306 -h 127.0.0.1 -e "SELECT 1;" >nul 2>&1
    if not errorlevel 1 (
        set MYSQL_CMD=%%i
        echo       Conexion correcta. Reutilizando MySQL existente en 3306.
        goto :configurar_db
    )
)

echo       No se pudo reutilizar el MySQL existente. Buscando puerto alternativo...
set MYSQL_PORT=
for /l %%p in (3307,1,3320) do (
    if not defined MYSQL_PORT (
        netstat -ano -p tcp | findstr "LISTENING" | findstr ":%%p " >nul 2>&1
        if errorlevel 1 set MYSQL_PORT=%%p
    )
)
if not defined MYSQL_PORT (
    echo ERROR: No se encontro un puerto libre entre 3307-3320.
    pause & exit /b 1
)
echo       Se usara el puerto !MYSQL_PORT! para MySQL.

:install_mysql
echo       MySQL no encontrado. Instalando MySQL...
echo       ^(esto puede tardar varios minutos segun la conexion^)
echo.

:: winget instala siempre en 3306, asi que solo lo intentamos si MYSQL_PORT=3306
if not "!MYSQL_PORT!"=="3306" goto :install_zip

winget --version >nul 2>&1
if not errorlevel 1 (
    echo       Instalando MySQL mediante winget...
    winget install Oracle.MySQL --silent --accept-source-agreements --accept-package-agreements
    if not errorlevel 1 (
        echo       MySQL instalado via winget.
        net start MySQL >nul 2>&1
        net start MySQL91 >nul 2>&1
        timeout /t 8 /nobreak >nul
        for /d %%d in ("%ProgramFiles%\MySQL\MySQL Server 9*") do (
            if exist "%%d\bin\mysql.exe" set MYSQL_CMD=%%d\bin\mysql.exe
        )
        for /d %%d in ("%ProgramFiles%\MySQL\MySQL Server 8*") do (
            if exist "%%d\bin\mysql.exe" set MYSQL_CMD=%%d\bin\mysql.exe
        )
        for /f "delims=" %%i in ('where mysql.exe 2^>nul') do set MYSQL_CMD=%%i
        goto :configurar_db
    )
    echo       winget no pudo instalar MySQL. Intentando descarga directa...
)

:install_zip
:: Descargar ZIP desde CDN de MySQL e instalar con puerto personalizado
set MYSQL_ZIP=%TEMP%\mysql-9.1.0-winx64.zip
echo       Descargando MySQL desde cdn.mysql.com (puerto !MYSQL_PORT!)...
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://cdn.mysql.com/Downloads/MySQL-9.1/mysql-9.1.0-winx64.zip' -OutFile '!MYSQL_ZIP!' -UseBasicParsing"
if errorlevel 1 (
    echo.
    echo ERROR: No se pudo descargar MySQL automaticamente.
    echo.
    echo Descarga MySQL manualmente desde:
    echo   https://dev.mysql.com/downloads/mysql/
    echo Elige la version ZIP para Windows x64, instala y vuelve a ejecutar este script.
    pause & exit /b 1
)

echo       Descarga completada. Instalando...
if exist "!MYSQL_DIR!" rd /s /q "!MYSQL_DIR!"
powershell -NoProfile -Command "Expand-Archive -Path '!MYSQL_ZIP!' -DestinationPath 'C:\' -Force"
if errorlevel 1 (echo ERROR: No se pudo extraer MySQL. & pause & exit /b 1)

for /d %%d in (C:\mysql-9.1*) do (
    if not "%%d"=="!MYSQL_DIR!" ren "%%d" "MySQL91" 2>nul
)

if not exist "!MYSQL_DIR!\bin\mysqld.exe" (
    echo ERROR: Instalacion corrupta, no se encontro mysqld.exe
    pause & exit /b 1
)

(
echo [mysqld]
echo basedir=C:/MySQL91
echo datadir=C:/MySQL91/data
echo port=!MYSQL_PORT!
echo bind-address=0.0.0.0
echo character-set-server=utf8mb4
echo collation-server=utf8mb4_unicode_ci
echo [client]
echo default-character-set=utf8mb4
echo port=!MYSQL_PORT!
) > "!MYSQL_DIR!\my.ini"

echo       Inicializando directorio de datos...
"!MYSQL_BIN!\mysqld" --defaults-file="!MYSQL_DIR!\my.ini" --initialize-insecure 2>nul
if errorlevel 1 (echo ERROR: Fallo al inicializar MySQL. & pause & exit /b 1)

"!MYSQL_BIN!\mysqld" --install !MYSQL_SVC! --defaults-file="!MYSQL_DIR!\my.ini" >nul 2>&1
net start !MYSQL_SVC!
if errorlevel 1 (echo ERROR: No se pudo iniciar el servicio !MYSQL_SVC!. & pause & exit /b 1)

echo       Esperando a que MySQL arranque...
timeout /t 8 /nobreak >nul
echo       MySQL instalado en puerto !MYSQL_PORT! y en ejecucion.
echo.

:configurar_db
:: -------------------------------------------------------
:: [3/5] Configurar base de datos y usuarios
:: -------------------------------------------------------
echo [3/5] Configurando base de datos y usuarios...

"!MYSQL_CMD!" -u root -P !MYSQL_PORT! -h 127.0.0.1 --connect-expired-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '!MYSQL_ROOT_PASS!';" 2>nul

"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo ERROR: No se pudo conectar a MySQL en el puerto !MYSQL_PORT!. Comprueba las credenciales.
    pause & exit /b 1
)

:: Reset completo: eliminar BD y usuarios previos para empezar limpio
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "DROP DATABASE IF EXISTS gearmind;" 2>nul
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "DROP USER IF EXISTS '!APP_DB_USER!'@'localhost';" 2>nul
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "DROP USER IF EXISTS '!SUPPORT_USER!'@'%%';" 2>nul

"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "CREATE DATABASE gearmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "CREATE USER IF NOT EXISTS '!APP_DB_USER!'@'localhost' IDENTIFIED BY '!APP_DB_PASS!';"
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "GRANT ALL PRIVILEGES ON gearmind.* TO '!APP_DB_USER!'@'localhost';"
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "CREATE USER IF NOT EXISTS '!SUPPORT_USER!'@'%%' IDENTIFIED BY '!SUPPORT_PASS!';"
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "GRANT SELECT, INSERT, UPDATE, DELETE ON gearmind.* TO '!SUPPORT_USER!'@'%%';"
"!MYSQL_CMD!" -u root -p"!MYSQL_ROOT_PASS!" -P !MYSQL_PORT! -h 127.0.0.1 -e "FLUSH PRIVILEGES;"
echo       Base de datos y usuarios configurados.
echo.

:: -------------------------------------------------------
:: [4/5] Abrir puerto en firewall de Windows
:: -------------------------------------------------------
echo [4/5] Configurando firewall de Windows...
netsh advfirewall firewall delete rule name="GearMind MySQL" >nul 2>&1
netsh advfirewall firewall add rule name="GearMind MySQL" dir=in action=allow protocol=TCP localport=!MYSQL_PORT! >nul
echo       Puerto !MYSQL_PORT! abierto en el firewall de Windows.
echo.

:: -------------------------------------------------------
:: [5/5] Inicializar BD, crear tablas y primer administrador
:: -------------------------------------------------------
echo [5/5] Inicializando base de datos...

(
echo APP_ENV=production
echo DB_HOST=localhost
echo DB_PORT=!MYSQL_PORT!
echo DB_NAME=gearmind
echo DB_USER=!APP_DB_USER!
echo DB_PASS=!APP_DB_PASS!
echo DB_POOL_MAX=10
echo TELEGRAM_BOT_TOKEN=
echo TELEGRAM_WEBHOOK_SECRET=
echo TELEGRAM_WEBHOOK_PATH=/api/telegram/webhook
echo TELEGRAM_WEBHOOK_PORT=8081
echo TELEGRAM_EMPRESA_ID=1
) > .env

"!JAVA_CMD!" -cp "app/GearMind.jar;app/lib/*" com.gearmind.infrastructure.database.DatabaseSetupMain
if errorlevel 1 (
    echo ERROR: Fallo al crear las tablas de la base de datos.
    pause & exit /b 1
)
echo       Tablas creadas correctamente.
echo.

echo ============================================================
echo   Configuracion del primer administrador
echo ============================================================
echo   Introduce los datos con los que accederas a GearMind.
echo   Podras crear mas usuarios desde la aplicacion.
echo ============================================================
echo.

"!JAVA_CMD!" -cp "app/GearMind.jar;app/lib/*" com.gearmind.infrastructure.database.FirstSetupMain
if errorlevel 1 (
    echo ERROR: Fallo al crear el administrador inicial.
    pause & exit /b 1
)

:: -------------------------------------------------------
echo.
echo ============================================================
echo   Instalacion completada
echo ============================================================
echo.
echo   Haz doble clic en launcher.bat para iniciar GearMind.
echo.
echo   Puerto MySQL en uso: !MYSQL_PORT!
echo.
echo   NOTA: Para soporte tecnico remoto, abre el puerto !MYSQL_PORT!
echo   en tu router apuntando a este ordenador.
echo   Si no sabes como hacerlo, contacta con tu tecnico de red.
echo.
pause
endlocal
