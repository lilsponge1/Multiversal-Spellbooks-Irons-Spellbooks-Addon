"""Build separate opt-in Ki diagnostics from the existing regression tests."""
from pathlib import Path
import argparse, importlib.util, json, os, subprocess, tempfile, zipfile

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('ki_build',ROOT/'tools/build_ki_standalone.py')
build=importlib.util.module_from_spec(spec);spec.loader.exec_module(build)
parser=argparse.ArgumentParser()
parser.add_argument('--jdk-bin',type=Path,required=True)
parser.add_argument('--classpath-file',type=Path,required=True)
parser.add_argument('--addon',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
assert not args.output.exists()
args.output.parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(dir=args.output.parent,prefix='ki-test-compile-') as temporary:
    temp=Path(temporary);sources=[]
    for name in ['SaiyanHarness','SaiyanPacketCheck']:
        path=ROOT/'src/test/java/local/ironsultimateexplosion'/(name+'.java')
        target=temp/(name+'.java')
        text=build.transform(path.read_text(encoding='utf-8')).replace('local.grandexplosiontest','local.kischooltest')
        target.write_text(text,encoding='utf-8');sources.append(target)
    sources+=sorted((ROOT/'src/standaloneTest/java').rglob('*.java'))
    classes=temp/'classes';classes.mkdir()
    cp=[args.addon.resolve()]+[Path(p).resolve() for p in json.loads(args.classpath_file.read_text(encoding='utf-8'))]
    arguments=['-J-Xmx192m','-J-XX:ActiveProcessorCount=1','-proc:none','-encoding','UTF-8','-cp','"'+os.pathsep.join(p.as_posix() for p in cp)+'"','-d','"'+classes.as_posix()+'"']
    arguments+=['"'+p.as_posix()+'"' for p in sources]
    argfile=temp/'javac.args';argfile.write_text('\n'.join(arguments)+'\n',encoding='utf-8')
    run=subprocess.run([str(args.jdk_bin/'javac.exe'),'@'+str(argfile)],capture_output=True,text=True)
    (args.output.parent/'ki-test-compile.log').write_text(run.stdout+run.stderr,encoding='utf-8')
    if run.returncode:print(run.stderr[-8000:]);raise SystemExit(run.returncode)
    payload={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
    payload['META-INF/MANIFEST.MF']=b'Manifest-Version: 1.0\nMixinConfigs: ki_school_test.mixins.json\n\n'
    payload['META-INF/mods.toml']=b'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="ki_school_test"\nversion="1.0.0"\ndisplayName="Ki disposable diagnostics"\n[[dependencies.ki_school_test]]\nmodId="ki_school"\nmandatory=true\nversionRange="[1,)"\nordering="AFTER"\nside="BOTH"\n'
    payload['pack.mcmeta']=b'{"pack":{"pack_format":15,"description":"Disposable Ki diagnostics"}}'
    payload['ki_school_test.mixins.json']=json.dumps({'required':True,'minVersion':'0.8','package':'local.kischooltest.mixin','compatibilityLevel':'JAVA_17','client':['HiddenWindow'],'injectors':{'defaultRequire':1}}).encode()
    with zipfile.ZipFile(args.output,'x',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in payload.items():archive.writestr(name,data)
print('Built separate native Ki regression and hidden-render diagnostics.')
