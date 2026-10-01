$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src/main/resources/assets/crimson_susanoo'
$geoDir = Join-Path $assets 'geo'
$animationDir = Join-Path $assets 'animations'
$entityTextureDir = Join-Path $assets 'textures/entity'
$spellTextureDir = Join-Path $assets 'textures/gui/spell_icons'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
function Write-AssetJson([string]$path, $value) {
    [System.IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 100), $script:utf8NoBom)
}
@($geoDir, $animationDir, $entityTextureDir, $spellTextureDir) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

$bones = [System.Collections.Generic.List[object]]::new()
function Add-Bone([string]$name, [int[]]$pivot, [string]$parent = '') {
    $bone = [ordered]@{ name = $name; pivot = $pivot; cubes = [System.Collections.Generic.List[object]]::new() }
    if ($parent) { $bone.parent = $parent }
    $script:bones.Add($bone)
    return $bone
}
function Add-Cube($bone, [int[]]$origin, [int[]]$size, [int[]]$uv, [double]$inflate = 0) {
    $scaledUv = @(($uv[0] * 4), ($uv[1] * 4))
    $faces = [ordered]@{
        north = @{ uv = $scaledUv; uv_size = @(($size[0]*4),($size[1]*4)) }
        south = @{ uv = $scaledUv; uv_size = @(($size[0]*4),($size[1]*4)) }
        east = @{ uv = $scaledUv; uv_size = @(($size[2]*4),($size[1]*4)) }
        west = @{ uv = $scaledUv; uv_size = @(($size[2]*4),($size[1]*4)) }
        up = @{ uv = $scaledUv; uv_size = @(($size[0]*4),($size[2]*4)) }
        down = @{ uv = $scaledUv; uv_size = @(($size[0]*4),($size[2]*4)) }
    }
    $cube = [ordered]@{ origin = $origin; size = $size; uv = $faces }
    if ($inflate -ne 0) { $cube.inflate = $inflate }
    $bone.cubes.Add($cube)
}

