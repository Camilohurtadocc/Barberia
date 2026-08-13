<#
.SYNOPSIS
    Comprueba QUE se esta sirviendo realmente, sin abrir el navegador.

.DESCRIPTION
    Responde a la pregunta "el contenedor tiene mi codigo nuevo o no".
    No reconstruye nada: solo diagnostica.
#>
$ErrorActionPreference = 'Continue'
$raiz = $PSScriptRoot

$backends = [ordered]@{
    'Barberia-gateway'   = 'servicio-gateway.jar'
    'Barberia-auth'      = 'servicio-auth.jar'
    'Barberia-servicios' = 'servicio-catalogo.jar'
    'Barberia-barberos'  = 'servicio-barberos.jar'
    'Barberia-citas'     = 'servicio-citas.jar'
}

Write-Host "`n=== 1. Fecha de los artefactos en el HOST ===" -ForegroundColor Cyan
foreach ($c in $backends.Keys) {
    $jar = Join-Path $raiz "$c\applications\app-service\build\libs\$($backends[$c])"
    if (Test-Path $jar) {
        "{0,-22} {1}" -f $c, (Get-Item $jar).LastWriteTime
    } else {
        Write-Host ("{0,-22} NO EXISTE  <-- el build de Docker fallara aqui" -f $c) -ForegroundColor Red
    }
}
$indice = Join-Path $raiz "barberia-frontend\dist\spa\index.html"
if (Test-Path $indice) {
    "{0,-22} {1}" -f 'frontend dist/spa', (Get-Item $indice).LastWriteTime
} else {
    Write-Host ("{0,-22} NO EXISTE  <-- falta 'quasar build'" -f 'frontend dist/spa') -ForegroundColor Red
}

Write-Host "`n=== 2. Fecha de las IMAGENES Docker ===" -ForegroundColor Cyan
Write-Host "    (si una imagen es mas VIEJA que su artefacto, no se reconstruyo)" -ForegroundColor DarkGray
docker images --filter "reference=barberia-*" --format "{{.Repository}}`t{{.CreatedAt}}"

Write-Host "`n=== 3. Estado de los contenedores ===" -ForegroundColor Cyan
docker compose -f "$raiz\docker-compose.yml" ps --format "{{.Name}}`t{{.Status}}`t{{.Ports}}"

Write-Host "`n=== 4. Codigo REAL dentro del contenedor frontend ===" -ForegroundColor Cyan
$hashHost = (Get-FileHash $indice -Algorithm SHA256).Hash
$hashCont = docker exec barberia-frontend sha256sum /usr/share/nginx/html/index.html 2>$null
if ($hashCont) {
    $hashCont = ($hashCont -split '\s+')[0].ToUpper()
    "host      : $($hashHost.Substring(0,32))"
    "contenedor: $($hashCont.Substring(0,32))"
    if ($hashHost -eq $hashCont) {
        Write-Host "    IGUALES: la imagen tiene tu build actual." -ForegroundColor Green
    } else {
        Write-Host "    DISTINTOS: la imagen es vieja. Ejecuta .\reiniciar-proyecto.ps1" -ForegroundColor Red
    }
} else {
    Write-Host "    contenedor barberia-frontend no esta corriendo" -ForegroundColor Yellow
}

Write-Host "`n=== 5. Respuesta HTTP (sin navegador, sin cache) ===" -ForegroundColor Cyan
$urls = @(
    'http://localhost/',
    'http://localhost:8080/actuator/health',
    'http://localhost:8080/api/servicios',
    'http://localhost:8080/api/barberos',
    'http://localhost:8080/api/citas'
)
foreach ($u in $urls) {
    try {
        $r = Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 5 -Headers @{ 'Cache-Control' = 'no-cache' }
        Write-Host ("    {0,-42} HTTP {1}" -f $u, $r.StatusCode) -ForegroundColor Green
    } catch {
        $codigo = $_.Exception.Response.StatusCode.value__
        if (-not $codigo) { $codigo = 'sin respuesta' }
        Write-Host ("    {0,-42} {1}" -f $u, $codigo) -ForegroundColor Red
    }
}

Write-Host "`n=== 6. Reglas de seguridad del gateway ===" -ForegroundColor Cyan
try {
    $login = Invoke-RestMethod -Uri 'http://localhost:8080/auth/login' -Method Post `
        -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}'
    Write-Host "    login admin/admin123                 OK (rol $($login.rol))" -ForegroundColor Green

    try {
        Invoke-WebRequest -Uri 'http://localhost:8080/api/servicios' -Method Post -UseBasicParsing `
            -ContentType 'application/json' -Body '{}' -TimeoutSec 5 | Out-Null
        Write-Host "    POST sin token                       DEBERIA SER 401" -ForegroundColor Red
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "    POST sin token                       401 correcto" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "    no se pudo hacer login: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== 7. Errores recientes en los logs ===" -ForegroundColor Cyan
$logs = docker compose -f "$raiz\docker-compose.yml" logs --tail 200 2>$null |
    Select-String -Pattern 'ERROR|Exception|FATAL|does not exist|Connection refused'
if ($logs) { $logs | Select-Object -Last 10 | ForEach-Object { Write-Host "    $($_.Line)" -ForegroundColor Yellow } }
else { Write-Host "    sin errores" -ForegroundColor Green }

Write-Host ""
