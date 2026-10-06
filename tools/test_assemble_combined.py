"""Exercise integration failures which would otherwise break Forge loading."""
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from assemble_combined import assemble, parse_manifest


class IntegrationTest(unittest.TestCase):
    def test_omega_and_armor_preserve_all_three_mixins_and_base_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, c, d, base, fresh, appended = (root / name for name in
                ("a.jar", "b.jar", "c.jar", "d.jar", "base.jar", "fresh.jar", "appended.jar"))
            self.fixture(a, "irons_ultimate_explosion", {"root.mixins.json": b"{}"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: root.mixins.json\r\n\r\n")
            self.fixture(b, "crimson_susanoo")
            self.fixture(c, "ignis_armor_compat", {"armor.mixins.json": b"{}"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: armor.mixins.json\r\n\r\n")
            self.fixture(d, "irons_omega_rush", {"omega.mixins.json": b"{}", "local/omegarush/OmegaMod.class": b"omega"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: omega.mixins.json\r\n\r\n")
            assemble([a, b, c], base)
            report = assemble([base, d], appended)
            assemble([a, b, c, d], fresh)
            self.assertEqual(report["mod_ids"], ["crimson_susanoo", "ignis_armor_compat", "irons_omega_rush", "irons_ultimate_explosion"])
            self.assertEqual(fresh.read_bytes(), appended.read_bytes())
            with zipfile.ZipFile(appended) as jar:
                self.assertEqual(parse_manifest(jar.read("META-INF/MANIFEST.MF"))["MixinConfigs"],
                                 "root.mixins.json,armor.mixins.json,omega.mixins.json")
                self.assertEqual(jar.read("local/omegarush/OmegaMod.class"), b"omega")
                for mod in report["mod_ids"]:
                    self.assertEqual(jar.read(f"assets/{mod}/original.txt"), b"original payload")
            with self.assertRaisesRegex(ValueError, "Payload collision"):
                assemble([appended, d], root / "duplicate.jar")

    def test_omega_without_optional_armor(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = [root / name for name in ("a.jar", "b.jar", "omega.jar")]
            for path, mod in zip(paths, ["irons_ultimate_explosion", "crimson_susanoo", "irons_omega_rush"]):
                self.fixture(path, mod)
            report = assemble(paths, root / "out.jar")
            self.assertEqual(report["mod_ids"], ["crimson_susanoo", "irons_omega_rush", "irons_ultimate_explosion"])

    def test_optional_armor_module_keeps_both_mixins(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, c, out = (root / name for name in ("a.jar", "b.jar", "c.jar", "out.jar"))
            self.fixture(a, "irons_ultimate_explosion", {"test.mixins.json": b"{}"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: test.mixins.json\r\n\r\n")
            self.fixture(b, "crimson_susanoo")
            self.fixture(c, "ignis_armor_compat", {"armor.mixins.json": b"{}"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: armor.mixins.json\r\n\r\n")
            report = assemble([a, b, c], out)
            self.assertEqual(report["mod_ids"], ["crimson_susanoo", "ignis_armor_compat", "irons_ultimate_explosion"])
            with zipfile.ZipFile(out) as jar:
                self.assertEqual(parse_manifest(jar.read("META-INF/MANIFEST.MF"))["MixinConfigs"],
                                 "test.mixins.json,armor.mixins.json")
                self.assertEqual(jar.read("assets/ignis_armor_compat/original.txt"), b"original payload")

    def test_unexpected_third_module_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, c, out = (root / name for name in ("a.jar", "b.jar", "c.jar", "out.jar"))
            self.fixture(a, "irons_ultimate_explosion")
            self.fixture(b, "crimson_susanoo")
            self.fixture(c, "unrelated_module")
            with self.assertRaisesRegex(ValueError, "Expected exactly"):
                assemble([a, b, c], out)
            self.assertFalse(out.exists())

    def fixture(self, path, mod_id, extra=None, manifest=None):
        files = {
            "META-INF/mods.toml": (
                'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="All Rights Reserved"\n'
                f'[[mods]] # comment\n# separated declaration\nmodId="{mod_id}"\nversion="1"\n'
                f'[[dependencies.{mod_id}]]\nmodId="forge"\nmandatory=true\n'
                'versionRange="[47,)"\nordering="NONE"\nside="BOTH"\n'
            ).encode(),
            "META-INF/MANIFEST.MF": manifest or b"Manifest-Version: 1.0\r\n\r\n",
            "pack.mcmeta": json.dumps({"pack": {"pack_format": 15, "description": mod_id}}).encode(),
            f"assets/{mod_id}/original.txt": b"original payload",
        }
        files.update(extra or {})
        with zipfile.ZipFile(path, "w") as jar:
            for name, data in files.items():
                jar.writestr(name, data)

    def test_metadata_mixins_and_payload_survive(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, out = (root / name for name in ("a.jar", "b.jar", "out.jar"))
            self.fixture(a, "irons_ultimate_explosion", {"test.mixins.json": b"{}"},
                         b"Manifest-Version: 1.0\r\nMixinConfigs: test.mixins.json\r\n\r\n")
            self.fixture(b, "crimson_susanoo")
            report = assemble([a, b], out)
            self.assertEqual(report["unchanged_payload_entries"], 3)
            with zipfile.ZipFile(out) as jar:
                self.assertEqual(parse_manifest(jar.read("META-INF/MANIFEST.MF"))["MixinConfigs"], "test.mixins.json")
                self.assertEqual(jar.read("assets/crimson_susanoo/original.txt"), b"original payload")
            with self.assertRaisesRegex(ValueError, "already exists"):
                assemble([a, b], out)

    def test_collision_and_missing_mixin_fail_before_writing(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, out = (root / name for name in ("a.jar", "b.jar", "out.jar"))
            self.fixture(a, "irons_ultimate_explosion", {"collision.class": b"a"})
            self.fixture(b, "crimson_susanoo", {"collision.class": b"b"})
            with self.assertRaisesRegex(ValueError, "Payload collision"):
                assemble([a, b], out)
            self.assertFalse(out.exists())
            self.fixture(a, "irons_ultimate_explosion", manifest=b"Manifest-Version: 1.0\r\nMixinConfigs: missing.json\r\n\r\n")
            self.fixture(b, "crimson_susanoo")
            with self.assertRaisesRegex(ValueError, "Missing manifest mixin"):
                assemble([a, b], out)
            self.assertFalse(out.exists())

    def test_signed_input_and_dependency_classes_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            a, b, out = (root / name for name in ("a.jar", "b.jar", "out.jar"))
            self.fixture(b, "crimson_susanoo")
            for name, message in [("META-INF/TEST.SF", "Signed inputs"), ("net/minecraft/Example.class", "dependency class")]:
                self.fixture(a, "irons_ultimate_explosion", {name: b"x"})
                with self.assertRaisesRegex(ValueError, message):
                    assemble([a, b], out)
                self.assertFalse(out.exists())


if __name__ == "__main__":
    unittest.main()
