@echo off
chcp 65001 > nul
echo ========================================
echo   OMAHA POKER v1.2
echo   Compilando con nuevas funciones...
echo ========================================
echo.

cd src
javac -encoding UTF-8 Main.java poker\core\*.java poker\ai\*.java poker\game\*.java

if %ERRORLEVEL% == 0 (
    echo.
    echo ✓ Compilación exitosa!
    echo.
    echo ========================================
    echo   Ejecutando juego...
    echo ========================================
    echo.
    java Main
) else (
    echo.
    echo ✗ Error en la compilación
    pause
)
