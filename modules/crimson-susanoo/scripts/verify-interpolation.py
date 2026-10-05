"""Compile and exercise the real GeckoLib easing API with the exported pose tracks."""
import json, os, subprocess, zipfile
from pathlib import Path

root=Path(__file__).resolve().parent.parent
out=root/'build/interpolation-check'; out.mkdir(parents=True,exist_ok=True)
gecko=root/'libs/geckolib-forge-1.20.1-4.8.4.jar'
with zipfile.ZipFile(gecko) as archive:
    (out/'mclib-20.jar').write_bytes(archive.read('META-INF/jarjar/mclib-20.jar'))
fastutil=Path.home()/'.gradle/caches/forge_gradle/maven_downloader/it/unimi/dsi/fastutil/8.5.9/fastutil-8.5.9.jar'
classpath=os.pathsep.join(map(str,(out,gecko,out/'mclib-20.jar',fastutil)))
clips=json.loads((root/'src/main/resources/assets/crimson_susanoo/animations/guardian.animation.json').read_text())['animations']
fixture=out/'tracks.tsv'
with fixture.open('w') as file:
    for clip_name,clip in clips.items():
        for bone,channels in clip['bones'].items():
            for channel,track in channels.items():
                if channel not in ('rotation','position'):continue
                for axis in range(3):
                    for time,value in track.items():
                        file.write(f'{clip_name}/{bone}/{channel}/{axis}\t{time}\t{value[axis]}\n')
java=Path(os.environ['JAVA_HOME'])/'bin'
sources=[root/'src/main/java/com/crimson_susanoo/animation'/name for name in ('SmoothKeyframeEasing.java','ClientAnimationClock.java')]
subprocess.run([str(java/'javac.exe'),'-cp',classpath,'-d',str(out),*map(str,sources),str(root/'scripts/InterpolationCheck.java')],check=True)
subprocess.run([str(java/'java.exe'),'-cp',classpath,'com.crimson_susanoo.animation.InterpolationCheck',str(fixture)],check=True)
