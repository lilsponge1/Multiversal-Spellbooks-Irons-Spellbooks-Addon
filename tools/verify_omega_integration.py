"""Verify a combined Omega Rush release against the two accepted input JARs."""
import argparse, json, tomllib
from pathlib import Path
from assemble_combined import load_jar, parse_manifest, digest, SHARED

def verify(base_path, omega_path, output_path):
    base, omega, output = [load_jar(p) for p in (base_path, omega_path, output_path)]
    checked = 0
    for files in (base, omega):
        for name, data in files.items():
            if name not in SHARED:
                assert output[name] == data, f"Changed input payload: {name}"
                checked += 1
    metadata = tomllib.loads(output['META-INF/mods.toml'].decode())
    assert {m['modId'] for m in metadata['mods']} == {
        'irons_ultimate_explosion', 'crimson_susanoo', 'ignis_armor_compat', 'irons_omega_rush'}
    for files in (base, omega):
        before = tomllib.loads(files['META-INF/mods.toml'].decode())
        for mod in before['mods']:
            assert mod in metadata['mods'], f"Changed mod metadata: {mod['modId']}"
        for name, section in before.get('dependencies', {}).items():
            assert metadata['dependencies'][name] == section
    mixins = parse_manifest(output['META-INF/MANIFEST.MF'])['MixinConfigs'].split(',')
    required = [name for files in (base, omega) for name in
        parse_manifest(files['META-INF/MANIFEST.MF']).get('MixinConfigs', '').split(',') if name]
    assert mixins == required and len(mixins) == 3
    for name in mixins:
        config = json.loads(output[name])
        for mixin in config.get('mixins', []) + config.get('client', []):
            assert (config['package'] + '.' + mixin).replace('.', '/') + '.class' in output
    assert not any('omegarushtest' in name or 'omega_rush_test' in name for name in output)
    sounds = json.loads(output['assets/irons_omega_rush/sounds.json'])
    for event, clip in [('car_drive', 'snd_cardrive'), ('bomb', 'snd_bomb')]:
        definition = sounds[event]['sounds'][0]
        assert definition['name'] == 'irons_omega_rush:' + clip
        assert definition['attenuation_distance'] == 32
        data = output['assets/irons_omega_rush/sounds/' + clip + '.ogg']
        assert data.startswith(b'OggS')
        packet = data.index(b'\x01vorbis')
        assert data[packet+11] == 1 and int.from_bytes(data[packet+12:packet+16], 'little') == 44100
    report = {'output': output_path.name, 'sha256': digest(output_path.read_bytes()),
        'unchanged_payload_entries': checked, 'mod_ids': sorted(m['modId'] for m in metadata['mods']),
        'mixin_configs': mixins, 'mono_vorbis_sounds': ['car_drive', 'bomb'], 'test_harness_excluded': True}
    return report

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path, required=True)
    parser.add_argument('--omega', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    report = verify(args.base, args.omega, args.output)
    args.output.with_suffix('.verification.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
