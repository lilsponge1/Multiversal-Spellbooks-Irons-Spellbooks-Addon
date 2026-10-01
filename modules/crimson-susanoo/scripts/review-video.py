"""Sample a supplied local recording into timestamped sheets for visual review."""
import argparse, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / '.tools/video'))
import av
from PIL import Image, ImageDraw

parser = argparse.ArgumentParser()
parser.add_argument('video', type=Path)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--start', type=float, default=0)
parser.add_argument('--end', type=float)
parser.add_argument('--interval', type=float, default=8)
parser.add_argument('--crop', type=int, nargs=4)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
container = av.open(str(args.video))
stream = container.streams.video[0]
duration = float(stream.duration * stream.time_base) if stream.duration else container.duration / av.time_base
metadata = {'video': str(args.video), 'seconds': duration,
            'fps': float(stream.average_rate), 'width': stream.width, 'height': stream.height}
(args.output / 'metadata.json').write_text(json.dumps(metadata, indent=2))
print(json.dumps(metadata))
times = []
t = args.start
while t < min(duration, args.end if args.end is not None else duration):
    times.append(t)
    t += args.interval
tiles = []
for t in times:
    container.seek(int(t / stream.time_base), stream=stream, backward=True)
    for frame in container.decode(stream):
        actual = float(frame.pts * stream.time_base)
        if actual + .0001 < t:
            continue
        original = frame.to_image()
        original.save(args.output / f'frame-{t:07.3f}.jpg', quality=92)
        picture = original.crop(args.crop) if args.crop else original
        picture.thumbnail((640, 360))
        tile = Image.new('RGB', (640, 390), (20, 20, 24))
        tile.paste(picture, ((640-picture.width)//2, 30))
        ImageDraw.Draw(tile).text((8, 8), f'{actual:.3f} seconds', fill='white')
        tiles.append(tile)
        break
for batch_start in range(0, len(tiles), 18):
    batch = tiles[batch_start:batch_start+18]
    rows = (len(batch)+2)//3
    sheet = Image.new('RGB', (1920, 390*rows), (20, 20, 24))
    for i, tile in enumerate(batch):
        sheet.paste(tile, ((i%3)*640, (i//3)*390))
    path = args.output / f'sheet-{batch_start//18:02d}.jpg'
    sheet.save(path, quality=92)
    print(str(path.resolve()))
container.close()
