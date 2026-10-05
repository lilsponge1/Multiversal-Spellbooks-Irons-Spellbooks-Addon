"""Prepare mono positional spell sounds from the user's recorded reference.

Optional --av-path selects an existing PyAV installation; no downloads occur.
"""
import argparse, hashlib, json, pathlib, sys, wave

parser = argparse.ArgumentParser()
parser.add_argument('--input', required=True, type=pathlib.Path)
parser.add_argument('--av-path', type=pathlib.Path)
args = parser.parse_args()
if args.av_path:
    sys.path.insert(0, str(args.av_path.resolve()))
import av
import numpy as np

root = pathlib.Path(__file__).resolve().parents[1]
preview = root/'build/audio-preview'
preview.mkdir(parents=True, exist_ok=True)
assets = root/'src/main/resources/assets/irons_ultimate_explosion/sounds/thundercrash'
sample_rate = 48000
parts = []
with av.open(str(args.input)) as container:
    resampler = av.AudioResampler(format='fltp', layout='mono', rate=sample_rate)
    for frame in container.decode(audio=0):
        parts.extend(f.to_ndarray().reshape(-1) for f in resampler.resample(frame))
    parts.extend(f.to_ndarray().reshape(-1) for f in resampler.resample(None))
source = np.concatenate(parts)
intervals = {'cast': (2.55, 3.35), 'flight': (3.35, 7.26), 'impact': (7.26, 11.65)}
sequence = source[int(2.55*sample_rate):int(11.65*sample_rate)]
gain = 0.70 / max(float(np.max(np.abs(sequence))), 1e-6)

def wav_file(path, audio):
    with wave.open(str(path), 'wb') as wav:
        wav.setparams((1, 2, sample_rate, 0, 'NONE', 'not compressed'))
        wav.writeframes((np.clip(audio, -1, 1)*32767).astype('<i2').tobytes())

wav_file(preview/'thundercrash-full-sequence.wav', sequence*gain)
report = {'sourceSha256': hashlib.sha256(args.input.read_bytes()).hexdigest(),
          'sourceName': args.input.name, 'sampleRate': sample_rate,
          'channels': 1, 'gainDb': float(20*np.log10(gain)), 'segments': {}}
for name, (start, end) in intervals.items():
    audio = source[int(start*sample_rate):int(end*sample_rate)].copy()*gain
    if name == 'flight':
        # Join tail to head without a hard discontinuity at every loop boundary.
        overlap = int(0.12*sample_rate)
        blend = np.linspace(0, 1, overlap, dtype=np.float32)
        joint = audio[-overlap:]*(1-blend)+audio[:overlap]*blend
        audio = np.concatenate((audio[overlap:-overlap], joint))
    else:
        onset = int(0.003*sample_rate)
        tail = int((0.18 if name == 'impact' else 0.015)*sample_rate)
        audio[:onset] *= np.linspace(0, 1, onset)
        audio[-tail:] *= np.linspace(1, 0, tail)
    wav_file(preview/f'thundercrash-{name}.wav', audio)
    output = assets/f'thundercrash_{name}.ogg'
    with av.open(str(output), 'w', format='ogg') as container:
        stream = container.add_stream('libvorbis', rate=sample_rate)
        stream.layout = 'mono'
        stream.bit_rate = 128000
        for offset in range(0, len(audio), 4096):
            frame = av.AudioFrame.from_ndarray(audio[offset:offset+4096].reshape(1,-1).astype('float32'), format='fltp', layout='mono')
            frame.sample_rate = sample_rate; frame.pts = offset
            for packet in stream.encode(frame): container.mux(packet)
        for packet in stream.encode(None): container.mux(packet)
    with av.open(str(output)) as container:
        decoded = np.concatenate([f.to_ndarray().reshape(-1) for f in container.decode(audio=0)])
    assert float(np.max(np.abs(decoded))) < 0.95, f'Insufficient headroom: {name}'
    report['segments'][name] = {'sourceStart': start, 'sourceEnd': end,
        'duration': len(audio)/sample_rate, 'decodedPeak': float(np.max(np.abs(decoded))),
        'sha256': hashlib.sha256(output.read_bytes()).hexdigest()}
(preview/'audio-preparation.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
