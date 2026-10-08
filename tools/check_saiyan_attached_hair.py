"""Check the actual SSJ3 deformation at full head yaw/pitch without starting Minecraft."""
from pathlib import Path
import argparse
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk-bin', type=Path, help='Java 17 bin directory; otherwise use PATH')
    parser.add_argument('--joml', type=Path, required=True, help='JOML 1.10.5 JAR from the Minecraft libraries')
    parser.add_argument('--output', type=Path, default=ROOT/'build/saiyan-attached-hair-check')
    args = parser.parse_args()
    assert args.joml.is_file(), 'JOML dependency is missing'
    args.output.mkdir(parents=True, exist_ok=True)

    def command(name):
        if args.jdk_bin:
            path = args.jdk_bin/(name + ('.exe' if (args.jdk_bin/(name+'.exe')).exists() else ''))
            assert path.is_file(), f'Missing JDK command: {path}'
            return str(path)
        path = shutil.which(name)
        assert path, f'{name} is not on PATH; pass --jdk-bin'
        return path

    sources = [ROOT/'src/main/java/local/ironsultimateexplosion'/name for name in ['SaiyanHairMesh.java', 'SaiyanHairPose.java']]
    sources.append(ROOT/'src/test/java/local/ironsultimateexplosion/SaiyanAttachedHairCheck.java')
    subprocess.run([command('javac'), '-J-Xmx128m', '-J-XX:ActiveProcessorCount=1', '-encoding', 'UTF-8',
                    '-cp', str(args.joml), '-d', str(args.output), *map(str, sources)], check=True)
    import os
    subprocess.run([command('java'), '-Xmx128m', '-XX:ActiveProcessorCount=1', '-cp',
                    str(args.output)+os.pathsep+str(args.joml),
                    'local.ironsultimateexplosion.SaiyanAttachedHairCheck', str(args.output)], check=True)
    result = json.loads((args.output/'pose-result.json').read_text(encoding='utf-8'))
    assert result['passed'], 'Attachment check failed'
    print('SSJ3 attachment, normals and sampled head/body clearance checks passed.')


if __name__ == '__main__':
    main()
