$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '..\src\main\resources\assets\irons_ultimate_explosion'
$modelPath = Join-Path $assetRoot 'geo\thunderstar_armor.geo.json'
$texturePath = Join-Path $assetRoot 'textures\models\armor\thunderstar.png'
$glowPath = Join-Path $assetRoot 'textures\models\armor\thunderstar_glowmask.png'
$iconPath = Join-Path $assetRoot 'textures\item\cuirass_of_the_thunderstar.png'
New-Item -ItemType Directory -Force -Path (Split-Path $modelPath), (Split-Path $texturePath), (Split-Path $iconPath) | Out-Null

# Original pixel-art materials and cuboid geometry, designed around the supplied
# layered pale cuirass, blue shoulder fabric and gold fasteners. No borrowed textures.
$palette = [ordered]@{
    gunmetal='#303844'; shadow='#171f2b'; silver='#99a4af'; edge='#ced3ce'
    ivory='#e1dcc0'; ivoryShade='#b0ac91'; gold='#bc9148'; goldLight='#efca77'
    blue='#456b8d'; fabric='#253e60'; energy='#83d0ec'; darkGold='#72592f'
    goldDisc='#cfab60'
}
$tiles=@{}; $texture=[Drawing.Bitmap]::new(256,128); $glow=[Drawing.Bitmap]::new(256,128)
try {
    $index=0
    foreach ($name in $palette.Keys) {
        $tileX=($index%8)*32; $tileY=[int][Math]::Floor($index/8)*32; $tiles[$name]=@($tileX,$tileY)
        $base=[Drawing.ColorTranslator]::FromHtml($palette[$name])
        for ($y=0;$y -lt 32;$y++) { for ($x=0;$x -lt 32;$x++) {
            $shade=[int](12-0.65*$y+5*[Math]::Cos(($x-9)*[Math]::PI/31))
            if ($x -lt 3 -or $y -lt 3) { $shade+=16 }
            if ($x -gt 28 -or $y -gt 28) { $shade-=20 }
            if ($name -in @('fabric','blue')) { $shade=[int](10*[Math]::Cos(($x-5)*[Math]::PI/13)-0.15*$y) }
            if ($name -eq 'energy') { $shade=[int](23-1.2*$y) }
            if ($name -eq 'goldDisc') {
                $radius=[Math]::Sqrt([Math]::Pow($x-15.5,2)+[Math]::Pow($y-15.5,2))
                if ($radius -gt 14.5) { continue }
                $shade=if ($radius -gt 13) { 24 } elseif ($radius -gt 10.5) { -30 } else { 7 }
                $shade += [int](16*(15.5-$y)/16+9*(15.5-$x)/16)
            }
            $r=[Math]::Clamp($base.R+$shade,0,255); $g=[Math]::Clamp($base.G+$shade,0,255); $b=[Math]::Clamp($base.B+$shade,0,255)
            $texture.SetPixel($tileX+$x,$tileY+$y,[Drawing.Color]::FromArgb(255,$r,$g,$b))
            if ($name -eq 'energy') { $glow.SetPixel($tileX+$x,$tileY+$y,[Drawing.Color]::FromArgb(255,$r,$g,$b)) }
        }}
        $index++
    }
    $texture.Save($texturePath,[Drawing.Imaging.ImageFormat]::Png)
    $glow.Save($glowPath,[Drawing.Imaging.ImageFormat]::Png)
} finally { $texture.Dispose(); $glow.Dispose() }

