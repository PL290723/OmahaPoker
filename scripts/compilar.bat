@echo off
chcp 65001 >nul
echo.
echo ════════════════════════════════════════════
echo   COMPILANDO OMAHA POKER
echo ════════════════════════════════════════════
echo.

cd src

echo Compilando clases...
javac Main.java poker/core/*.java poker/ai/*.java poker/game/*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [✓] Compilación exitosa
    echo.
    echo Archivos compilados en: src/
    echo Para ejecutar: ejecutar.bat
) else (
    echo.
    echo [✗] Error en la compilación
    echo.
    echo Verifica que:
    echo 1. Java JDK esté instalado (java -version)
    echo 2. JAVA_HOME esté configurado correctamente
    echo 3. Los archivos .java estén sin errores
)

cd ..
pause
