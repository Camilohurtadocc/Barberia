<#
.SYNOPSIS
    Compila, empaqueta y despliega la barberia entera en el Kubernetes de Docker Desktop.

.DESCRIPTION
    Hace el ciclo completo de una tirada:

      1. Limpia los builds anteriores
      2. Compila los 5 JAR (gradlew clean bootJar) y el bundle del frontend
      3. Construye las 6 imagenes Docker
      4. Despliega con Helm
      5. Espera a que los pods esten listos y pasa unas pruebas de humo
      6. Deja el gateway expuesto en localhost

    Los Dockerfile NO compilan nada: solo copian el artefacto ya construido. Por
    eso el orden importa y no se puede saltar el paso de Gradle.

.PARAMETER SoloDesplegar
    Salta la compilacion y usa los artefactos e imagenes que ya existan. Util
    para reintentar un despliegue sin esperar otra vez a Gradle.

.PARAMETER SinExponer
    Termina sin abrir el port-forward. Para usar el script en cadena.

.PARAMETER Puerto
    Puerto local del port-forward final. Por defecto 8080.

.EXAMPLE
    .\desplegar.ps1
    .\desplegar.ps1 -SoloDesplegar
    .\desplegar.ps1 -Puerto 9090
#>

[CmdletBinding()]
param(
    [switch]$SoloDesplegar,
    [switch]$SinExponer,
    [int]$Puerto = 8080
)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$release = 'barberia'
$namespace = 'barberia'

# ---------------------------------------------------------------------------
# Utilidades de salida
# ---------------------------------------------------------------------------
$script:paso = 0
function Titulo($texto) {
    $script:paso++
    Write-Host ""
    Write-Host ("=" * 72) -ForegroundColor DarkCyan
    Write-Host ("  PASO $script:paso - $texto") -ForegroundColor Cyan
    Write-Host ("=" * 72) -ForegroundColor DarkCyan
}
function Info($texto)  { Write-Host "  $texto" -ForegroundColor Gray }
function Bien($texto)  { Write-Host "  [OK] $texto" -ForegroundColor Green }
function Aviso($texto) { Write-Host "  [!]  $texto" -ForegroundColor Yellow }
function Morir($texto) { Write-Host ""; Write-Host "  [X] $texto" -ForegroundColor Red; Write-Host ""; exit 1 }

# Los ejecutables nativos no lanzan excepcion al fallar: hay que mirar el codigo.
function ComprobarSalida($queHacia) {
    if ($LASTEXITCODE -ne 0) { Morir "Fallo al $queHacia (codigo $LASTEXITCODE)" }
}

# Ejecuta un comando nativo tragandose toda su salida, incluida la de error.
#
# Existe por el mismo motivo que la redireccion de VersionMayorDeJdk: en Windows
# PowerShell 5.1, capturar el stderr de un .exe convierte cada linea en un
# ErrorRecord y con $ErrorActionPreference = 'Stop' aborta el script aunque el
# programa haya terminado bien (docker escribe avisos por stderr de continuo).
# Aqui interesa solo el codigo de salida, que queda en $LASTEXITCODE.
function EjecutarSilencioso {
    param([scriptblock]$Bloque)
    $preferenciaPrevia = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Bloque 2>&1 | Out-Null
    } finally {
        $ErrorActionPreference = $preferenciaPrevia
    }
}

# ---------------------------------------------------------------------------
# Componentes. El orden de la lista es el orden de construccion.
#
# 'carpeta' es el repo, 'jar' el nombre que produce bootJar (viene de
# rootProject.name) e 'imagen' el nombre que espera el values.yaml del chart.
# ---------------------------------------------------------------------------
$servicios = @(
    @{ carpeta = 'Barberia-gateway';   jar = 'servicio-gateway';   imagen = 'barberia-gateway' }
    @{ carpeta = 'Barberia-servicios'; jar = 'servicio-catalogo';  imagen = 'barberia-servicio-catalogo' }
    @{ carpeta = 'Barberia-barberos';  jar = 'servicio-barberos';  imagen = 'barberia-servicio-barberos' }
    @{ carpeta = 'Barberia-citas';     jar = 'servicio-citas';     imagen = 'barberia-servicio-citas' }
    @{ carpeta = 'Barberia-auth';      jar = 'servicio-auth';      imagen = 'barberia-servicio-auth' }
)
$despliegues = @('postgres', 'servicio-catalogo', 'servicio-barberos', 'servicio-citas',
                 'servicio-auth', 'servicio-gateway', 'barberia-frontend')

