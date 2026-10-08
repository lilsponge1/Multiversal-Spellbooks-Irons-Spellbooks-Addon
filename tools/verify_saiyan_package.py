"""Validate the deliverable's assets, native icon location, protocol and dependency boundaries."""
import json, sys, zipfile
from pathlib import Path
from PIL import Image
from io import BytesIO

path=Path(sys.argv[1])
with zipfile.ZipFile(path) as jar:
    names=set(jar.namelist())
    required=[
        'local/ironsultimateexplosion/SaiyanManager.class',
        'local/ironsultimateexplosion/SaiyanClient.class',
        'local/ironsultimateexplosion/mixin/SaiyanCastInputMixin.class',
        'assets/irons_ultimate_explosion/textures/gui/spell_icons/saiyan_ascension.png',
        'assets/irons_ultimate_explosion/textures/item/book_of_saiyan_ascension.png',
        'assets/irons_ultimate_explosion/textures/entity/saiyan_energy.png',
        'assets/irons_ultimate_explosion/models/item/book_of_saiyan_ascension.json',
        'assets/irons_ultimate_explosion/player_animation/saiyan_charge.json',
        'data/curios/tags/items/spellbook.json',
        'assets/irons_ultimate_explosion/models/item/saiyan_wig.json',
        'local/ironsultimateexplosion/SaiyanWig.class',
        'local/ironsultimateexplosion/SaiyanWigRenderer.class',
        'data/curios/tags/items/head.json',
        'data/irons_ultimate_explosion/recipes/saiyan_wig.json']
    required += ['assets/irons_ultimate_explosion/sounds/saiyan/yell.ogg',
                 'assets/irons_ultimate_explosion/sounds/saiyan/yell_2.ogg']
    third='local/ironsultimateexplosion/SaiyanCombatPacket.class' in names
    if third:
        required+=['local/ironsultimateexplosion/SaiyanSequenceSound.class']
        required+=['assets/irons_ultimate_explosion/sounds/saiyan/'+name+'.ogg' for name in ['ssj3_charge','ssj3_yell','ssj3_music']]
    for name in required: assert name in names, 'Missing '+name
    for name in names:
        if name.endswith('.json'): json.loads(jar.read(name).decode('utf-8-sig'))
        if name.endswith('.class'):
            assert not name.startswith(('io/redspace/','net/minecraft/','net/minecraftforge/','software/bernie/','local/grandexplosiontest/','local/saiyanclienttest/')), 'Unexpected class '+name
            assert 'Harness' not in name, 'Test class packaged '+name
            assert 'SaiyanPacketCheck' not in name, 'Packet test packaged '+name
    for name in required:
        if name.endswith('.png'):
            im=Image.open(BytesIO(jar.read(name))); im.verify()
    mixins=json.loads(jar.read('irons_ultimate_explosion.mixins.json'))
    assert 'SaiyanCastInputMixin' in mixins['client'] and 'SaiyanCastInputMixin' not in mixins['mixins']
    sounds=json.loads(jar.read('assets/irons_ultimate_explosion/sounds.json'))
    for name in ['saiyan_charge','saiyan_charge_2','saiyan_crack','saiyan_burst','saiyan_down']:
        assert name in sounds
        assert all(s['type']=='event' and s['name'].startswith(('minecraft:', 'irons_spellbooks:')) for s in sounds[name]['sounds'])
    for name, file in [('saiyan_yell','yell'),('saiyan_yell_2','yell_2')]:
        assert sounds[name]['sounds'][0]['name']=='irons_ultimate_explosion:saiyan/'+file
        assert jar.read('assets/irons_ultimate_explosion/sounds/saiyan/'+file+'.ogg').startswith(b'OggS')
    lang=json.loads(jar.read('assets/irons_ultimate_explosion/lang/en_us.json'))
    if third:
        for event,file in [('saiyan_charge_3','ssj3_charge'),('saiyan_yell_3','ssj3_yell'),('saiyan_music_3','ssj3_music')]:
            assert sounds[event]['sounds'][0]['name']=='irons_ultimate_explosion:saiyan/'+file
            assert sounds[event]['subtitle'] in lang
            assert jar.read('assets/irons_ultimate_explosion/sounds/saiyan/'+file+'.ogg').startswith(b'OggS')
        assert lang['ui.irons_ultimate_explosion.saiyan.ssj3']=='Super Saiyan III'
        assert lang['ui.irons_ultimate_explosion.saiyan.drain'].count('%s')==3
    assert lang['spell.irons_ultimate_explosion.saiyan_ascension']=='Saiyan Ascension'
    assert lang['item.irons_ultimate_explosion.saiyan_wig']=='Saiyan Wig'
    assert 'irons_ultimate_explosion:saiyan_wig' in json.loads(jar.read('data/curios/tags/items/head.json'))['values']
    assert json.loads(jar.read('data/irons_ultimate_explosion/recipes/saiyan_wig.json'))['result']['item']=='irons_ultimate_explosion:saiyan_wig'
    assert 'data/irons_ultimate_explosion/recipes/book_of_saiyan_ascension.json' not in names
    assert 'saiyan-offscreen.mixins.json' not in names
    print('Saiyan package verified: native icon, Curios Head wig, recipe, animation, sounds, client-only input mixin and no dependency/test classes.')
