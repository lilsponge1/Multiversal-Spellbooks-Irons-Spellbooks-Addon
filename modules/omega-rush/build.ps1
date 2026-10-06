param(
    [string]$JdkBin = (Join-Path $PSScriptRoot '..\..\..\analysis\build\temurin17-jdk\jdk-17.0.20.1+1\bin'),
    [string]$LibraryDir = (Join-Path $PSScriptRoot '..\..\..\analysis\runtime-smoke\vs-freeze-diag\libraries'),
    [string]$ModDir = (Join-Path $PSScriptRoot '..\..\..\analysis\magic-compat\inputs'),
    [string]$MinecraftJar = (Join-Path $env:APPDATA 'PrismLauncher\libraries\net\minecraftforge\forge\1.20.1-47.4.10\forge-1.20.1-47.4.10-client.jar'),
    [string]$MinecraftServerJar = (Join-Path $PSScriptRoot '..\..\..\irons-ultimate-explosion\test-server\libraries\net\minecraftforge\forge\1.20.1-47.4.10\forge-1.20.1-47.4.10-server.jar'),
    [string]$SrgMinecraftJar = (Join-Path $PSScriptRoot '..\..\..\analysis\build\.gradle-user-home\caches\fabric-loom\1.20.1\forge\1.20.1-47.4.2\minecraft-merged-srg-patched.jar'),
    [string]$OutputJar = (Join-Path $PSScriptRoot 'build\omega-rush-0.2.2.jar')
)
$ErrorActionPreference='Stop'
$classes=Join-Path $PSScriptRoot 'build\classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$taskClassesRoot=[IO.Path]::GetFullPath($classes)
if(-not $taskClassesRoot.StartsWith([IO.Path]::GetFullPath($PSScriptRoot)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Class output must remain inside this module.'}
Get-ChildItem -LiteralPath $taskClassesRoot -Filter '*.class' -Recurse -File | ForEach-Object {Remove-Item -LiteralPath $_.FullName}
$jars=@(
    Get-Item -LiteralPath $MinecraftJar
    Get-Item -LiteralPath $MinecraftServerJar
    Get-Item -LiteralPath $SrgMinecraftJar
    Get-ChildItem -LiteralPath $LibraryDir -Filter '*.jar' -Recurse -File
    Get-ChildItem -LiteralPath $ModDir -File | Where-Object { $_.Name -match '^(irons_spellbooks|irons_lib|geckolib-forge|player-animation-lib-forge|curios-forge)-' }
)
$argsPath=Join-Path $PSScriptRoot 'build\javac.args'
$lines=@('-proc:none','-encoding','UTF-8','-cp',('"'+(($jars.FullName | ForEach-Object { $_.Replace('\','/') }) -join ';')+'"'),'-d',('"'+$classes.Replace('\','/')+'"'))
$lines+=Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Filter '*.java' -Recurse -File | ForEach-Object { '"'+$_.FullName.Replace('\','/')+'"' }
[IO.File]::WriteAllLines($argsPath,$lines)
& (Join-Path $JdkBin 'javac.exe') ('@'+$argsPath)
if($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$manifest=Join-Path $PSScriptRoot 'build\manifest.mf'
[IO.File]::WriteAllText($manifest,"Manifest-Version: 1.0`nMixinConfigs: irons_omega_rush.mixins.json`n`n")
$taskTemp=Join-Path $PSScriptRoot 'build\tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
& (Join-Path $JdkBin 'jar.exe') ('-J-Djava.io.tmpdir='+$taskTemp) --create --file $OutputJar --manifest $manifest -C $classes . -C (Join-Path $PSScriptRoot 'src\main\resources') .
if($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Get-FileHash -LiteralPath $OutputJar -Algorithm SHA256
