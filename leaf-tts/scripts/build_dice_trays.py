#!/usr/bin/env python3
"""Build the two open Graft Bed dice trays and stage their contents in a TTS save."""
import argparse
import copy
import json
from pathlib import Path
from PIL import Image, ImageOps

P = argparse.ArgumentParser()
P.add_argument('--save', type=Path, default=Path('source/TS_Save_latest.json'))
P.add_argument('--assets', type=Path, default=Path('assets/trays'))
P.add_argument('--output', type=Path, default=Path('generated/dice_trays'))
P.add_argument('--tts-root', default=None)
P.add_argument('--tray', choices=('both', 'low', 'high'), default='both')
args = P.parse_args()
if args.tts_root is None:
    args.tts_root = 'Z:' + str(Path.cwd().resolve())
args.output.mkdir(parents=True, exist_ok=True)

# Dimensions: actual strip trays are 7.5" long by 2.75" wide.
UPI = 3.06 / 3.5 * 1.67
W = 7.5 * UPI          # long axis (local X, world Z after rotY=270)
D = 2.75 * UPI         # short axis (local Z, world X)
H = 1.0 * UPI
T = 0.12 * UPI
FLOOR = 0.12 * UPI
TRAY_Y = 1.05

# Edit these if the two trays need moving relative to the other trays.
TRAY_SETTINGS = {
    'low': {
        'art': 'Graft Bed_ Strip D4, D6, D8',
        'bag': 'Dice Tray 1 - D4 D6 D8',
        'x': 7.5,
'z': 10.0,
'guid': 'f0b001',
        'contents': ('D8', 'D6', 'D4'),  # top to bottom in the artwork
    },
    'high': {
        'art': 'Graft Bed_ Strip D10, D12, D20',
        'bag': 'Dice Tray 2 - D10 D12 D20',
        'x': 12.25,
        'z': 10.0,
        'guid': 'f0b101',
        'contents': ('D20', 'D12', 'D10'),
    },
}

# World Z positions of the centers of the three compartments, top to bottom.
# Move individual dice types by adjusting their coordinates below.
DICE_OFFSETS = {
    'D4': (0.0, -W/3),
    'D6': (0.0, 0.0),
    'D8': (0.0, +W/3),
    'D10': (0.0, -W/3),
    'D12': (0.0, 0.0),
    'D20': (0.0, +W/3),
}
DICE_GRID_SPACING = 0.85  # same nine-position arrangement for each supply
DICE_BASE_Y = TRAY_Y + H + 0.48


def artwork_path(stem):
    for ext in ('.png', '.jpg'):
        path = args.assets / (stem + ext)
        if path.is_file():
            return path
    raise FileNotFoundError(f'Missing {stem}.png or {stem}.jpg in {args.assets}')


