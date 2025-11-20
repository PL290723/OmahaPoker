@echo off
chcp 65001 >nul
echo.
echo ╔═══════════════════════════════════════════════════════════╗
echo ║                                                           ║
echo ║        ♠♥  OMAHA POKER - DESAFÍO DE CAMPEONES  ♣♦        ║
echo ║                                                           ║
echo ║              Script de Compilación y Ejecución            ║
echo ║                                                           ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

cd src

echo [1/3] Compilando archivos Java...
javac Main.java poker/core/*.java poker/ai/*.java poker/game/*.java

if %ERRORLEVEL% EQU 0 (
    echo [✓] Compilación exitosa
    echo.
    echo [2/3] Ejecutando el juego...
    echo.
    java Main
) else (
    echo [✗] Error en la compilación
    echo.
    echo Verifica que tengas Java JDK instalado correctamente.
    echo Ejecuta: java -version
    pause
)

echo.
echo [3/3] Limpieza completada
cd ..
pause
