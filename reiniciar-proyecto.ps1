<#
.SYNOPSIS
    Reconstruye y levanta todo el proyecto barberia desde cero, en el orden correcto.

.DESCRIPTION
    El orden importa: los Dockerfile NO compilan nada, solo copian artefactos ya
    construidos en el host (los .jar de Gradle y dist/spa de Quasar). Si esos
    artefactos estan viejos o no existen, la imagen sale vieja o el build falla.

    Este script garantiza la secuencia y ABORTA al primer fallo, que es
    justamente lo que 'docker compose build' no hace: compose se detiene en el
    servicio que falla, pero 'docker compose up -d' arranca igual con las
    imagenes viejas y parece que "los cambios no se reflejan".

.PARAMETER Rapido
    Usa la cache de Docker al construir las imagenes (mucho mas rapido).
    Sin este switch se construye con --no-cache.

.PARAMETER BorrarVolumenes
    Elimina tambien el volumen de PostgreSQL. OJO: borra los datos.

.EXAMPLE
    .\reiniciar-proyecto.ps1
    .\reiniciar-proyecto.ps1 -Rapido
    .\reiniciar-proyecto.ps1 -BorrarVolumenes
#>
[CmdletBinding()]
param(
    [switch]$Rapido,
    [switch]$BorrarVolumenes
)

# NO se usa 'Stop': docker, gradle y npm escriben su progreso en stderr, y con
# ErrorActionPreference='Stop' PowerShell 5.1 convierte esas lineas en
# NativeCommandError y aborta el script aunque el comando haya ido bien
# (se nota sobre todo al redirigir la salida con 2>&1 o al canalizarla).
# El control de errores real se hace comprobando $LASTEXITCODE.
$ErrorActionPreference = 'Continue'
$raiz = $PSScriptRoot
$inicio = Get-Date

# Servicios backend: carpeta -> nombre del jar que espera su Dockerfile
$backends = [ordered]@{
    'Barberia-gateway'   = 'servicio-gateway.jar'
    'Barberia-auth'      = 'servicio-auth.jar'
    'Barberia-servicios' = 'servicio-catalogo.jar'
    'Barberia-barberos'  = 'servicio-barberos.jar'
    'Barberia-citas'     = 'servicio-citas.jar'
}

function Escribir-Paso($texto) {
    Write-Host ""
    Write-Host "==> $texto" -ForegroundColor Cyan
}

function Escribir-Ok($texto) {
    Write-Host "    [OK] $texto" -ForegroundColor Green
}

function Abortar($texto) {
    Write-Host ""
    Write-Host "[ERROR] $texto" -ForegroundColor Red
    Write-Host "Se detiene aqui a proposito: seguir adelante dejaria imagenes viejas corriendo." -ForegroundColor Red
    exit 1
}

# Los comandos nativos (gradlew, npm, docker) no lanzan excepciones: hay que
# revisar $LASTEXITCODE a mano o el script sigue como si nada hubiera pasado.
function Verificar-Salida($queFallo) {
    if ($LASTEXITCODE -ne 0) { Abortar $queFallo }
}

# ---------------------------------------------------------------------------
Escribir-Paso "0/6  Comprobando que Docker responde"
docker info *> $null
Verificar-Salida "Docker no responde. Abre Docker Desktop y espera a que arranque."
Escribir-Ok "Docker operativo"

# ---------------------------------------------------------------------------
Escribir-Paso "1/6  Bajando contenedores del proyecto"
# Se limpia SOLO este proyecto. No se usa 'docker system prune -a' porque eso
# borraria las imagenes de TODOS tus otros proyectos.
if ($BorrarVolumenes) {
    docker compose -f "$raiz\docker-compose.yml" down --remove-orphans --volumes
    Write-Host "    Volumen de PostgreSQL eliminado" -ForegroundColor Yellow
} else {
    docker compose -f "$raiz\docker-compose.yml" down --remove-orphans
}

# 'docker compose down' solo borra lo que creo compose. Un contenedor llamado
# barberia-postgres creado a mano con 'docker run' SOBREVIVE aqui, se queda en la
# red 'bridge' en vez de 'barberia_default', y entonces los microservicios fallan
# con 'UnknownHostException: postgres' aunque todo lo demas este bien.
$huerfano = docker ps -aq --filter "name=^barberia-postgres$" 2>$null
if ($huerfano) {
    $esDeCompose = docker inspect $huerfano --format '{{index .Config.Labels "com.docker.compose.project"}}' 2>$null
    if (-not $esDeCompose) {
        Write-Host "    barberia-postgres quedo de un 'docker run' manual: se elimina" -ForegroundColor Yellow
        docker rm -f $huerfano *> $null
    }
}
Escribir-Ok "Contenedores detenidos"

