<#
.SYNOPSIS
    Construye el frontend y lo publica en la rama gh-pages.

.DESCRIPTION
    Alternativa manual al workflow .github/workflows/deploy-frontend.yml, para
    cuando quieres publicar sin esperar a GitHub Actions.

    Usa un worktree de git en lugar de copiar ficheros a mano: asi la rama
    gh-pages queda con EXACTAMENTE el contenido de dist/spa y no se arrastran
    restos de despliegues anteriores (que es como acabaron 10.500 ficheros de
    codigo fuente dentro de gh-pages).

    IMPORTANTE: nunca edites a mano el index.html publicado. Las rutas de los
    chunks dinamicos van horneadas dentro del propio JS por build.publicPath;
    un index.html retocado a mano no las cambia y la pagina sale en blanco.

.EXAMPLE
    .\desplegar-frontend.ps1
    .\desplegar-frontend.ps1 -SoloConstruir
#>
[CmdletBinding()]
param(
    # Construye y valida, pero no publica nada.
    [switch]$SoloConstruir
)

$ErrorActionPreference = 'Stop'

$raiz     = $PSScriptRoot
$frontend = Join-Path $raiz 'barberia-frontend'
$dist     = Join-Path $frontend 'dist\spa'
$worktree = Join-Path $env:TEMP 'barberia-gh-pages'
$publicPath = '/Barberia/'

function Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Ok($texto)   { Write-Host "    OK  $texto" -ForegroundColor Green }

# ---------------------------------------------------------------- 1. Build
Paso 'Construyendo el frontend'
Push-Location $frontend
try {
    if (-not (Test-Path 'node_modules')) {
        Write-Host '    node_modules no existe, instalando dependencias...'
        npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci fallo' }
    }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'El build fallo' }
}
finally { Pop-Location }
Ok 'Build generado'

# ------------------------------------------------------------ 2. Validacion
# Si publicPath no quedo aplicado, mejor detenerse aqui que publicar una
# pagina en blanco y volver a depurar desde la consola del navegador.
Paso 'Validando que el build sirve para GitHub Pages'

$indexHtml = Join-Path $dist 'index.html'
if (-not (Test-Path $indexHtml)) { throw "No se genero $indexHtml" }

$html = Get-Content $indexHtml -Raw
if ($html -notmatch [regex]::Escape("$publicPath" + 'assets/')) {
    throw "index.html no apunta a ${publicPath}assets/ - revisa build.publicPath en quasar.config.js"
}
Ok "index.html apunta a ${publicPath}assets/"

# El JS es lo que de verdad importa: aqui es donde fallaba antes.
$bundle = Get-ChildItem (Join-Path $dist 'assets') -Filter 'index-*.js' | Select-Object -First 1
if ($null -eq $bundle) { throw 'No se encontro el bundle principal en dist/spa/assets' }
$js = Get-Content $bundle.FullName -Raw
if ($js -notmatch [regex]::Escape($publicPath)) {
    throw "El bundle no lleva $publicPath horneado - el build se hizo con otro publicPath"
}
Ok "El bundle resuelve sus chunks contra $publicPath"

if ($SoloConstruir) {
    Write-Host "`nListo (solo construir). Nada publicado." -ForegroundColor Yellow
    exit 0
}

# ----------------------------------------------------------- 3. Publicacion
Paso 'Publicando en la rama gh-pages'

if (Test-Path $worktree) {
    git -C $raiz worktree remove --force $worktree 2>$null
    if (Test-Path $worktree) { Remove-Item $worktree -Recurse -Force }
}

git -C $raiz fetch origin gh-pages 2>$null

# --detach evita dejar la rama local enganchada al worktree.
if (git -C $raiz ls-remote --exit-code --heads origin gh-pages 2>$null) {
    git -C $raiz worktree add --detach $worktree origin/gh-pages
} else {
    git -C $raiz worktree add --detach $worktree
    git -C $worktree checkout --orphan gh-pages
}
if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar el worktree de gh-pages' }

try {
    # Borrado total del contenido anterior (menos .git) y copia limpia.
    Get-ChildItem $worktree -Force |
        Where-Object { $_.Name -ne '.git' } |
        Remove-Item -Recurse -Force

    Copy-Item (Join-Path $dist '*') $worktree -Recurse -Force

    # Sin .nojekyll, GitHub Pages pasa la salida por Jekyll y descarta
    # cualquier fichero o carpeta que empiece por guion bajo.
    New-Item (Join-Path $worktree '.nojekyll') -ItemType File -Force | Out-Null

    git -C $worktree add --all
    if (git -C $worktree status --porcelain) {
        $sha = git -C $raiz rev-parse --short HEAD
        git -C $worktree commit -m "Deploy frontend desde $sha"
        git -C $worktree push origin HEAD:gh-pages
        if ($LASTEXITCODE -ne 0) { throw 'El push a gh-pages fallo' }
        Ok 'Publicado'
    } else {
        Ok 'Sin cambios respecto a lo ya publicado'
    }
}
finally {
    git -C $raiz worktree remove --force $worktree 2>$null
}

Write-Host "`nhttps://camilohurtadocc.github.io/Barberia/" -ForegroundColor Green
Write-Host 'GitHub Pages tarda ~1 min en refrescar. Recarga con Ctrl+F5.' -ForegroundColor DarkGray
