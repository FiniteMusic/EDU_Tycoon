@echo off
chcp 65001 >nul
cd /d "%~dp0"
py "herramientas\editor_niveles_tiled.py" --preparar
if errorlevel 1 echo ERROR: no se pudo crear el editor de niveles.
pause