# ===========================================================================
Titulo "Comprobando el entorno"
# ===========================================================================

# --- Helm --------------------------------------------------------------
# No suele estar en el PATH: el repo trae una copia en .\helm\helm.exe.
$helm = $null
if (Get-Command helm -ErrorAction SilentlyContinue) {
    $helm = 'helm'
} else {
    foreach ($ruta in @("$raiz\helm\helm.exe", "$raiz\helm\windows-amd64\helm.exe")) {
        if (Test-Path $ruta) { $helm = $ruta; break }
    }
}
if (-not $helm) { Morir "No encuentro helm, ni en el PATH ni en .\helm\helm.exe" }
Bien "helm: $helm"

# --- kubectl y cluster -------------------------------------------------
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) { Morir "kubectl no esta en el PATH" }
$contexto = (kubectl config current-context 2>$null)
if ($LASTEXITCODE -ne 0) { Morir "kubectl no puede hablar con ningun cluster. Arranca Docker Desktop y habilita Kubernetes." }
Bien "contexto de kubectl: $contexto"
if ($contexto -ne 'docker-desktop') {
    Aviso "El contexto no es 'docker-desktop'. Se desplegara igualmente sobre '$contexto'."
}

# --- Docker ------------------------------------------------------------
EjecutarSilencioso { docker info }
if ($LASTEXITCODE -ne 0) { Morir "El demonio de Docker no responde. Arranca Docker Desktop." }
Bien "demonio de Docker operativo"

# --- JDK 25 ------------------------------------------------------------
# El toolchain de Gradle exige 25 (main.gradle). En esta maquina JAVA_HOME
# apunta a un JDK 17, asi que si hace falta se busca el 25 por su cuenta y se
# le pasa a Gradle solo para este proceso.
function VersionMayorDeJdk($carpetaJdk) {
    $exe = Join-Path $carpetaJdk 'bin\java.exe'
    if (-not (Test-Path $exe)) { return 0 }

    # 'java -version' escribe la version en stderr, no en stdout. En Windows
    # PowerShell 5.1 redirigir el stderr de un ejecutable nativo con 2>&1
    # envuelve cada linea en un ErrorRecord; como este script corre con
    # $ErrorActionPreference = 'Stop', eso bastaba para abortarlo entero antes
    # de compilar nada. Se baja la preferencia solo dentro de esta funcion.
    $preferenciaPrevia = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $salida = (& $exe -version 2>&1 | Out-String)
    } catch {
        return 0
    } finally {
        $ErrorActionPreference = $preferenciaPrevia
    }

    # Formato: openjdk version "25.0.3" 2026-04-21 LTS
    if ($salida -match '"(\d+)') { return [int]$Matches[1] }
    return 0
}

if (-not $SoloDesplegar) {
    $jdk25 = $null
    if ($env:JAVA_HOME -and (VersionMayorDeJdk $env:JAVA_HOME) -ge 25) {
        $jdk25 = $env:JAVA_HOME
    } else {
        $candidatos = @()
        foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium",
                            "$env:ProgramFiles\Amazon Corretto", "$env:ProgramFiles\Microsoft")) {
            if (Test-Path $base) {
                $candidatos += Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
                               Where-Object { $_.Name -match '2[5-9]' } | Select-Object -ExpandProperty FullName
            }
        }
        foreach ($c in $candidatos) { if ((VersionMayorDeJdk $c) -ge 25) { $jdk25 = $c; break } }
    }
    if (-not $jdk25) {
        Morir "No encuentro un JDK 25+. El toolchain de Gradle lo exige (main.gradle). Instalalo o exporta JAVA_HOME apuntando a el."
    }
    $env:JAVA_HOME = $jdk25
    Bien "JDK para Gradle: $jdk25"

    if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { Morir "npm no esta en el PATH" }
    Bien "npm disponible"
}

