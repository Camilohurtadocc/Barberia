<#
.SYNOPSIS
    Comprueba que lo publicado en GitHub Pages carga de verdad.

.DESCRIPTION
    Descarga el index.html real que sirve GitHub Pages, extrae de el todas las
    referencias a assets y pide cada una para ver que codigo HTTP devuelve.

    Es la prueba que faltaba: mirar la consola del navegador dice QUE fichero
    falta, pero no si el problema es la ruta del HTML o la que el JS lleva
    horneada dentro. Esto comprueba las dos.
#>
[CmdletBinding()]
param(
    [string]$Url = 'https://camilohurtadocc.github.io/Barberia/'
)

$ErrorActionPreference = 'Stop'
if (-not $Url.EndsWith('/')) { $Url += '/' }

# Windows PowerShell 5.1 no tiene -SkipHttpErrorCheck: un 404 llega como
# excepcion, y hay que sacar el codigo de la respuesta que trae dentro.
function Estado($u) {
    try {
        $r = Invoke-WebRequest -Uri $u -Method Head -UseBasicParsing -TimeoutSec 20
        return [int]$r.StatusCode
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}

function Descargar($u) {
    return (Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 20).Content
}

Write-Host "`n==> index.html en $Url" -ForegroundColor Cyan
$codigo = Estado $Url
if ($codigo -ne 200) {
    Write-Host "    HTTP $codigo - la pagina raiz no responde." -ForegroundColor Red
    Write-Host '    Revisa Settings > Pages: rama gh-pages, carpeta / (root).' -ForegroundColor Yellow
    exit 1
}
Write-Host '    HTTP 200' -ForegroundColor Green

$html = Descargar $Url

# Rutas relativas en el HTML son una senal de alarma: significan que el
# index.html no salio del build o que se edito a mano.
if ($html -match '(src|href)="assets/') {
    Write-Host "`n    AVISO: el index.html usa rutas RELATIVAS (assets/...)." -ForegroundColor Yellow
    Write-Host '    Deberia usar /Barberia/assets/... - parece un index.html editado a mano.' -ForegroundColor Yellow
}

$refs = [regex]::Matches($html, '(?:src|href)="([^"]*assets/[^"]+)"') |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique

Write-Host "`n==> Assets referenciados por el HTML ($($refs.Count))" -ForegroundColor Cyan
$fallos = 0
foreach ($ref in $refs) {
    $abs = if ($ref -match '^https?://') { $ref } else { ([uri]::new([uri]$Url, $ref)).AbsoluteUri }
    $c = Estado $abs
    if ($c -eq 200) {
        Write-Host "    200  $ref" -ForegroundColor Green
    } else {
        Write-Host "    $c  $ref" -ForegroundColor Red
        $fallos++
    }
}

# La parte que de verdad rompia: los chunks que pide el JS, no el HTML.
$entry = $refs | Where-Object { $_ -match 'index-.*\.js$' } | Select-Object -First 1
if ($entry) {
    $entryAbs = ([uri]::new([uri]$Url, $entry)).AbsoluteUri
    Write-Host "`n==> Chunks dinamicos que pide el bundle" -ForegroundColor Cyan
    $js = Descargar $entryAbs

    $prefijo = [regex]::Match($js, '`(/[^`"]*?/)`\s*\+\s*\w\}')
    if ($prefijo.Success) {
        Write-Host "    publicPath horneado en el bundle: $($prefijo.Groups[1].Value)" -ForegroundColor Green
    } elseif ($js -match 'new URL\(\w+,\s*import\.meta\.url\)') {
        Write-Host '    El bundle resuelve chunks contra import.meta.url (publicPath vacio).' -ForegroundColor Red
        Write-Host '    Esto genera rutas /Barberia/assets/assets/... y da 404.' -ForegroundColor Red
        $fallos++
    }

    $deps = [regex]::Matches($js, '"(assets/[^"]+\.(?:js|css))"') |
        ForEach-Object { $_.Groups[1].Value } |
        Sort-Object -Unique |
        Select-Object -First 8

    foreach ($dep in $deps) {
        $abs = "$Url$dep"
        $c = Estado $abs
        if ($c -eq 200) {
            Write-Host "    200  $dep" -ForegroundColor Green
        } else {
            Write-Host "    $c  $dep" -ForegroundColor Red
            $fallos++
        }
    }
}

Write-Host ''
if ($fallos -eq 0) {
    Write-Host 'Todo carga correctamente.' -ForegroundColor Green
} else {
    Write-Host "$fallos recurso(s) fallando. Reconstruye y vuelve a publicar." -ForegroundColor Red
    exit 1
}
