param(
    [Parameter(Mandatory = $true)][string]$JdkBin,
    [Parameter(Mandatory = $true)][string]$ClasspathFile,
    [string]$Python = 'python',
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build/release/ki-school-1.0.0.jar')
)
$ErrorActionPreference = 'Stop'
& $Python (Join-Path $PSScriptRoot 'tools/build_ki_standalone.py') --jdk-bin $JdkBin --classpath-file $ClasspathFile --output $OutputJar
if ($LASTEXITCODE -ne 0) { throw 'Independent Ki School build failed' }
