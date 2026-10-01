param(
    [Parameter(Mandatory = $true)]
    [string]$GrandExplosionJar,
    [string]$CrimsonJar = (Join-Path $PSScriptRoot 'modules/crimson-susanoo/dist/crimson_susanoo-0.1.0.jar'),
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build/release/multiversal-spellbooks-0.3.2-crimson-0.1.0.jar'),
    [string]$Python = 'python',
    [switch]$BuildCrimson,
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
& (Join-Path $module 'scripts/verify-assets.ps1')
& (Join-Path $module 'scripts/verify-release.ps1') -JarPath $CrimsonJar
& $Python (Join-Path $PSScriptRoot 'tools/assemble_combined.py') --grand-explosion $GrandExplosionJar --crimson $CrimsonJar --output $OutputJar
if ($LASTEXITCODE -ne 0) { throw 'Combined JAR verification failed' }
Write-Output "Combined release: $OutputJar"
