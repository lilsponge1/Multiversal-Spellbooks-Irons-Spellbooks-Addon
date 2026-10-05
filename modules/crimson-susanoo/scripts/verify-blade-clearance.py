"""Sample the exported blade through complete clips, including bounded net weapon lowering."""
import json, math, sys
from pathlib import Path

root=Path(__file__).resolve().parent.parent
assets=root/'src/main/resources/assets/crimson_susanoo'
bones={b['name']:b for b in json.loads((assets/'geo/guardian.geo.json').read_text())['minecraft:geometry'][0]['bones']}
clips=json.loads((assets/'animations/guardian.animation.json').read_text())['animations']

def sample(track,t):
    if not track: return (0,0,0)
    if isinstance(track,list): return track
    keys=sorted((float(k),v) for k,v in track.items())
    if t<=keys[0][0]: return keys[0][1]
    for i,((a,x),(b,y)) in enumerate(zip(keys,keys[1:])):
        if t<=b:
            u=(t-a)/(b-a)
            result=[]
            for axis,(p,q) in enumerate(zip(x,y)):
                s=(q-p)/(b-a); m0=m1=0
                if i>0:
                    c,v=keys[i-1]; m0=tangent((p-v[axis])/(a-c),s,a-c,b-a)
                if i+2<len(keys):
                    c,v=keys[i+2]; m1=tangent(s,(v[axis]-q)/(c-b),b-a,c-b)
                result.append((2*u**3-3*u**2+1)*p+(u**3-2*u**2+u)*(b-a)*m0
                              +(-2*u**3+3*u**2)*q+(u**3-u**2)*(b-a)*m1)
            return tuple(result)
    return keys[-1][1]

def tangent(previous,next,previous_length,next_length):
    if previous*next<=0:return 0
    a=2*next_length+previous_length; b=next_length+2*previous_length
    return (a+b)/(a/previous+b/next)

def rotate(p,pivot,r):
    x,y,z=(p[i]-pivot[i] for i in range(3))
    a,b,c=map(math.radians,(-r[0],-r[1],r[2]))
    y,z=y*math.cos(a)-z*math.sin(a),y*math.sin(a)+z*math.cos(a)
    x,z=x*math.cos(b)+z*math.sin(b),-x*math.sin(b)+z*math.cos(b)
    x,y=x*math.cos(c)-y*math.sin(c),x*math.sin(c)+y*math.cos(c)
    return x+pivot[0],y+pivot[1],z+pivot[2]

def transforms(clip,time):
    result=[]; name='sword'
    while name:
        bone=bones[name]; ch=clip['bones'].get(name,{})
        result.append(((-bone['pivot'][0],*bone['pivot'][1:]),sample(ch.get('rotation'),time),sample(ch.get('position'),time)))
        name=bone.get('parent')
    return result

def world(p,chain):
    for pivot,r,offset in chain:
        p=rotate(p,pivot,r)
        p=(p[0]-offset[0],p[1]+offset[1],p[2]+offset[2])
    return p

def blade_points():
    # Same four cross-section vertices and all 48 slices as CrimsonKatana.
    for i in range(49):
        t=i/48; width=.42*(1-.24*t)*min(1,(1-t)/.15)*16
        thick=.095*(1-t*.7)*16
        center=(23-3*t,37+4*t*t,-(24+80*t))
        for dx,dy in ((-width/2,0),(0,thick),(width/2,0),(0,-thick)):
            yield (center[0]-dy,center[1]-dx,center[2]),t

def check():
    failed=False
    for name in ('idle','walk','follow','cleave','slash','crescent'):
        clip=clips['animation.guardian.'+name]; end=clip['animation_length']
        points=list(blade_points()); worst=(float('inf'),0,0)
        for step in range(math.ceil(end*120)+1):
            time=min(end,step/120); chain=transforms(clip,time)
            # Transform an affine basis once per sample rather than each vertex.
            o=world((0,0,0),chain); basis=[world(p,chain)[1]-o[1] for p in ((1,0,0),(0,1,0),(0,0,1))]
            for p,t in points:
                height=(o[1]+sum(p[i]*basis[i] for i in range(3))-8)/16
                worst=min(worst,(height,time,t))
        print(f'{name}: minimum steel height {worst[0]:.4f} blocks at {worst[1]:.4f}s, blade fraction {worst[2]:.3f}',flush=True)
        if worst[0]<.05: failed=True
    if failed: raise SystemExit('FAIL: blade has less than 0.05 block clearance under maximum support lowering')
    print('PASS: complete active clips clear flat ground with eight-pixel net weapon lowering. The full-stair arm compensation is checked separately by FootPlantingCheck. Terrain, pose transitions and live rendering still require separate checks.')

if __name__=='__main__': check()