function Face($material) {
    $tile=$tiles[$material]
    return [ordered]@{ uv=@(($tile[0]+1),($tile[1]+1)); uv_size=@(30,30) }
}
function Cube($origin,$size,$front,$side=$front,$top=$front,$rotation=$null) {
    $part=[ordered]@{ origin=$origin; size=$size; uv=[ordered]@{
        north=(Face $front); south=(Face $side); east=(Face $side); west=(Face $side); up=(Face $top); down=(Face $side)
    }}
    if ($rotation) { $part.rotation=$rotation; $part.pivot=@(($origin[0]+$size[0]/2),($origin[1]+$size[1]/2),($origin[2]+$size[2]/2)) }
    return $part
}
$bones=[Collections.Generic.List[object]]::new()
function Bone($name,$pivot,$parent,$cubes=$null) {
    $bone=[ordered]@{ name=$name; pivot=$pivot }
    if ($parent) { $bone.parent=$parent }; if ($cubes) { $bone.cubes=@($cubes) }
    $script:bones.Add($bone)
}
Bone 'bipedBody' @(0,24,0) $null
$body=[Collections.Generic.List[object]]::new()
$body.Add((Cube @(-4.4,12,-2.45) @(8.8,11.8,4.9) 'gunmetal' 'shadow' 'silver'))
$body.Add((Cube @(-3.8,22.8,1.7) @(7.6,2.7,1.0) 'blue' 'fabric' 'silver'))
foreach ($sign in @(-1,1)) {
    $x=if ($sign -lt 0) { -4.2 } else { 2.9 }
    $body.Add((Cube @($x,22.8,-1.3) @(1.3,2.25,3.0) 'blue' 'fabric' 'silver' @(0,0,(-9*$sign))))
}
$body.Add((Cube @(-3.7,18.5,-3.15) @(7.4,4.25,0.65) 'ivory' 'ivoryShade' 'edge'))
$body.Add((Cube @(-3.0,16.1,-3.25) @(6,2.5,0.65) 'ivory' 'ivoryShade' 'edge'))
$body.Add((Cube @(-1.95,14.1,-3.30) @(3.9,2.5,0.6) 'ivory' 'ivoryShade' 'edge'))
$body.Add((Cube @(-0.8,13.8,-3.4) @(1.6,1.2,0.5) 'ivory' 'ivoryShade' 'goldLight' @(0,0,45)))
foreach ($sign in @(-1,1)) {
    $x=if ($sign -lt 0) { -3.9 } else { 2.15 }
    $body.Add((Cube @($x,15.1,-3.45) @(1.75,3,0.55) 'ivory' 'ivoryShade' 'edge' @(0,0,(-24*$sign))))
    $x=if ($sign -lt 0) { -4.5 } else { 2.6 }
    $body.Add((Cube @($x,13.2,-2.9) @(1.9,3.15,0.55) 'blue' 'fabric' 'silver' @(0,0,(-15*$sign))))
}
# Faceted overlapping breast plates around an inset stormstar core.
$body.Add((Cube @(-4,19.4,-3.4) @(3.1,2.5,0.5) 'ivory' 'ivoryShade' 'edge' @(0,0,-12)))
$body.Add((Cube @(0.9,19.4,-3.4) @(3.1,2.5,0.5) 'ivory' 'ivoryShade' 'edge' @(0,0,12)))
$body.Add((Cube @(-0.62,19.45,-3.85) @(1.24,1.24,0.18) 'goldLight' 'gold' 'goldLight' @(0,0,45)))
$body.Add((Cube @(-0.18,19.8,-4.08) @(0.36,0.5,0.1) 'energy'))
$body.Add((Cube @(-4.6,12,-2.8) @(9.2,1.05,5.6) 'darkGold' 'gold' 'goldLight'))
$body.Add((Cube @(-1.7,11.95,-3.65) @(2.15,1.4,0.55) 'goldLight' 'gold' 'goldLight' @(0,0,8)))
$body.Add((Cube @(-1.35,12.25,-3.82) @(1.35,0.65,0.18) 'darkGold'))
foreach ($sign in @(-1,1)) {
    $x=if ($sign -lt 0) { -5.35 } else { 2.4 }
    $body.Add((Cube @($x,10.4,-2.95) @(2.95,2.0,0.6) 'ivory' 'silver' 'edge' @(0,0,(-23*$sign))))
    $body.Add((Cube @($x,10.1,-3.0) @(2.95,0.28,0.5) 'gold' 'darkGold' 'goldLight' @(0,0,(-23*$sign))))
}
# Pair of raised gold discs and the reference's split crest silhouette.
foreach ($x in @(-2.85,2.85)) {
    $body.Add((Cube @(($x-1.12),20.05,-3.88) @(2.24,1.5,0.40) 'gold' 'darkGold' 'goldLight'))
    $body.Add((Cube @(($x-0.75),19.68,-3.88) @(1.5,2.24,0.40) 'gold' 'darkGold' 'goldLight'))
    $body.Add((Cube @(($x-1.1),19.7,-4.03) @(2.2,2.2,0.10) 'goldDisc' 'gold' 'goldLight'))
    for ($i=0;$i -lt 3;$i++) {
        $direction=[Math]::Sign($x)
        $body.Add((Cube @((($x-0.25)-$direction*$i*0.38),(18.1-$i*0.63),-3.98) @(0.65,0.22,0.13) 'darkGold' 'gold' 'goldLight' @(0,0,(-24*$direction))))
    }
}
for ($i=0;$i -lt 3;$i++) {
    $body.Add((Cube @((-1.1+$i*0.3),(17.0-$i*0.65),-4) @((2.2-$i*0.6),0.2,0.14) 'darkGold' 'gold' 'goldLight'))
}
# Back view: mirrored layered plates, protected spine and two narrow energy vents.
$body.Add((Cube @(-4,17,2.55) @(8,5.7,0.65) 'gunmetal' 'silver' 'edge'))
$body.Add((Cube @(-3.4,18.1,3.05) @(6.8,3.4,0.45) 'silver' 'gunmetal' 'edge'))
$body.Add((Cube @(-0.4,15.8,3.3) @(0.8,5.5,0.3) 'gold' 'darkGold' 'goldLight'))
foreach ($sign in @(-1,1)) {
    $x=if ($sign -lt 0) { -3.7 } else { 0.6 }
    $body.Add((Cube @($x,19.4,3.05) @(3.1,2.7,0.38) 'silver' 'ivory' 'edge' @(0,0,(-12*$sign))))
}
foreach ($x in @(-2.6,2.35)) { $body.Add((Cube @($x,18.5,3.55) @(0.25,2.5,0.15) 'energy')) }
foreach ($x in @(-4.65,3.6)) {
    for ($i=0;$i -lt 3;$i++) { $body.Add((Cube @($x,(14+$i*1.5),-2.75) @(1.05,1.1,5.5) 'blue' 'gunmetal' 'silver')) }
}
# Long split cloth is added below on Iron's existing leg-following extension bones.
Bone 'armorBody' @(0,24,0) 'bipedBody' $body.ToArray()
foreach ($sign in @(-1,1)) {
    $side=if ($sign -lt 0) { 'Right' } else { 'Left' }
    $x=if ($sign -lt 0) { -9.5 } else { 4.2 }
    $outer=if ($sign -lt 0) { $x-0.25 } else { $x+3.9 }
    $arm=@(
        (Cube @($x,19.6,-3) @(5.3,4.3,6) 'blue' 'gunmetal' 'silver'),
        (Cube @(($x-0.25),22.2,-3.15) @(5.8,1.2,6.3) 'ivory' 'silver' 'edge' @(0,0,(-8*$sign))),
        (Cube @(($x+0.4),23.55,-2.55) @(4.5,0.85,5.1) 'ivory' 'ivoryShade' 'edge' @(0,0,(-16*$sign))),
        (Cube @($outer,22.8,-2.15) @(1.45,2.6,4.3) 'ivory' 'silver' 'edge' @(0,0,(-22*$sign))),
        (Cube @($x,21.0,-3.4) @(5.3,0.85,0.5) 'ivory' 'ivoryShade' 'edge' @(0,0,(-9*$sign))),
        (Cube @(($x+0.2),20.2,-3.5) @(4.9,0.85,0.45) 'ivory' 'ivoryShade' 'edge' @(0,0,(-9*$sign))),
        (Cube @(($x+0.25),19.5,-3.45) @(4.8,0.6,0.55) 'gold' 'darkGold' 'goldLight'),
        (Cube @(($x+0.9),16.8,-2.5) @(3.65,2.85,5) 'gunmetal' 'shadow' 'silver'),
        (Cube @(($x+1.1),18.1,-2.95) @(3.25,0.35,0.35) 'silver')
    )
    Bone ('biped'+$side+'Arm') @((5*$sign),22,0) $null
    Bone ('armor'+$side+'Arm') @((5*$sign),22,0) ('biped'+$side+'Arm') $arm
}
Bone 'bipedHead' @(0,24,0) $null
Bone 'armorHead' @(0,24,0) 'bipedHead'
# Same hierarchy as Iron's Infernal Sorcerer robe extensions; chest-slot cloth follows the legs.
foreach ($sign in @(-1,1)) {
    $side=if ($sign -lt 0) { 'Right' } else { 'Left' }
    Bone ('biped'+$side+'Leg') @((2*$sign),12,0) $null
    Bone ('armor'+$side+'Leg') @((2*$sign),12,0) ('biped'+$side+'Leg')
    Bone ('armor'+$side+'Boot') @((2*$sign),12,0) ('biped'+$side+'Leg')
    $x=if ($sign -lt 0) { -4.35 } else { 0.75 }
    $tail=@(
        (Cube @($x,5.1,-2.85) @(3.6,6.3,0.35) 'blue' 'fabric' 'gold'),
        (Cube @(($x+0.2),1.1,-2.65) @(3.25,4.25,0.30) 'fabric' 'blue' 'silver' @(0,0,(-5*$sign))),
        (Cube @(($x+0.1),1.05,-2.94) @(3.2,0.2,0.18) 'gold' 'darkGold' 'goldLight' @(0,0,(-5*$sign))),
        (Cube @(($x+0.3),5.3,-3.03) @(0.24,5.7,0.13) 'ivoryShade' 'gold' 'gold'),
        (Cube @(($x+2.7),5.3,-3.03) @(0.24,5.7,0.13) 'ivoryShade' 'gold' 'gold'),
        (Cube @(($x+1.1),7.0,-3.07) @(1.25,1.25,0.1) 'silver' 'blue' 'silver' @(0,0,45)),
        (Cube @($x,3.4,2.6) @(3.4,7.9,0.28) 'fabric' 'blue' 'silver')
    )
    Bone ('armorTorsoExtension'+$side+'Leg') @((2*$sign),12,0) ('biped'+$side+'Leg') $tail
}
$model=[ordered]@{ format_version='1.12.0'; 'minecraft:geometry'=@([ordered]@{
    description=[ordered]@{ identifier='geometry.irons_ultimate_explosion.thunderstar_armor'; texture_width=256; texture_height=128; visible_bounds_width=3; visible_bounds_height=3; visible_bounds_offset=@(0,1,0) }
    bones=$bones.ToArray()
}) }
[IO.File]::WriteAllText($modelPath,($model|ConvertTo-Json -Depth 15)+"`n",[Text.UTF8Encoding]::new($false))

