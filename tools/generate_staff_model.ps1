$ErrorActionPreference = 'Stop'

$assetRoot = Join-Path $PSScriptRoot '..\src\main\resources\assets\irons_ultimate_explosion'
$modelPath = Join-Path $assetRoot 'models\item\cinderstar_staff.json'
$palettePath = Join-Path $assetRoot 'textures\item\cinderstar_palette.png'

# Each palette column is a solid Minecraft texel. Keeping material colors in one
# texture makes every model part use the same palette without a custom renderer.
$colors = @(
    '#3B2523', # 0 charred wood
    '#E3D4AA', # 1 pale rune shaft
    '#C99539', # 2 gold
    '#65443B', # 3 bronze
    '#A60B1B', # 4 crystal dark red
    '#F24724', # 5 crystal ember
    '#302A2C', # 6 blackened bronze
    '#FFB74D', # 7 hot highlight
    '#80503A', # 8 wood grain
    '#B9AA87', # 9 shaft shadow
    '#D51B20', # 10 crystal red
    '#9D694A', # 11 bronze highlight
    '#FF7950', # 12 crystal glint
    '#660613', # 13 crystal shadow
    '#4D3938', # 14 metal shadow
    '#7B541F'  # 15 gold shadow
)

Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
for ($x = 0; $x -lt 16; $x++) {
    $color = [System.Drawing.ColorTranslator]::FromHtml($colors[$x])
    for ($y = 0; $y -lt 16; $y++) { $bitmap.SetPixel($x, $y, $color) }
}
$bitmap.Save($palettePath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

$elements = [System.Collections.Generic.List[object]]::new()
function Add-Part([double[]]$from, [double[]]$to, [int]$material) {
    $uv = @($material, 0, ($material + 1), 1)
    $faces = @{}
    foreach ($direction in @('north', 'south', 'east', 'west', 'up', 'down')) {
        $faces[$direction] = @{ uv = $uv; texture = '#palette' }
    }
    $elements.Add(@{ from = $from; to = $to; faces = $faces })
}

# A continuous grip and shaft. Bands overlap the shaft so no cubes hover.
Add-Part @(6.9,0,6.9) @(9.1,1.1,9.1) 3
Add-Part @(7.2,0.8,7.2) @(8.8,8.4,8.8) 0
Add-Part @(7.0,2.0,7.0) @(9.0,2.5,9.0) 8
Add-Part @(7.0,6.8,7.0) @(9.0,7.4,9.0) 2
Add-Part @(7.25,7.4,7.25) @(8.75,22.9,8.75) 1
Add-Part @(7.15,11.1,7.15) @(8.85,11.5,8.85) 9
Add-Part @(7.15,16.3,7.15) @(8.85,16.7,8.85) 9
Add-Part @(7.05,21.8,7.05) @(8.95,22.5,8.95) 2

# A single connected C-shaped head, built as overlapping stepped metalwork.
Add-Part @(6.1,22.6,6.6) @(9.9,24.4,9.4) 3
Add-Part @(5.1,23.5,6.7) @(7.1,25.5,9.3) 6
Add-Part @(4.0,25.0,6.8) @(5.8,28.7,9.2) 6
Add-Part @(4.0,28.0,6.8) @(6.8,29.5,9.2) 3
Add-Part @(5.0,23.8,6.5) @(6.4,24.7,6.8) 11
Add-Part @(4.0,25.4,6.55) @(4.7,28.3,6.85) 11
Add-Part @(9.0,23.5,6.7) @(11.7,25.4,9.3) 6
Add-Part @(10.9,24.8,6.8) @(12.5,29.9,9.2) 6
Add-Part @(10.0,29.0,6.8) @(13.4,30.5,9.2) 3
Add-Part @(12.6,30.0,6.9) @(15.0,31.1,9.1) 6
Add-Part @(11.8,25.2,6.55) @(12.5,29.4,6.85) 11
Add-Part @(10.4,29.8,6.55) @(13.2,30.5,6.85) 11
Add-Part @(8.6,29.1,7.1) @(11.3,30.0,8.9) 3
Add-Part @(8.2,28.4,7.3) @(9.2,29.7,8.7) 2
Add-Part @(7.2,23.7,6.45) @(8.8,24.2,6.75) 2

# Suspended faceted ember crystal, large enough to read at inventory scale.
Add-Part @(7.4,26.0,7.0) @(9.9,28.6,9.0) 13
Add-Part @(7.0,26.5,7.35) @(10.2,28.1,8.65) 4
Add-Part @(7.6,26.2,6.7) @(9.6,28.5,7.05) 10
Add-Part @(8.0,27.7,6.48) @(9.4,28.55,6.72) 5
Add-Part @(8.2,28.1,6.32) @(8.8,28.55,6.5) 12
Add-Part @(8.0,25.4,7.4) @(9.2,26.2,8.6) 4

$model = [ordered]@{
    texture_size = @(16,16)
    textures = @{ palette = 'irons_ultimate_explosion:item/cinderstar_palette'; particle = 'irons_ultimate_explosion:item/cinderstar_palette' }
    elements = $elements
    display = [ordered]@{
        # Counter the downward arm pose and place the hand near the lower middle of the shaft.
        thirdperson_righthand = @{ rotation = @(0,0,40); translation = @(0,-4,0); scale = @(0.94,0.94,0.94) }
        thirdperson_lefthand = @{ rotation = @(0,0,-40); translation = @(0,-4,0); scale = @(0.94,0.94,0.94) }
        firstperson_righthand = @{ rotation = @(0,0,0); translation = @(2,-2,0); scale = @(0.66,0.66,0.66) }
        firstperson_lefthand = @{ rotation = @(0,0,0); translation = @(-2,-2,0); scale = @(0.66,0.66,0.66) }
        gui = @{ rotation = @(30,225,0); translation = @(0,-2,0); scale = @(0.48,0.48,0.48) }
        ground = @{ translation = @(0,3,0); scale = @(0.4,0.4,0.4) }
        fixed = @{ rotation = @(0,180,0); translation = @(0,-2,0); scale = @(0.55,0.55,0.55) }
    }
}
$json = $model | ConvertTo-Json -Depth 12
[System.IO.File]::WriteAllText((Resolve-Path $modelPath).Path, $json + "`n", [System.Text.UTF8Encoding]::new($false))
