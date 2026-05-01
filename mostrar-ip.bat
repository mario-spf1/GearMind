@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo ============================================================
echo   IP de este PC en la red local
echo ============================================================
echo.
echo   Apunta una de estas IPs en los PCs cliente del taller
echo   cuando ejecuten setup-client.bat en modo CLIENTE:
echo.

set FOUND=
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 ^| Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and ($_.PrefixOrigin -eq 'Dhcp' -or $_.PrefixOrigin -eq 'Manual') } ^| Select-Object -ExpandProperty IPAddress" 2^>nul') do (
    echo       %%i
    set FOUND=1
)

if not defined FOUND (
    echo       No se detectaron IPs de red local.
    echo       Comprueba que este PC esta conectado a la red.
)

echo.
echo   Nombre del PC: %COMPUTERNAME%
echo   ^(tambien puedes usar este nombre en lugar de la IP^)
echo.
pause
endlocal