# ---------------------------------------------------------------------------
Escribir-Paso "2/6  Compilando los JAR de los 5 microservicios"
foreach ($carpeta in $backends.Keys) {
    $jar = $backends[$carpeta]
    $ruta = Join-Path $raiz $carpeta
    $destino = Join-Path $ruta "applications\app-service\build\libs\$jar"

    Write-Host "    - $carpeta ..." -NoNewline
    Push-Location $ruta
    try {
        & .\gradlew.bat clean bootJar --quiet *> $null
        $codigo = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($codigo -ne 0) {
        Write-Host ""
        Abortar "Fallo la compilacion de $carpeta. Ejecuta:  cd $carpeta ; .\gradlew.bat bootJar   para ver el detalle."
    }
    if (-not (Test-Path $destino)) {
        Write-Host ""
        Abortar "Gradle termino sin error pero no genero $jar en $destino"
    }
    # El jar tiene que ser de ESTA ejecucion, no uno viejo que sobrevivio.
    $fecha = (Get-Item $destino).LastWriteTime
    if ($fecha -lt $inicio) {
        Write-Host ""
        Abortar "$jar es de $fecha, anterior al inicio del script. No se regenero."
    }
    Write-Host " OK ($fecha)" -ForegroundColor Green
}

# ---------------------------------------------------------------------------
Escribir-Paso "3/6  Compilando el frontend Quasar"
$frontend = Join-Path $raiz "barberia-frontend"
$indice = Join-Path $frontend "dist\spa\index.html"

Push-Location $frontend
try {
    if (-not (Test-Path (Join-Path $frontend "node_modules"))) {
        Write-Host "    node_modules no existe, instalando dependencias..."
        npm install
        Verificar-Salida "Fallo npm install"
    }
    # dist/spa se borra a proposito: si el build falla, no queremos que quede
    # la version anterior para que Docker la copie sin avisar.
    if (Test-Path (Join-Path $frontend "dist")) {
        Remove-Item (Join-Path $frontend "dist") -Recurse -Force -ErrorAction Stop
    }
    npx quasar build
    Verificar-Salida "Fallo 'quasar build'"
} finally {
    Pop-Location
}

if (-not (Test-Path $indice)) {
    Abortar "quasar build no genero dist\spa\index.html"
}
$fechaIndice = (Get-Item $indice).LastWriteTime
if ($fechaIndice -lt $inicio) {
    Abortar "dist\spa\index.html es de $fechaIndice, anterior al inicio del script."
}
Escribir-Ok "Frontend compilado ($fechaIndice)"

# ---------------------------------------------------------------------------
Escribir-Paso "4/6  Construyendo las imagenes Docker"
if ($Rapido) {
    Write-Host "    (modo rapido: con cache)"
    docker compose -f "$raiz\docker-compose.yml" build
} else {
    Write-Host "    (sin cache; usa -Rapido si tienes prisa)"
    docker compose -f "$raiz\docker-compose.yml" build --no-cache
}
Verificar-Salida "Fallo la construccion de imagenes. Mira el mensaje de arriba: si dice 'not found' sobre un .jar, el paso 2 no dejo el artefacto donde el Dockerfile lo busca."
Escribir-Ok "Imagenes construidas"

# ---------------------------------------------------------------------------
Escribir-Paso "5/6  Levantando los contenedores"
docker compose -f "$raiz\docker-compose.yml" up -d
Verificar-Salida "Fallo 'docker compose up'"

Write-Host ""
docker compose -f "$raiz\docker-compose.yml" ps

# ---------------------------------------------------------------------------
Escribir-Paso "6/6  Esperando a que los servicios respondan"
$comprobaciones = @(
    @{ Nombre = 'Frontend (nginx)';    Url = 'http://localhost/' },
    @{ Nombre = 'Gateway';             Url = 'http://localhost:8080/actuator/health' },
    @{ Nombre = 'Catalogo via gateway';Url = 'http://localhost:8080/api/servicios' },
    @{ Nombre = 'Barberos via gateway';Url = 'http://localhost:8080/api/barberos' },
    @{ Nombre = 'Citas via gateway';   Url = 'http://localhost:8080/api/citas' }
)

foreach ($c in $comprobaciones) {
    $ok = $false
    foreach ($intento in 1..30) {
        try {
            $r = Invoke-WebRequest -Uri $c.Url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { $ok = $true; break }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if ($ok) {
        Write-Host ("    [OK]    {0,-24} {1}" -f $c.Nombre, $c.Url) -ForegroundColor Green
    } else {
        Write-Host ("    [FALLA] {0,-24} {1}" -f $c.Nombre, $c.Url) -ForegroundColor Red
        Write-Host "            Revisa:  docker compose logs --tail 50" -ForegroundColor Yellow
    }
}

# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "Hash del index.html que sirve nginx (compara con el de tu navegador):" -ForegroundColor Cyan
$hashLocal = (Get-FileHash $indice -Algorithm SHA256).Hash.Substring(0, 16)
Write-Host "    local  dist\spa\index.html : $hashLocal"
try {
    $servido = (Invoke-WebRequest -Uri 'http://localhost/' -UseBasicParsing -TimeoutSec 5).Content
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($servido)
    $sha = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
    $hashServido = ([BitConverter]::ToString($sha) -replace '-', '').Substring(0, 16)
    Write-Host "    servido por nginx        : $hashServido"
    if ($hashLocal -ne $hashServido) {
        Write-Host "    (difieren por saltos de linea; compara el nombre del bundle JS, mas abajo)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "    no se pudo leer http://localhost/" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Bundle JS que referencia el HTML servido (debe cambiar en cada build con cambios):" -ForegroundColor Cyan
try {
    $html = (Invoke-WebRequest -Uri 'http://localhost/' -UseBasicParsing -TimeoutSec 5).Content
    [regex]::Matches($html, 'assets/[A-Za-z0-9._-]+\.js') | ForEach-Object { Write-Host "    $($_.Value)" }
} catch {
    Write-Host "    no disponible" -ForegroundColor Yellow
}

$duracion = (Get-Date) - $inicio
Write-Host ""
Write-Host ("Listo en {0:mm\:ss}. Abre http://localhost/ con Ctrl+Shift+R" -f $duracion) -ForegroundColor Green
Write-Host ""
