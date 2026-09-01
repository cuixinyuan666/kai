# Build Kai Windows portable zip (green / no-installer version).
# Skips Android modules via -PdesktopOnly=true.
# Uses subst drive when the project path contains non-ASCII chars (jlink/jpackage break on Chinese paths).
$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot

$jdk21 = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if (Test-Path -LiteralPath $jdk21) {
    $env:JAVA_HOME = $jdk21
    $env:Path = "$jdk21\bin;$env:Path"
}

if (-not (Test-Path -LiteralPath (Join-Path $projectRoot "gradlew.bat"))) {
    Write-Error "gradlew.bat not found. Run this script from the Kai project root."
    exit 1
}

$versionLine = Select-String -Path (Join-Path $projectRoot "gradle\libs.versions.toml") -Pattern '^appVersion = "(.+)"' | Select-Object -First 1
if (-not $versionLine) {
    Write-Error "Could not read appVersion from gradle\libs.versions.toml"
    exit 1
}
$version = $versionLine.Matches[0].Groups[1].Value

function Test-AsciiPath([string]$path) {
    foreach ($ch in $path.ToCharArray()) {
        if ([int]$ch -gt 127) { return $false }
    }
    return $true
}

$buildRoot = $projectRoot
$substDrive = $null
if (-not (Test-AsciiPath $projectRoot)) {
    $substDrive = "K:"
    if (Test-Path -LiteralPath "${substDrive}\") {
        subst $substDrive /d 2>$null
    }
    subst $substDrive $projectRoot | Out-Null
    $buildRoot = "${substDrive}\"
    Write-Host "Non-ASCII project path detected; building via $substDrive -> $projectRoot" -ForegroundColor Yellow
}

try {
    Set-Location -LiteralPath $buildRoot

    Write-Host "Building Kai $version Windows portable (desktop only, no Android)..." -ForegroundColor Cyan
    & .\gradlew.bat -PdesktopOnly=true createReleaseDistributable @args
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $source = Join-Path $buildRoot "composeApp\build\compose\binaries\main-release\app\Kai"
    if (-not (Test-Path -LiteralPath $source)) {
        Write-Error "Build output not found: $source"
        exit 1
    }

    $dest = Join-Path $projectRoot "Kai-$version-windows.zip"
    if (Test-Path -LiteralPath $dest) {
        Remove-Item -LiteralPath $dest -Force
    }
    Compress-Archive -Path "$source\*" -DestinationPath $dest -Force

    $exePath = Join-Path $source "Kai.exe"
    Write-Host ""
    Write-Host "Done." -ForegroundColor Green
    Write-Host "  Zip:     $dest"
    Write-Host "  Run:     $exePath"
    Write-Host "  (Unzip anywhere and run Kai.exe — no install required.)"
}
finally {
    if ($substDrive) {
        subst $substDrive /d 2>$null
    }
    Set-Location -LiteralPath $projectRoot
}
