$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$env:JAVA_HOME){throw 'Set JAVA_HOME to Java 17.'}
$output=Join-Path $root 'build/locomotion-check'
New-Item -ItemType Directory -Force $output | Out-Null
$sources=@('LocomotionPose','FootPlanting','AttackPoseBlend') | ForEach-Object { Join-Path $root "src/main/java/com/crimson_susanoo/client/$_.java" }
& "$env:JAVA_HOME/bin/javac.exe" -d $output @sources (Join-Path $PSScriptRoot 'LocomotionPoseCheck.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$env:JAVA_HOME/bin/java.exe" -cp $output com.crimson_susanoo.client.LocomotionPoseCheck
exit $LASTEXITCODE
