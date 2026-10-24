"""Build Ki alone from the reviewed Saiyan implementation, without bundling other addons."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, tempfile, zipfile

ROOT = Path(__file__).resolve().parents[1]
REPLACEMENTS = [('local.ironsultimateexplosion', 'local.kischool'),
                ('irons_ultimate_explosion', 'ki_school'), ('GrandExplosionMod', 'KiSchoolMod'),
                ('ExplosionNetwork', 'KiNetwork'), ('ExplosionConfig', 'KiConfig'),
                ('ThundercrashManager', 'KiCompatibility')]

def transform(text):
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    return text

METADATA = '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"
[[mods]]
modId="ki_school"
version="1.0.0"
displayName="Ki School - Saiyan Ascension"
authors="Sponge"
description="Craftable Saiyan Wig with Super Saiyan I, II and III. Independent addon."
[[dependencies.ki_school]]
modId="forge"
mandatory=true
versionRange="[47.4.10,)"
ordering="NONE"
side="BOTH"
[[dependencies.ki_school]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
ordering="NONE"
side="BOTH"
[[dependencies.ki_school]]
modId="irons_spellbooks"
mandatory=true
versionRange="[1.20.1-3.16.3,1.20.1-3.16.4)"
ordering="AFTER"
side="BOTH"
[[dependencies.ki_school]]
modId="curios"
mandatory=true
versionRange="[5,)"
ordering="AFTER"
side="BOTH"
[[dependencies.ki_school]]
modId="playeranimator"
mandatory=true
versionRange="[1,)"
ordering="AFTER"
side="BOTH"
'''

def build(jdk_bin, classpath_file, output):
    assert not output.exists(), 'Preserve accepted JARs; choose a fresh output path.'
    dependencies = [Path(p).resolve() for p in json.loads(classpath_file.read_text(encoding='utf-8-sig'))]
    assert dependencies and all(p.is_file() for p in dependencies)
    output.parent.mkdir(parents=True, exist_ok=True)
    audit = {'source': {}, 'binaryResourcesUnchanged': {}, 'generatedClasses': []}
    with tempfile.TemporaryDirectory(dir=output.parent, prefix='ki-compile-') as temporary:
        temp = Path(temporary)
        source_dir = temp / 'java/local/kischool'
        source_dir.mkdir(parents=True)
        sources = []
        original = ROOT / 'src/main/java/local/ironsultimateexplosion'
        for path in sorted(original.glob('Saiyan*.java')) + [original / 'mixin/SaiyanCastInputMixin.java']:
            target = source_dir / path.relative_to(original)
            target.parent.mkdir(exist_ok=True)
            target.write_text(transform(path.read_text(encoding='utf-8')), encoding='utf-8')
            sources.append(target)
            audit['source'][path.relative_to(ROOT).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
        sources += sorted((ROOT / 'src/standalone/java').rglob('*.java'))
        classes = temp / 'classes'
        classes.mkdir()
        arguments = ['-J-Xmx192m', '-J-XX:ActiveProcessorCount=1', '-proc:none', '-encoding', 'UTF-8',
            '-cp', '"' + os.pathsep.join(p.as_posix() for p in dependencies) + '"', '-d', '"' + classes.as_posix() + '"']
        arguments += ['"' + p.as_posix() + '"' for p in sources]
        argfile = temp / 'javac.args'
        argfile.write_text('\n'.join(arguments) + '\n', encoding='utf-8')
        compiled = subprocess.run([str(jdk_bin / ('javac.exe' if os.name == 'nt' else 'javac')), '@' + str(argfile)], capture_output=True, text=True)
        (output.parent / 'ki-compile.log').write_text(compiled.stdout + compiled.stderr, encoding='utf-8')
        if compiled.returncode:
            print(compiled.stderr[-8000:])
            raise SystemExit(compiled.returncode)
        payload = {p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob('*.class')}
        audit['generatedClasses'] = sorted(payload)
        resources = ROOT / 'src/main/resources'
        for path in resources.rglob('*'):
            if not path.is_file(): continue
            name = path.relative_to(resources).as_posix()
            if 'saiyan' not in name.lower() or not name.startswith(('assets/', 'data/')): continue
            new_name = name.replace('irons_ultimate_explosion', 'ki_school')
            data = path.read_bytes()
            if path.suffix == '.json': data = transform(data.decode('utf-8')).encode('utf-8')
            else: audit['binaryResourcesUnchanged'][name] = hashlib.sha256(data).hexdigest()
            payload[new_name] = data
        for name in ['head', 'spellbook']:
            path = resources / f'data/curios/tags/items/{name}.json'
            payload[path.relative_to(resources).as_posix()] = transform(path.read_text(encoding='utf-8')).encode()
        for name in ['lang/en_us.json', 'sounds.json']:
            data = json.loads((resources / 'assets/irons_ultimate_explosion' / name).read_text(encoding='utf-8'))
            selected = {key: value for key, value in data.items() if 'saiyan' in key}
            assert selected
            payload['assets/ki_school/' + name] = transform(json.dumps(selected, indent=2)).encode()
        payload['META-INF/mods.toml'] = METADATA.encode()
        payload['META-INF/MANIFEST.MF'] = b'Manifest-Version: 1.0\nMixinConfigs: ki_school.mixins.json\n\n'
        payload['pack.mcmeta'] = b'{"pack":{"pack_format":15,"description":"Ki School - Saiyan Ascension"}}'
        payload['ki_school.mixins.json'] = json.dumps({'required': True, 'minVersion': '0.8',
            'package': 'local.kischool.mixin', 'compatibilityLevel': 'JAVA_17',
            'client': ['SaiyanCastInputMixin'], 'injectors': {'defaultRequire': 1}}).encode()
        for name, data in payload.items():
            if name.endswith('.json') or name == 'pack.mcmeta': json.loads(data)
        assert all(not name.endswith('.class') or name.startswith('local/kischool/') for name in payload)
        assert all(not name.startswith(('assets/irons_ultimate_explosion/', 'assets/irons_omega_rush/', 'local/omegarush/')) for name in payload)
        assert len([name for name in payload if name.endswith('/SaiyanWig.class')]) == 1
        with zipfile.ZipFile(output, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in sorted(payload.items()):
                entry = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
                entry.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(entry, data)
    digest = hashlib.sha256(output.read_bytes()).hexdigest().upper()
    Path(str(output) + '.sha256').write_text(digest + '  ' + output.name + '\n', encoding='utf-8')
    audit.update({'artifact': output.name, 'sha256': digest, 'modId': 'ki_school', 'networkProtocol': 1,
        'otherModIdsBundled': False, 'sourceFiles': len(sources), 'entries': len(payload)})
    Path(str(output) + '.build.json').write_text(json.dumps(audit, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: audit[key] for key in ['artifact', 'sha256', 'modId', 'sourceFiles', 'entries', 'otherModIdsBundled']}, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk-bin', type=Path, required=True)
    parser.add_argument('--classpath-file', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/release/ki-school-1.0.0.jar')
    args = parser.parse_args()
    build(args.jdk_bin.resolve(), args.classpath_file.resolve(), args.output.resolve())
