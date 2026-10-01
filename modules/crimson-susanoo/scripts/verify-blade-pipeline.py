"""Check complete production pose layers against the exported katana, without a client."""
import csv, importlib.util, math, os, subprocess, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('blade',ROOT/'scripts/verify-blade-clearance.py')
blade=importlib.util.module_from_spec(spec); spec.loader.exec_module(blade)
NAMES=list(blade.bones)
OUT=ROOT/'build/blade-pipeline-check'; OUT.mkdir(parents=True,exist_ok=True)
BASELINE='--baseline' in sys.argv
if not BASELINE:
    model=(ROOT/'src/main/java/com/crimson_susanoo/client/CrimsonModel.java').read_text()
    assert model.index('locomotionPoses.computeIfAbsent') < model.index('FootPlanting.supportWeapon(blended)') < model.index('blend.remember(blended)'), 'Renderer must finalize weapon support after counterbalance and before recording history'
arm_bone=blade.bones['right_arm']; shoulder_cube=blade.bones['right_shoulder']['cubes'][0]
a=shoulder_cube['origin']; size=shoulder_cube['size']
shoulder_bounds=((-a[0]-size[0],-a[0]),(a[1],a[1]+size[1]),(a[2],a[2]+size[2]))

def values(clip,time):
    for name in NAMES:
        channels=clip['bones'].get(name,{})
        r=blade.sample(channels.get('rotation'),time); p=blade.sample(channels.get('position'),time)
        yield from (-math.radians(r[0]),-math.radians(r[1]),math.radians(r[2]),-p[0],p[1],p[2])

def fixture(path):
    with path.open('w',newline='') as f:
        out=csv.writer(f,delimiter='\t'); out.writerow(NAMES)
        for fps in (30,60,144):
            for action,clip_name,hit in ((1,'cleave',11),(2,'crescent',14),(3,'slash',20)):
                clip=blade.clips['animation.guardian.'+clip_name]
                walk=blade.clips['animation.guardian.follow']
                idle=blade.clips['animation.guardian.idle']
                end=clip['animation_length']*20
                for drop in (0,1):
                    for offset in (0,3,6):
                        start=12+offset; length=start+end+40
                        label=f'{clip_name}-fps{fps}-step{drop}-offset{offset}'
                        for i in range(math.ceil(length*fps/20)+1):
                            t=min(length,i*20/fps)
                            if t<start:
                                active=0; phase=0; z=t*.24; yaw=0
                                authored=walk; sample_time=(t/20)%walk['animation_length']
                            elif t<=start+end:
                                active=action; phase=t-start
                                z=start*.24+(0 if action==2 else .72*max(0,min(1,(phase-(hit-7))/6)))
                                yaw=0 if drop else 25*min(1,phase/max(1,hit-4))
                                authored=clip; sample_time=phase/20
                            else:
                                active=0; phase=0; recovery=t-start-end
                                z=start*.24+(0 if action==2 else .72)+min(recovery,20)*.24
                                yaw=0 if drop else 25
                                authored=walk if recovery<20 else idle
                                sample_time=((recovery if recovery<20 else recovery-20)/20)%authored['animation_length']
                            out.writerow((label,t,drop,z,yaw,active,phase,*values(authored,sample_time)))

def chain(pose):
    result=[]; name='sword'
    while name:
        bone=blade.bones[name]; v=pose[name]
        result.append(((-bone['pivot'][0],*bone['pivot'][1:]),
                       (-math.degrees(v[0]),-math.degrees(v[1]),math.degrees(v[2])),(-v[3],v[4],v[5])))
        name=bone.get('parent')
    return result

def arm_lift(pose):
    v=tuple(pose['right_arm'][3:6])
    for name in ('right_shoulder','ribcage','waist','pelvis'):
        r=pose[name]
        v=blade.rotate(v,(0,0,0),(-math.degrees(r[0]),-math.degrees(r[1]),math.degrees(r[2])))
    return v

def check(path):
    points=list(blade.blade_points()); minimum={}; failures=[]; samples=0; max_lowering=0; max_arm_lift=0; scenarios=set()
    with path.open(newline='') as f:
        reader=csv.reader(f,delimiter='\t'); names=next(reader)
        for row in reader:
            label=row[0]; t=float(row[1]); action=int(row[5]); phase=float(row[6]); scenarios.add(label)
            v=list(map(float,row[7:])); pose={name:v[i*6:i*6+6] for i,name in enumerate(names)}
            lift=arm_lift(pose); lowering=-pose['pelvis'][4]-lift[1]; max_lowering=max(max_lowering,lowering)
            if lowering>8.00001 or abs(lift[0])>1e-5 or abs(lift[2])>1e-5:
                if len(failures)<6:failures.append(f'{label} tick{t:.3f}: net lowering {lowering:.6f}px; nonvertical lift {lift}')
            translation=pose['right_arm'][3:6]
            max_arm_lift=max(max_arm_lift,math.sqrt(sum(v*v for v in translation)))
            anchor=(-arm_bone['pivot'][0]+translation[0],arm_bone['pivot'][1]+translation[1],arm_bone['pivot'][2]+translation[2])
            if any(not lo-1e-5<=v<=hi+1e-5 for v,(lo,hi) in zip(anchor,shoulder_bounds)) and len(failures)<6:
                failures.append(f'{label} tick{t:.3f}: arm joint leaves primary shoulder armor {anchor}')
            transforms=chain(pose); origin=blade.world((0,0,0),transforms)
            basis=[blade.world(p,transforms)[1]-origin[1] for p in ((1,0,0),(0,1,0),(0,0,1))]
            height=min((origin[1]+sum(p[i]*basis[i] for i in range(3)))/16 for p,_ in points)
            key='idle/moving' if not action else ('cleave','crescent','slash')[action-1]
            minimum[key]=min(minimum.get(key,(math.inf,'',0)),(height,label,t))
            if height<.05 and len(failures)<6:failures.append(f'{label} tick{t:.3f}: steel height {height:.6f} blocks')
            samples+=1
    for name,(height,label,t) in minimum.items():print(f'{name}: minimum full-pipeline steel height {height:.6f} blocks ({label} tick{t:.3f})',flush=True)
    print(f'{len(scenarios)} scenarios / {samples} full-rig poses; maximum net weapon lowering {max_lowering:.6f} pixels',flush=True)
    print(f'Maximum arm support translation {max_arm_lift:.6f} pixels; joint anchor checked inside exported shoulder armor',flush=True)
    if failures:raise SystemExit('FAIL:\n'+'\n'.join(failures))
    print('PASS: sampled moving/idle attack entry and recovery clear the high reference plane with production layers. Idealized split support; native controller transitions, real terrain collision and Solas rendering still need live checks.')

fixture_path=OUT/'authored.tsv'; result=OUT/('baseline.tsv' if BASELINE else 'corrected.tsv')
fixture(fixture_path)
java=Path(os.environ['JAVA_HOME'])/'bin'
sources=[ROOT/'scripts/BladePoseTrace.java']+[ROOT/f'src/main/java/com/crimson_susanoo/client/{name}.java' for name in ('FootPlanting','AttackPoseBlend','LocomotionPose')]
subprocess.run([str(java/'javac.exe'),'-d',str(OUT),*map(str,sources)],check=True)
subprocess.run([str(java/'java.exe'),'-cp',str(OUT),'com.crimson_susanoo.client.BladePoseTrace',str(fixture_path),str(result),*(['baseline'] if BASELINE else [])],check=True)
check(result)