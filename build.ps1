param(
    [string]$JdkBin = (Join-Path $PSScriptRoot '..\analysis\build\temurin17-jdk\jdk-17.0.20.1+1\bin'),
    [string]$LibraryDir = (Join-Path $PSScriptRoot '..\analysis\runtime-smoke\vs-freeze-diag\libraries'),
    [string]$ModDir = (Join-Path $PSScriptRoot '..\analysis\magic-compat\inputs'),
    [string]$MinecraftJar = (Join-Path $env:APPDATA 'PrismLauncher\libraries\net\minecraftforge\forge\1.20.1-47.4.10\forge-1.20.1-47.4.10-client.jar'),
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build\grand-explosion-0.3.0.jar')
)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'generate-art.ps1')
& (Join-Path $PSScriptRoot 'tools\generate_staff_model.ps1')
& (Join-Path $PSScriptRoot 'tools\generate_armor_art.ps1')
$classes = Join-Path $PSScriptRoot 'build\classes'
$out = $OutputJar
$argsPath = Join-Path $PSScriptRoot 'build\javac.args'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$jars = @(
    Get-Item -LiteralPath $MinecraftJar
    Get-Item -LiteralPath (Join-Path $PSScriptRoot 'test-server\libraries\net\minecraftforge\forge\1.20.1-47.4.10\forge-1.20.1-47.4.10-server.jar')
    # Runtime patch JARs omit unchanged vanilla classes. Resolve those from the SRG cache last.
    Get-Item -LiteralPath (Join-Path $PSScriptRoot '..\analysis\build\.gradle-user-home\caches\fabric-loom\1.20.1\forge\1.20.1-47.4.2\minecraft-merged-srg-patched.jar')
    Get-ChildItem -LiteralPath $LibraryDir -Filter '*.jar' -Recurse -File | Where-Object { $_.Name -in @(
        'forge-1.20.1-47.4.10-universal.jar',
        'eventbus-6.0.5.jar',
        'fmlcore-1.20.1-47.4.10.jar',
        'fmlloader-1.20.1-47.4.10.jar',
        'javafmllanguage-1.20.1-47.4.10.jar',
        'modlauncher-10.0.9.jar',
        'mergetool-1.1.5-api.jar',
        'core-3.6.4.jar',
        'toml-3.6.4.jar',
        'brigadier-1.1.8.jar',
        'gson-2.10.1.jar',
        'guava-31.1-jre.jar',
        'fastutil-8.5.9.jar',
        'joml-1.10.5.jar',
        'log4j-api-2.19.0.jar',
        'netty-buffer-4.1.82.Final.jar',
        'netty-common-4.1.82.Final.jar',
        'mixin-0.8.5.jar',
        'authlib-4.0.43.jar'
    ) }
    Get-ChildItem -LiteralPath $ModDir -Filter 'irons_spellbooks-*.jar' -File
    Get-ChildItem -LiteralPath $ModDir -Filter 'irons_lib-*.jar' -File
    Get-ChildItem -LiteralPath $ModDir -Filter 'geckolib-forge-*.jar' -File
    Get-ChildItem -LiteralPath $ModDir -Filter 'player-animation-lib-forge-*.jar' -File
    Get-ChildItem -LiteralPath $ModDir -Filter 'Ballistix-1.20.1-1.1.1.jar' -File
    Get-ChildItem -LiteralPath $ModDir -Filter 'traveloptics-6.3.0-1.20.1.jar' -File
)
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Filter '*.java' -Recurse -File)
$cp = (($jars.FullName | ForEach-Object { $_.Replace('\', '/') }) -join ';')
$lines = @('-proc:none', '-encoding', 'UTF-8', '-cp', ('"' + $cp + '"'), '-d', ('"' + $classes.Replace('\', '/') + '"'))
$lines += $sources.FullName | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines($argsPath, $lines)
& (Join-Path $JdkBin 'javac.exe') ('@' + $argsPath)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$manifest = Join-Path $PSScriptRoot 'build\release-manifest.mf'
[System.IO.File]::WriteAllText($manifest, "Manifest-Version: 1.0`nMixinConfigs: irons_ultimate_explosion.mixins.json`n`n")
& (Join-Path $JdkBin 'jar.exe') --create --file $out --manifest $manifest -C $classes . -C (Join-Path $PSScriptRoot 'src\main\resources') .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output $out
