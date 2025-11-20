# Script para eliminar símbolos Unicode incompatibles con la consola de Windows
# Reemplaza símbolos de box drawing y otros caracteres especiales

$reemplazos = @{
    '╔' = '+'
    '╗' = '+'
    '╚' = '+'
    '╝' = '+'
    '╠' = '+'
    '╣' = '+'
    '║' = '|'
    '═' = '='
    '★' = '*'
    '✓' = 'OK'
    '✗' = 'X'
    '►' = '>'
    '▶' = '>'
    '●' = '*'
    '■' = '#'
    '◆' = '*'
}

# Buscar todos los archivos .java
$archivos = Get-ChildItem -Path "src" -Filter "*.java" -Recurse

foreach ($archivo in $archivos) {
    Write-Host "Procesando: $($archivo.FullName)"
    
    $contenido = Get-Content $archivo.FullName -Raw -Encoding UTF8
    $modificado = $false
    
    foreach ($simbolo in $reemplazos.Keys) {
        if ($contenido -match [regex]::Escape($simbolo)) {
            $contenido = $contenido -replace [regex]::Escape($simbolo), $reemplazos[$simbolo]
            $modificado = $true
        }
    }
    
    if ($modificado) {
        Set-Content -Path $archivo.FullName -Value $contenido -Encoding UTF8 -NoNewline
        Write-Host "  -> Modificado" -ForegroundColor Green
    } else {
        Write-Host "  -> Sin cambios" -ForegroundColor Gray
    }
}

Write-Host "`nProceso completado!" -ForegroundColor Cyan
