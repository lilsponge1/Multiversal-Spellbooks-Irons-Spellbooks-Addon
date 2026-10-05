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
            return @(0..2 | ForEach-Object {
                $axis=$_; $x=$keys[$i-1].Value[$axis]; $y=$keys[$i].Value[$axis]
                $s=($y-$x)/($b-$a); $m0=0.0; $m1=0.0
                if($i -gt 1){
                    $p=[double]::Parse($keys[$i-2].Name,$culture); $h=$a-$p
                    $m0=Tangent (($x-$keys[$i-2].Value[$axis])/$h) $s $h ($b-$a)
                }
                if($i+1 -lt $keys.Count){
                    $n=[double]::Parse($keys[$i+1].Name,$culture); $h=$n-$b
                    $m1=Tangent $s (($keys[$i+1].Value[$axis]-$y)/$h) ($b-$a) $h
                }
                (2*$u*$u*$u-3*$u*$u+1)*$x+($u*$u*$u-2*$u*$u+$u)*($b-$a)*$m0+(-2*$u*$u*$u+3*$u*$u)*$y+($u*$u*$u-$u*$u)*($b-$a)*$m1
            })
        }
    }
    return $keys[-1].Value
}
function Tangent([double]$p,[double]$n,[double]$a,[double]$b){
    if($p*$n -le 0){return 0}
    $w1=2*$b+$a; $w2=$b+2*$a
    return ($w1+$w2)/($w1/$p+$w2/$n)
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
