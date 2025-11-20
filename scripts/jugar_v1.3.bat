@echo off
chcp 65001 > nul
echo ========================================
echo   OMAHA POKER v1.3 - CON PERFILES
echo   Compilando...
echo ========================================
echo.

cd src
javac -encoding UTF-8 Main.java poker\core\*.java poker\ai\*.java poker\game\*.java 2>nul

if %ERRORLEVEL% == 0 (
    echo ✓ Compilación exitosa!
    echo.
    echo ========================================
    echo   Ejecutando juego...
    echo ========================================
    echo.
    java Main
) else (
    echo ✗ Error en la compilación
    echo Ejecutando con errores detallados...
    javac -encoding UTF-8 Main.java poker\core\*.java poker\ai\*.java poker\game\*.java
    pause
)
