@echo off
setlocal
cd /d "%~dp0"
java -cp "app/GearMind.jar;app/lib/*" com.gearmind.presentation.App
endlocal