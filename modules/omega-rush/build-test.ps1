$ErrorActionPreference='Stop'
$taskJdk=Join-Path $PSScriptRoot '..\..\..\analysis\build\temurin17-jdk\jdk-17.0.20.1+1\bin'
$taskClasses=Join-Path $PSScriptRoot 'build\test-classes'
New-Item -ItemType Directory -Path $taskClasses -Force | Out-Null
$taskLines=@(Get-Content -LiteralPath (Join-Path $PSScriptRoot 'build\javac.args') | Select-Object -First 7)
$taskLines[4]=$taskLines[4].TrimEnd('"')+';'+(Join-Path $PSScriptRoot 'build\classes').Replace('\','/')+'"'
$taskLines[6]='"'+$taskClasses.Replace('\','/')+'"'
$taskLines+=Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\test\java') -Recurse -Filter '*.java' -File | ForEach-Object { '"'+$_.FullName.Replace('\','/')+'"' }
$taskArgs=Join-Path $PSScriptRoot 'build\test-javac.args'
[IO.File]::WriteAllLines($taskArgs,$taskLines)
& (Join-Path $taskJdk 'javac.exe') ('@'+$taskArgs)
if($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $taskJdk 'jar.exe') ('-J-Djava.io.tmpdir='+(Join-Path $PSScriptRoot 'build\tmp')) --create --file (Join-Path $PSScriptRoot 'build\omega-rush-test.jar') -C $taskClasses . -C (Join-Path $PSScriptRoot 'src\test\resources') .
if($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
