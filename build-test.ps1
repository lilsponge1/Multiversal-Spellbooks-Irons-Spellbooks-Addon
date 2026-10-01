$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jdk = Join-Path $root '..\analysis\build\temurin17-jdk\jdk-17.0.20.1+1\bin'
$classes = Join-Path $root 'build\test-classes'
$argsPath = Join-Path $root 'build\test-javac.args'
$out = Join-Path $root 'build\grand-explosion-test.jar'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$lines = @(Get-Content -LiteralPath (Join-Path $root 'build\javac.args') | Select-Object -First 7)
$authlib = Join-Path $root '..\analysis\runtime-smoke\vs-freeze-diag\libraries\com\mojang\authlib\4.0.43\authlib-4.0.43.jar'
$lines[4] = $lines[4].TrimEnd('"') + ';' + (Join-Path $root 'build\classes').Replace('\', '/') + ';' + $authlib.Replace('\', '/') + '"'
$lines[6] = '"' + $classes.Replace('\', '/') + '"'
$lines += @(Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse -File | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
$lines += @(Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Filter '*.java' -Recurse -File | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
[System.IO.File]::WriteAllLines($argsPath, $lines)
& (Join-Path $jdk 'javac.exe') ('@' + $argsPath)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $jdk 'jar.exe') --create --file $out -C $classes 'local/grandexplosiontest' -C (Join-Path $root 'src\test\resources') .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output $out