def build_model(tag, cfg):
    out = args.output
    art = ImageOps.mirror(Image.open(artwork_path(cfg['art'])).convert('RGB'))
    grass = Image.open(args.assets / 'grassy_field.png').convert('RGB')
    atlas = Image.new('RGB', (1024, 1024))
    atlas.paste(art.resize((512, 512), Image.Resampling.LANCZOS), (0, 0))
    g = ImageOps.fit(grass, (512, 512))
    for at in ((512, 0), (0, 512), (512, 512)):
        atlas.paste(g, at)
    prefix = f'Dice_Tray_{tag}'
    atlas.save(out / f'{prefix}_Texture.png')
    vertices, uvs, faces = [], [], []

    def quad(points, coordinates):
        vi = len(vertices) + 1
        ti = len(uvs) + 1
        vertices.extend(points)
        uvs.extend(coordinates)
        faces.append((tuple(range(vi,vi+4)), tuple(range(ti,ti+4))))

    def box(x0,x1,y0,y1,z0,z1, floor_top=False):
        grassuv=[(.52,.98),(.98,.98),(.98,.52),(.52,.52)]
        flooruv=[(.01,.99),(.49,.99),(.49,.51),(.01,.51)]
        quads=[
            ([(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)], flooruv if floor_top else grassuv),
            ([(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)], grassuv),
            ([(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(x1,y0,z0)], grassuv),
            ([(x1,y0,z1),(x1,y1,z1),(x0,y1,z1),(x0,y0,z1)], grassuv),
            ([(x0,y0,z1),(x0,y1,z1),(x0,y1,z0),(x0,y0,z0)], grassuv),
            ([(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)], grassuv),
        ]
        for ps, ts in quads:
            quad(ps, ts)

        # The artwork is designed for the INSIDE floor, not under the outer walls.
    # Make the structural floor grass-textured, then map the complete artwork
    # onto a separate inset quad, one wall thickness in from every edge.
    box(-W/2, W/2, 0, FLOOR, -D/2, D/2, False)
    floor_uv = [(.01,.99),(.49,.99),(.49,.51),(.01,.51)]
    art_y = FLOOR + 0.001  # prevents coplanar Z-fighting with the floor
    quad(
        [(-W/2+T,art_y,-D/2+T),
         (-W/2+T,art_y, D/2-T),
         ( W/2-T,art_y, D/2-T),
         ( W/2-T,art_y,-D/2+T)],
        floor_uv
    )
    box(-W/2, -W/2+T, FLOOR, H, -D/2, D/2)
    box(W/2-T, W/2, FLOOR, H, -D/2, D/2)
    box(-W/2+T, W/2-T, FLOOR, H, -D/2, -D/2+T)
    box(-W/2+T, W/2-T, FLOOR, H, D/2-T, D/2)
    # Interior walls span the short direction. The long local X axis becomes world Z.
    for x in (-W/6, W/6):
        box(x-T/2, x+T/2, FLOOR, H, -D/2+T, D/2-T)

    obj=[f'mtllib {prefix}.mtl', f'o Leaf_And_Let_Die_{prefix}']
    obj += ['v %.6f %.6f %.6f' % v for v in vertices]
    obj += ['vt %.6f %.6f' % uv for uv in uvs]
    obj += ['usemtl Tray']
    obj += ['f '+' '.join(f'{v}/{t}' for v,t in zip(vs,ts)) for vs,ts in faces]
    (out / f'{prefix}.obj').write_text('\n'.join(obj)+'\n')
    (out / f'{prefix}.mtl').write_text(f'newmtl Tray\nKd 1.0 1.0 1.0\nmap_Kd {prefix}_Texture.png\n')
    return prefix


def place_dice(data, bag, cfg, prefix, tag):
    gid = cfg['guid']
    # Stable, distinct GUIDs: each tray gets 30 reserved GUIDs (tray + up to 29 dice).
    start = int(gid, 16)
    existing = {f'{start+i:06x}' for i in range(30)}
    data['ObjectStates'] = [o for o in data['ObjectStates'] if o.get('GUID') not in existing]
    mesh = f'{args.tts_root}/generated/dice_trays/{prefix}.obj'
    tex = f'{args.tts_root}/generated/dice_trays/{prefix}_Texture.png'
    tray = {
      'GUID': gid, 'Name': 'Custom_Model',
      'Transform': {'posX':cfg['x'],'posY':TRAY_Y,'posZ':cfg['z'],
                    'rotX':0,'rotY':270,'rotZ':0,'scaleX':1,'scaleY':1,'scaleZ':1},
      'Nickname': ('D4 / D6 / D8 Dice Tray' if tag == 'low' else 'D10 / D12 / D20 Dice Tray'),
      'Description': 'Open three-compartment Graft Bed dice tray.',
      'GMNotes':f"Generated from {cfg['art']}",
      'ColorDiffuse':{'r':1,'g':1,'b':1}, 'Locked':False, 'Grid':False, 'Snap':False,
      'LuaScript':'','LuaScriptState':'','XmlUI':'',
      'CustomMesh':{'MeshURL':mesh,'DiffuseURL':tex,'NormalURL':'','ColliderURL':mesh,
                    'Convex':False,'MaterialIndex':0,'Type':0,'CastShadows':True},
    }
    data['ObjectStates'].append(tray)
    if not bag:
        raise ValueError(f"Could not find game-box bag: {cfg['bag']}")
    grouped = {}
    for o in bag.get('ContainedObjects', []):
        grouped.setdefault(o.get('Nickname'), []).append(o)
    index = 1
    for name in cfg['contents']:
        supplied = grouped.get(name, [])
        dx, dz = DICE_OFFSETS[name]
        for i, obj in enumerate(supplied):
            d = copy.deepcopy(obj)
            d['GUID'] = f'{start+index:06x}'
            index += 1
            # 3 x 3 positions, centered on the correct section of the artwork.
            c = i % 3 - 1
            r = i // 3 - 1
            d['Transform']['posX'] = cfg['x'] + dx + DICE_GRID_SPACING * c
            d['Transform']['posY'] = DICE_BASE_Y
            d['Transform']['posZ'] = cfg['z'] + dz + DICE_GRID_SPACING * r
            d['Transform']['rotX'] = 0
            d['Transform']['rotY'] = 0
            d['Transform']['rotZ'] = 0
            data['ObjectStates'].append(d)
    print(tag, ':', ', '.join(f'{n}={len(grouped.get(n,[]))}' for n in cfg['contents']))


chosen = ('low','high') if args.tray == 'both' else (args.tray,)
prefixes = {tag: build_model(tag,cfg) for tag,cfg in TRAY_SETTINGS.items() if tag in chosen}
if args.save.is_file():
    data = json.loads(args.save.read_text())
    main = next((o for o in data.get('ObjectStates',[]) if o.get('Nickname','').startswith('Game Box')), None)
    if main is None:
        raise ValueError('No Game Box found in input TTS save')
    for tag in chosen:
        cfg = TRAY_SETTINGS[tag]
        bag = next((o for o in main.get('ContainedObjects',[]) if o.get('Nickname')==cfg['bag']), None)
        place_dice(data, bag, cfg, prefixes[tag], tag)
    path = args.output / 'Dice_Trays_Test_Save.json'
    path.write_text(json.dumps(data,indent=2)+'\n')
    print('Written',path)
else:
    raise FileNotFoundError(f'Input TTS save not found: {args.save}')
