param(
    [ValidateRange(2, 16)]
    [int]$MemoryGb = 5,
    [switch]$Diagnostics,
    [switch]$TracePursuit,
    [switch]$BossEncounter
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$serverRoot = Join-Path $projectRoot 'run-pack'
$forgeArgs = 'libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt'
if (-not (Test-Path -LiteralPath (Join-Path $serverRoot $forgeArgs))) {
    throw "Pack test server is not installed at $serverRoot"
}

$portableJava = Join-Path $env:TEMP 'crimson-susanoo-tools/jdk-17.0.20.1+1/bin/java.exe'
$candidates = @()
if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin/java.exe') }
$candidates += $portableJava
foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate) {
        $version = (& $candidate -version 2>&1 | Select-Object -First 1).ToString()
        if ($version -match 'version "17\.') { $java = $candidate; break }
    }
}
if (-not $java) { throw 'Java 17 was not found. Set JAVA_HOME to a Java 17 installation.' }

$javaArgs = @("-Xmx${MemoryGb}G")
if ($BossEncounter) { $javaArgs += '-Dcrimson_susanoo.bossEncounter=true' }
if ($Diagnostics) { $javaArgs += '-Dcrimson_susanoo.diagnostics=true' }
if ($TracePursuit) { $javaArgs += '-Dcrimson_susanoo.tracePursuit=true' }
$javaArgs += '@user_jvm_args.txt', "@$forgeArgs", 'nogui'

Push-Location $serverRoot
try {
    Write-Host "Starting disposable pack server with Java 17 at 127.0.0.1:25566"
    & $java @javaArgs
} finally {
    Pop-Location
}
