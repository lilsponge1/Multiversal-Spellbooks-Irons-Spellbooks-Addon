$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to Java 17 before running this check.' }
$output = Join-Path $root 'build/pose-check'
New-Item -ItemType Directory -Force $output | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -d $output (Join-Path $root 'src/main/java/com/crimson_susanoo/client/AttackPoseBlend.java') (Join-Path $PSScriptRoot 'AttackPoseBlendCheck.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$env:JAVA_HOME/bin/java.exe" -cp $output com.crimson_susanoo.client.AttackPoseBlendCheck
exit $LASTEXITCODE
