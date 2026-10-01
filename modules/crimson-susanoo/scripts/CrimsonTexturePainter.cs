using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class CrimsonTexturePainter
{
    const int Size = 1024;
    const int Tile = 256;
    static readonly int[,] BaseColors = {
        {83,5,15,216}, {255,91,8,242}, {159,56,48,173}, {29,2,11,190},
        {255,224,58,255}, {247,46,10,250}, {169,12,27,220}, {34,6,20,120},
        {128,13,20,245}, {255,105,13,250}, {238,73,11,250}, {255,220,82,255},
        {83,5,15,216}, {83,5,15,216}, {83,5,15,216}, {83,5,15,216}
    };

    static double Clamp(double value, double min, double max) { return Math.Max(min, Math.Min(max, value)); }
    static byte B(double value) { return (byte)Math.Round(Clamp(value, 0, 255)); }
    static double Lerp(double a, double b, double t) { return a + (b - a) * Clamp(t, 0, 1); }

    // Narrow stress fractures, with a softer red heat stain around each molten seam.
    static readonly double[,] Fractures = {
        {3,0, 4.5,5}, {4.5,5, 3.5,9}, {3.5,9, 6,14},
        {6,14, 5,20}, {5,20, 7.5,26}, {7.5,26, 6.5,34},
        {4.5,5, 8,7}, {8,7, 10,6}, {6,14, 9,16},
        {9,16, 11,21}, {5,20, 2,23},
        {21,4, 19,10}, {19,10, 21,15}, {21,15, 18,20},
        {18,20, 19,27}, {19,27, 17,35}, {17,35, 19,42},
        {19,10, 15,12}, {21,15, 25,17}, {18,20, 14,23}
    };

    static double CrackDistance(double u, double v)
    {
        double nearest = 100;
        for (int n = 0; n < Fractures.GetLength(0); n++)
        {
            double dx = Fractures[n,2] - Fractures[n,0];
            double dy = Fractures[n,3] - Fractures[n,1];
            double t = Clamp(((u-Fractures[n,0])*dx + (v-Fractures[n,1])*dy)/(dx*dx+dy*dy), 0, 1);
            double x = u-Fractures[n,0]-t*dx, y = v-Fractures[n,1]-t*dy;
            nearest = Math.Min(nearest, Math.Sqrt(x*x+y*y));
        }
        return nearest;
    }

    public static void Paint(string texturePath, string glowPath)
    {
        byte[] body = new byte[Size * Size * 4];
        byte[] glow = new byte[body.Length];
        for (int y = 0; y < Size; y++)
        for (int x = 0; x < Size; x++)
        {
            int tile = x / Tile + 4 * (y / Tile);
            double u = (x % Tile + 0.5) / 4.0;
            double v = (y % Tile + 0.5) / 4.0;
            double broad = 6 * Math.Sin(u * .12 + v * .07) + 3 * Math.Sin(u * .31 - v * .14);
            double r = BaseColors[tile,0], g = BaseColors[tile,1], b = BaseColors[tile,2];
            double a = tile == 12 ? 255 : BaseColors[tile,3], emission = 0;
            if (tile == 11) // smooth eyes; the broad gradient survives Solas bloom
            {
                double dx = (u - 3.5) / 4.4, dy = (v - 1.5) / 2.4;
                double heat = Clamp(1 - Math.Sqrt(dx*dx + dy*dy), 0, 1);
                r = 255; g = Lerp(88, 248, heat); b = Lerp(12, 184, heat*heat);
                emission = Lerp(85, 235, heat);
            }
            else if (tile == 8) // dark crimson steel with continuous heated waves
            {
                double flame = .5 + .24*Math.Sin(v*.24 + u*.12) + .17*Math.Sin(v*.51 - u*.09);
                flame = Clamp(flame, 0, 1);
                r = Lerp(58, 145, flame); g = Lerp(8, 42, flame);
                b = Lerp(22, 18, flame);
            }
            else if (tile == 9) // polished heated edge; Solas must still shade its shape
            {
                double hot = Clamp(.68 + .18*Math.Sin(v*.21 + u*.17), 0, 1);
                r = Lerp(155, 218, hot); g = Lerp(53, 135, hot);
                b = Lerp(20, 39, hot);
            }
            else if (tile == 10) // quiet molten hamon beneath the edge
            {
                double hot = Clamp(.45 + .3*Math.Sin(v*.21 + u*.28), 0, 1);
                r = Lerp(130, 214, hot); g = Lerp(23, 100, hot);
                b = Lerp(12, 24, hot); emission = 35 + 65*hot;
            }
            else if (tile == 4 || tile == 5)
            {
                r = Clamp(r + broad*.35, 0, 255); g = Clamp(g + broad*.3, 0, 255);
                emission = tile == 4 ? 145 : 135;
            }
            else
            {
                // Continuous low-frequency variation keeps armor readable without pixel noise.
                r = Clamp(r + broad, 0, 255);
                g = Clamp(g + broad*.38, 0, 255);
                b = Clamp(b + broad*.22, 0, 255);
            }
            if (tile == 0 || tile == 12)
            {
                double distance = CrackDistance(u, v);
                double stain = Math.Pow(Clamp(1-distance/.95, 0, 1), 2);
                double core = Math.Pow(Clamp(1-distance/.32, 0, 1), .7);
                r = Lerp(r, 175, stain); g = Lerp(g, 22, stain);
                r = Lerp(r, 255, core); g = Lerp(g, 170, core); b = Lerp(b, 32, core);
                // Only the thin fracture glows; the surrounding armor retains shader lighting.
                if (core > .015) emission = 45 + 115*core;
            }
            int i = (y*Size + x)*4;
            body[i] = B(b); body[i+1] = B(g); body[i+2] = B(r); body[i+3] = B(a);
            // GeckoLib considers any nonzero glowmask pixel emissive, even if its alpha is zero.
            // Leave unlit armor fully transparent black so Solas shades the base texture normally.
            if (emission > 0)
            {
                glow[i] = B(b); glow[i+1] = B(g); glow[i+2] = B(r); glow[i+3] = B(emission);
            }
        }
        Save(body, texturePath);
        Save(glow, glowPath);
        // Opaque neutral material for vertex-colored fire geometry. Sampling the
        // glowmask previously multiplied every flame by its partial texture alpha.
        using (var fire = new Bitmap(2, 2, PixelFormat.Format32bppArgb))
        {
            using (var graphics = Graphics.FromImage(fire)) graphics.Clear(Color.White);
            fire.Save(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(texturePath), "guardian_fire.png"), ImageFormat.Png);
        }
    }

    static void Save(byte[] data, string path)
    {
        using (var bitmap = new Bitmap(Size, Size, PixelFormat.Format32bppArgb))
        {
            var rect = new Rectangle(0, 0, Size, Size);
            var bits = bitmap.LockBits(rect, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
            try { Marshal.Copy(data, 0, bits.Scan0, data.Length); }
            finally { bitmap.UnlockBits(bits); }
            bitmap.Save(path, ImageFormat.Png);
        }
    }
}
