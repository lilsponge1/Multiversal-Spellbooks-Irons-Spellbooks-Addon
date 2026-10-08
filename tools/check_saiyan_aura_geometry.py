"""Compare emitted aura vertices with the frozen, native-reviewed wig8 geometry.

Runs only extracted Java math and a vertex recorder: no Minecraft, graphics device,
server, window or network. This checks geometry equivalence, not shader appearance
or game performance. Java is limited to one processor and a 96 MiB heap.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def member(source, marker):
    start = source.index(marker)
    opening = source.index('{', start)
    depth, end = 1, opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]


HEADER = r'''
import java.util.Arrays;
public final class SaiyanAuraGeometryCheck {
    private static long positionObjects, curveCalls;
    private static final class Matrix4f {}
    private static final class Vec3 {
        final double f_82479_, f_82480_, f_82481_;
        Vec3(double x, double y, double z) {
            f_82479_ = x; f_82480_ = y; f_82481_ = z; positionObjects++;
        }
    }
    private static final class Math {
        static final double PI = java.lang.Math.PI;
        static double sin(double v) { curveCalls++; return java.lang.Math.sin(v); }
        static double cos(double v) { curveCalls++; return java.lang.Math.cos(v); }
        static double pow(double a, double b) { curveCalls++; return java.lang.Math.pow(a, b); }
        static double hypot(double a, double b) { curveCalls++; return java.lang.Math.hypot(a, b); }
        static double min(double a, double b) { return java.lang.Math.min(a, b); }
        static float min(float a, float b) { return java.lang.Math.min(a, b); }
        static double max(double a, double b) { return java.lang.Math.max(a, b); }
    }
    private static final class VertexConsumer {
        final int[] values = new int[4096 * 10];
        int vertices;
        private void value(int offset, float value) {
            values[vertices * 10 + offset] = Float.floatToRawIntBits(value);
        }
        VertexConsumer m_252986_(Matrix4f m, float x, float y, float z) {
            value(0, x); value(1, y); value(2, z); return this;
        }
        VertexConsumer m_7421_(float u, float v) { value(3, u); value(4, v); return this; }
        VertexConsumer m_85950_(float r, float g, float b, float a) {
            value(5, r); value(6, g); value(7, b); value(8, a); return this;
        }
        VertexConsumer m_85969_(int light) { values[vertices * 10 + 9] = light; return this; }
        void m_5752_() { vertices++; }
    }
'''

MAIN = r'''
    public static void main(String[] args) {
        Matrix4f matrix = new Matrix4f();
        // Initialize static edge constants outside the per-draw accounting.
        Current.flame(new VertexConsumer(), matrix, 0, 1.25, 3.55, 1, 2, new Vec3(0, 1, 0));
        long oldObjects = 0, newObjects = 0, oldCalls = 0, newCalls = 0;
        int cases = 0, totalVertices = 0;
        for (int tier : new int[]{1, 2})
        for (double time : new double[]{0, 1, 12.5, 246.25, 100000})
        for (float strength : new float[]{.15f, .6f, 1f})
        for (double[] camera : new double[][]{{0, 2, 0}, {4, 1, -6}, {-2, 1, 0}}) {
            Vec3 eye = new Vec3(camera[0], camera[1], camera[2]);
            double radius = (tier == 2 ? 1.25 : 1.05) * strength;
            double height = tier == 2 ? 3.55 : 3.10;
            VertexConsumer before = new VertexConsumer(), after = new VertexConsumer();
            positionObjects = curveCalls = 0;
            Baseline.flame(before, matrix, time, radius, height, strength, tier, eye);
            oldObjects += positionObjects; oldCalls += curveCalls;
            positionObjects = curveCalls = 0;
            Current.flame(after, matrix, time, radius, height, strength, tier, eye);
            newObjects += positionObjects; newCalls += curveCalls;
            if (before.vertices != after.vertices) throw new AssertionError("Vertex count changed");
            int used = before.vertices * 10;
            int mismatch = Arrays.mismatch(before.values, 0, used, after.values, 0, used);
            if (mismatch >= 0) throw new AssertionError("Vertex value changed in case " + cases + " at " + mismatch);
            cases++; totalVertices += before.vertices;
        }
        if (newObjects != 0 || newCalls >= oldCalls) throw new AssertionError("Redundant work not reduced");
        int thirdCases=0,thirdVertices=0;
        for(double time:new double[]{0,60,120,180,240,300,100000})
        for(float strength:new float[]{.15f,.5f,1,2})
        for(float pale:new float[]{0,.35f,.85f})
        for(double distance:new double[]{0,8,40}) {
            var out=new VertexConsumer();
            Current.flame(out,matrix,time,2.2*strength,6.15,strength,3,new Vec3(distance,1,0),pale);
            if(out.vertices!=(distance>32?864:1728))throw new AssertionError("Unbounded SSJ3 aura");
            for(int i=0;i<out.vertices;i++) {
                for(int j=0;j<9;j++)if(!Float.isFinite(Float.intBitsToFloat(out.values[i*10+j])))throw new AssertionError("Nonfinite SSJ3 vertex");
                for(int j=3;j<9;j++){float v=Float.intBitsToFloat(out.values[i*10+j]);if(v<0||v>1.001)throw new AssertionError("Unbounded UV or colour");}
                if(Float.intBitsToFloat(out.values[i*10+1])<0 || Float.intBitsToFloat(out.values[i*10+1])>6.2)throw new AssertionError("Aura height");
            }
            thirdCases++;thirdVertices+=out.vertices;
        }
        System.out.println("{\"thirdCases\":"+thirdCases+",\"thirdVerticesChecked\":"+thirdVertices+",\"thirdFiniteAndBounded\":true,\"cases\":" + cases + ",\"verticesCompared\":" + totalVertices
            + ",\"bitIdentical\":true,\"oldTemporaryPositionObjects\":" + oldObjects
            + ",\"newTemporaryPositionObjects\":" + newObjects
            + ",\"oldCurveCalls\":" + oldCalls + ",\"newCurveCalls\":" + newCalls + "}");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk-bin', type=Path, required=True)
    args = parser.parse_args()
    source = ROOT / 'src/main/java/local/ironsultimateexplosion/SaiyanVisuals.java'
    baseline = ROOT / 'src/test/fixtures/saiyan-flame-wig8.java.txt'
    current = source.read_text(encoding='utf-8')
    common = [member(current, '    private static void flame('),
              member(current, '    private static void flame(VertexConsumer out,Matrix4f'),
              member(current, '    private static FlameRow flameRow('),
              member(current, '    private static void flameVertex('),
              member(current, '    private static void vertex(VertexConsumer out, Matrix4f m, float')]
    constants = current[current.index('    private static final float[] FLAME_EDGES'):current.index('    private static int budget;')]
    code = (HEADER + '\n    private static final class Baseline {\n'
            + baseline.read_text(encoding='utf-8') + '\n    }\n'
            + '\n    private static final class Current {\n' + constants
            + '\n\n'.join(common) + '\n    }\n' + MAIN)
    output = ROOT / 'build/saiyan-aura-geometry-check'
    output.mkdir(parents=True, exist_ok=True)
    java_source = output / 'SaiyanAuraGeometryCheck.java'
    java_source.write_text(code, encoding='utf-8')
    suffix = '.exe' if os.name == 'nt' else ''
    options = {'check': True, 'timeout': 30, 'capture_output': True, 'text': True,
               'creationflags': subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0}
    subprocess.run([str(args.jdk_bin / ('javac' + suffix)), '-J-XX:ActiveProcessorCount=1',
                    '-J-Xmx96m', '-proc:none', '-encoding', 'UTF-8', '-d', str(output), str(java_source)], **options)
    result = subprocess.run([str(args.jdk_bin / ('java' + suffix)), '-XX:ActiveProcessorCount=1',
                             '-Xmx96m', '-cp', str(output), 'SaiyanAuraGeometryCheck'], **options)
    report = json.loads(result.stdout)
    report['scope'] = 'Extracted Java geometry; raw float vertex values and ordering, not native rendering or FPS'
    report['curveCallReductionPercent'] = round(100 * (1 - report['newCurveCalls'] / report['oldCurveCalls']), 2)
    report_path = output / 'result.json'
    report_path.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