# ---------------------------------------------------------------------------
# El tag de las imagenes: una marca de tiempo, distinta en cada ejecucion.
#
# Esto NO es cosmetico, es lo que hace que el despliegue surta efecto. El
# Kubernetes de Docker Desktop corre sobre containerd, que tiene su propio
# almacen de imagenes, separado del demonio Docker donde construimos. Docker
# Desktop copia la imagen a containerd cuando kubelet la pide, pero con un tag
# repetido (":latest") kubelet ve que ya la tiene guardada y no pide nada: el
# pod se queda ejecutando la imagen de la tirada anterior indefinidamente.
# Con un tag nuevo cada vez, kubelet no lo tiene en cache, lo pide, y recibe la
# imagen recien construida.
# ---------------------------------------------------------------------------
$tag = Get-Date -Format 'yyyyMMdd-HHmmss'
Info "Tag de esta tirada: $tag"

# ===========================================================================
if (-not $SoloDesplegar) {
    Titulo "Compilando los JAR (gradlew clean bootJar)"
    foreach ($s in $servicios) {
        $dir = Join-Path $raiz $s.carpeta
        if (-not (Test-Path $dir)) { Morir "No existe la carpeta $($s.carpeta)" }
        Info "$($s.carpeta) ..."
        Push-Location $dir
        try {
            & .\gradlew.bat clean bootJar --console=plain -q
            ComprobarSalida "compilar $($s.carpeta)"
        } finally { Pop-Location }

        $jarPath = Join-Path $dir "applications\app-service\build\libs\$($s.jar).jar"
        if (-not (Test-Path $jarPath)) { Morir "$($s.carpeta) compilo pero no aparece $($s.jar).jar" }
        $mb = [Math]::Round((Get-Item $jarPath).Length / 1MB, 1)
        Bien "$($s.jar).jar ($mb MB)"
    }

    # =======================================================================
    Titulo "Compilando el frontend (npm run build)"
    $dirFront = Join-Path $raiz 'barberia-frontend'
    Push-Location $dirFront
    try {
        if (-not (Test-Path (Join-Path $dirFront 'node_modules'))) {
            Info "No hay node_modules, instalando dependencias ..."
            npm install --no-fund --no-audit
            ComprobarSalida "instalar las dependencias del frontend"
        }
        # Quasar reutiliza dist/spa: si queda basura de un build anterior con
        # otro hash, nginx acabaria sirviendo assets huerfanos.
        if (Test-Path 'dist\spa') { Remove-Item 'dist\spa' -Recurse -Force }
        npm run build
        ComprobarSalida "compilar el frontend"
    } finally { Pop-Location }
    if (-not (Test-Path (Join-Path $dirFront 'dist\spa\index.html'))) {
        Morir "El build del frontend no genero dist\spa\index.html"
    }
    Bien "dist\spa listo"

    # =======================================================================
    Titulo "Construyendo las imagenes Docker"
    foreach ($s in $servicios) {
        Info "$($s.imagen) ..."
        $contextoBuild = Join-Path $raiz $s.carpeta
        EjecutarSilencioso { docker build -q -t "$($s.imagen):$tag" -t "$($s.imagen):latest" $contextoBuild }
        ComprobarSalida "construir la imagen $($s.imagen)"
        Bien "$($s.imagen):$tag"
    }
    Info "barberia-frontend ..."
    EjecutarSilencioso { docker build -q -t "barberia-frontend:$tag" -t "barberia-frontend:latest" $dirFront }
    ComprobarSalida "construir la imagen del frontend"
    Bien "barberia-frontend:$tag"

} else {
    Titulo "Compilacion omitida (-SoloDesplegar)"
    # Sin compilar no hay imagenes con el tag nuevo, asi que se reetiquetan las
    # 'latest' que ya existan. Si falta alguna, se aborta antes de desplegar.
    $todas = @($servicios | ForEach-Object { $_.imagen }) + @('barberia-frontend')
    foreach ($img in $todas) {
        # 'docker images -q' no escribe nada por stderr: devuelve vacio si no
        # existe. Asi se evita la conversion de stderr en ErrorRecord.
        if (-not (docker images -q "${img}:latest")) {
            Morir "No existe la imagen ${img}:latest. Ejecuta el script sin -SoloDesplegar."
        }
        docker tag "${img}:latest" "${img}:$tag"
        ComprobarSalida "reetiquetar $img"
        Bien "${img}:$tag (reetiquetada)"
    }
}

# ===========================================================================
Titulo "Desplegando con Helm"
# ===========================================================================
$chart = Join-Path $raiz 'helm\barberia'
if (-not (Test-Path $chart)) { Morir "No encuentro el chart en $chart" }

