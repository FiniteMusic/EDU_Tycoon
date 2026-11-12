@echo off
chcp 65001 >nul
cd /d "%~dp0"
py "herramientas\editor_niveles_tiled.py" --sincronizar
if errorlevel 1 (
  echo ERROR: revisa Tiled antes de compilar.
  pause
  exit /b 1
)
echo Puedes abrir el juego con Android Studio, o instalarlo con:
echo gradlew.bat :android:installDebug --console=plain
pause
