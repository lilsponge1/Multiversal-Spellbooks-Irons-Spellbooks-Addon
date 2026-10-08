"""Disposable full-pack check. Copies inputs; never loads an existing world or deploys to a live pack."""
from pathlib import Path
import argparse, json, re, shutil, subprocess, time

parser=argparse.ArgumentParser()
parser.add_argument('--java', required=True)
parser.add_argument('--addon', required=True)
parser.add_argument('--harness', required=True)
parser.add_argument('--fixture', required=True)
args=parser.parse_args()
args.java=str(Path(args.java).resolve())
root=Path(__file__).resolve().parents[1]
fixture=Path(args.fixture).resolve()
if not fixture.is_relative_to(root/'.tools'):
    raise SystemExit('The fixture must be inside this workspace .tools directory.')
source=root.parent/'crimson-susanoo/run-pack'
if fixture.exists(): raise SystemExit('Use a new fixture path; existing fixtures are preserved.')
fixture.mkdir(parents=True)
for folder in ['libraries','mods','config','defaultconfigs']:
    if not (source/folder).exists(): continue
    if folder=='mods':
        (fixture/folder).mkdir()
        for path in (source/folder).glob('*.jar'):
            if path.name.startswith(('multiversal-spellbooks-', 'grand-explosion-', 'crimson_susanoo-', 'ignis_armor_compat-')): continue
            shutil.copy2(path, fixture/folder/path.name)
    else: shutil.copytree(source/folder,fixture/folder)
shutil.copy2(source/'eula.txt',fixture/'eula.txt')
for path in [Path(args.addon),Path(args.harness)]: shutil.copy2(path,fixture/'mods'/path.name)
(fixture/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25569\nonline-mode=false\nlevel-name=world\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nenable-rcon=false\nmotd=Disposable Saiyan check\n',encoding='utf-8')
log=fixture/'console.log'
command=[args.java,'-XX:ActiveProcessorCount=2','-Xms512M','-Xmx3G','-Dsaiyan.diagnostics=true','@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt','nogui']
with log.open('w',encoding='utf-8') as output:
    process=subprocess.Popen(command,cwd=fixture,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    deadline=time.monotonic()+360
    next_progress=time.monotonic()+25
    while process.poll() is None and time.monotonic()<deadline:
        contents=log.read_text(encoding='utf-8',errors='replace')
        if 'Saiyan diagnostics:' in contents and 'Done (' in contents: break
        if 'Exception in thread "main"' in contents and 'Done (' not in contents:
            process.terminate(); process.wait(timeout=20); break
        if time.monotonic()>next_progress:
            print('Disposable server is loading; latest log: '+contents.splitlines()[-1][-180:] if contents else 'Waiting for server output',flush=True)
            next_progress=time.monotonic()+25
        time.sleep(1)
    forced=False
    if process.poll() is None:
        process.stdin.write('stop\n');process.stdin.flush()
        try: process.wait(timeout=90)
        except subprocess.TimeoutExpired:
            forced=True
            dump=subprocess.run([str(Path(args.java).parent/'jcmd.exe'),str(process.pid),'Thread.print'],capture_output=True,text=True,timeout=20)
            (fixture/'shutdown-threads.txt').write_text(dump.stdout+dump.stderr,encoding='utf-8')
            process.terminate();process.wait(timeout=20)
contents=log.read_text(encoding='utf-8',errors='replace')
match=re.search(r'Saiyan diagnostics: (\d+) passed, (\d+) failed',contents)
result={'fixture':str(fixture),'startup':'Done (' in contents,'passed':int(match[1]) if match else None,'failed':int(match[2]) if match else None,'exit_code':process.returncode,'forced':forced}
(fixture/'result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2),flush=True)
for line in contents.splitlines():
    if any(marker in line for marker in ['SAIYAN_FAIL','at local.ironsultimateexplosion.Saiyan', 'at local.grandexplosiontest.Saiyan']): print(line[-500:])
if not match or int(match[2]) or not result['startup'] or forced or process.returncode: raise SystemExit(1)
