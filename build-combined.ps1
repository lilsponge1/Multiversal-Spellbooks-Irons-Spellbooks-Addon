param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Modules')]
    [string]$GrandExplosionJar,
    [Parameter(Mandatory = $true, ParameterSetName = 'CombinedBase')]
    [string]$CombinedBaseJar,
    [string]$CrimsonJar = (Join-Path $PSScriptRoot 'modules/crimson-susanoo/dist/crimson_susanoo-0.1.1.jar'),
    [string]$IgnisArmorJar = (Join-Path $PSScriptRoot 'modules/ignis-armor-compat/dist/ignis_armor_compat-0.1.0.jar'),
    [string]$OmegaJar = (Join-Path $PSScriptRoot 'modules/omega-rush/build/omega-rush-0.2.2.jar'),
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build/release/multiversal-spellbooks-0.3.3-crimson-0.1.1-ignis-0.1.0-omega-0.2.2.jar'),
    [string]$Python = 'python',
    [switch]$BuildCrimson,
    [switch]$BuildIgnisArmor,
    [switch]$BuildOmega,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
if ($CombinedBaseJar -and ($BuildCrimson -or $BuildIgnisArmor -or -not $OmegaJar)) {
    throw 'CombinedBaseJar requires OmegaJar and preserves its existing Crimson/Ignis payloads; use the Modules parameter set to rebuild them.'
}
if ($BuildOmega) {
    & (Join-Path $PSScriptRoot 'modules/omega-rush/build.ps1') -OutputJar $OmegaJar
    if ($LASTEXITCODE -ne 0) { throw 'Omega Rush module build failed' }
}
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
if ($CombinedBaseJar) {
    $assemblyArguments = @('--combined-base', $CombinedBaseJar, '--output', $OutputJar)
} else {
    & (Join-Path $module 'scripts/verify-assets.ps1')
    & (Join-Path $module 'scripts/verify-release.ps1') -JarPath $CrimsonJar
    $assemblyArguments = @('--grand-explosion', $GrandExplosionJar, '--crimson', $CrimsonJar, '--output', $OutputJar)
    if ($IgnisArmorJar) { $assemblyArguments += @('--ignis-armor', $IgnisArmorJar) }
}
if ($OmegaJar) { $assemblyArguments += @('--omega', $OmegaJar) }
& $Python (Join-Path $PSScriptRoot 'tools/assemble_combined.py') @assemblyArguments
if ($LASTEXITCODE -ne 0) { throw 'Combined JAR verification failed' }
Write-Output "Combined release: $OutputJar"
