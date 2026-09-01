# Run Kai desktop app (debug). Uses Compose Desktop :run task (mainClass from build.gradle.kts).
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

$jdk21 = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if (Test-Path -LiteralPath $jdk21) {
    $env:JAVA_HOME = $jdk21
    $env:Path = "$jdk21\bin;$env:Path"
}

if (-not (Test-Path -LiteralPath ".\gradlew.bat")) {
    Write-Error "gradlew.bat not found. Run this script from the Kai project root."
    exit 1
}

& .\gradlew.bat -PdesktopOnly=true :composeApp:run @args
exit $LASTEXITCODE
