"""Check the renderer's actual cross sections against the outer head and helmet."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/main/java/local/ironsultimateexplosion/SaiyanWigRenderer.java'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jdk-bin', required=True, type=Path)
    args = parser.parse_args()
    source = SOURCE.read_text(encoding='utf-8')
    start = source.index('    static double[][][] locks(int tier) {')
    end = source.index('    private static Vec3 p(', start)
    method = source[start:end]
    # These cover the other vertices and the armor-specific transform.
    assert 'box(out,matrix,normal,-4.55,-8.85,-4.55,4.55,-8.55,4.55,' in source
    assert 'if (!slot.entity().m_6844_(EquipmentSlot.HEAD).m_41619_()) pose.m_85837_(0, -.75, 0);' in source
    fixture = ROOT / 'build/saiyan-hair-geometry-check'
    fixture.mkdir(parents=True, exist_ok=True)
    java = fixture / 'HairBounds.java'
    java.write_text('public class HairBounds {\n' + method + '''
    public static void main(String[] args) {
        int sections = 0;
        for (int tier = 0; tier <= 2; tier++) {
            double[][][] geometry = locks(tier);
            if (geometry.length != 11) throw new AssertionError("missing hair lock");
            for (double[][] lock : geometry) {
                if (lock.length != 4) throw new AssertionError("missing cross section");
                for (double[] s : lock) {
                    if (s.length != 5 || s[3] <= 0 || s[4] <= 0)
                        throw new AssertionError("invalid cross section");
                    for (double value : s) if (!Double.isFinite(value))
                        throw new AssertionError("non-finite vertex");
                    // Each section's four corners share this Y coordinate. All
                    // intervening quad vertices are linear interpolations.
                    if (s[1] > -8.55) throw new AssertionError("hair crosses outer skin");
                    if (s[1] - .75 > -9.3) throw new AssertionError("hair crosses helmet");
                    sections++;
                }
            }
        }
        if (sections != 132) throw new AssertionError("unexpected section count");
        System.out.println("132 cross sections clear the head and standard helmet");
    }
}
''', encoding='utf-8')
    subprocess.run([str(args.jdk_bin / 'javac.exe'), '-J-XX:ActiveProcessorCount=1',
                    '-J-Xmx128m', '-d', str(fixture), str(java)], check=True)
    result = subprocess.run([str(args.jdk_bin / 'java.exe'), '-XX:ActiveProcessorCount=1',
                             '-Xmx64m', '-cp', str(fixture), 'HairBounds'],
                            check=True, capture_output=True, text=True)
    report = {'passed': True, 'forms': 3, 'locksPerForm': 11, 'crossSections': 132,
              'headOuterLayerTop': -8.5, 'lowestWigVertex': -8.55,
              'helmetTop': -9.0, 'helmetLift': 0.75,
              'geometryNote': 'Quads linearly join cross sections above the head; the cap also clears the outer skin layer.',
              'sourceSha256': hashlib.sha256(SOURCE.read_bytes()).hexdigest().upper()}
    (fixture / 'result.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(result.stdout.strip())


if __name__ == '__main__':
    main()
