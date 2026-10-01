param([ValidateSet('isometric','front','side')][string]$View = 'isometric')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$modelPath = Join-Path $root 'src/main/resources/assets/crimson_susanoo/geo/guardian.geo.json'
$outputDir = Join-Path $root 'preview'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$geometry = (Get-Content -LiteralPath $modelPath -Raw | ConvertFrom-Json).'minecraft:geometry'[0]
$bitmap = [System.Drawing.Bitmap]::new(700,700)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(8,4,12))
$haze = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(16,229,27,17))
for ($r = 230; $r -ge 100; $r -= 20) { $graphics.FillEllipse($haze,350-$r,360-$r,2*$r,2*$r) }
$haze.Dispose()

function Project([double]$x,[double]$y,[double]$z) {
    if ($View -eq 'front') {
        return [System.Drawing.PointF]::new([float](350 + $x * 3.0),[float](570 - $y * 3.6))
    }
    if ($View -eq 'side') {
        return [System.Drawing.PointF]::new([float](240 - $z * 3.8),[float](570 - $y * 3.6))
    }
    return [System.Drawing.PointF]::new([float](350 + ($x - $z) * 2.45),[float](570 - $y * 3.6 + ($x + $z) * 0.6))
}
function Face($a,$b,$c,$d,$color,$outline) {
    $points = [System.Drawing.PointF[]]@((Project @a),(Project @b),(Project @c),(Project @d))
    $brush = [System.Drawing.SolidBrush]::new($color)
    $pen = [System.Drawing.Pen]::new($outline,1.5)
    $graphics.FillPolygon($brush,$points)
    $graphics.DrawPolygon($pen,$points)
    $brush.Dispose();$pen.Dispose()
}
$palette = @(
    @([System.Drawing.Color]::FromArgb(205,111,9,24),[System.Drawing.Color]::FromArgb(255,247,66,22)),
    @([System.Drawing.Color]::FromArgb(240,255,108,18),[System.Drawing.Color]::FromArgb(255,255,189,57)),
    @([System.Drawing.Color]::FromArgb(170,190,77,65),[System.Drawing.Color]::FromArgb(255,244,112,77)),
    @([System.Drawing.Color]::FromArgb(190,37,6,16),[System.Drawing.Color]::FromArgb(255,116,25,34)),
    @([System.Drawing.Color]::FromArgb(255,255,223,54),[System.Drawing.Color]::FromArgb(255,255,247,155)),
    @([System.Drawing.Color]::FromArgb(250,255,52,10),[System.Drawing.Color]::FromArgb(255,255,197,57)),
    @([System.Drawing.Color]::FromArgb(220,177,12,30),[System.Drawing.Color]::FromArgb(255,255,73,40)),
    @([System.Drawing.Color]::FromArgb(120,45,7,25),[System.Drawing.Color]::FromArgb(255,116,25,34)),
    @([System.Drawing.Color]::FromArgb(245,128,13,20),[System.Drawing.Color]::FromArgb(255,241,90,18)),
    @([System.Drawing.Color]::FromArgb(250,255,105,13),[System.Drawing.Color]::FromArgb(255,255,218,73)),
    @([System.Drawing.Color]::FromArgb(250,238,73,11),[System.Drawing.Color]::FromArgb(255,255,182,38)),
    @([System.Drawing.Color]::FromArgb(255,255,220,82),[System.Drawing.Color]::FromArgb(255,255,245,180))
)
foreach ($bone in $geometry.bones) {
    foreach ($cube in $bone.cubes) {
        $x0=[double]$cube.origin[0];$x1=$x0+[double]$cube.size[0]
        $y0=[double]$cube.origin[1];$y1=$y0+[double]$cube.size[1]
        $z0=[double]$cube.origin[2];$z1=$z0+[double]$cube.size[2]
        $uv=$cube.uv.north.uv
        $index=[int][Math]::Floor([double]$uv[0]/256)+4*[int][Math]::Floor([double]$uv[1]/256)
        $base=$palette[$index][0];$edge=$palette[$index][1]
        if ($View -eq 'side') {
            Face @($x0,$y0,$z0) @($x0,$y0,$z1) @($x0,$y1,$z1) @($x0,$y1,$z0) $base $edge
        } else {
            Face @($x0,$y0,$z0) @($x1,$y0,$z0) @($x1,$y1,$z0) @($x0,$y1,$z0) $base $edge
        }
        if ($View -eq 'isometric') {
            Face @($x1,$y0,$z0) @($x1,$y0,$z1) @($x1,$y1,$z1) @($x1,$y1,$z0) $base $edge
            Face @($x0,$y1,$z0) @($x1,$y1,$z0) @($x1,$y1,$z1) @($x0,$y1,$z1) $base $edge
        }
    }
}
$font = [System.Drawing.Font]::new('Segoe UI',23,[System.Drawing.FontStyle]::Bold)
$label = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255,255,126,73))
$graphics.DrawString('CRIMSON SUSANOO', $font, $label, 174, 620)
$label.Dispose();$font.Dispose()
$file = switch ($View) {
    'front' { 'guardian-front.png' }
    'side' { 'guardian-side.png' }
    default { 'guardian-concept.png' }
}
$path = Join-Path $outputDir $file
$bitmap.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose();$bitmap.Dispose()
Write-Output $path