& $helm upgrade --install $release $chart --namespace $namespace --create-namespace --set "imageTag=$tag"
ComprobarSalida "desplegar el chart"
Bien "release '$release' desplegada en el namespace '$namespace'"

# ===========================================================================
Titulo "Esperando a que los pods esten listos"
# ===========================================================================
foreach ($d in $despliegues) {
    Info "$d ..."
    # 600s y no 300: justo despues de compilar cinco JAR y seis imagenes la
    # maquina viene cargada, y el arranque de una JVM que ademas ejecuta el DDL
    # de Hibernate y data.sql se pasaba de los cinco minutos. El despliegue
    # acababa bien solo, pero el script ya lo habia dado por fallido.
    kubectl rollout status "deploy/$d" -n $namespace --timeout=600s
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Aviso "'$d' no llego a estar listo. Ultimos eventos y logs:"
        kubectl describe "deploy/$d" -n $namespace | Select-String -Pattern 'Events:' -Context 0,10
        kubectl logs "deploy/$d" -n $namespace --tail=30
        Morir "El despliegue de '$d' no termino a tiempo"
    }
}
Write-Host ""
kubectl get pods -n $namespace

# ===========================================================================
Titulo "Pruebas de humo"
# ===========================================================================
#
# El gateway se publica como LoadBalancer y Docker Desktop lo saca en localhost,
# así que normalmente no hace falta ningún túnel. Se comprueba primero si ya
# responde; solo si no lo hace (porque alguien haya puesto serviceType a
# ClusterIP, o porque el balanceador tarde en asignarse) se abre un port-forward
# temporal en un puerto alto que no choque con nada.
$tipoServicio = (kubectl get svc servicio-gateway -n $namespace -o jsonpath='{.spec.type}' 2>$null)
$pf = $null
$puertoPrueba = $Puerto

function RespondeElGateway($puerto) {
    try {
        Invoke-WebRequest -Uri "http://localhost:$puerto/api/servicios" -UseBasicParsing -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        # Cualquier respuesta HTTP vale: significa que hay alguien al otro lado.
        # Solo un fallo de conexión (sin Response) indica que no hay ruta.
        return $null -ne $_.Exception.Response
    }
}

if ($tipoServicio -eq 'LoadBalancer') {
    Info "Servicio publicado como LoadBalancer; esperando a que responda en localhost:$Puerto ..."
    $listo = $false
    foreach ($intento in 1..20) {
        if (RespondeElGateway $Puerto) { $listo = $true; break }
        Start-Sleep -Seconds 3
    }
    if ($listo) {
        Bien "Accesible en http://localhost:$Puerto sin tuneles"
    } else {
        Aviso "El balanceador no responde aun; se usara un port-forward para las pruebas."
        $tipoServicio = 'ClusterIP'
    }
}

if ($tipoServicio -ne 'LoadBalancer') {
    $puertoPrueba = 18080
    $pf = Start-Process kubectl `
            -ArgumentList "port-forward","-n",$namespace,"svc/servicio-gateway","${puertoPrueba}:8080" `
            -NoNewWindow -PassThru
    Start-Sleep -Seconds 8
}

$fallos = 0
function Comprobar($descripcion, $url, $metodo, $cuerpo, $esperado) {
    $parametros = @{ Uri = $url; Method = $metodo; UseBasicParsing = $true; TimeoutSec = 20 }
    if ($cuerpo) { $parametros.Body = $cuerpo; $parametros.ContentType = 'application/json' }
    try {
        $r = Invoke-WebRequest @parametros
        $codigo = $r.StatusCode
    } catch {
        if ($_.Exception.Response) { $codigo = $_.Exception.Response.StatusCode.value__ } else { $codigo = 0 }
    }
    if ($codigo -eq $esperado) {
        Write-Host ("  [OK] {0,-44} {1}" -f $descripcion, $codigo) -ForegroundColor Green
    } else {
        Write-Host ("  [X]  {0,-44} {1} (esperaba {2})" -f $descripcion, $codigo, $esperado) -ForegroundColor Red
        $script:fallos++
    }
}

