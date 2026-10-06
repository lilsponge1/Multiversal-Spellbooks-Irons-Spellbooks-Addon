"""Convert the supplied WAV clips to mono Ogg Vorbis for positional Minecraft audio."""
from pathlib import Path
import sys, json, hashlib

root=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(root/'build/audio-tools'))
import soundfile as sf
import numpy as np

out=root/'src/main/resources/assets/irons_omega_rush/sounds'
out.mkdir(parents=True,exist_ok=True)
report=[]
for name in ['snd_cardrive','snd_bomb']:
    source=Path.home()/'Downloads'/f'{name}.wav'
    data,rate=sf.read(source,dtype='float32',always_2d=True)
    mono=np.mean(data,axis=1)
    target=out/f'{name}.ogg'
    sf.write(target,mono,rate,format='OGG',subtype='VORBIS')
    decoded,decoded_rate=sf.read(target,dtype='float32',always_2d=True)
    info=sf.info(target)
    assert info.format=='OGG' and info.subtype=='VORBIS' and info.channels==1
    assert decoded_rate==rate and len(decoded)==len(mono) and np.isfinite(decoded).all()
    assert np.max(np.abs(decoded))>0
    report.append({'source':str(source),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
        'ogg':str(target),'channels':info.channels,'sample_rate':rate,'frames':info.frames,
        'seconds':info.duration,'format':info.format,'codec':info.subtype})
(root/'build/audio-conversion.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
