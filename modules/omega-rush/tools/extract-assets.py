"""Extract authorized reference frames, keeping their source alpha and common center."""
from pathlib import Path
import json, hashlib
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
source=Path(r'C:\Users\jraym\Downloads\569577.png')
image=Image.open(source).convert('RGBA')
assert image.size==(2532,759), 'Unexpected reference dimensions; inspect before extracting.'
assets=root/'src/main/resources/assets/irons_omega_rush'
textures=assets/'textures/particle'
textures.mkdir(parents=True,exist_ok=True)
sequences={}
for name,y0,y1,count in [('a',39,266,8),('b',284,511,5),('c',529,756,7)]:
    names=[]
    for index in range(count):
        x0=3 if index==0 else 319+(index-1)*316
        x1=317+index*316
        crop=image.crop((x0,y0,x1,y1))
        canvas=Image.new('RGBA',(320,320))
        canvas.alpha_composite(crop,((320-crop.width)//2,(320-crop.height)//2))
        canvas=canvas.resize((256,256),Image.Resampling.NEAREST)
        path=textures/f'explosion_{name}_{index}.png'
        canvas.save(path)
        names.append(f'irons_omega_rush:explosion_{name}_{index}')
    sequences[name]=names
particles=assets/'particles'; particles.mkdir(parents=True,exist_ok=True)
(particles/'rainbow_ring.json').write_text(json.dumps({'textures':sequences['c']},indent=2))
(particles/'rainbow_pulse.json').write_text(json.dumps({'textures':sequences['b']},indent=2))
(particles/'rainbow_globe.json').write_text(json.dumps({'textures':sequences['a']},indent=2))
effect=assets/'textures/effect'; effect.mkdir(parents=True,exist_ok=True)
Image.new('RGBA',(64,64),(255,255,255,255)).save(effect/'white.png')
icon=Image.new('RGBA',(64,64)); draw=ImageDraw.Draw(icon)
colors=['#ff48cc','#8650ff','#00dcff','#50ff65','#ffe84a','#ff8245']
for i,color in enumerate(colors):
    draw.arc((5,5,58,58),i*60,i*60+63,fill=color,width=7)
draw.polygon([(33,12),(26,30),(15,32),(27,37),(30,52),(37,36),(49,32),(38,27)],fill='white')
spell=assets/'textures/spell'; spell.mkdir(parents=True,exist_ok=True); icon.save(spell/'omega_rush.png')
lang=assets/'lang'; lang.mkdir(parents=True,exist_ok=True)
(lang/'en_us.json').write_text(json.dumps({
    'spell.irons_omega_rush.omega_rush':'Omega Rush',
    'spell.irons_omega_rush.omega_rush.description':'Gather rainbow energy, steer through the sky, and leave a cascading wall of explosions. Sneak to end flight.',
    'ui.irons_omega_rush.damage':'Trail damage: %s',
    'ui.irons_omega_rush.duration':'Flight: %s seconds',
    'ui.irons_omega_rush.radius':'Explosion radius: %s blocks',
    'message.irons_omega_rush.unavailable':'Omega Rush requires unobstructed player flight without another movement spell.'
},indent=2))
manifest={'source':str(source),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'dimensions':image.size,'sequences':sequences}
(root/'build/asset-manifest.json').write_text(json.dumps(manifest,indent=2))
print('Extracted 20 transparent explosion frames, spell icon and overlay texture.')
