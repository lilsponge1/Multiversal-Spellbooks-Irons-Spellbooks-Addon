param([switch]$ReportOnly,[string]$Trace)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$env:JAVA_HOME){throw 'Set JAVA_HOME to Java 17.'}
$output=Join-Path $root 'build/gait-cadence-check'
New-Item -ItemType Directory -Force $output | Out-Null
$sources=@(
    (Join-Path $root 'src/main/java/com/crimson_susanoo/client/FootPlanting.java'),
    (Join-Path $root 'src/main/java/com/crimson_susanoo/client/AttackPoseBlend.java'),
    (Join-Path $PSScriptRoot 'FootPlantingCheck.java'),
    (Join-Path $PSScriptRoot 'GaitCadenceCheck.java'))
& "$env:JAVA_HOME/bin/javac.exe" -d $output @sources
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
$options=@()
if($ReportOnly){$options+='--report'}
if($Trace){$options+=(Resolve-Path -LiteralPath $Trace).Path}
& "$env:JAVA_HOME/bin/java.exe" -cp $output com.crimson_susanoo.client.GaitCadenceCheck @options
exit $LASTEXITCODE