$rootBone = Add-Bone 'root' @(0,0,0)
$pelvis = Add-Bone 'pelvis' @(0,46,0) 'root'
Add-Cube $pelvis @(-13,40,-7) @(26,12,14) @(0,0)
Add-Cube $pelvis @(-10,45,-9) @(20,4,3) @(128,64)
$waist = Add-Bone 'waist' @(0,52,0) 'pelvis'
$ribcage = Add-Bone 'ribcage' @(0,67,0) 'waist'
Add-Cube $ribcage @(-8,53,-5) @(16,35,10) @(192,64)
Add-Cube $ribcage @(-13,53,-8) @(26,3,4) @(128,0)
foreach ($y in @(58,64,70,76,82)) {
    Add-Cube $ribcage @(-15,$y,-10) @(30,2,4) @(128,0)
    Add-Cube $ribcage @(-15,$y,-7) @(3,4,13) @(128,0)
    Add-Cube $ribcage @(12,$y,-7) @(3,4,13) @(128,0)
}
$armor = Add-Bone 'chest_armor' @(0,73,0) 'ribcage'
Add-Cube $armor @(-17,68,-12) @(11,22,5) @(0,0)
Add-Cube $armor @(6,68,-12) @(11,22,5) @(0,0)
Add-Cube $armor @(-7,52,-13) @(14,20,5) @(0,0)
Add-Cube $armor @(-12,70,-15) @(24,4,3) @(128,64)
# Furnace opening replaces the solid center emblem.
Add-Cube $armor @(-14,53,5) @(28,37,4) @(0,192) # opaque rear armor
Add-Cube $armor @(-6,68,-14) @(2,18,3) @(192,0)
Add-Cube $armor @(4,68,-14) @(2,18,3) @(192,0)
Add-Cube $armor @(-14,78,-16) @(10,4,3) @(192,0)
Add-Cube $armor @(4,78,-16) @(10,4,3) @(192,0)
Add-Cube $armor @(-9,61,-14) @(18,3,3) @(64,0)
# Visible ribs form the furnace grille.
foreach ($y in @(61,67,73,79)) {
    Add-Cube $ribcage @(-14,$y,-12) @(9,2,3) @(64,0)
    Add-Cube $ribcage @(5,$y,-12) @(9,2,3) @(64,0)
}
$head = Add-Bone 'head' @(0,99,0) 'ribcage'
Add-Cube $head @(-11,90,-10) @(22,23,20) @(0,0)
Add-Cube $head @(-13,103,-13) @(26,10,4) @(0,0)
Add-Cube $head @(-10,94,-14) @(20,6,4) @(192,0)
$eyes = Add-Bone 'eyes' @(0,100,-15) 'head'
Add-Cube $eyes @(-9,99,-15) @(7,3,2) @(192,128)
Add-Cube $eyes @(2,99,-15) @(7,3,2) @(192,128)
Add-Cube $head @(-11,102,-17) @(10,2,2) @(192,0) # heavy eye sockets
Add-Cube $head @(1,102,-17) @(10,2,2) @(192,0)
Add-Cube $head @(-11,97,-17) @(8,1,2) @(64,0)  # ember-lit lower lids
Add-Cube $head @(3,97,-17) @(8,1,2) @(64,0)
Add-Cube $head @(-12,94,-17) @(3,4,2) @(64,0)  # cheek flame scars
Add-Cube $head @(9,94,-17) @(3,4,2) @(64,0)
Add-Cube $head @(-1,96,-18) @(2,5,3) @(192,0) # narrow nose bridge
Add-Cube $head @(-2,95,-19) @(4,2,2) @(64,0)
Add-Cube $head @(-9,91,-15) @(18,2,2) @(64,0)
Add-Cube $head @(-12,103,-15) @(10,3,3) @(192,0)
Add-Cube $head @(2,103,-15) @(10,3,3) @(192,0)
Add-Cube $head @(-3,108,-16) @(6,5,3) @(64,64)
Add-Cube $head @(-12,88,-15) @(4,9,3) @(128,64)
Add-Cube $head @(8,88,-15) @(4,9,3) @(128,64)
Add-Cube $head @(-8,86,-16) @(3,9,3) @(64,0)
Add-Cube $head @(5,86,-16) @(3,9,3) @(64,0)
Add-Cube $head @(-5,85,-14) @(10,5,3) @(0,0)
Add-Cube $head @(-13,109,-9) @(5,19,10) @(128,64)
Add-Cube $head @(8,109,-9) @(5,19,10) @(128,64)
Add-Cube $head @(-5,112,-8) @(10,16,12) @(0,0)
Add-Cube $head @(-12,122,-10) @(3,6,8) @(64,0)
Add-Cube $head @(9,122,-10) @(3,6,8) @(64,0)
Add-Cube $head @(-3,122,-9) @(6,6,9) @(64,0)
Add-Cube $head @(-11,108,-16) @(6,3,3) @(192,0)
Add-Cube $head @(5,108,-16) @(6,3,3) @(192,0)
Add-Cube $head @(-4,115,-10) @(8,9,2) @(64,0)
Add-Cube $head @(-2,124,-11) @(4,4,3) @(0,64)
Add-Cube $head @(-8,87,-18) @(16,8,3) @(192,0)  # recessed oni grin
Add-Cube $head @(-8,94,-19) @(16,2,3) @(64,0)
Add-Cube $head @(-7,90,-19) @(3,5,3) @(0,64)     # four firelit fangs
Add-Cube $head @(-1,91,-19) @(2,4,3) @(0,64)
Add-Cube $head @(4,90,-19) @(3,5,3) @(0,64)
Add-Cube $head @(-6,85,-18) @(3,5,3) @(64,0)
Add-Cube $head @(3,85,-18) @(3,5,3) @(64,0)
$leftShoulder = Add-Bone 'left_shoulder' @(-19,85,0) 'ribcage'
Add-Cube $leftShoulder @(-36,79,-13) @(23,19,26) @(0,0)
Add-Cube $leftShoulder @(-35,94,-9) @(20,5,18) @(128,64)
Add-Cube $leftShoulder @(-36,77,-14) @(24,3,3) @(64,0)
Add-Cube $leftShoulder @(-34,97,-9) @(5,10,12) @(64,0)
Add-Cube $leftShoulder @(-24,97,-8) @(4,7,10) @(128,64)
Add-Cube $leftShoulder @(-34,82,-16) @(16,3,3) @(64,0)
Add-Cube $leftShoulder @(-31,88,-16) @(13,3,3) @(192,0)
$rightShoulder = Add-Bone 'right_shoulder' @(19,85,0) 'ribcage'
Add-Cube $rightShoulder @(13,79,-13) @(23,19,26) @(0,0)
Add-Cube $rightShoulder @(15,94,-9) @(20,5,18) @(128,64)
Add-Cube $rightShoulder @(12,77,-14) @(24,3,3) @(64,0)
Add-Cube $rightShoulder @(29,97,-9) @(5,10,12) @(64,0)
Add-Cube $rightShoulder @(20,97,-8) @(4,7,10) @(128,64)
Add-Cube $rightShoulder @(18,82,-16) @(16,3,3) @(64,0)
Add-Cube $rightShoulder @(18,88,-16) @(13,3,3) @(192,0)
$leftArm = Add-Bone 'left_arm' @(-23,79,0) 'left_shoulder'
Add-Cube $leftArm @(-30,59,-7) @(14,22,14) @(0,0)
$leftForearm = Add-Bone 'left_forearm' @(-23,60,0) 'left_arm'
Add-Cube $leftForearm @(-30,42,-7) @(14,20,14) @(0,0)
Add-Cube $leftForearm @(-32,39,-9) @(18,10,18) @(128,64)
Add-Cube $leftForearm @(-31,49,-9) @(18,4,18) @(192,0)
$leftHand = Add-Bone 'left_hand' @(-23,36,-3) 'left_forearm'
Add-Cube $leftHand @(-29,31,-6) @(12,10,12) @(0,0)
Add-Cube $leftHand @(-29,35,-10) @(12,3,4) @(64,0)
$rightArm = Add-Bone 'right_arm' @(23,79,0) 'right_shoulder'
Add-Cube $rightArm @(16,59,-7) @(14,22,14) @(0,0)
$rightForearm = Add-Bone 'right_forearm' @(23,60,0) 'right_arm'
Add-Cube $rightForearm @(16,42,-7) @(14,20,14) @(0,0)
Add-Cube $rightForearm @(14,39,-9) @(18,10,18) @(128,64)
Add-Cube $rightForearm @(13,49,-9) @(18,4,18) @(192,0)
$rightHand = Add-Bone 'right_hand' @(23,36,-3) 'right_forearm'
Add-Cube $rightHand @(17,31,-6) @(12,10,12) @(0,0)
Add-Cube $rightHand @(17,35,-10) @(12,3,4) @(64,0)
$leftLeg = Add-Bone 'left_leg' @(-8,44,0) 'pelvis'
# Extend the upper leg inside the pelvis so planted-foot hip compensation cannot open a seam.
Add-Cube $leftLeg @(-14,23,-7) @(12,29,14) @(0,0)
$leftShin = Add-Bone 'left_shin' @(-8,24,0) 'left_leg'
Add-Cube $leftShin @(-14,7,-7) @(12,19,14) @(0,0)
$leftFoot = Add-Bone 'left_foot' @(-8,8,0) 'left_shin'
Add-Cube $leftFoot @(-15,0,-11) @(14,8,21) @(0,0)
Add-Cube $leftLeg @(-15,30,-9) @(14,5,18) @(128,64)
Add-Cube $leftShin @(-15,18,-11) @(14,6,4) @(192,0)
Add-Cube $leftFoot @(-14,7,-12) @(12,4,5) @(64,0)
$rightLeg = Add-Bone 'right_leg' @(8,44,0) 'pelvis'
Add-Cube $rightLeg @(2,23,-7) @(12,29,14) @(0,0)
$rightShin = Add-Bone 'right_shin' @(8,24,0) 'right_leg'
Add-Cube $rightShin @(2,7,-7) @(12,19,14) @(0,0)
$rightFoot = Add-Bone 'right_foot' @(8,8,0) 'right_shin'
Add-Cube $rightFoot @(1,0,-11) @(14,8,21) @(0,0)
Add-Cube $rightLeg @(1,30,-9) @(14,5,18) @(128,64)
Add-Cube $rightShin @(1,18,-11) @(14,6,4) @(192,0)
Add-Cube $rightFoot @(2,7,-12) @(12,4,5) @(64,0)
# One forward-held, katana-like sword. The grip passes through the right hand
# (x 17..29, y 31..41, z -6..6); the guard and blade start beyond its knuckles.
$sword = Add-Bone 'sword' @(23,36,-3) 'right_hand'
Add-Cube $sword @(21,34,-10) @(4,4,21) @(192,0)   # long wrapped tsuka
Add-Cube $sword @(20,33,9) @(6,6,4) @(64,0)        # pommel
Add-Cube $sword @(17,34,-14) @(14,5,3) @(192,0)    # compact tsuba
Add-Cube $sword @(20,34,-19) @(7,4,5) @(64,0)      # copper habaki

