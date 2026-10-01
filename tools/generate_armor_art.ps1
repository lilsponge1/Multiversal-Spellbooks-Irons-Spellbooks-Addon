$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '..\src\main\resources\assets\irons_ultimate_explosion'
$geoPath = Join-Path $assetRoot 'geo\cinderstar_armor.geo.json'
$skinPath = Join-Path $assetRoot 'textures\models\armor\cinderstar.png'
$iconRoot = Join-Path $assetRoot 'textures\item'
New-Item -ItemType Directory -Force -Path (Split-Path $geoPath), (Split-Path $skinPath), $iconRoot | Out-Null

# Each face samples a hand-colored tile. Reusing a compact palette keeps the
# many voxel parts consistent without borrowing another mod's armor texture.
$palette = [ordered]@{
    ink       = '#201921'
    charcoal  = '#302634'
    shade     = '#4b2937'
    wine      = '#7b293a'
    crimson   = '#b53c47'
    highlight = '#d65b58'
    gold      = '#dcad4a'
    paleGold  = '#f3da87'
    leather   = '#63432f'
    darkBrown = '#392b2b'
    ivory     = '#e7d3aa'
    ember     = '#ff8b42'
    cream     = '#f4e8d1'
    pearl     = '#eee8e4'
    slate     = '#3a3b4f'
    orange    = '#ee7134'
    brightOrange = '#ffab4f'
}
$tiles = @{}
$atlas = [System.Drawing.Bitmap]::new(64, 64)
try {
    $i = 0
    foreach ($name in $palette.Keys) {
        $tx = ($i % 8) * 8; $ty = [int][Math]::Floor($i / 8) * 8
        $tiles[$name] = @($tx, $ty)
        $base = [System.Drawing.ColorTranslator]::FromHtml($palette[$name])
        for ($y = 0; $y -lt 8; $y++) { for ($x = 0; $x -lt 8; $x++) {
            $jitter = (($x * 3 + $y * 5 + $i * 7) % 7) - 3
            $r = [Math]::Clamp($base.R + $jitter, 0, 255)
            $g = [Math]::Clamp($base.G + $jitter, 0, 255)
            $b = [Math]::Clamp($base.B + $jitter, 0, 255)
            $atlas.SetPixel($tx + $x, $ty + $y, [System.Drawing.Color]::FromArgb($r, $g, $b))
        }}
        $i++
    }
    $atlas.Save($skinPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $atlas.Dispose() }

function Face($name) {
    $t = $tiles[$name]
    return [ordered]@{ uv = @(([int]$t[0] + 1), ([int]$t[1] + 1)); uv_size = @(6, 6) }
}
function Cube($origin, $size, $front, $side = $front, $top = $front, $bottom = $side) {
    return [ordered]@{
        origin = $origin
        size = $size
        uv = [ordered]@{
            north = (Face $front); east = (Face $side); south = (Face $side)
            west = (Face $side); up = (Face $top); down = (Face $bottom)
        }
    }
}
$bones = [System.Collections.Generic.List[object]]::new()
function Bone($name, $pivot, $parent, $cubes) {
    $b = [ordered]@{ name = $name; pivot = $pivot }
    if ($parent) { $b.parent = $parent }
    if ($cubes) { $b.cubes = @($cubes) }
    $script:bones.Add($b)
}

# Hat: a wide connected brim, dark tapering crown, crimson band and gold buckle.
Bone 'bipedHead' @(0,24,0) $null $null
Bone 'armorHead' @(0,24,0) 'bipedHead' @(
    (Cube @(-10,31,-10) @(20,0.8,20) 'ink' 'charcoal' 'shade'),
    (Cube @(-6,31.8,-6) @(12,3.2,12) 'charcoal' 'ink' 'shade'),
    (Cube @(-6.2,34,-6.2) @(12.4,1.2,12.4) 'wine' 'shade' 'crimson'),
    (Cube @(-1,34,-6.45) @(2,1.2,0.45) 'paleGold' 'gold' 'paleGold'),
    (Cube @(-4.8,35,-4.8) @(9.6,3.2,9.6) 'charcoal' 'ink' 'shade'),
    (Cube @(-3.4,38.2,-3.4) @(7.2,3.2,7.2) 'charcoal' 'ink' 'shade'),
    (Cube @(-1.8,41.4,-2.4) @(4.6,3.2,4.6) 'charcoal' 'ink' 'shade'),
    (Cube @(0,44.6,-1.6) @(2.5,2.4,2.5) 'charcoal' 'ink' 'shade')
)

# The cream shoulder panels and asymmetric leggings echo the reference outfit.
# The model deliberately has no hair or face geometry: those remain the skin.
Bone 'bipedBody' @(0,24,0) $null $null
Bone 'armorBody' @(0,24,0) 'bipedBody' @(
    (Cube @(-4.5,12,-2.65) @(9,12,5.3) 'crimson' 'wine' 'highlight'),
    (Cube @(-4.55,21.4,-2.8) @(9.1,2.6,5.6) 'cream' 'ivory' 'cream'),
    (Cube @(-4.55,17.8,-3.05) @(9.1,3.7,0.5) 'crimson' 'wine' 'highlight'),
    (Cube @(-1.8,16.2,-3.25) @(3.6,6.4,0.55) 'ink' 'charcoal' 'slate'),
    (Cube @(-0.45,20.4,-3.45) @(0.9,2,0.55) 'pearl' 'pearl' 'cream'),
    (Cube @(-0.55,17.8,-3.55) @(1.1,1.4,0.6) 'pearl' 'pearl' 'cream'),
    (Cube @(-4.6,13.2,-3.2) @(9.2,1.05,0.8) 'gold' 'paleGold' 'paleGold'),
    (Cube @(-1.1,13,-3.45) @(2.2,1.4,0.6) 'paleGold' 'gold' 'paleGold'),
    (Cube @(-4.7,11.7,-2.9) @(9.4,1.4,5.8) 'wine' 'shade' 'crimson')
)
Bone 'armorLeggingTorsoLayer' @(0,24,0) 'bipedBody' @(
    (Cube @(-4.45,10.7,-2.65) @(8.9,1.4,5.3) 'wine' 'shade' 'crimson')
)
Bone 'bipedRightArm' @(-5,22,0) $null $null
Bone 'armorRightArm' @(-5,22,0) 'bipedRightArm' @(
    (Cube @(-8.4,12,-2.5) @(3.9,12,5) 'crimson' 'wine' 'highlight'),
    (Cube @(-8.5,20,-2.55) @(4,4.1,5.1) 'cream' 'ivory' 'cream'),
    (Cube @(-8.5,17.3,-2.6) @(4,0.85,5.2) 'gold' 'gold' 'paleGold'),
    (Cube @(-8.5,11.8,-2.6) @(4,1.25,5.2) 'slate' 'ink' 'charcoal')
)
Bone 'bipedLeftArm' @(5,22,0) $null $null
Bone 'armorLeftArm' @(5,22,0) 'bipedLeftArm' @(
    (Cube @(4.5,12,-2.5) @(3.9,12,5) 'crimson' 'wine' 'highlight'),
    (Cube @(4.5,20,-2.55) @(4,4.1,5.1) 'cream' 'ivory' 'cream'),
    (Cube @(4.5,17.3,-2.6) @(4,0.85,5.2) 'gold' 'gold' 'paleGold'),
    (Cube @(4.5,11.8,-2.6) @(4,1.25,5.2) 'slate' 'ink' 'charcoal')
)
Bone 'bipedRightLeg' @(-1.9,12,0) $null $null
Bone 'armorRightLeg' @(-1.9,12,0) 'bipedRightLeg' @(
    (Cube @(-4.05,0,-2.15) @(4.2,12.1,4.3) 'pearl' 'ivory' 'cream'),
    (Cube @(-4.5,9.6,-2.7) @(4.6,2.9,5.4) 'crimson' 'wine' 'highlight'),
    (Cube @(-4.5,9.25,-2.75) @(4.6,0.7,5.5) 'gold' 'paleGold' 'paleGold')
)
Bone 'armorRightBoot' @(-1.9,12,0) 'bipedRightLeg' @(
    (Cube @(-4.35,-0.2,-2.4) @(4.7,5.2,5.1) 'orange' 'ember' 'brightOrange'),
    (Cube @(-4.4,4,-2.5) @(4.8,0.8,5.2) 'gold' 'paleGold' 'paleGold'),
    (Cube @(-4.35,-0.25,-3.1) @(4.7,1.2,1.3) 'brightOrange' 'orange' 'brightOrange')
)
Bone 'bipedLeftLeg' @(1.9,12,0) $null $null
Bone 'armorLeftLeg' @(1.9,12,0) 'bipedLeftLeg' @(
    (Cube @(-0.15,0,-2.15) @(4.2,12.1,4.3) 'slate' 'charcoal' 'shade'),
    (Cube @(-0.1,9.6,-2.7) @(4.6,2.9,5.4) 'wine' 'shade' 'crimson'),
    (Cube @(-0.15,9.25,-2.75) @(4.7,0.7,5.5) 'gold' 'paleGold' 'paleGold')
)
Bone 'armorLeftBoot' @(1.9,12,0) 'bipedLeftLeg' @(
    (Cube @(-0.35,-0.2,-2.4) @(4.7,5.2,5.1) 'orange' 'ember' 'brightOrange'),
    (Cube @(-0.4,4,-2.5) @(4.8,0.8,5.2) 'gold' 'paleGold' 'paleGold'),
    (Cube @(-0.35,-0.25,-3.1) @(4.7,1.2,1.3) 'brightOrange' 'orange' 'brightOrange')
)
$geometry = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{
            identifier = 'geometry.irons_ultimate_explosion.cinderstar_armor'
            texture_width = 64; texture_height = 64
            visible_bounds_width = 4; visible_bounds_height = 5.5
            visible_bounds_offset = @(0,2.25,0)
        }
        bones = @($bones.ToArray())
    })
}
[System.IO.File]::WriteAllText($geoPath, ($geometry | ConvertTo-Json -Depth 60), [System.Text.UTF8Encoding]::new($false))

