param([string]$JarPath)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$distRoot = (Resolve-Path -LiteralPath (Join-Path $projectRoot 'dist')).Path
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $distRoot 'crimson_susanoo-0.1.1.jar'
}
$sourceJar = (Resolve-Path -LiteralPath $JarPath).Path
& (Join-Path $PSScriptRoot 'verify-release.ps1') -JarPath $sourceJar
$jarHash = (Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash
$shortHash = $jarHash.Substring(0, 6).ToLowerInvariant()
$jarName = 'crimson_susanoo-0.1.1.jar'
$bundlePath = Join-Path $distRoot "crimson_susanoo-0.1.1-test-$shortHash.zip"
if (Test-Path -LiteralPath $bundlePath) { throw "Candidate package already exists: $bundlePath" }
$partialPath = "$bundlePath.partial"

$instructions = @"
Crimson Susanoo 0.1.1 - test candidate
Addon SHA256: $jarHash

Requires Minecraft 1.20.1, Java 17 and the matching Forge 47.4.10 modpack:
Iron's Spells 'n Spellbooks 3.16.3, Cataclysm 3.31, GeckoLib 4.8.4,
and their existing pack dependencies. This ZIP contains only our addon.

INSTALL FOR TESTING
1. Close Minecraft and stop the server before changing their mods folders.
2. Back up any older Crimson Susanoo JAR outside mods. Keep one addon copy.
3. Put $jarName in both the server's mods folder and every client's mods folder.
4. Compare each copy's SHA256 with SHA256SUMS.txt. All players and the server
   need the same candidate; the version number alone does not identify it.
5. Keep Solas and client rendering mods on clients. Start the server normally,
   without Crimson Susanoo diagnostics, bossEncounter or tracePursuit JVM flags.

The server creates its balance file at:
<level-name>/serverconfig/crimson_susanoo-server.toml
Default friendlyFire and blockGriefing are false. Upkeep is 12 mana/second,
maximum duration 90 seconds, and cooldown 300 seconds after the summon ends.

CRAFT AND USE
At Iron's Scroll Forge use 1 Legendary Ink, 1 Paper and 1 Blaze Rod.
Select Crimson Susanoo to craft its level-one Legendary Fire scroll.
/crimson_susanoo hunt enables pursuit; /crimson_susanoo guard returns to guarding;
/crimson_susanoo dismiss ends your summon. These commands do not require an op.

ACCEPTANCE STATUS
This is a test candidate. Complete the remaining continuous movement/visual
checks and two-player synchronization/performance check before main-server use.
Use the module README.md and root INTEGRATION.md for evidence and remaining checks.

ROLLBACK
Stop the server and close clients, then restore the previous addon and any
changed addon configuration on server and clients together. Restore a world
backup only if needed, since that discards progress made after the backup.
"@

function Add-CandidateText($Archive, [string]$Name, [string]$Text) {
    $entry = $Archive.CreateEntry($Name)
    $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
    try { $writer.Write($Text) } finally { $writer.Dispose() }
}

$outputStream = [IO.File]::Open($partialPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
    $archive = [IO.Compression.ZipArchive]::new($outputStream, [IO.Compression.ZipArchiveMode]::Create, $true)
    try {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $sourceJar, $jarName) | Out-Null
        Add-CandidateText $archive 'INSTALL.txt' $instructions
        Add-CandidateText $archive 'SHA256SUMS.txt' "$jarHash  $jarName`r`n"
    } finally { $archive.Dispose() }
} finally { $outputStream.Dispose() }

$archive = [IO.Compression.ZipFile]::OpenRead($partialPath)
$hasher = [Security.Cryptography.SHA256]::Create()
try {
    $expectedEntries = @($jarName, 'INSTALL.txt', 'SHA256SUMS.txt')
    if ($archive.Entries.Count -ne 3 -or
        (Compare-Object ($archive.Entries.FullName | Sort-Object) ($expectedEntries | Sort-Object))) {
        throw 'Candidate package has unexpected contents'
    }
    $jarStream = $archive.GetEntry($jarName).Open()
    try { $bundledHash = [BitConverter]::ToString($hasher.ComputeHash($jarStream)).Replace('-', '') }
    finally { $jarStream.Dispose() }
    if ($bundledHash -ne $jarHash) { throw 'Bundled addon hash mismatch' }
    $checksumReader = [IO.StreamReader]::new($archive.GetEntry('SHA256SUMS.txt').Open())
    try { $checksumText = $checksumReader.ReadToEnd() } finally { $checksumReader.Dispose() }
    if ($checksumText -ne "$jarHash  $jarName`r`n") { throw 'Bundled checksum mismatch' }
} finally { $archive.Dispose(); $hasher.Dispose() }

Move-Item -LiteralPath $partialPath -Destination $bundlePath
[pscustomobject]@{ Bundle = $bundlePath; JarSHA256 = $jarHash; Bytes = (Get-Item -LiteralPath $bundlePath).Length }
