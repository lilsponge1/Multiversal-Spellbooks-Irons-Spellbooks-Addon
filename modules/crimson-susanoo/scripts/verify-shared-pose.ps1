param([switch]$Baseline)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to Java 17.' }
$output=Join-Path $root 'build/shared-pose-check'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$dependencyJars=@((Join-Path $root 'libs/geckolib-forge-1.20.1-4.8.4.jar'))
$dependencyJars += Get-ChildItem -LiteralPath (Join-Path $root 'run-pack/libraries') -Filter '*.jar' -Recurse | Where-Object {$_.Name -match '^(fastutil|joml)-'} | Select-Object -ExpandProperty FullName
# mclib is distributed inside GeckoLib; use that exact bundled dependency.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$gecko=[IO.Compression.ZipFile]::OpenRead($dependencyJars[0])
try {
    $entry=$gecko.Entries | Where-Object {$_.FullName -match '^META-INF/jarjar/mclib.*\.jar$'} | Select-Object -First 1
    if (!$entry) { throw 'GeckoLib bundled mclib dependency missing.' }
    $mclib=Join-Path $output $entry.Name
    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$mclib,$true)
    $dependencyJars+=$mclib
} finally { $gecko.Dispose() }
$classpath=$dependencyJars -join [IO.Path]::PathSeparator
& "$env:JAVA_HOME/bin/javac.exe" -cp $classpath -d $output (Join-Path $root 'src/main/java/com/crimson_susanoo/client/BonePose.java') (Join-Path $PSScriptRoot 'SharedBonePoseCheck.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$arguments=@('-cp',($output+[IO.Path]::PathSeparator+$classpath),'com.crimson_susanoo.client.SharedBonePoseCheck')
if ($Baseline) { $arguments+='--baseline' }
& "$env:JAVA_HOME/bin/java.exe" @arguments
exit $LASTEXITCODE
