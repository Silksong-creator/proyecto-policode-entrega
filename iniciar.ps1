$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

$script:PythonCommand = $null
$script:UsePyLauncher = $false

function Find-Python {
    $pyLauncher = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($pyLauncher) {
        $versionOutput = & $pyLauncher.Source -3 --version 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0 -and $versionOutput -match 'Python\s+(\d+)\.(\d+)') {
            if ([int]$Matches[1] -gt 3 -or ([int]$Matches[1] -eq 3 -and [int]$Matches[2] -ge 10)) {
                $script:PythonCommand = $pyLauncher.Source
                $script:UsePyLauncher = $true
                return $true
            }
        }
    }

    $pythonCommand = Get-Command python.exe -ErrorAction SilentlyContinue
    if ($pythonCommand -and $pythonCommand.Source -notmatch '\\WindowsApps\\python\.exe$') {
        $versionOutput = & $pythonCommand.Source --version 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0 -and $versionOutput -match 'Python\s+(\d+)\.(\d+)') {
            if ([int]$Matches[1] -gt 3 -or ([int]$Matches[1] -eq 3 -and [int]$Matches[2] -ge 10)) {
                $script:PythonCommand = $pythonCommand.Source
                $script:UsePyLauncher = $false
                return $true
            }
        }
    }
    return $false
}

function Invoke-Python {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    if ($script:UsePyLauncher) {
        & $script:PythonCommand -3 @Arguments
    } else {
        & $script:PythonCommand @Arguments
    }
    if ($LASTEXITCODE -ne 0) { throw "Python terminó con código $LASTEXITCODE." }
}

function Has-Jdk {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    $runtime = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $compiler -or -not $runtime) { return $false }
    $versionOutput = & $compiler.Source -version 2>&1 | Out-String
    return ($LASTEXITCODE -eq 0 -and $versionOutput -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 17)
}

function Has-Node {
    $node = Get-Command node.exe -ErrorAction SilentlyContinue
    $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $node -or -not $npm) { return $false }
    $versionOutput = & $node.Source --version 2>&1 | Out-String
    return ($LASTEXITCODE -eq 0 -and $versionOutput -match '^v(\d+)' -and [int]$Matches[1] -ge 18)
}

function Install-MissingRuntimes {
    $missing = @()
    if (-not (Find-Python)) { $missing += @{ Name = "Python 3.12"; Id = "Python.Python.3.12" } }
    if (-not (Has-Jdk)) { $missing += @{ Name = "Eclipse Temurin JDK 17"; Id = "EclipseAdoptium.Temurin.17.JDK" } }
    if (-not (Has-Node)) { $missing += @{ Name = "Node.js LTS"; Id = "OpenJS.NodeJS.LTS" } }

    if ($missing.Count -eq 0) {
        Write-Host "Python 3.10+, JDK 17+ y Node.js 18+ están disponibles." -ForegroundColor Green
        return $false
    }

    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if (-not $winget) {
        Write-Host "No se encontró winget. Instala las herramientas faltantes desde sus sitios oficiales:" -ForegroundColor Yellow
        Write-Host "Python: https://www.python.org/downloads/windows/"
        Write-Host "JDK:    https://adoptium.net/temurin/releases/"
        Write-Host "Node:   https://nodejs.org/en/download"
        $missingNames = $missing | ForEach-Object { $_.Name }
        throw "Faltan herramientas requeridas: $($missingNames -join ', ')."
    }

    foreach ($item in $missing) {
        Write-Host "Instalando $($item.Name) con winget..." -ForegroundColor Cyan
        & $winget.Source install --exact --id $item.Id --source winget --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -ne 0) { throw "winget no pudo instalar $($item.Name). Código: $LASTEXITCODE" }
    }

    Write-Host "Se instalaron las herramientas faltantes. Cierra esta ventana, abre otra y vuelve a ejecutar iniciar.bat." -ForegroundColor Yellow
    return $true
}

