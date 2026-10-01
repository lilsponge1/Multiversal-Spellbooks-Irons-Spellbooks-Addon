$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src/main/resources/assets/crimson_susanoo'
$geo = (Get-Content "$assets/geo/guardian.geo.json" -Raw | ConvertFrom-Json).'minecraft:geometry'[0]
$clips = (Get-Content "$assets/animations/guardian.animation.json" -Raw | ConvertFrom-Json).animations
function Rotate-Point([double[]]$point, [double[]]$pivot, [double[]]$angles) {
    $x=$point[0]-$pivot[0]; $y=$point[1]-$pivot[1]; $z=$point[2]-$pivot[2]
    # GeckoLib's Bedrock conversion negates X coordinates and X/Y angles.
    $a=-$angles[0]*[Math]::PI/180; $b=-$angles[1]*[Math]::PI/180; $c=$angles[2]*[Math]::PI/180
    $ny=$y*[Math]::Cos($a)-$z*[Math]::Sin($a); $nz=$y*[Math]::Sin($a)+$z*[Math]::Cos($a)
    $nx=$x*[Math]::Cos($b)+$nz*[Math]::Sin($b); $z=-$x*[Math]::Sin($b)+$nz*[Math]::Cos($b)
    $x=$nx*[Math]::Cos($c)-$ny*[Math]::Sin($c); $y=$nx*[Math]::Sin($c)+$ny*[Math]::Cos($c)
    return @(($x+$pivot[0]),($y+$pivot[1]),($z+$pivot[2]))
}
$sword = $geo.bones | Where-Object name -eq 'sword'
$arm = $geo.bones | Where-Object name -eq 'right_arm'
$sp=@(-$sword.pivot[0],$sword.pivot[1],$sword.pivot[2])
$ap=@(-$arm.pivot[0],$arm.pivot[1],$arm.pivot[2])
foreach ($case in @(@('cleave','0.55'),@('slash','1.0'))) {
    $clip=$clips.PSObject.Properties["animation.guardian.$($case[0])"].Value
    $sr=@(0,0,0)
    $ar=$clip.bones.right_arm.rotation.PSObject.Properties[$case[1]].Value
    if (!$sr -or !$ar) { throw 'Missing exact damage-frame pose' }
    # Include animated ancestors: upper-body choreography must not move the blade
    # away from the server damage frame. Require explicit keys on moving ancestors.
    $ancestorTransforms = @()
    $parentName = $sword.parent
    while ($parentName) {
        $parentBone = $geo.bones | Where-Object name -eq $parentName
        $channel = $clip.bones.PSObject.Properties[$parentName]
        if ($channel -and $channel.Value.rotation) {
            $key = $channel.Value.rotation.PSObject.Properties[$case[1]]
            if (!$key) { throw "Missing exact damage-frame rotation for $parentName" }
            $ancestorTransforms += @{ pivot = @(-$parentBone.pivot[0],$parentBone.pivot[1],$parentBone.pivot[2]); rotation = $key.Value }
        }
        $parentName = $parentBone.parent
    }
    # Sample the continuous centerline used by CrimsonKatana (model pixels).
    $hits=0; $supportHits=0; $heights=@()
    . {
        for ($i=0; $i -le 192; $i++) {
            $t=$i/192.0; $point=@(-(24+4*$t*$t),(36+3*$t),-(18+80*$t))
            $point=Rotate-Point $point $sp $sr
            foreach ($transform in $ancestorTransforms) {
                $point=Rotate-Point $point $transform.pivot $transform.rotation
            }
            $forward=-$point[2]/16; $height=$point[1]/16; $side=$point[0]/16
            if ($forward -ge 4.5 -and $forward -le 5.5) {
                $heights += $height
                if ([Math]::Abs($side) -le .5 -and $height -ge .3 -and $height -le 1.8) { $hits++ }
                # Foot support lowers the weapon by at most eight pixels; the full-stair
                # arm lift is independently checked by FootPlantingCheck.
                # A sample inside the torso at both endpoints also stays inside
                # throughout that continuous vertical support envelope.
                if ([Math]::Abs($side) -le .5 -and $height-.5 -ge .3 -and $height -le 1.8) { $supportHits++ }
            }
        }
    }
    if (!$hits) { throw "$($case[0]) misses the representative humanoid torso corridor at its damage frame" }
    if (!$supportHits) { throw "$($case[0]) loses its torso corridor after bounded weapon support lowering" }
    $range=$heights | Measure-Object -Minimum -Maximum
    Write-Host ('{0}: damage frame {1}s, blade height {2:N2}..{3:N2} blocks at 4.5..5.5 blocks forward; {4} center-corridor samples' -f $case[0],$case[1],$range.Minimum,$range.Maximum,$hits)
    Write-Host "$($case[0]): $supportHits corridor samples survive the full 0..8-pixel support-lowering envelope"
}
Write-Host 'Offline geometry check only; live hit timing, moving targets and shader appearance require Minecraft testing.'
foreach ($case in @(@('cleave','0.55'),@('slash','1.0'))) {
    $clip=$clips.PSObject.Properties["animation.guardian.$($case[0])"].Value
    $footPositions=@()
    foreach ($side in @('left','right')) {
        $boneName="${side}_foot"
        $foot=$geo.bones | Where-Object name -eq $boneName
        $point=@(-$foot.pivot[0],0,0)
        while ($boneName) {
            $bone=$geo.bones | Where-Object name -eq $boneName
            $channel=$clip.bones.PSObject.Properties[$boneName].Value
            if ($channel.rotation) {
                $rotation=$channel.rotation.PSObject.Properties[$case[1]].Value
                if (!$rotation) { throw "Missing contact pose for $boneName" }
                $point=Rotate-Point $point @(-$bone.pivot[0],$bone.pivot[1],$bone.pivot[2]) $rotation
            }
            if ($channel.position) {
                $offset=$channel.position.PSObject.Properties[$case[1]].Value
                if (!$offset) { throw "Missing contact translation for $boneName" }
                $point=@(($point[0]-$offset[0]),($point[1]+$offset[1]),($point[2]+$offset[2]))
            }
            $boneName=$bone.parent
        }
        if ([Math]::Abs($point[1]) -gt .15) { throw "$($case[0]) $side sole is not grounded at contact: $($point[1]) pixels" }
        $footPositions += $point[2]
    }
    if ([Math]::Abs($footPositions[0]-$footPositions[1]) -lt 18) { throw "$($case[0]) lacks a full planted stride" }
    Write-Host "$($case[0]): both soles grounded, planted stride exceeds 18 model pixels"
}
foreach ($name in @('cleave','slash')) {
    $clip=$clips.PSObject.Properties["animation.guardian.$name"].Value
    $samples=0; $maxLift=0.0
    foreach ($side in @('left','right')) {
        foreach ($frame in $clip.bones.PSObject.Properties["${side}_leg"].Value.rotation.PSObject.Properties) {
            # Forward kinematics on heel/toe corners checks the exported hierarchy,
            # rather than repeating the generator's joint-shortening formula.
            foreach ($z in @(-11,10)) {
                $boneName="${side}_foot"
                $foot=$geo.bones | Where-Object name -eq $boneName
                $point=@(-$foot.pivot[0],0,$z)
                while ($boneName -ne 'pelvis') {
                    $bone=$geo.bones | Where-Object name -eq $boneName
                    $channel=$clip.bones.PSObject.Properties[$boneName].Value
                    $rotation=$channel.rotation.PSObject.Properties[$frame.Name].Value
                    if (!$rotation) { throw "Missing baked rotation $boneName at $($frame.Name)" }
                    $point=Rotate-Point $point @(-$bone.pivot[0],$bone.pivot[1],$bone.pivot[2]) $rotation
                    if ($channel.position) {
                        $offset=$channel.position.PSObject.Properties[$frame.Name].Value
                        $point=@(($point[0]-$offset[0]),($point[1]+$offset[1]),($point[2]+$offset[2]))
                    }
                    $boneName=$bone.parent
                }
                if ($point[1] -lt -.02) { throw "$name $side foot clips ground at $($frame.Name): $($point[1])" }
                $maxLift=[Math]::Max($maxLift,$point[1]); $samples++
            }
        }
    }
    if ($maxLift -lt 6.9) { throw "$name lacks a deliberate lifted step" }
    Write-Host "$name`: $samples exported heel/toe samples clear the floor; peak lift $maxLift pixels"
}
