$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$iconPath = Join-Path $PSScriptRoot '../src/main/resources/assets/crimson_susanoo/textures/gui/spell_icons/crimson_susanoo.png'
# Editable vector-like source, supersampled before packaging at Iron's 64px size.
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
public static class CrimsonSpellIcon {
    static Color C(string hex) { return ColorTranslator.FromHtml(hex); }
    static PointF[] P(params float[] xy) {
        PointF[] p = new PointF[xy.Length / 2];
        for (int i=0; i<p.Length; i++) p[i]=new PointF(xy[i*2],xy[i*2+1]);
        return p;
    }
    static void Poly(Graphics g, string color, params float[] xy) {
        using (var b=new SolidBrush(C(color))) g.FillPolygon(b,P(xy));
    }
    static void Stroke(Graphics g, string color, float w, params float[] xy) {
        using(var p=new Pen(C(color),w)) { p.LineJoin=LineJoin.Round; p.StartCap=LineCap.Round; p.EndCap=LineCap.Round; g.DrawLines(p,P(xy)); }
    }
    static void Flame(Graphics g, string color, float shift) {
        using(var p=new GraphicsPath()) {
            p.AddBezier(9,45,2,34,10,29,6,22);
            p.AddBezier(6,22,18,26,11,35,21,33);
            p.AddBezier(21,33,17,24,25,20,23,13);
            p.AddBezier(23,13,37,24,27,34,38,35);
            p.AddBezier(38,35,44,32,42,25,46,22);
            p.AddBezier(46,22,44,39,54,42,44,49);
            p.AddBezier(44,49,32,56,17,55,9,45);
            using(var b=new LinearGradientBrush(new PointF(0,21),new PointF(0,54),C(color),C("#570b22"))) {
                var state=g.Save(); g.TranslateTransform(0,shift); g.FillPath(b,p); g.Restore(state);
            }
        }
    }
    public static void Paint(string path) {
        using(var hi=new Bitmap(256,256,PixelFormat.Format32bppArgb))
        using(var g=Graphics.FromImage(hi)) {
            g.Clear(Color.Transparent); g.ScaleTransform(4,4);
            g.SmoothingMode=SmoothingMode.AntiAlias;
            using(var disk=new GraphicsPath()) {
                disk.AddEllipse(2,2,60,60);
                using(var glow=new PathGradientBrush(disk)) {
                    glow.CenterColor=C("#952410"); glow.SurroundColors=new[]{C("#120810")}; g.FillPath(glow,disk);
                }
            }
            Flame(g,"#ff681b",0);
            // Broad, readable armor silhouette and shoulder plates.
            Poly(g,"#260c19",7,49,17,39,35,39,46,48,43,59,10,59);
            Poly(g,"#a02a31",7,48,16,39,23,43,18,51);
            Poly(g,"#c54835",31,43,36,39,45,48,36,51);
            Stroke(g,"#ee8544",1,8,47,16,41,21,44);
            Poly(g,"#5a1729",17,45,26,42,36,45,34,57,21,59);
            // Furnace opening with an ivory-hot center.
            Poly(g,"#ff6617",23,47,29,45,32,49,28,56,24,54);
            Poly(g,"#ffd25e",25,48,29,48,30,51,27,55,25,52);
            Poly(g,"#fff3bd",27,49,29,51,27,53);
            // Horned samurai mask; strong eyes survive small spell-wheel rendering.
            Poly(g,"#26121e",14,20,21,14,30,14,38,20,37,34,32,41,25,44,17,38);
            Poly(g,"#9f2939",15,20,23,17,26,22,24,40,17,35);
            Poly(g,"#691529",26,21,30,17,37,21,35,35,26,41);
            Poly(g,"#dd593d",14,23,9,10,12,5,15,15,21,18,20,23);
            Poly(g,"#ae3739",30,18,36,13,40,5,40,15,36,24);
            Stroke(g,"#ffb358",.9f,12,7,14,16,20,20);
            Stroke(g,"#ed7b4a",.9f,38,9,36,16,32,20);
            Poly(g,"#250b17",15,25,24,27,26,25,28,27,36,25,34,32,27,34,24,33,17,32);
            Poly(g,"#ff731e",15,24,23,27,23,31,17,29,12,27);
            Poly(g,"#ff731e",29,27,36,24,40,25,34,30,29,31);
            Stroke(g,"#fff3ab",1.8f,17,27,22,29);
            Stroke(g,"#fff3ab",1.8f,30,29,35,27);
            Poly(g,"#d85237",24,29,26,25,29,30,26,34);
            Stroke(g,"#ec9462",.8f,19,35,24,37,29,37,33,34);
            // A curved katana held diagonally, orange rim and pale cutting edge.
            using(var blade=new GraphicsPath()) {
                blade.AddBezier(38,46,44,35,51,22,56,7);
                blade.AddBezier(56,7,60,21,50,40,42,49);
                blade.CloseFigure();
                using(var p=new Pen(Color.FromArgb(90,255,65,8),5)) g.DrawPath(p,blade);
                using(var b=new LinearGradientBrush(new PointF(38,48),new PointF(56,7),C("#ff731e"),C("#fff0ae"))) g.FillPath(b,blade);
                using(var p=new Pen(C("#fff6d0"),.8f)) g.DrawBezier(p,42,48,50,37,59,20,56,8);
            }
            Stroke(g,"#30101d",5,39,49,34,58);
            Stroke(g,"#e99742",1,38,51,40,52);
            Stroke(g,"#e99742",1,36,54,38,55);
            Stroke(g,"#fdb54c",2.4f,35,45,44,50);
            using(var ember=new SolidBrush(C("#ffc65b"))) { g.FillEllipse(ember,8,19,1.3f,2.2f); g.FillEllipse(ember,44,13,1,2); g.FillEllipse(ember,6,37,1,1.5f); }
            using(var icon=new Bitmap(64,64,PixelFormat.Format32bppArgb))
            using(var output=Graphics.FromImage(icon)) {
                output.InterpolationMode=InterpolationMode.HighQualityBicubic;
                output.PixelOffsetMode=PixelOffsetMode.HighQuality;
                output.DrawImage(hi,0,0,64,64); icon.Save(path,ImageFormat.Png);
            }
        }
    }
}
'@
[CrimsonSpellIcon]::Paint($iconPath)
Write-Output "Generated smooth Crimson Susanoo spell icon: $iconPath"