function Color($hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Polygon($graphics, $brush, [int[]]$coords) {
    $points = [System.Drawing.Point[]]::new($coords.Length / 2)
    for ($i = 0; $i -lt $points.Length; $i++) { $points[$i] = [System.Drawing.Point]::new($coords[2*$i],$coords[2*$i+1]) }
    $graphics.FillPolygon($brush, $points)
}
$red = [System.Drawing.SolidBrush]::new((Color '#ad3849'))
$dark = [System.Drawing.SolidBrush]::new((Color '#29212c'))
$gold = [System.Drawing.SolidBrush]::new((Color '#e9bb59'))
$shade = [System.Drawing.SolidBrush]::new((Color '#612f3b'))
$brown = [System.Drawing.SolidBrush]::new((Color '#5a3a2f'))
$cream = [System.Drawing.SolidBrush]::new((Color '#f4e8d1'))
$pearl = [System.Drawing.SolidBrush]::new((Color '#eee8e4'))
$orange = [System.Drawing.SolidBrush]::new((Color '#ee7134'))
try {
    foreach ($kind in @('hat','robe','leggings','boots')) {
        $bitmap = [System.Drawing.Bitmap]::new(32,32)
        $g = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $g.Clear([System.Drawing.Color]::Transparent)
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
            switch ($kind) {
                'hat' {
                    Polygon $g $dark @(3,23, 29,23, 27,27, 5,27)
                    Polygon $g $dark @(9,22, 13,9, 20,4, 18,10, 23,22)
                    Polygon $g $shade @(9,19, 23,19, 24,22, 8,22)
                    $g.FillRectangle($gold, 16,19,4,3)
                }
                'robe' {
                    Polygon $g $cream @(10,5, 22,5, 25,11, 7,11)
                    Polygon $g $red @(9,9, 23,9, 27,29, 5,29)
                    Polygon $g $red @(7,10, 11,12, 8,22, 3,21)
                    Polygon $g $red @(21,12, 25,10, 29,21, 24,22)
                    $g.FillRectangle($cream, 4,9,5,4)
                    $g.FillRectangle($cream, 23,9,5,4)
                    $g.FillRectangle($dark, 13,10,6,12)
                    $g.FillRectangle($pearl, 15,12,2,3)
                    $g.FillRectangle($pearl, 15,18,2,2)
                    $g.FillRectangle($gold, 3,17,6,2)
                    $g.FillRectangle($gold, 23,17,6,2)
                    $g.FillRectangle($gold, 7,25,18,2)
                }
                'leggings' {
                    $g.FillRectangle($dark, 8,5,16,8)
                    Polygon $g $red @(7,10, 25,10, 24,17, 8,17)
                    $g.FillRectangle($pearl, 9,16,6,12)
                    $g.FillRectangle($dark, 18,16,6,12)
                    $g.FillRectangle($gold, 8,15,17,2)
                }
                'boots' {
                    $g.FillRectangle($orange, 7,7,7,17)
                    $g.FillRectangle($orange, 18,7,7,17)
                    $g.FillRectangle($orange, 5,22,11,5)
                    $g.FillRectangle($orange, 17,22,11,5)
                    $g.FillRectangle($gold, 7,10,7,2)
                    $g.FillRectangle($gold, 18,10,7,2)
                }
            }
            $g.FillRectangle($gold, 2,29,3,1)
        } finally { $g.Dispose() }
        try { $bitmap.Save((Join-Path $iconRoot "cinderstar_$kind.png"), [System.Drawing.Imaging.ImageFormat]::Png) }
        finally { $bitmap.Dispose() }
    }
} finally { $red.Dispose(); $dark.Dispose(); $gold.Dispose(); $shade.Dispose(); $brown.Dispose(); $cream.Dispose(); $pearl.Dispose(); $orange.Dispose() }
