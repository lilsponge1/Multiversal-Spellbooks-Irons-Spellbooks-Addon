"""Encode the isolated yell master into full-length charge audio.
The local voice-isolation step is documented in art/saiyan/audio-source.json.
Usage: python tools/generate_saiyan_audio.py --ffmpeg FFMPEG
"""
from pathlib import Path
import argparse, hashlib, json, subprocess, tempfile, wave
import numpy as np
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--voice-stem',type=Path,default=root/'art/saiyan/yell_isolated_master.wav')
parser.add_argument('--ffmpeg',type=Path,required=True)
parser.add_argument('--output-dir',type=Path,default=root/'src/main/resources/assets/irons_ultimate_explosion/sounds/saiyan')
args=parser.parse_args()
output=args.output_dir
output.mkdir(parents=True,exist_ok=True)
result=json.loads((root/'art/saiyan/audio-source.json').read_text())
result['isolatedMasterSha256']=hashlib.sha256(args.voice_stem.read_bytes()).hexdigest()
result['samples']=[]
with tempfile.TemporaryDirectory(prefix='saiyan-audio-') as temp:
    for name,duration,target,tempo in [('yell',2.5,-17,.72),('yell_2',3,-16,.6)]:
        filters=f'highpass=f=100,lowpass=f=8000,afftdn=nr=4:nf=-50,atempo={tempo},loudnorm=I={target}:TP=-2:LRA=7,aresample=44100,apad,atrim=duration={duration},asetpts=N/SR/TB,afade=t=in:d=0.035,afade=t=out:st={duration-.05}:d=0.05'
        asset=output/(name+'.ogg')
        subprocess.run([str(args.ffmpeg),'-y','-hide_banner','-loglevel','error','-i',str(args.voice_stem),'-af',filters,'-ac','1','-c:a','libvorbis','-q:a','5',str(asset)],check=True)
        decoded=Path(temp)/(name+'.wav')
        subprocess.run([str(args.ffmpeg),'-y','-hide_banner','-loglevel','error','-i',str(asset),'-c:a','pcm_s16le',str(decoded)],check=True)
        with wave.open(str(decoded),'rb') as w:
            rate=w.getframerate(); actual=w.getnframes()/rate
            assert w.getnchannels()==1 and abs(actual-duration)<.025,(name,actual)
            signal=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(float)/32768
        windows=[float(np.sqrt(np.mean(signal[i:i+4410]**2))) for i in range(4410,len(signal)-4410,4410)]
        assert min(windows)>.005,'Unexpected silent gap in charge audio'
        result['samples'].append({'name':name,'seconds':actual,'rate':rate,'minInterior100msRms':min(windows),
                                  'sha256':hashlib.sha256(asset.read_bytes()).hexdigest()})
if output == root/'src/main/resources/assets/irons_ultimate_explosion/sounds/saiyan':
    (root/'art/saiyan/audio-source.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
