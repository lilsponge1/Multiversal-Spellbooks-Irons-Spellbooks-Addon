"""Local CPU UVR MDX vocal separation, with NumPy STFT/overlap-add.
Model convention/settings: github.com/Anjok07/ultimatevocalremovergui
Inference convention: github.com/seanghay/uvr-mdx-infer (Apache-2.0)
No uploaded audio, no runtime dependency in the Minecraft mod.
"""
from pathlib import Path
import sys, wave, json, time, subprocess, argparse
import numpy as np
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--video',type=Path,required=True)
parser.add_argument('--model',type=Path,required=True)
parser.add_argument('--parameters',type=Path,required=True)
parser.add_argument('--runtime',type=Path,required=True)
parser.add_argument('--ffmpeg',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--stem',choices=['vocals','instrumental'],default='vocals')
parser.add_argument('--start',type=float,default=7)
parser.add_argument('--duration',type=float,default=6)
parser.add_argument('--voice-start',type=float,default=1.85)
parser.add_argument('--voice-end',type=float,default=3.65)
parser.add_argument('--context-path',type=Path,default=root/'.tools/saiyan-yell-context.wav')
args=parser.parse_args()
sys.path.insert(0,str(args.runtime))
import onnxruntime as ort
ffmpeg=args.ffmpeg; video=args.video
source=args.context_path
subprocess.run([str(ffmpeg),'-y','-hide_banner','-loglevel','error','-ss',str(args.start),'-i',str(video),'-t',str(args.duration),'-vn','-ac','2','-ar','44100','-c:a','pcm_s16le',str(source)],check=True)
with wave.open(str(source),'rb') as w:
    rate=w.getframerate(); mixture=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(np.float32).reshape(-1,2).T/32768
mixture *= .9 / max(.01, float(np.max(np.abs(mixture))))
params=next(iter(json.loads(args.parameters.read_text()).values()))
nfft=params['mdx_n_fft_scale_set']; dims=params['mdx_dim_f_set']; frames=2**params['mdx_dim_t_set'];hop=1024
length=hop*(frames-1); trim=nfft//2; keep=length-2*trim
window=np.hanning(nfft+1)[:-1].astype(np.float32)
def stft(signal):
    padded=np.pad(signal,((0,0),(trim,trim)),mode='reflect')
    view=np.lib.stride_tricks.sliding_window_view(padded,nfft,axis=-1)[:,::hop,:]*window
    spectrum=np.fft.rfft(view,axis=-1).transpose(0,2,1)
    channels=np.stack((spectrum.real,spectrum.imag),axis=1).reshape(1,4,nfft//2+1,frames)
    return channels[:,:,:dims,:].astype(np.float32)
def istft(pred):
    shaped=pred.reshape(2,2,dims,frames)
    complex_spec=shaped[:,0]+1j*shaped[:,1]
    complex_spec=np.pad(complex_spec,((0,0),(0,nfft//2+1-dims),(0,0)))
    pieces=np.fft.irfft(complex_spec.transpose(0,2,1),n=nfft,axis=-1)*window
    out=np.zeros((2,length+nfft),dtype=np.float64); normalization=np.zeros(length+nfft)
    for i in range(frames):
        start=i*hop;out[:,start:start+nfft]+=pieces[:,i];normalization[start:start+nfft]+=window**2
    out/=np.maximum(normalization,1e-8)
    return out[:,trim:trim+length].astype(np.float32)
# Check the transform before applying the trained model.
probe=np.zeros((2,length),np.float32); probe[:,:mixture.shape[1] if mixture.shape[1]<length else length]=mixture[:,:length]
reconstructed=istft(stft(probe))
assert np.sqrt(np.mean((probe[:,trim:-trim]-reconstructed[:,trim:-trim])**2))<.003
opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
session=ort.InferenceSession(str(args.model),opts,providers=['CPUExecutionProvider'])
pad=(-mixture.shape[1])%keep
extended=np.pad(mixture,((0,0),(trim,pad+trim)))
outputs=[]; started=time.monotonic()
for index,start in enumerate(range(0,mixture.shape[1]+pad,keep)):
    spectrogram=stft(extended[:,start:start+length])
    # Signed ensemble reduces asymmetric inference artifacts.
    pred=(session.run(None,{'input':spectrogram})[0]-session.run(None,{'input':-spectrogram})[0])*.5
    outputs.append(istft(pred)[:,trim:-trim])
    print(f'Voice separation chunk {index+1}: {time.monotonic()-started:.1f}s',flush=True)
vocals=np.concatenate(outputs,axis=1)[:,:mixture.shape[1]]*params['compensate']
# A music excerpt uses the complementary stem to remove all character voices.
stem=vocals if args.stem=='vocals' else mixture-vocals
voice=stem[:,round(args.voice_start*rate):round(args.voice_end*rate)]
args.output.parent.mkdir(parents=True,exist_ok=True)
with wave.open(str(args.output),'wb') as w:
    w.setnchannels(2);w.setsampwidth(2);w.setframerate(rate)
    w.writeframes((np.clip(voice.T,-1,1)*32767).astype('<i2').tobytes())
print('Isolated '+args.stem+' master saved: '+str(args.output),flush=True)
