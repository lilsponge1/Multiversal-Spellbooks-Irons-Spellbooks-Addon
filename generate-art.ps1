$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assets = Join-Path $PSScriptRoot 'src\main\resources\assets\irons_ultimate_explosion'
$iconPath = Join-Path $assets 'textures\gui\spell_icons\grand_explosion.png'
New-Item -ItemType Directory -Force -Path (Split-Path $iconPath) | Out-Null

$icon = [System.Drawing.Bitmap]::new(32,32)
try {
    for($y=0; $y -lt 32; $y++) {
        for($x=0; $x -lt 32; $x++) {
            $dx=$x-15.5; $dy=$y-15.5; $r=[Math]::Sqrt($dx*$dx+$dy*$dy)
            $angle=[Math]::Atan2($dy,$dx)
            $ray=[Math]::Abs([Math]::Cos(8*$angle))
            if($r -lt 5) { $c=[System.Drawing.Color]::FromArgb(255,255,246,170) }
            elseif($r -lt 10 + 3*$ray) { $c=[System.Drawing.Color]::FromArgb(255,255,149,39) }
            elseif($r -lt 15 - 3*(1-$ray)) { $c=[System.Drawing.Color]::FromArgb(230,187,33,19) }
            elseif($r -lt 16 - 1.5*(1-$ray)) { $c=[System.Drawing.Color]::FromArgb(160,80,23,25) }
            else { $c=[System.Drawing.Color]::Transparent }
            $icon.SetPixel($x,$y,$c)
        }
    }
    $icon.Save($iconPath,[System.Drawing.Imaging.ImageFormat]::Png)
} finally { $icon.Dispose() }

# Original generated artwork; the source survives every rebuild.
$thunderPath = Join-Path $assets 'textures\gui\spell_icons\thundercrash.png'
$sourcePath = Join-Path $PSScriptRoot 'art\thundercrash-icon.png'
if (Test-Path -LiteralPath $sourcePath) {
    $source = [System.Drawing.Image]::FromFile($sourcePath)
    $thunder = [System.Drawing.Bitmap]::new(128,128)
    $graphics = [System.Drawing.Graphics]::FromImage($thunder)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $scale = 118.0 / [Math]::Max($source.Width, $source.Height)
        $width = [int][Math]::Round($source.Width * $scale)
        $height = [int][Math]::Round($source.Height * $scale)
        $rect = [System.Drawing.Rectangle]::new([int]((128-$width)/2),[int]((128-$height)/2),$width,$height)
        $graphics.DrawImage($source,$rect)
        $thunder.Save($thunderPath,[System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $thunder.Dispose(); $source.Dispose() }
    return
}

# Legacy fallback for checkouts without the artwork source.
$thunder = [System.Drawing.Bitmap]::new(32,32)
try {
    for ($y=0; $y -lt 32; $y++) { for ($x=0; $x -lt 32; $x++) {
        $distance = [Math]::Sqrt([Math]::Pow($x-20,2)+[Math]::Pow($y-11,2))
        $trail = [Math]::Abs($x+$y-31) - 0.1*($x-4)
        $color = [System.Drawing.Color]::Transparent
        if ($distance -lt 4) { $color = [System.Drawing.Color]::FromArgb(255,235,255,255) }
        elseif ($distance -lt 7) { $color = [System.Drawing.Color]::FromArgb(240,80,230,255) }
        elseif ($trail -lt 2 -and $x -gt 2 -and $x -lt 22) { $color = [System.Drawing.Color]::FromArgb(220,70,180,255) }
        elseif ($distance -lt 10 -and (($x+$y)%4 -eq 0)) { $color = [System.Drawing.Color]::FromArgb(180,100,220,255) }
        $thunder.SetPixel($x,$y,$color)
    }}
    $thunder.Save($thunderPath,[System.Drawing.Imaging.ImageFormat]::Png)
} finally { $thunder.Dispose() }
