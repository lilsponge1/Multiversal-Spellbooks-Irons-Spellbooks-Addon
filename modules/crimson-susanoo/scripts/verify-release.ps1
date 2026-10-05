param([string]$JarPath)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $root 'dist/crimson_susanoo-0.1.1.jar'
}
$jar = (Resolve-Path -LiteralPath $JarPath -ErrorAction Stop).Path
$assets = Join-Path $root 'src/main/resources/assets/crimson_susanoo'
$javaRoot = Join-Path $root 'src/main/java'
$javaSources = @(Get-ChildItem -LiteralPath $javaRoot -Recurse -File -Filter '*.java')
$entries = @(
    'geo/guardian.geo.json',
    'animations/guardian.animation.json',
    'textures/entity/guardian.png',
    'textures/entity/guardian_fire.png',
    'textures/entity/guardian_glowmask.png',
    'textures/gui/spell_icons/crimson_susanoo.png',
    'lang/en_us.json'
)

$archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
$hash = [System.Security.Cryptography.SHA256]::Create()
try {
    $requiredEntries = @('META-INF/mods.toml')
    foreach ($source in $javaSources) {
        $relative = $source.FullName.Substring($javaRoot.Length).TrimStart('\', '/')
        $requiredEntries += ($relative.Replace('\', '/') -replace '\.java$', '.class')
    }
    foreach ($entryName in $requiredEntries) {
        if ($null -eq $archive.GetEntry($entryName)) {
            throw "Release JAR is missing $entryName"
        }
    }
    foreach ($relative in $entries) {
        $source = Join-Path $assets ($relative.Replace('/', [System.IO.Path]::DirectorySeparatorChar))
        $entryName = "assets/crimson_susanoo/$relative"
        $entry = $archive.GetEntry($entryName)
        if ($null -eq $entry) { throw "Release JAR is missing $entryName" }
        if ($relative -in @('geo/guardian.geo.json', 'animations/guardian.animation.json')) {
            $headerStream = $entry.Open()
            try {
                $firstByte = $headerStream.ReadByte()
                if ($firstByte -ne [int][char]'{' ) {
                    throw "GeckoLib JSON in release JAR must begin with { (UTF-8 without BOM): $entryName"
                }
            } finally {
                $headerStream.Dispose()
            }
        }
        $sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
        $stream = $entry.Open()
        try {
            $entryHash = [BitConverter]::ToString($hash.ComputeHash($stream)).Replace('-', '')
        } finally {
            $stream.Dispose()
        }
        if ($sourceHash -ne $entryHash) {
            throw "Release JAR contains stale $entryName; rebuild before distribution"
        }
    }

    $forbiddenPrefixes = @(
        'io/redspace/ironsspellbooks/',
        'com/github/L_Ender/cataclysm/',
        'software/bernie/geckolib/',
        'com/eliotlash/',
        'net/minecraft/',
        'net/minecraftforge/'
    )
    foreach ($entry in $archive.Entries) {
        if (!$entry.FullName.EndsWith('.class')) { continue }
        foreach ($prefix in $forbiddenPrefixes) {
            if ($entry.FullName.StartsWith($prefix, [System.StringComparison]::Ordinal)) {
                throw "Release JAR bundles a dependency class: $($entry.FullName)"
            }
        }
    }
} finally {
    $hash.Dispose()
    $archive.Dispose()
}

$latestJavaSource = $javaSources | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
if ($null -ne $latestJavaSource -and (Get-Item -LiteralPath $jar).LastWriteTimeUtc -lt $latestJavaSource.LastWriteTimeUtc) {
    throw "Release JAR predates Java source change in $($latestJavaSource.Name); rebuild before distribution"
}
$jarHash = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
Write-Output "Release JAR matches source assets and excludes dependency classes: $jarHash"
