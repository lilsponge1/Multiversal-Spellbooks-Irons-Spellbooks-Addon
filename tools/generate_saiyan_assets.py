"""Saiyan visual resources; provided reference-video audio is built separately."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
ASSETS = ROOT / 'assets/irons_ultimate_explosion'

def save_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def save_image(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

book = Image.new('RGBA', (32, 32))
d = ImageDraw.Draw(book)
d.polygon([(4,7),(23,3),(28,7),(28,25),(10,30),(4,26)], fill='#432344', outline='#241a2b')
d.polygon([(7,9),(24,5),(27,8),(27,25),(10,29),(7,26)], fill='#fff0aa')
for y in range(11,26,3): d.line([(10,y),(25,y-4)],fill='#bf9454')
d.polygon([(4,7),(21,3),(24,6),(24,23),(7,28),(4,25)], fill='#be7121', outline='#582632')
d.polygon([(7,9),(20,6),(21,21),(8,25)],fill='#572a3c',outline='#ffcb48')
d.polygon([(9,20),(10,14),(13,16),(14,9),(17,15),(19,12),(18,20),(13,22)],fill='#ffbf27')
d.polygon([(12,20),(14,14),(16,20),(14,22)],fill='#fff7b5')
d.line([(18,8),(17,12),(21,12),(19,17)], fill='#bffbff',width=1)
d.line([(4,10),(6,11),(6,24)], fill='#f6ac2b',width=1)
save_image(ASSETS/'textures/item/book_of_saiyan_ascension.png',book)
icon_master = Path(__file__).resolve().parents[1] / 'art/saiyan/saiyan_ascension_master.png'
icon = Image.open(icon_master).convert('RGBA').resize((32,32), Image.Resampling.NEAREST)
save_image(ASSETS/'textures/gui/spell_icons/saiyan_ascension.png',icon)
# Soft translucent energy texel field; geometry supplies the gold/cream colors.
energy=Image.new('RGBA',(32,32))
for y in range(32):
    for x in range(32):
        edge=max(0,1-abs(x-15.5)/16)
        energy.putpixel((x,y),(255,255,255,int(255*(.25+.75*edge))))
save_image(ASSETS/'textures/entity/saiyan_energy.png',energy)
save_json(ASSETS/'models/item/book_of_saiyan_ascension.json',{'parent':'minecraft:item/generated','textures':{'layer0':'irons_ultimate_explosion:item/book_of_saiyan_ascension'}})
save_json(ROOT/'data/curios/tags/items/spellbook.json',{'replace':False,'values':['irons_ultimate_explosion:book_of_saiyan_ascension']})
save_json(ROOT/'data/curios/tags/items/head.json',{'replace':False,'values':['irons_ultimate_explosion:saiyan_wig']})
save_json(ROOT/'data/irons_ultimate_explosion/recipes/saiyan_wig.json',{
    'type':'minecraft:crafting_shaped','pattern':['WWW','LNG','WPW'],
    'key':{'G':{'item':'minecraft:golden_apple'},'L':{'item':'irons_spellbooks:lightning_bottle'},
           'W':{'item':'minecraft:black_wool'},'N':{'item':'minecraft:nether_star'},'P':{'item':'minecraft:blaze_powder'}},
    'result':{'item':'irons_ultimate_explosion:saiyan_wig'}})
legacy_recipe = ROOT/'data/irons_ultimate_explosion/recipes/book_of_saiyan_ascension.json'
legacy_recipe.unlink(missing_ok=True)
def hair_box(start, end, rotation=None):
    element={'from':start,'to':end,'faces':{face:{'texture':'#hair'} for face in ('north','south','east','west','up','down')}}
    if rotation is not None: element['rotation']=rotation
    return element
wig_elements=[hair_box([3,5,3],[13,9,13])]
for start,end,origin,angle in [
    ([6,8,7],[8,19,10],[7,8,8.5],-22.5),
    ([10,8,8],[12,17,11],[11,8,9.5],22.5),
    ([3,8,9],[5,14,12],[4,8,10.5],-45),
    ([12,8,10],[14,13,13],[13,8,11.5],45),
    ([5,8,12],[7,14,15],[6,8,13.5],-22.5),
    ([9,8,11],[11,16,14],[10,8,12.5],22.5)]:
    wig_elements.append(hair_box(start,end,{'origin':origin,'axis':'z','angle':angle}))
save_json(ASSETS/'models/item/saiyan_wig.json',{
    'textures':{'hair':'minecraft:block/black_wool','particle':'minecraft:block/black_wool'},
    'elements':wig_elements,'display':{
        'gui':{'rotation':[25,-30,0],'translation':[0,-2,0],'scale':[.62,.62,.62]},
        'ground':{'translation':[0,3,0],'scale':[.35,.35,.35]},
        'firstperson_righthand':{'rotation':[0,-30,0],'scale':[.5,.5,.5]},
        'thirdperson_righthand':{'rotation':[75,0,0],'scale':[.5,.5,.5]}}})
save_json(ASSETS/'player_animation/saiyan_charge.json',{'format_version':'1.8.0','animations':{'saiyan_charge':{
    'loop':True,'animation_length':.6,'bones':{
        'body':{'position':[0,-2.5,0],'rotation':{'0.0':[14,0,0],'.3':[17,0,0],'.6':[14,0,0]}},
        'head':{'position':[0,-2.5,0],'rotation':[-18,0,0]},
        'right_arm':{'position':[0,-2.5,0],'rotation':[-52,0,35]},
        'left_arm':{'position':[0,-2.5,0],'rotation':[-52,0,-35]},
        'right_leg':{'rotation':[-24,0,18]},'left_leg':{'rotation':[-24,0,-18]}}}}})
sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
for name,ref,pitch in [('saiyan_charge','irons_spellbooks:spell.black_hole.charge',1),
                       ('saiyan_charge_2','irons_spellbooks:loop.electrocute',1),
                       ('saiyan_crack','irons_spellbooks:entity.lightning_strike.strike',1),
                       ('saiyan_burst','minecraft:entity.generic.explode',.55),
                       ('saiyan_down','minecraft:block.beacon.deactivate',.85)]:
    entry={'name':ref,'type':'event'}
    if pitch != 1: entry['pitch']=pitch
    sounds[name]={'subtitle':f'subtitles.irons_ultimate_explosion.{name}','sounds':[entry]}
for name, file in [('saiyan_yell', 'yell'), ('saiyan_yell_2', 'yell_2')]:
    sounds[name]={'subtitle':'subtitles.irons_ultimate_explosion.saiyan_yell',
                  'sounds':[{'name':f'irons_ultimate_explosion:saiyan/{file}','stream':False}]}
save_json(ASSETS/'sounds.json',sounds)
lang_path=ASSETS/'lang/en_us.json'; lang=json.loads(lang_path.read_text(encoding='utf-8'))
lang.update({
 'spell.irons_ultimate_explosion.saiyan_ascension':'Saiyan Ascension',
 'spell.irons_ultimate_explosion.saiyan_ascension.guide':'Transform into Super Saiyan to boost your combat power. Tap again for Super Saiyan II: greater power, heavier mana drain. Both forms slow mana regeneration. Hold to power down.',
 'item.irons_ultimate_explosion.book_of_saiyan_ascension':'Book of Saiyan Ascension',
 'item.irons_ultimate_explosion.saiyan_wig':'Saiyan Wig',
 'tooltip.irons_ultimate_explosion.saiyan_wig':'Imbued with Saiyan Ascension. Wear in Curios: Head.',
 'tooltip.irons_ultimate_explosion.saiyan_book':'Two forms. One ascension.',
 'ui.irons_ultimate_explosion.saiyan.cost':'Activation: %s / %s mana',
 'ui.irons_ultimate_explosion.saiyan.drain':'Drain: %s / %s mana per second',
 'ui.irons_ultimate_explosion.saiyan.controls':'Tap to ascend; hold %ss to power down',
 'ui.irons_ultimate_explosion.saiyan.charging':'Saiyan Ascension: powering up',
 'ui.irons_ultimate_explosion.saiyan.ssj1':'Super Saiyan',
 'ui.irons_ultimate_explosion.saiyan.ssj2':'Super Saiyan II',
 'message.irons_ultimate_explosion.saiyan.mana':'Not enough mana to ascend',
 'subtitles.irons_ultimate_explosion.saiyan_charge':'Saiyan energy gathers',
 'subtitles.irons_ultimate_explosion.saiyan_charge_2':'Saiyan power surges',
 'subtitles.irons_ultimate_explosion.saiyan_yell':'Saiyan roars with power',
 'subtitles.irons_ultimate_explosion.saiyan_crack':'Ascension lightning cracks',
 'subtitles.irons_ultimate_explosion.saiyan_burst':'Saiyan energy erupts',
 'subtitles.irons_ultimate_explosion.saiyan_down':'Saiyan energy fades'})
save_json(lang_path,lang)
print('Generated Saiyan icon, legacy book, wig, energy texture, head tag, recipe, animation, sound events and translations.')
