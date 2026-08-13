<#
.SYNOPSIS
    Muestra el estado real de las 4 bases de datos: tablas, conteos y primeras filas.

.DESCRIPTION
    Responde a "las tablas existen y tienen datos?" sin abrir pgAdmin.
    No modifica nada: solo consulta.

    Complementa a verificar.ps1, que mira el lado HTTP (imagenes, contenedores,
    endpoints). Este mira el lado de datos.

.EXAMPLE
    .\verificar-bd.ps1
#>
$ErrorActionPreference = 'Continue'

$contenedor = 'barberia-postgres'

# base -> tabla que le corresponde (patron database-per-service: una tabla por base)
$bases = [ordered]@{
    'barberia_servicios' = 'servicios'
    'barberia_barberos'  = 'barberos'
    'barberia_citas'     = 'citas'
    'barberia_auth'      = 'usuarios'
}

function Consultar($base, $sql) {
    # -A sin alinear, -t sin cabecera: salida limpia para parsear.
    docker exec $contenedor psql -U postgres -d $base -Atc $sql 2>&1
}

# ---------------------------------------------------------------------------
Write-Host "`n=== 1. Contenedor de PostgreSQL ===" -ForegroundColor Cyan

$estado = docker inspect $contenedor --format '{{.State.Status}}' 2>$null
if (-not $estado) {
    Write-Host "    '$contenedor' no existe. Levantalo con:  docker compose up -d postgres" -ForegroundColor Red
    exit 1
}
Write-Host "    estado   : $estado"
Write-Host "    puertos  : $((docker port $contenedor) -join ', ')"

# La causa numero uno de 'UnknownHostException: postgres' es que el contenedor
# se creo a mano con 'docker run' y quedo fuera de la red del proyecto.
$red = docker inspect $contenedor --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}'
Write-Host "    red      : $red"
if ($red -notmatch 'barberia') {
    Write-Host "    [PROBLEMA] No esta en la red barberia_default." -ForegroundColor Red
    Write-Host "               Los microservicios NO podran resolver el host 'postgres'." -ForegroundColor Red
    Write-Host "               Arreglo:  docker rm -f $contenedor ; docker compose up -d" -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
Write-Host "`n=== 2. Bases de datos existentes ===" -ForegroundColor Cyan

$existentes = docker exec $contenedor psql -U postgres -Atc "SELECT datname FROM pg_database WHERE datname LIKE 'barberia%' ORDER BY datname;" 2>&1
foreach ($b in $bases.Keys) {
    if ($existentes -contains $b) {
        Write-Host ("    [OK]    {0}" -f $b) -ForegroundColor Green
    } else {
        Write-Host ("    [FALTA] {0}  <-- recrea el volumen para que corra init-db.sql" -f $b) -ForegroundColor Red
    }
}

# ---------------------------------------------------------------------------
Write-Host "`n=== 3. Tablas y numero de registros ===" -ForegroundColor Cyan

foreach ($base in $bases.Keys) {
    $tabla = $bases[$base]
    if ($existentes -notcontains $base) { continue }

    $tablas = Consultar $base "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;"
    if (-not $tablas) {
        Write-Host ("    [SIN TABLAS] {0}" -f $base) -ForegroundColor Red
        Write-Host "                 El servicio no logro conectarse. Mira:  docker compose logs servicio-X" -ForegroundColor Yellow
        continue
    }

    $n = Consultar $base "SELECT count(*) FROM $tabla;"
    Write-Host ("    {0,-20} tablas: {1,-24} {2}: {3} registros" -f $base, ($tablas -join ','), $tabla, $n) -ForegroundColor Green
}

# ---------------------------------------------------------------------------
Write-Host "`n=== 4. Primeras 5 filas de cada tabla ===" -ForegroundColor Cyan

foreach ($base in $bases.Keys) {
    $tabla = $bases[$base]
    if ($existentes -notcontains $base) { continue }

    Write-Host "`n  --- $base.$tabla ---" -ForegroundColor White
    # Sin -A aqui: queremos la tabla formateada, que se lee mejor.
    docker exec $contenedor psql -U postgres -d $base -c "SELECT * FROM $tabla ORDER BY id LIMIT 5;" 2>&1 |
        ForEach-Object { "    $_" }
}

# ---------------------------------------------------------------------------
Write-Host "`n=== 5. Lo que devuelve la API (debe coincidir con la BD) ===" -ForegroundColor Cyan

$endpoints = [ordered]@{
    'servicios' = 'http://localhost:8080/api/servicios'
    'barberos'  = 'http://localhost:8080/api/barberos'
    'citas'     = 'http://localhost:8080/api/citas'
}
foreach ($nombre in $endpoints.Keys) {
    try {
        $r = Invoke-RestMethod -Uri $endpoints[$nombre] -TimeoutSec 5
        $cuenta = @($r).Count
        Write-Host ("    {0,-12} {1} registros via gateway" -f $nombre, $cuenta) -ForegroundColor Green
    } catch {
        Write-Host ("    {0,-12} sin respuesta ({1})" -f $nombre, $_.Exception.Message) -ForegroundColor Red
    }
}

Write-Host "`nEn pgAdmin conecta a  localhost : 5433  (usuario postgres / admin123)" -ForegroundColor Cyan
Write-Host ""