$geometry = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{
            identifier = 'geometry.crimson_susanoo.guardian'
            texture_width = 1024
            texture_height = 1024
            visible_bounds_width = 7
            visible_bounds_height = 9
            visible_bounds_offset = @(0,4,0)
        }
        bones = $bones
    })
}
Write-AssetJson (Join-Path $geoDir 'guardian.geo.json') $geometry

$animations = [ordered]@{ format_version = '1.8.0'; animations = [ordered]@{} }
$animations.animations['animation.guardian.idle'] = [ordered]@{
    loop = $true; animation_length = 2
    bones = [ordered]@{
        head = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,-4,0); '1.0' = @(0,4,0); '2.0' = @(0,-4,0) } }
        sword = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-42,0,5); '1.0' = @(-44,0,2); '2.0' = @(-42,0,5) } }
    }
}
$animations.animations['animation.guardian.walk'] = [ordered]@{
    loop = $true; animation_length = 1
    bones = [ordered]@{
        left_leg = [ordered]@{ rotation = [ordered]@{ '0.0' = @(25,0,0); '0.5' = @(-25,0,0); '1.0' = @(25,0,0) } }
        right_leg = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-25,0,0); '0.5' = @(25,0,0); '1.0' = @(-25,0,0) } }
        left_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-12,0,0); '0.5' = @(12,0,0); '1.0' = @(-12,0,0) } }
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(12,0,0); '0.5' = @(-12,0,0); '1.0' = @(12,0,0) } }
    }
}
$animations.animations['animation.guardian.follow'] = [ordered]@{
    loop = $true; animation_length = 1.2
    bones = [ordered]@{
        root = [ordered]@{
            position = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(0,0.35,0); '0.6' = @(0,0,0); '0.9' = @(0,0.35,0); '1.2' = @(0,0,0) }
        }
        left_leg = [ordered]@{ rotation = [ordered]@{ '0.0' = @(30,0,0); '0.3' = @(0,0,0); '0.6' = @(-30,0,0); '0.9' = @(0,0,0); '1.2' = @(30,0,0) } }
        right_leg = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-30,0,0); '0.3' = @(0,0,0); '0.6' = @(30,0,0); '0.9' = @(0,0,0); '1.2' = @(-30,0,0) } }
        chest_armor = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,4,0); '0.6' = @(0,-4,0); '1.2' = @(0,4,0) } }
        head = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,-3,0); '0.6' = @(0,3,0); '1.2' = @(0,-3,0) } }
        left_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-15,0,0); '0.6' = @(15,0,0); '1.2' = @(-15,0,0) } }
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(5,0,0); '0.6' = @(-5,0,0); '1.2' = @(5,0,0) } }
    }
}
$animations.animations['animation.guardian.cleave'] = [ordered]@{
    animation_length = 0.85
    bones = [ordered]@{
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,-55,0); '0.4' = @(0,-55,0); '0.55' = @(0,17,0); '0.65' = @(0,60,0); '0.85' = @(0,0,0) } }
        # Maledictus-inspired coil and counterbalance, retimed to our tick-11 contact.
        # Rotate the complete upper body rather than twisting armor off the ribs.
        ribcage = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.25' = @(0,-16,0); '0.4' = @(0,-16,0); '0.55' = @(0,0,0); '0.65' = @(0,12,0); '0.85' = @(0,0,0) } }
        head = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.25' = @(0,12,0); '0.4' = @(0,12,0); '0.55' = @(0,0,0); '0.65' = @(0,-8,0); '0.85' = @(0,0,0) } }
        left_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(-18,0,15); '0.55' = @(12,0,-12); '0.65' = @(18,0,-15); '0.85' = @(0,0,0) } }
        sword = [ordered]@{ rotation = [ordered]@{ '0.0' = @(12,0,0); '0.55' = @(12,0,0); '0.65' = @(12,0,0); '0.85' = @(-42,0,3) } }
    }
}
$animations.animations['animation.guardian.crescent'] = [ordered]@{
    animation_length = 1
    bones = [ordered]@{
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-100,0,-20); '0.45' = @(-110,0,-30); '0.7' = @(35,0,50); '1.0' = @(0,0,0) } }
        ribcage = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.35' = @(0,-14,0); '0.5' = @(0,-14,0); '0.7' = @(0,0,0); '0.8' = @(0,10,0); '1.0' = @(0,0,0) } }
        head = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.35' = @(0,10,0); '0.5' = @(0,10,0); '0.7' = @(0,0,0); '1.0' = @(0,0,0) } }
        left_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.35' = @(-25,0,18); '0.5' = @(-25,0,18); '0.7' = @(18,0,-12); '1.0' = @(0,0,0) } }
        sword = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.7' = @(0,0,45); '1.0' = @(-42,0,3) } }
    }
}
$animations.animations['animation.guardian.slash'] = [ordered]@{
    animation_length = 1.3
    bones = [ordered]@{
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(-100,0,0); '0.8' = @(-100,0,0); '1.0' = @(0,0,0); '1.1' = @(0,0,0); '1.3' = @(0,0,0) } }
        # Ignis-inspired held anticipation, sharp release and restrained recoil.
        ribcage = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(-8,0,0); '0.8' = @(-8,0,0); '1.0' = @(0,0,0); '1.1' = @(6,0,0); '1.3' = @(0,0,0) } }
        head = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(6,0,0); '0.8' = @(6,0,0); '1.0' = @(0,0,0); '1.3' = @(0,0,0) } }
        left_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '0.3' = @(-32,0,20); '0.8' = @(-32,0,20); '1.0' = @(16,0,-12); '1.1' = @(20,0,-14); '1.3' = @(0,0,0) } }
        sword = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,17,0); '0.8' = @(-15,17,0); '1.0' = @(16,17,0); '1.1' = @(20,17,0); '1.3' = @(-42,0,3) } }
    }
}
$animations.animations['animation.guardian.walk'].bones['sword'] = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-42,0,3); '1.0' = @(-42,0,3) } }
$animations.animations['animation.guardian.follow'].bones['sword'] = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-42,0,3); '1.2' = @(-42,0,3) } }
$animations.animations['animation.guardian.hurt'] = [ordered]@{
    animation_length = 0.4
    bones = [ordered]@{ chest_armor = [ordered]@{ rotation = [ordered]@{ '0.0' = @(-12,0,0); '0.4' = @(0,0,0) } } }
}
$animations.animations['animation.guardian.death'] = [ordered]@{
    animation_length = 1.5
    bones = [ordered]@{ root = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '1.5' = @(0,0,80) }; scale = [ordered]@{ '0.0' = @(1,1,1); '1.5' = @(0.2,0.2,0.2) } } }
}
$animations.animations['animation.guardian.dismiss'] = [ordered]@{
    animation_length = 1.5
    bones = [ordered]@{
        sword = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.4' = @(0.1,0.1,0.1) } }
        left_leg = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.55' = @(0.6,0.6,0.6); '0.9' = @(0.01,0.01,0.01) } }
        right_leg = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.55' = @(0.6,0.6,0.6); '0.9' = @(0.01,0.01,0.01) } }
        chest_armor = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.75' = @(1,1,1); '1.2' = @(0.01,0.01,0.01) } }
        left_shoulder = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.8' = @(1,1,1); '1.3' = @(0.01,0.01,0.01) } }
        right_shoulder = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.8' = @(1,1,1); '1.3' = @(0.01,0.01,0.01) } }
        eyes = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '1.2' = @(1,1,1); '1.5' = @(0.01,0.01,0.01) } }
        root = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.8' = @(0.98,0.98,0.98); '1.5' = @(0.01,0.01,0.01) } }
    }
}
$animations.animations['animation.guardian.collapse'] = [ordered]@{
    animation_length = 1.5
    bones = [ordered]@{
        root = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.25' = @(0.9,1.1,0.9); '0.4' = @(1.1,0.85,1.1); '0.65' = @(0.7,1.15,0.7); '0.9' = @(0.5,0.5,0.5); '1.5' = @(0.01,0.01,0.01) } }
        sword = [ordered]@{ scale = [ordered]@{ '0.0' = @(1,1,1); '0.5' = @(0.2,0.2,0.2); '1.0' = @(0.01,0.01,0.01) } }
    }
}
$animations.animations['animation.guardian.manifest'] = [ordered]@{
    animation_length = 3
    bones = [ordered]@{
        root = [ordered]@{ scale = [ordered]@{ '0.0' = @(0.4,0.4,0.4); '1.0' = @(0.7,0.7,0.7); '2.0' = @(1,1,1) } }
        chest_armor = [ordered]@{ scale = [ordered]@{ '0.0' = @(0.01,0.01,0.01); '1.0' = @(0.01,0.01,0.01); '2.0' = @(1,1,1) } }
        left_shoulder = [ordered]@{ scale = [ordered]@{ '0.0' = @(0.01,0.01,0.01); '1.0' = @(0.01,0.01,0.01); '2.0' = @(1,1,1) } }
        right_shoulder = [ordered]@{ scale = [ordered]@{ '0.0' = @(0.01,0.01,0.01); '1.0' = @(0.01,0.01,0.01); '2.0' = @(1,1,1) } }
        right_arm = [ordered]@{ rotation = [ordered]@{ '0.0' = @(0,0,0); '2.0' = @(0,0,0); '2.55' = @(10,0,-5); '2.75' = @(-20,0,6); '3.0' = @(-15,0,3) } }
        eyes = [ordered]@{ scale = [ordered]@{ '0.0' = @(0.01,0.01,0.01); '2.0' = @(0.01,0.01,0.01); '2.35' = @(1,1,1); '3.0' = @(1,1,1) } }
        sword = [ordered]@{
            scale = [ordered]@{ '0.0' = @(0.01,0.01,0.01); '2.0' = @(0.01,0.01,0.01); '2.5' = @(1,1,1); '3.0' = @(1,1,1) }
            rotation = [ordered]@{ '0.0' = @(0,0,0); '2.0' = @(0,0,0); '2.55' = @(-35,0,0); '2.75' = @(55,0,0); '3.0' = @(45,0,0) }
        }
    }
}
# Articulated arms: the wrist carries the grip and blade together. The elbow
# unfolds at the existing damage frames, preserving the verified contact pose.
foreach ($name in @('idle','walk','follow','cleave','slash','crescent')) {
    $clip = $animations.animations["animation.guardian.$name"]
    $clip.bones['right_hand'] = $clip.bones.sword
    $clip.bones.Remove('sword')
}
$animations.animations['animation.guardian.cleave'].bones.right_arm.rotation['0.0'] = @(0,0,0)
$animations.animations['animation.guardian.cleave'].bones.right_hand.rotation = [ordered]@{ '0.0'=@(-42,0,3); '0.25'=@(-8,-8,-10); '0.4'=@(8,-5,-7); '0.55'=@(12,0,0); '0.65'=@(18,8,8); '0.85'=@(-42,0,3) }
$animations.animations['animation.guardian.crescent'].bones.right_arm.rotation['0.0'] = @(0,0,0)
$animations.animations['animation.guardian.crescent'].bones.right_hand.rotation['0.0'] = @(-42,0,3)
$animations.animations['animation.guardian.slash'].bones.right_hand.rotation['0.0'] = @(-42,0,3)
$jointTracks = @{
    cleave = @([ordered]@{ '0.0'=@(0,0,0); '0.22'=@(-38,0,0); '0.4'=@(-28,0,0); '0.55'=@(0,0,0); '0.68'=@(-12,0,0); '0.85'=@(0,0,0) }, [ordered]@{ '0.0'=@(0,0,0); '0.3'=@(-52,0,0); '0.55'=@(-22,0,0); '0.68'=@(-36,0,0); '0.85'=@(0,0,0) })
    slash = @([ordered]@{ '0.0'=@(0,0,0); '0.3'=@(-48,0,0); '0.65'=@(-42,0,0); '0.8'=@(-35,0,0); '1.0'=@(0,0,0); '1.1'=@(-10,0,0); '1.3'=@(0,0,0) }, [ordered]@{ '0.0'=@(0,0,0); '0.35'=@(-62,0,0); '0.8'=@(-48,0,0); '1.0'=@(-18,0,0); '1.12'=@(-28,0,0); '1.3'=@(0,0,0) })
    crescent = @([ordered]@{ '0.0'=@(0,0,0); '0.3'=@(-44,0,0); '0.45'=@(-32,0,0); '0.7'=@(0,0,0); '0.82'=@(-18,0,0); '1.0'=@(0,0,0) }, [ordered]@{ '0.0'=@(0,0,0); '0.35'=@(-55,0,0); '0.7'=@(-16,0,0); '0.85'=@(-30,0,0); '1.0'=@(0,0,0) })
}
foreach ($name in $jointTracks.Keys) {
    $clip = $animations.animations["animation.guardian.$name"]
    $clip.bones['right_forearm'] = @{ rotation=$jointTracks[$name][0] }
    $clip.bones['left_forearm'] = @{ rotation=$jointTracks[$name][1] }
    $end = $clip.animation_length.ToString('0.0', [Globalization.CultureInfo]::InvariantCulture)
    $clip.bones['left_hand'] = @{ rotation=[ordered]@{ '0.0'=@(0,0,0); '0.3'=@(-10,0,-12); $end=@(0,0,0) } }
}
foreach ($name in @('idle','walk','follow')) {
    $clip = $animations.animations["animation.guardian.$name"]
    $end = $clip.animation_length.ToString('0.0', [Globalization.CultureInfo]::InvariantCulture)
    $mid = ($clip.animation_length/2).ToString('0.0', [Globalization.CultureInfo]::InvariantCulture)
    $clip.bones['right_forearm'] = @{ rotation=[ordered]@{ '0.0'=@(-5,0,0); $mid=@(-11,0,0); $end=@(-5,0,0) } }
    $clip.bones['left_forearm'] = @{ rotation=[ordered]@{ '0.0'=@(-10,0,0); $mid=@(-4,0,0); $end=@(-10,0,0) } }
}
# Knees fold on the swing leg, ankles counter-rotate to keep soles near level.
foreach ($name in @('walk','follow')) {
    $clip=$animations.animations["animation.guardian.$name"]
    $duration=$clip.animation_length
    $clip.bones.Remove('chest_armor')
    $waistTrack=[ordered]@{}
    foreach ($side in @('left','right')) {
        $thigh=[ordered]@{}; $knee=[ordered]@{}; $ankle=[ordered]@{}
        for ($step=0; $step -le 8; $step++) {
            $phase=$step*[Math]::PI/4 + $(if ($side -eq 'right') { [Math]::PI } else { 0 })
            $angle=22*[Math]::Cos($phase)
            $bend=30*[Math]::Max(0,[Math]::Sin($phase))
            $time=($duration*$step/8).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
            $thigh[$time]=@($angle,0,0)
            $knee[$time]=@($bend,0,0)
            $ankle[$time]=@((-$angle-$bend),0,0)
            $waistTrack[$time]=@(0,(4*[Math]::Sin($step*[Math]::PI/4)),(1.5*[Math]::Cos($step*[Math]::PI/4)))
        }
        $clip.bones["${side}_leg"]=@{rotation=$thigh}
        $clip.bones["${side}_shin"]=@{rotation=$knee}
        $clip.bones["${side}_foot"]=@{rotation=$ankle}
    }
    $clip.bones['waist']=@{rotation=$waistTrack}
}
foreach ($name in @('cleave','slash','crescent')) {
    $clip=$animations.animations["animation.guardian.$name"]
    $hit=@{cleave='0.55';slash='1.0';crescent='0.7'}[$name]
    $end=$clip.animation_length.ToString('0.0',[Globalization.CultureInfo]::InvariantCulture)
    $clip.bones['waist']=@{rotation=[ordered]@{ '0.0'=@(0,0,0); '0.25'=@(-2,-6,-2); $hit=@(0,0,0); $end=@(0,0,0) }}
}
# Retarget the bosses' staged weight transfer to this rig. Keep the exact contact
# keys; give the weapon momentum past contact and a longer, staggered recovery.
foreach ($name in @('cleave','slash','crescent')) {
    $clip=$animations.animations["animation.guardian.$name"]
    $hit=@{cleave=.55;slash=1.0;crescent=.7}[$name]
    $oldEnd=$clip.animation_length
    $newEnd=$oldEnd+.30
    foreach ($bone in $clip.bones.Values) {
        if (!$bone.rotation) { continue }
        $retimed=[ordered]@{}
        foreach ($key in $bone.rotation.Keys) {
            $t=[double]::Parse($key,[Globalization.CultureInfo]::InvariantCulture)
            if ($t -gt $hit) { $t=$hit+($t-$hit)*2 }
            $retimed[$t.ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)]=$bone.rotation[$key]
        }
        $bone.rotation=$retimed
    }
    $clip.animation_length=$newEnd
    $end=$newEnd.ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $contact=$hit.ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $prep=($hit*.48).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $release=($hit-.10).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $over=($hit+.17).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $settle=($newEnd-.15).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    # Hip leads the shoulder; the free arm balances the blade's momentum.
    $clip.bones.waist.rotation=[ordered]@{'0.0'=@(0,0,0);$prep=@(-2,-12,-2);$release=@(-1,-8,-1);$contact=@(0,0,0);$over=@(3,10,2);$settle=@(1,2,0);$end=@(0,0,0)}
    $clip.bones.right_forearm.rotation['0.0']=@(-5,0,0)
    $clip.bones.right_forearm.rotation[$end]=@(-5,0,0)
    $clip.bones.left_forearm.rotation['0.0']=@(-10,0,0)
    $clip.bones.left_forearm.rotation[$end]=@(-10,0,0)
    foreach ($side in @('left','right')) {
        $bend=if($side -eq 'left'){12}else{8}
        $clip.bones["${side}_leg"]=@{rotation=[ordered]@{'0.0'=@(0,0,0);$prep=@((-$bend*.5),0,0);$contact=@(0,0,0);$over=@((-$bend*.25),0,0);$end=@(0,0,0)}}
        $clip.bones["${side}_shin"]=@{rotation=[ordered]@{'0.0'=@(0,0,0);$prep=@($bend,0,0);$contact=@(0,0,0);$over=@(($bend*.5),0,0);$end=@(0,0,0)}}
        $clip.bones["${side}_foot"]=@{rotation=[ordered]@{'0.0'=@(0,0,0);$prep=@((-$bend*.5),0,0);$contact=@(0,0,0);$over=@((-$bend*.25),0,0);$end=@(0,0,0)}}
    }
}
# Sweep through contact, then lower into guard instead of reversing the entire
# swing at once. Elbow and wrist settle after the shoulder, like Ignis's recovery.
$animations.animations['animation.guardian.cleave'].bones.right_arm.rotation=[ordered]@{'0.0'=@(0,0,0);'0.2'=@(-8,-32,-6);'0.36'=@(-5,-55,-4);'0.55'=@(0,17,0);'0.73'=@(8,55,8);'0.96'=@(6,18,5);'1.15'=@(0,0,0)}
$animations.animations['animation.guardian.cleave'].bones.right_hand.rotation=[ordered]@{'0.0'=@(-42,0,3);'0.24'=@(-18,-6,-6);'0.4'=@(2,-4,-4);'0.55'=@(12,0,0);'0.77'=@(24,7,6);'0.99'=@(-26,3,4);'1.15'=@(-42,0,3)}
# A long blade cannot follow a player-sized sword's deep downward recovery.
# Preserve the hit pose, then let yaw/roll carry the cut outward as the wrist lifts.
$animations.animations['animation.guardian.slash'].bones.right_arm.rotation=[ordered]@{'0.0'=@(0,0,0);'0.35'=@(-76,-10,-8);'0.65'=@(-94,-8,-5);'0.82'=@(-82,-4,-2);'1.0'=@(0,0,0);'1.16'=@(2,4,4);'1.4'=@(2,2,2);'1.6'=@(0,0,0)}
$animations.animations['animation.guardian.slash'].bones.right_hand.rotation=[ordered]@{'0.0'=@(-42,0,3);'0.38'=@(-28,12,-3);'0.8'=@(-15,17,0);'1.0'=@(16,17,0);'1.2'=@(12,12,2);'1.44'=@(-28,3,3);'1.6'=@(-42,0,3)}
# A deliberate lead-foot step, plant and rear-foot gather, rather than a small
# symmetrical knee dip. Model forward is -Z; upper-body contact keys stay intact.
foreach ($name in @('cleave','slash')) {
    $clip=$animations.animations["animation.guardian.$name"]
    $hit=@{cleave=.55;slash=1.0}[$name]
    $end=$clip.animation_length.ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $lift=($hit-.30).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $plant=($hit-.10).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $contact=$hit.ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $gather=($hit+.32).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
    $lead=if($name -eq 'cleave'){'left'}else{'right'}
    $rear=if($lead -eq 'left'){'right'}else{'left'}
    $clip.bones["${lead}_leg"]=@{
        position=[ordered]@{'0.0'=@(0,0,0);$lift=@(0,0,0);$plant=@(0,-2.34,0);$contact=@(0,-2.34,0);$gather=@(0,0,0);$end=@(0,0,0)}
        rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(-45,0,0);$plant=@(-28,0,0);$contact=@(-28,0,0);$gather=@(-18,0,0);$end=@(0,0,0)}
    }
    $clip.bones["${lead}_shin"].rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(70,0,0);$plant=@(28,0,0);$contact=@(28,0,0);$gather=@(36,0,0);$end=@(0,0,0)}
    $clip.bones["${lead}_foot"].rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(-25,0,0);$plant=@(0,0,0);$contact=@(0,0,0);$gather=@(-18,0,0);$end=@(0,0,0)}
    $clip.bones["${rear}_leg"]=@{
        position=[ordered]@{'0.0'=@(0,0,0);$lift=@(0,0,0);$plant=@(0,-2.6,0);$contact=@(0,-2.6,0);$gather=@(0,0,0);$end=@(0,0,0)}
        rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(-8,0,0);$plant=@(18,0,0);$contact=@(18,0,0);$gather=@(-22,0,0);$end=@(0,0,0)}
    }
    $clip.bones["${rear}_shin"].rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(16,0,0);$plant=@(8,0,0);$contact=@(8,0,0);$gather=@(44,0,0);$end=@(0,0,0)}
    $clip.bones["${rear}_foot"].rotation=[ordered]@{'0.0'=@(0,0,0);$lift=@(-8,0,0);$plant=@(-26,0,0);$contact=@(-26,0,0);$gather=@(-22,0,0);$end=@(0,0,0)}
}
# Bake monotone cubic Hermite curves to ordinary GeckoLib vector keyframes.
# Continuous velocity at shared keys; zero tangents at reversals avoid overshoot.
foreach ($name in @('idle','walk','follow','cleave','slash','crescent')) {
    $clip = $animations.animations["animation.guardian.$name"]
    foreach ($bone in $clip.bones.Values) {
      foreach ($channel in @('rotation','position')) {
        if (!$bone[$channel] -or $bone[$channel] -isnot [System.Collections.IDictionary]) { continue }
        $track=$bone[$channel]; $keys=@($track.Keys); $times=@($keys | ForEach-Object { [double]::Parse($_,[Globalization.CultureInfo]::InvariantCulture) })
        $result=[ordered]@{}
        for ($i=0; $i -lt $keys.Count-1; $i++) {
            $dt=$times[$i+1]-$times[$i]; $steps=[Math]::Max(1,[Math]::Ceiling($dt*60))
            for ($j=0; $j -lt $steps; $j++) {
                $u=$j/$steps; $value=@()
                for ($axis=0; $axis -lt 3; $axis++) {
                    $a=$track[$keys[$i]][$axis]; $b=$track[$keys[$i+1]][$axis]; $s=($b-$a)/$dt
                    $m0=0.0; $m1=0.0
                    if ($i -gt 0) { $p=($a-$track[$keys[$i-1]][$axis])/($times[$i]-$times[$i-1]); if ($p*$s -gt 0) { $m0=2*$p*$s/($p+$s) } }
                    if ($i+2 -lt $keys.Count) { $n=($track[$keys[$i+2]][$axis]-$b)/($times[$i+2]-$times[$i+1]); if ($n*$s -gt 0) { $m1=2*$n*$s/($n+$s) } }
                    $value += (2*$u*$u*$u-3*$u*$u+1)*$a+($u*$u*$u-2*$u*$u+$u)*$dt*$m0+(-2*$u*$u*$u+3*$u*$u)*$b+($u*$u*$u-$u*$u)*$dt*$m1
                }
                $time=($times[$i]+$u*$dt).ToString('0.0#####',[Globalization.CultureInfo]::InvariantCulture)
                $result[$time]=$value
            }
        }
        $result[$keys[-1]]=$track[$keys[-1]]; $bone[$channel]=$result
      }
    }
}
# Solve sole height at every baked frame, not just the planted contact keys.
# Interpolated joint shortening otherwise lets a foot sink between valid poses.
foreach ($name in @('cleave','slash')) {
    $clip=$animations.animations["animation.guardian.$name"]
    $hit=@{cleave=.55;slash=1.0}[$name]
    $lead=if($name -eq 'cleave'){'left'}else{'right'}
    foreach ($side in @('left','right')) {
        $leg=$clip.bones["${side}_leg"]
        $shin=$clip.bones["${side}_shin"]
        $foot=$clip.bones["${side}_foot"]
        $leg.position=[ordered]@{}
        foreach ($key in $leg.rotation.Keys) {
            $t=[double]::Parse($key,[Globalization.CultureInfo]::InvariantCulture)
            $hip=$leg.rotation[$key][0]; $knee=$shin.rotation[$key][0]
            $foot.rotation[$key]=@((-$hip-$knee),0,0)
            $liftHeight=0.0
            if ($side -eq $lead -and $t -lt $hit-.10) {
                $peak=$hit-.30; $land=$hit-.10
                $u=if($t -le $peak){$t/$peak}else{($land-$t)/($land-$peak)}
                $liftHeight=7*$u*$u*(3-2*$u)
            } elseif ($t -gt $hit+.08) {
                $start=$hit+.08; $peak=$hit+.32; $finish=$clip.animation_length
                $u=if($t -le $peak){($t-$start)/($peak-$start)}else{($finish-$t)/($finish-$peak)}
                $liftHeight=if($side -eq $lead){2}else{5}
                $liftHeight=$liftHeight*$u*$u*(3-2*$u)
            }
            $shortening=36-20*[Math]::Cos($hip*[Math]::PI/180)-16*[Math]::Cos(($hip+$knee)*[Math]::PI/180)
            $leg.position[$key]=@(0,($liftHeight-$shortening),0)
        }
    }
}
Write-AssetJson (Join-Path $animationDir 'guardian.animation.json') $animations

Add-Type -AssemblyName System.Drawing
Add-Type -Path (Join-Path $PSScriptRoot 'CrimsonTexturePainter.cs') -ReferencedAssemblies System.Drawing
[CrimsonTexturePainter]::Paint((Join-Path $entityTextureDir 'guardian.png'), (Join-Path $entityTextureDir 'guardian_glowmask.png'))

& (Join-Path $PSScriptRoot 'generate-spell-icon.ps1')
