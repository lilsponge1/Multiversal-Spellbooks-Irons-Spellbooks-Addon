param(
    [Parameter(Mandatory = $true)]
    [string]$PackZip
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression

$projectRoot = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Path $libs -Force | Out-Null

$names = @(
    'irons_spellbooks-1.20.1-3.16.3.jar',
    'L_Enders_Cataclysm-3.31.jar',
    'geckolib-forge-1.20.1-4.8.4.jar',
    'irons_lib-1.20.1-2.1.0.jar',
    'lionfishapi-3.0.jar',
    'player-animation-lib-forge-1.0.2-rc1+1.20.jar',
    'curios-forge-5.14.1+1.20.1.jar'
)

$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $PackZip).Path)
try {
    foreach ($name in $names) {
        $entry = $archive.GetEntry("minecraft/mods/$name")
        if ($null -eq $entry) { throw "Missing required pack mod: $name" }
        $destination = Join-Path $libs $name
        $source = $entry.Open()
        try {
            $target = [System.IO.File]::Create($destination)
            try { $source.CopyTo($target) } finally { $target.Dispose() }
        } finally { $source.Dispose() }
        Write-Host "Extracted $name"
    }
} finally {
    $archive.Dispose()
}