function Prepare-Project {
    Write-Host "Preparando dependencias del proyecto..." -ForegroundColor Cyan
    if ($script:UsePyLauncher) {
        & $script:PythonCommand -3 -m pip --version 2>$null | Out-Null
    } else {
        & $script:PythonCommand -m pip --version 2>$null | Out-Null
    }
    if ($LASTEXITCODE -ne 0) {
        Invoke-Python -Arguments @("-m", "ensurepip", "--upgrade")
    }
    Invoke-Python -Arguments @("-m", "pip", "install", "--disable-pip-version-check", "-r", (Join-Path $ProjectRoot "python/requirements.txt"))

    $package = Get-Content (Join-Path $ProjectRoot "javascript/package.json") -Raw | ConvertFrom-Json
    $hasNpmDependencies = ($package.dependencies -and $package.dependencies.PSObject.Properties.Count -gt 0) -or
                          ($package.devDependencies -and $package.devDependencies.PSObject.Properties.Count -gt 0)
    if ($hasNpmDependencies) {
        $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
        & $npm install --prefix (Join-Path $ProjectRoot "javascript") --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw "npm install terminó con código $LASTEXITCODE." }
    } else {
        Write-Host "JavaScript no declara paquetes externos; se usa Node.js y su biblioteca estándar." -ForegroundColor Green
    }

    $buildDir = Join-Path $ProjectRoot "java/build"
    New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
    $sources = Get-ChildItem (Join-Path $ProjectRoot "java/src") -Filter *.java | ForEach-Object FullName
    & (Get-Command javac.exe -ErrorAction Stop).Source -encoding UTF-8 -d $buildDir @sources
    if ($LASTEXITCODE -ne 0) { throw "La compilación Java terminó con código $LASTEXITCODE." }
    Write-Host "Dependencias listas y Java compilado." -ForegroundColor Green
}

function Start-PythonGui {
    Invoke-Python -Arguments @((Join-Path $ProjectRoot "python/src/gui.py"))
}

function Start-JavaGui {
    $buildDir = Join-Path $ProjectRoot "java/build"
    & (Get-Command java.exe -ErrorAction Stop).Source -cp $buildDir InventarioGUI
}

function Start-WebGui {
    $url = "http://localhost:8080/javascript/gui/"
    Write-Host "Abriendo $url. Deja esta ventana abierta; Ctrl+C detiene el servidor." -ForegroundColor Green
    Start-Process $url
    Invoke-Python -Arguments @("-m", "http.server", "8080", "--directory", $ProjectRoot)
}

function Run-Tests {
    Write-Host "Pruebas de Python..." -ForegroundColor Cyan
    Invoke-Python -Arguments @("-m", "unittest", "discover", "-s", (Join-Path $ProjectRoot "python/tests"), "-v")

    Write-Host "Pruebas de Java..." -ForegroundColor Cyan
    $buildDir = Join-Path $ProjectRoot "java/build"
    $testSource = Join-Path $ProjectRoot "java/tests/InventarioTest.java"
    & (Get-Command javac.exe -ErrorAction Stop).Source -encoding UTF-8 -cp $buildDir -d $buildDir $testSource
    if ($LASTEXITCODE -ne 0) { throw "No se pudieron compilar las pruebas Java." }
    & (Get-Command java.exe -ErrorAction Stop).Source -cp $buildDir InventarioTest
    if ($LASTEXITCODE -ne 0) { throw "Las pruebas Java fallaron." }

    Write-Host "Pruebas de JavaScript..." -ForegroundColor Cyan
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    & $npm test --prefix (Join-Path $ProjectRoot "javascript")
    if ($LASTEXITCODE -ne 0) { throw "Las pruebas JavaScript fallaron." }
    Write-Host "Todas las suites terminaron correctamente." -ForegroundColor Green
}

try {
    if (Install-MissingRuntimes) { exit 0 }
    Prepare-Project

    $running = $true
    while ($running) {
        Write-Host ""
        Write-Host "=== Inventario Policode | Equipo 4 ===" -ForegroundColor Cyan
        Write-Host "1. Abrir interfaz Python / Tkinter"
        Write-Host "2. Abrir interfaz Java / Swing"
        Write-Host "3. Abrir interfaz JavaScript / Web"
        Write-Host "4. Ejecutar pruebas de las tres versiones"
        Write-Host "5. Preparar dependencias y recompilar Java"
        Write-Host "0. Salir"
        $choice = Read-Host "Elige una opción"
        switch ($choice) {
            "1" { Start-PythonGui }
            "2" { Start-JavaGui }
            "3" { Start-WebGui }
            "4" { Run-Tests }
            "5" { Prepare-Project }
            "0" { $running = $false }
            default { Write-Host "Opción no válida." -ForegroundColor Yellow }
        }
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
