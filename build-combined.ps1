param(
    [Parameter(Mandatory = $true)]
    [string]$GrandExplosionJar,
    [string]$CrimsonJar = (Join-Path $PSScriptRoot 'modules/crimson-susanoo/dist/crimson_susanoo-0.1.1.jar'),
    [string]$IgnisArmorJar = (Join-Path $PSScriptRoot 'modules/ignis-armor-compat/dist/ignis_armor_compat-0.1.0.jar'),
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build/release/multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0.jar'),
    [string]$Python = 'python',
    [switch]$BuildCrimson,
    [switch]$BuildIgnisArmor,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$module = Join-Path $PSScriptRoot 'modules/crimson-susanoo'
if ($BuildCrimson) {
    Push-Location $module
    try {
        $arguments = @('--no-daemon', 'build')
        if ($Offline) { $arguments += '--offline' }
        & ./gradlew.bat @arguments
        if ($LASTEXITCODE -ne 0) { throw 'Crimson module build failed' }
    } finally { Pop-Location }
}
if ($BuildIgnisArmor) {
    Push-Location (Join-Path $PSScriptRoot 'modules/ignis-armor-compat')
    try {
        $arguments = @('--no-daemon', 'build')
        if ($Offline) { $arguments += '--offline' }
        & ./gradlew.bat @arguments
        if ($LASTEXITCODE -ne 0) { throw 'Ignis armor module build failed' }
    } finally { Pop-Location }
}
& (Join-Path $module 'scripts/verify-assets.ps1')
& (Join-Path $module 'scripts/verify-release.ps1') -JarPath $CrimsonJar
$assemblyArguments = @('--grand-explosion', $GrandExplosionJar, '--crimson', $CrimsonJar, '--output', $OutputJar)
if ($IgnisArmorJar) { $assemblyArguments += @('--ignis-armor', $IgnisArmorJar) }
& $Python (Join-Path $PSScriptRoot 'tools/assemble_combined.py') @assemblyArguments
if ($LASTEXITCODE -ne 0) { throw 'Combined JAR verification failed' }
Write-Output "Combined release: $OutputJar"
