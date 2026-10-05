$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src/main/resources/assets/crimson_susanoo'
foreach ($relative in @('geo/guardian.geo.json', 'animations/guardian.animation.json')) {
    $bytes = [System.IO.File]::ReadAllBytes((Join-Path $assets $relative))
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        throw "GeckoLib JSON must be UTF-8 without BOM: $relative"
    }
}
$geometry = Get-Content -LiteralPath (Join-Path $assets 'geo/guardian.geo.json') -Raw | ConvertFrom-Json
$animations = Get-Content -LiteralPath (Join-Path $assets 'animations/guardian.animation.json') -Raw | ConvertFrom-Json
foreach ($clip in $animations.animations.PSObject.Properties) {
    if ($clip.Value.bones.right_arm.position) {
        throw "Ground support owns right-arm translation; authored position track conflicts in $($clip.Name)"
    }
}
$lang = Get-Content -LiteralPath (Join-Path $assets 'lang/en_us.json') -Raw | ConvertFrom-Json
$model = $geometry.'minecraft:geometry'[0]
$boneNames = @($model.bones | ForEach-Object name)
$requiredBones = @('ribcage','head','eyes','left_shoulder','right_shoulder','left_arm','right_arm','left_forearm','right_forearm','left_hand','right_hand','waist','left_shin','right_shin','left_foot','right_foot','sword')
$requiredAnimations = @('manifest','idle','walk','follow','cleave','crescent','slash','hurt','death','dismiss','collapse')
foreach ($name in $requiredBones) { if ($name -notin $boneNames) { throw "Missing model bone: $name" } }
foreach ($side in @('left','right')) {
    $forearm = $model.bones | Where-Object name -eq "${side}_forearm"
    $hand = $model.bones | Where-Object name -eq "${side}_hand"
    if ($forearm.parent -ne "${side}_arm" -or $hand.parent -ne "${side}_forearm") {
        throw "Broken articulated $side arm hierarchy"
    }
}
if (($model.bones | Where-Object name -eq 'sword').parent -ne 'right_hand') {
    throw 'Sword must inherit the gripping hand transform'
}
if (($model.bones | Where-Object name -eq 'right_arm').pivot[0] -ge 0 -or
    ($model.bones | Where-Object name -eq 'right_hand').pivot[0] -ge 0) {
    throw 'The weapon arm must be on the anatomical right (negative Bedrock X)'
}
foreach ($name in $requiredAnimations) {
    if ("animation.guardian.$name" -notin @($animations.animations.PSObject.Properties.Name)) {
        throw "Missing animation: $name"
    }
}
foreach ($animation in $animations.animations.PSObject.Properties) {
    foreach ($bone in $animation.Value.bones.PSObject.Properties) {
        foreach ($channel in $bone.Value.PSObject.Properties) {
            $times = @($channel.Value.PSObject.Properties.Name | ForEach-Object { [double]::Parse($_, [System.Globalization.CultureInfo]::InvariantCulture) })
            if ($times.Count -eq 0 -or $times[0] -ne 0) {
                throw "Animation $($animation.Name), bone $($bone.Name), channel $($channel.Name) must start at 0"
            }
            for ($i = 1; $i -lt $times.Count; $i++) {
                if ($times[$i] -le $times[$i - 1]) {
                    throw "Animation $($animation.Name), bone $($bone.Name), channel $($channel.Name) has unordered keyframes"
                }
            }
        }
    }
}
$manifest = $animations.animations.'animation.guardian.manifest'
foreach ($clip in $animations.animations.PSObject.Properties) {
    if ($clip.Value.bones.pelvis.position) {
        throw "Pelvis translation belongs to terrain support; animation $($clip.Name) would conflict with it"
    }
}
$idle = $animations.animations.'animation.guardian.idle'
$follow = $animations.animations.'animation.guardian.follow'
if ($idle.bones.root.position) { throw 'Idle animation must keep both feet planted' }
if ((@($follow.bones.root.position.PSObject.Properties | ForEach-Object { [double]$_.Value[1] }) | Measure-Object -Maximum).Maximum -gt 0.5) {
    throw 'Follow animation lifts the guardian too high above the ground'
}
if ($manifest.animation_length -ne 3 -or
    $manifest.bones.eyes.scale.'2.0'[0] -ge 0.1 -or
    $manifest.bones.eyes.scale.'2.35'[0] -lt 0.9 -or
    $manifest.bones.sword.scale.'2.0'[0] -ge 0.1 -or
    $manifest.bones.sword.scale.'3.0'[0] -lt 0.9 -or
    $manifest.bones.sword.scale.'2.5'[0] -lt 0.9 -or
    $manifest.bones.sword.rotation.'2.55'[0] -gt -20 -or
    $manifest.bones.sword.rotation.'2.75'[0] -lt 30 -or
    $manifest.bones.sword.rotation.'3.0'[0] -lt 30) {
    throw 'Final-second eye and sword manifestation timing is invalid'
}
$dismiss = $animations.animations.'animation.guardian.dismiss'
if ($dismiss.animation_length -ne 1.5 -or
    $dismiss.bones.left_leg.scale.'0.9'[0] -ge 0.1 -or
    $dismiss.bones.chest_armor.scale.'1.2'[0] -ge 0.1 -or
    $dismiss.bones.eyes.scale.'1.2'[0] -lt 0.9 -or
    $dismiss.bones.eyes.scale.'1.5'[0] -ge 0.1) {
    throw 'Feet-upward dismissal or lingering-eye timing is invalid'
}
if (($boneNames | Where-Object { $_ -match 'wing|bow|shield' }).Count -gt 0) { throw 'Forbidden model equipment found' }
$sword = $model.bones | Where-Object name -EQ 'sword' | Select-Object -First 1
$swordCubes = @($sword.cubes)
$bladeFront = ($swordCubes | ForEach-Object { [double]$_.origin[2] } | Measure-Object -Minimum).Minimum
$swordTop = ($swordCubes | ForEach-Object { [double]$_.origin[1] + [double]$_.size[1] } | Measure-Object -Maximum).Maximum
$outwardTip = ($swordCubes | Where-Object { $_.origin[2] -lt -75 } | ForEach-Object { [double]$_.origin[0] } | Measure-Object -Maximum).Maximum
$gripThroughHand = @($swordCubes | Where-Object {
    $_.origin[0] -le -23 -and ($_.origin[0] + $_.size[0]) -ge -23 -and
    $_.origin[1] -le 36 -and ($_.origin[1] + $_.size[1]) -ge 36 -and
    $_.origin[2] -le -6 -and ($_.origin[2] + $_.size[2]) -ge 6
}).Count -gt 0
$hand = $model.bones | Where-Object name -eq 'right_hand'
$knuckleFront = ($hand.cubes | ForEach-Object { [double]$_.origin[2] } | Measure-Object -Minimum).Minimum
$guard = $swordCubes[2]
if ($swordCubes[0].origin[2] -ge ($guard.origin[2] + $guard.size[2])) {
    throw 'The wrapped grip must meet the guard without an air gap'
}
if ($knuckleFront - ($guard.origin[2] + $guard.size[2]) -lt 6 -or $guard.size[1] -le $guard.size[0]) {
    throw 'The guard must clear the knuckles and share the vertical blade roll'
}
# The refined katana uses a gentler lateral curve than the first blocky blade.
if ($swordTop -gt 60 -or !$gripThroughHand -or $swordCubes.Count -ne 4) {
    throw "Sword must be held through the right grip and project forward/outward, not upward (front=$bladeFront top=$swordTop outward=$outwardTip grip=$gripThroughHand)"
}
$cubes = @($model.bones | ForEach-Object cubes | Where-Object { $null -ne $_ })
$height = ($cubes | ForEach-Object { [double]$_.origin[1] + [double]$_.size[1] } | Measure-Object -Maximum).Maximum
if ($height -lt 128) { throw "Guardian height is under 8 blocks: $height pixels" }
$texture = [System.Drawing.Bitmap]::new((Join-Path $assets 'textures/entity/guardian.png'))
$glow = [System.Drawing.Bitmap]::new((Join-Path $assets 'textures/entity/guardian_glowmask.png'))
$icon = [System.Drawing.Bitmap]::new((Join-Path $assets 'textures/gui/spell_icons/crimson_susanoo.png'))
try {
    if ($texture.Width -ne 1024 -or $texture.Height -ne 1024) { throw 'Unexpected guardian texture size' }
    if ($glow.Width -ne $texture.Width -or $glow.Height -ne $texture.Height) { throw 'Glowmask size mismatch' }
    if ($icon.Width -ne 64 -or $icon.Height -ne 64) { throw 'Unexpected spell icon size' }
    $eyeTones = [System.Collections.Generic.HashSet[int]]::new()
    $bladeTones = [System.Collections.Generic.HashSet[int]]::new()
    $bladeGlow = [System.Collections.Generic.HashSet[int]]::new()
    for ($y = 512; $y -lt 524; $y++) { for ($x = 768; $x -lt 796; $x++) {
        [void]$eyeTones.Add($texture.GetPixel($x,$y).ToArgb())
    } }
    for ($y = 512; $y -lt 528; $y++) { for ($x = 0; $x -lt 28; $x++) {
        [void]$bladeTones.Add($texture.GetPixel($x,$y).ToArgb())
    } }
    for ($y = 512; $y -lt 528; $y++) { for ($x = 512; $x -lt 516; $x++) {
        [void]$bladeGlow.Add([int]$glow.GetPixel($x,$y).A)
    } }
    if ($eyeTones.Count -lt 12 -or $bladeTones.Count -lt 12 -or $bladeGlow.Count -lt 8) {
        throw 'Eye or sword lacks smooth gradient variation'
    }
    foreach ($point in @(@(0,512), @(256,512))) {
        if ($glow.GetPixel($point[0],$point[1]).ToArgb() -ne 0) {
            throw 'The blade or cutting edge is fully emissive and washes out under Solas'
        }
    }
    foreach ($point in @(@(128,128), @(896,128), @(896,384), @(128,896))) {
        if ($glow.GetPixel($point[0],$point[1]).ToArgb() -ne 0) {
            throw 'Unlit guardian material has nonzero glowmask color; GeckoLib would render it emissive'
        }
    }
    # Count the UV area actually sampled by armor, including the opaque rear plate.
    foreach ($tileY in @(0,768)) {
        $lit = 0
        $crackTones = [System.Collections.Generic.HashSet[int]]::new()
        for ($y = 0; $y -lt 172; $y++) { for ($x = 0; $x -lt 112; $x++) {
            $pixel = $glow.GetPixel($x,$y+$tileY)
            if ($pixel.A -gt 0) { $lit++; [void]$crackTones.Add([int]$pixel.A) }
        } }
        $coverage = $lit / (112.0*172)
        if ($coverage -lt .015 -or $coverage -gt .12 -or $crackTones.Count -lt 12) {
            throw "Armor fractures must remain narrow and smoothly shaded (coverage=$coverage)"
        }
        Write-Host ('Armor fracture emission coverage: {0:P1}' -f $coverage)
    }
} finally { $texture.Dispose(); $glow.Dispose(); $icon.Dispose() }
if (!$lang.'spell.crimson_susanoo.crimson_susanoo') { throw 'Missing translated spell name' }
Write-Host "Assets valid: $($model.bones.Count) bones, $($cubes.Count) cubes, $height pixels tall, $($requiredAnimations.Count) animations"
