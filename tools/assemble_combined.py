"""Assemble independently compiled Forge addons without bundling dependencies.

Requires Python 3.11+. Input JARs must already use production (SRG) names.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import tomllib
import zipfile
from pathlib import Path, PurePosixPath

SHARED = {"META-INF/mods.toml", "META-INF/MANIFEST.MF", "pack.mcmeta"}
EXPECTED_IDS = {"irons_ultimate_explosion", "crimson_susanoo"}
FORBIDDEN_CLASSES = (
    "net/minecraft/", "net/minecraftforge/", "io/redspace/ironsspellbooks/",
    "com/github/L_Ender/cataclysm/", "software/bernie/geckolib/",
)


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def load_jar(path: Path) -> dict[str, bytes]:
    files: dict[str, bytes] = {}
    with zipfile.ZipFile(path) as jar:
        for entry in jar.infolist():
            name = entry.filename
            if entry.is_dir():
                continue
            if name in files:
                raise ValueError(f"Duplicate entry in {path.name}: {name}")
            if name.startswith("/") or "\\" in name or ".." in PurePosixPath(name).parts:
                raise ValueError(f"Unsafe entry: {name}")
            if re.match(r"META-INF/[^/]+\.(SF|RSA|DSA|EC)$", name, re.I):
                raise ValueError("Signed inputs cannot be combined; rebuild unsigned inputs")
            if name.endswith(".class") and name.startswith(FORBIDDEN_CLASSES):
                raise ValueError(f"Input bundles dependency class: {name}")
            files[name] = jar.read(entry)
    for name in SHARED:
        if name not in files:
            raise ValueError(f"{path.name} is missing {name}")
    return files


def merge_mods(inputs: list[dict[str, bytes]]) -> bytes:
    bodies, mods = [], []
    common = None
    for files in inputs:
        text = files["META-INF/mods.toml"].decode("utf-8-sig")
        metadata = tomllib.loads(text)
        first_table = re.search(r"(?m)^\s*\[\[mods\]\]", text)
        if first_table is None:
            raise ValueError("Missing mods array")
        header = tomllib.loads(text[:first_table.start()])
        if set(header) != {"modLoader", "loaderVersion", "license"}:
            raise ValueError("Review additional global mods.toml fields before combining")
        if common is not None and header != common:
            raise ValueError("Incompatible loader, loader range or license")
        common = header
        bodies.append(text[first_table.start():].strip())
        mods.extend(metadata["mods"])
    ids = [mod["modId"] for mod in mods]
    if set(ids) != EXPECTED_IDS or len(ids) != len(EXPECTED_IDS):
        raise ValueError(f"Expected exactly the two addon IDs, found {ids}")
    header_text = "\n".join(f"{key}={json.dumps(value)}" for key, value in common.items())
    merged = header_text + "\n\n" + "\n\n".join(bodies) + "\n"
    actual = tomllib.loads(merged)
    if actual["mods"] != mods:
        raise ValueError("Merged mod metadata changed")
    for files in inputs:
        original = tomllib.loads(files["META-INF/mods.toml"].decode("utf-8-sig"))
        for key, value in original.items():
            if key not in common and key != "mods":
                for namespace, section in value.items():
                    if actual[key][namespace] != section:
                        raise ValueError(f"Merged {key}.{namespace} changed")
    return merged.encode("utf-8")


def parse_manifest(data: bytes) -> dict[str, str]:
    unfolded: list[str] = []
    for line in data.decode("utf-8").splitlines():
        if line.startswith(" "):
            if not unfolded:
                raise ValueError("Invalid manifest continuation")
            unfolded[-1] += line[1:]
        elif line:
            unfolded.append(line)
    result = {}
    for line in unfolded:
        key, separator, value = line.partition(": ")
        if not separator or key in result or key == "Name":
            raise ValueError("Unsupported manifest sections or duplicate attributes")
        result[key] = value
    if result.get("Manifest-Version") != "1.0":
        raise ValueError("Manifest-Version must be 1.0")
    return result


def merge_manifest(inputs: list[dict[str, bytes]]) -> bytes:
    primary = parse_manifest(inputs[0]["META-INF/MANIFEST.MF"])
    mixins = []
    for index, files in enumerate(inputs):
        attrs = parse_manifest(files["META-INF/MANIFEST.MF"])
        for key, value in attrs.items():
            if key == "MixinConfigs":
                for name in value.split(","):
                    name = name.strip()
                    if name not in files:
                        raise ValueError(f"Missing manifest mixin config: {name}")
                    if name not in mixins:
                        mixins.append(name)
            elif index and not key.startswith(("Specification-", "Implementation-", "Build-")):
                if key not in {"Manifest-Version", "Created-By"}:
                    if key in primary and primary[key] != value:
                        raise ValueError(f"Conflicting manifest attribute: {key}")
                    primary[key] = value
    if mixins:
        primary["MixinConfigs"] = ",".join(mixins)
    lines = []
    for key, value in primary.items():
        line = f"{key}: {value}".encode("utf-8")
        # Wrap by bytes without splitting a UTF-8 code point.
        while len(line) > 70:
            cut = 70
            while line[cut] & 0xC0 == 0x80:
                cut -= 1
            lines.append(line[:cut])
            line = b" " + line[cut:]
        lines.append(line)
    return b"\r\n".join(lines) + b"\r\n\r\n"


def merge_pack(inputs: list[dict[str, bytes]]) -> bytes:
    packs = [json.loads(files["pack.mcmeta"]) for files in inputs]
    base = packs[0]
    for other in packs[1:]:
        if {k: v for k, v in base.items() if k != "pack"} != {k: v for k, v in other.items() if k != "pack"}:
            raise ValueError("Review additional pack metadata before combining")
        if {k: v for k, v in base["pack"].items() if k != "description"} != {k: v for k, v in other["pack"].items() if k != "description"}:
            raise ValueError("Incompatible resource pack metadata")
    base["pack"]["description"] = "Multiversal Spellbooks: Grand Explosion, Thundercrash and Crimson Susanoo"
    return (json.dumps(base, indent=2) + "\n").encode("utf-8")


def assemble(paths: list[Path], output: Path) -> dict:
    paths = [path.resolve(strict=True) for path in paths]
    output = output.resolve()
    sidecars = [Path(str(output) + suffix) for suffix in (".sha256", ".integration.json")]
    if output in paths or any(path.exists() for path in [output, *sidecars]):
        raise ValueError("Output or sidecar already exists; choose a new output path")
    inputs = [load_jar(path) for path in paths]
    payload: dict[str, bytes] = {}
    for files in inputs:
        for name, data in files.items():
            if name in SHARED:
                continue
            if name in payload:
                raise ValueError(f"Payload collision between addons: {name}")
            payload[name] = data
    expected_hashes = {name: digest(data) for name, data in payload.items()}
    payload["META-INF/mods.toml"] = merge_mods(inputs)
    payload["META-INF/MANIFEST.MF"] = merge_manifest(inputs)
    payload["pack.mcmeta"] = merge_pack(inputs)
    output.parent.mkdir(parents=True, exist_ok=True)
    # Exclusive creation prevents accidental overwrites of accepted releases.
    with zipfile.ZipFile(output, "x", compression=zipfile.ZIP_DEFLATED) as jar:
        for name, data in sorted(payload.items()):
            entry = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o100644 << 16
            jar.writestr(entry, data)
    verified = load_jar(output)
    if verified != payload:
        raise ValueError("Output content verification failed")
    report = {
        "output": output.name,
        "sha256": digest(output.read_bytes()),
        "inputs": [{"file": path.name, "sha256": digest(path.read_bytes())} for path in paths],
        "mod_ids": sorted(EXPECTED_IDS),
        "unchanged_payload_entries": len(expected_hashes),
        "payload_sha256": expected_hashes,
        "shared_metadata": sorted(SHARED),
    }
    sidecars[0].write_text(f"{report['sha256']}  {output.name}\n", encoding="utf-8")
    sidecars[1].write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--grand-explosion", type=Path, required=True)
    parser.add_argument("--crimson", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = assemble([args.grand_explosion, args.crimson], args.output)
    print(f"Verified {result['unchanged_payload_entries']} unchanged payload entries; two mod IDs")
    print(f"SHA256: {result['sha256']}")