$base = "http://localhost:$puertoPrueba"
try {
    Comprobar "GET /  (landing servida por el gateway)" "$base/"                  'Get'  $null 200
    Comprobar "GET /api/servicios  (publico)"           "$base/api/servicios"     'Get'  $null 200
    Comprobar "GET /api/barberos   (publico)"           "$base/api/barberos"      'Get'  $null 200
    Comprobar "GET /api/citas      (sin token)"         "$base/api/citas"         'Get'  $null 401
    Comprobar "POST /auth/login"                        "$base/auth/login"        'Post' '{"username":"admin","password":"admin123"}' 200

    # El login tiene que devolver un token utilizable de verdad, no solo un 200.
    try {
        $login = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType 'application/json' `
                    -Body '{"username":"admin","password":"admin123"}' -TimeoutSec 20
        if ($login.token) {
            $cabeceras = @{ Authorization = "Bearer $($login.token)" }
            $r = Invoke-WebRequest -Uri "$base/api/citas" -Headers $cabeceras -UseBasicParsing -TimeoutSec 20
            if ($r.StatusCode -eq 200) {
                Write-Host ("  [OK] {0,-44} {1}" -f "GET /api/citas  (con token $($login.rol))", 200) -ForegroundColor Green
            } else {
                Write-Host ("  [X]  GET /api/citas con token -> $($r.StatusCode)") -ForegroundColor Red
                $fallos++
            }
        } else {
            Write-Host "  [X]  El login no devolvio token" -ForegroundColor Red
            $fallos++
        }
    } catch {
        Write-Host "  [X]  Fallo comprobando el token: $($_.Exception.Message)" -ForegroundColor Red
        $fallos++
    }
} finally {
    if ($pf -and -not $pf.HasExited) { Stop-Process -Id $pf.Id -Force -ErrorAction SilentlyContinue }
}

Write-Host ""
if ($fallos -gt 0) {
    Morir "$fallos prueba(s) de humo han fallado. El despliegue esta arriba pero no se comporta como deberia."
}
Bien "Todas las pruebas de humo han pasado"

# ===========================================================================
Titulo "Limpiando tags antiguos"
# ===========================================================================
# Cada tirada deja un tag con marca de tiempo. Sin esto se acumulan y cada
# imagen de estas ocupa unos 400 MB.
$imagenes = @($servicios | ForEach-Object { $_.imagen }) + @('barberia-frontend')
foreach ($img in $imagenes) {
    $viejos = docker images $img --format '{{.Tag}}' |
              Where-Object { $_ -match '^\d{8}-\d{6}$' -and $_ -ne $tag } |
              Sort-Object -Descending | Select-Object -Skip 2
    foreach ($t in $viejos) { EjecutarSilencioso { docker rmi "${img}:$t" } }
    if ($viejos) { Info "$img : $($viejos.Count) tag(s) antiguo(s) eliminado(s)" }
}

# ===========================================================================
Titulo "Listo"
# ===========================================================================
Write-Host ""
Write-Host "  La barberia esta desplegada en el namespace '$namespace'." -ForegroundColor Green
Write-Host ""
Write-Host "  Todo entra por el gateway, que sirve tanto la web como las APIs:" -ForegroundColor Gray
Write-Host "    http://localhost:$Puerto/                 landing" -ForegroundColor White
Write-Host "    http://localhost:$Puerto/api/servicios    catalogo (JSON)" -ForegroundColor White
Write-Host "    http://localhost:$Puerto/auth/login       login (POST)" -ForegroundColor White
Write-Host ""
Write-Host "  Usuario de prueba: admin / admin123" -ForegroundColor Gray
Write-Host ""

# Con LoadBalancer la dirección ya funciona por sí sola y no hay que dejar nada
# abierto: el sitio sigue en pie mientras el clúster esté encendido, incluso
# después de cerrar esta ventana.
if ($tipoServicio -eq 'LoadBalancer') {
    Write-Host "  El gateway esta publicado como LoadBalancer: la direccion de arriba" -ForegroundColor Gray
    Write-Host "  funciona tal cual, sin tuneles ni ventanas abiertas." -ForegroundColor Gray
    Write-Host ""
    exit 0
}

if ($SinExponer) {
    Write-Host "  Para exponerlo:" -ForegroundColor Gray
    Write-Host "    kubectl port-forward -n $namespace svc/servicio-gateway ${Puerto}:8080" -ForegroundColor White
    Write-Host ""
    exit 0
}

Write-Host "  Abriendo el port-forward en el puerto $Puerto (Ctrl+C para parar) ..." -ForegroundColor Cyan
Write-Host ""
kubectl port-forward -n $namespace svc/servicio-gateway "${Puerto}:8080"