# Inventory silhouette: discrete 32px artwork, with the same layered materials.
$icon=[Drawing.Bitmap]::new(32,32); $graphics=[Drawing.Graphics]::FromImage($icon)
function IconRect($color,$x,$y,$w,$h) {
    $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    try { $graphics.FillRectangle($brush,$x,$y,$w,$h) } finally { $brush.Dispose() }
}
try {
    $graphics.Clear([Drawing.Color]::Transparent)
    IconRect '#171f2b' 3 5 26 9; IconRect '#99a4af' 4 6 24 6
    IconRect '#d8d0b0' 6 5 6 4; IconRect '#d8d0b0' 20 5 6 4
    IconRect '#303844' 9 9 14 18; IconRect '#d8d0b0' 10 9 12 12
    IconRect '#ced3ce' 10 9 12 2; IconRect '#9a947f' 10 18 12 3
    IconRect '#29415c' 4 10 5 4; IconRect '#29415c' 23 10 5 4
    IconRect '#e0bd6e' 11 11 3 3; IconRect '#aa8442' 12 12 2 2
    IconRect '#e0bd6e' 18 11 3 3; IconRect '#aa8442' 19 12 2 2
    IconRect '#303844' 15 13 2 5; IconRect '#71c9e7' 15 14 2 2
    IconRect '#e0bd6e' 13 18 6 1; IconRect '#e0bd6e' 14 19 4 1
    IconRect '#99a4af' 10 21 12 2; IconRect '#aa8442' 9 23 14 2
    IconRect '#e0bd6e' 14 23 4 2; IconRect '#1f3048' 9 25 5 5
    IconRect '#29415c' 18 25 5 5; IconRect '#aa8442' 9 29 5 1
    IconRect '#aa8442' 18 29 5 1
    IconRect '#456b8d' 9 25 6 6; IconRect '#253e60' 17 25 6 6
    IconRect '#bc9148' 10 30 5 1; IconRect '#bc9148' 17 30 5 1
    IconRect '#e1dcc0' 4 3 3 4; IconRect '#e1dcc0' 25 3 3 4
    IconRect '#e1dcc0' 14 13 4 6; IconRect '#efca77' 15 14 2 2
    IconRect '#83d0ec' 15 14 1 1
    $icon.Save($iconPath,[Drawing.Imaging.ImageFormat]::Png)
} finally { $graphics.Dispose(); $icon.Dispose() }
