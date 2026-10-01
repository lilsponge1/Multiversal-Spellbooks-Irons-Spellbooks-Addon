$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$env:JAVA_HOME){throw 'Set JAVA_HOME to Java 17.'}
$output=Join-Path $root 'build/foot-check'
New-Item -ItemType Directory -Force $output | Out-Null
$clips=(Get-Content (Join-Path $root 'src/main/resources/assets/crimson_susanoo/animations/guardian.animation.json') -Raw | ConvertFrom-Json).animations
$rows=[Collections.Generic.List[string]]::new()
$culture=[Globalization.CultureInfo]::InvariantCulture
function Sample($track,[double]$time){
    if(!$track){return @(0,0,0)}
    $keys=@($track.PSObject.Properties)
    for($i=1;$i -lt $keys.Count;$i++){
        $b=[double]::Parse($keys[$i].Name,$culture)
        if($time -le $b){
            $a=[double]::Parse($keys[$i-1].Name,$culture)
            $u=($time-$a)/($b-$a)
            return @(0..2 | ForEach-Object { (1-$u)*$keys[$i-1].Value[$_] + $u*$keys[$i].Value[$_] })
        }
    }
    return $keys[-1].Value
}
$action=0
foreach($name in @('cleave','crescent','slash')){
    $action++
    $clip=$clips.PSObject.Properties["animation.guardian.$name"].Value
    for($tick=0;$tick -le $clip.animation_length*20;$tick+=.25){
        $values=[Collections.Generic.List[string]]::new()
        $values.Add($action.ToString());$values.Add($tick.ToString($culture))
        foreach($side in @('left','right')){foreach($part in @('_leg','_shin','_foot')){
            $bone=$clip.bones.PSObject.Properties[$side+$part].Value
            $r=Sample $bone.rotation ($tick/20)
            $p=Sample $bone.position ($tick/20)
            foreach($v in @((-$r[0]*[Math]::PI/180),(-$r[1]*[Math]::PI/180),($r[2]*[Math]::PI/180),(-$p[0]),$p[1],$p[2])){
                $values.Add(([double]$v).ToString('R',$culture))
            }
        }}
        $rows.Add(($values -join "`t"))
    }
}
$fixture=Join-Path $output 'attack-poses.tsv'
[IO.File]::WriteAllLines($fixture,$rows,[Text.UTF8Encoding]::new($false))
& "$env:JAVA_HOME/bin/javac.exe" -d $output (Join-Path $root 'src/main/java/com/crimson_susanoo/client/FootPlanting.java') (Join-Path $PSScriptRoot 'FootPlantingCheck.java') (Join-Path $root 'src/main/java/com/crimson_susanoo/client/AttackPoseBlend.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$env:JAVA_HOME/bin/java.exe" -cp $output com.crimson_susanoo.client.FootPlantingCheck $fixture
exit $LASTEXITCODE