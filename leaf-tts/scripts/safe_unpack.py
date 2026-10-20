#!/usr/bin/env python3
"""Only accept regular files, inside leaf-tts relative paths, from ZIP/TGZ."""
import sys, tarfile, zipfile, shutil
from pathlib import Path

arc, root, backup = map(Path, sys.argv[1:4])
root = root.resolve()
allowed = (
    'scripts/', 'tools/', 'config/',
    'generated/vp_tray/', 'generated/wisp_butterfly_tray/', 'generated/token_tray/', 'generated/dice_trays/', 'generated/player_box/', 'generated/game_box/',
    'assets/trays/', 'assets/files.txt',
    'source/TS_Save_latest.json', 'output/TS_Save_latest.json',
    'README-VP-TRAY-WORKFLOW.md', 'example/'
)

def clean(name):
    p = Path(name)
    if name.startswith('leaf-tts/'):
        p = Path(name[len('leaf-tts/'):])
    if p.is_absolute() or '..' in p.parts or not p.parts:
        return None
    s = p.as_posix()
    if not any(s == a or s.startswith(a) for a in allowed):
        return None
    return p

items = []
if zipfile.is_zipfile(arc):
    with zipfile.ZipFile(arc) as z:
        for entry in z.infolist():
            if entry.is_dir():
                continue
            p = clean(entry.filename)
            if p is None:
                raise SystemExit('Rejected unsafe path: ' + entry.filename)
            items.append((p, z.read(entry)))
elif tarfile.is_tarfile(arc):
    with tarfile.open(arc, 'r:*') as t:
        for m in t:
            if m.isdir():
                continue
            p = clean(m.name)
            if p is None or not m.isfile():
                raise SystemExit('Rejected unsafe tar entry: ' + m.name)
            items.append((p, t.extractfile(m).read()))
else:
    raise SystemExit('Expected zip or tar.gz archive')

for p, content in items:
    dest = root / 'output/TS_Save_latest.json' if p == Path('source/TS_Save_latest.json') else root / p
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        old = backup / p
        old.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(dest, old)
    dest.write_bytes(content)
    print('Installed:', dest.relative_to(root))
