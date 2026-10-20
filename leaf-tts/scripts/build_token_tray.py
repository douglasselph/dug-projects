#!/usr/bin/env python3
"""Build a textured open Token Tray OBJ and TTS save from an existing save."""
import argparse, copy, json
from pathlib import Path
from PIL import Image, ImageOps

p = argparse.ArgumentParser()
p.add_argument('--save', type=Path, default=Path('source/TS_Save_latest.json'))
p.add_argument('--assets', type=Path, default=Path('assets/trays'))
p.add_argument('--output', type=Path, default=Path('generated/token_tray'))
p.add_argument('--tts-root', default=None)
args = p.parse_args()
if args.tts_root is None:
    args.tts_root = 'Z:' + str(Path.cwd().resolve())
out = args.output
out.mkdir(parents=True, exist_ok=True)

art_path = next((args.assets / f'Tray_ Critters, Tokens{ext}' for ext in ('.png', '.jpg') if (args.assets / f'Tray_ Critters, Tokens{ext}').is_file()), None)
if art_path is None:
    raise FileNotFoundError('Missing tray artwork: Tray_ Critters, Tokens.png or Tray_ Critters, Tokens.jpg in ' + str(args.assets))
art = Image.open(art_path).convert('RGB')
art = ImageOps.mirror(art)
grass = Image.open(args.assets / 'grassy_field.png').convert('RGB')
S = 1024
atlas = Image.new('RGB', (S, S))
atlas.paste(art.resize((512, 512), Image.Resampling.LANCZOS), (0, 0))
g = ImageOps.fit(grass, (512, 512))
atlas.paste(g, (512, 0)); atlas.paste(g, (0, 512)); atlas.paste(g, (512, 512))
atlas.save(out / 'Token_Tray_Texture.png')

UPI = 3.06 / 3.5 * 1.67
W = 4.5 * UPI
D = 4.5 * UPI
H = 1.0 * UPI
T = 0.12 * UPI
FLOOR = 0.12 * UPI
# Divider locations estimated from source art proportions.
X1 = -W/2 + (127/386.0) * W   # Bee / Worm divider (top only)
X2 = -W/2 + (254/386.0) * W   # Worm / Sunlight divider (top only)
XB = -W/2 + (191/386.0) * W   # Water / Mulch divider (bottom only)
ZH = D/2 - (240/417.0) * D    # Top / bottom divider

verts = []; uv = []; faces = []
def quad(points, uvcoords):
    vi = len(verts) + 1; ti = len(uv) + 1
    verts.extend(points); uv.extend(uvcoords)
    faces.append((tuple(range(vi, vi+4)), tuple(range(ti, ti+4))))

def box(x0, x1, y0, y1, z0, z1, floor_top=False):
    grassuv=[(.52,.98),(.98,.98),(.98,.52),(.52,.52)]
    flooruv=[(.01,.99),(.49,.99),(.49,.51),(.01,.51)]
    quads = [
        ([(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)], flooruv if floor_top else grassuv),
        ([(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)], grassuv),
        ([(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(x1,y0,z0)], grassuv),
        ([(x1,y0,z1),(x1,y1,z1),(x0,y1,z1),(x0,y0,z1)], grassuv),
        ([(x0,y0,z1),(x0,y1,z1),(x0,y1,z0),(x0,y0,z0)], grassuv),
        ([(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)], grassuv),
    ]
    for pts, uvs in quads:
        quad(pts, uvs)

def wall(x0, x1, z0, z1):
    box(-z1, -z0, FLOOR, H, x0, x1)


# ---------------------------------------------------------
# Floor and outer walls
# ---------------------------------------------------------

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


# ---------------------------------------------------------
# Interior wall calculations
# ---------------------------------------------------------

# Interior dimensions, excluding outer wall thickness.
LEFT   = -W/2 + T
RIGHT  =  W/2 - T
BOTTOM = -D/2 + T
TOP    =  D/2 - T

INNER_WIDTH  = RIGHT - LEFT
INNER_DEPTH  = TOP - BOTTOM

# Place the horizontal divider 1.5 inches from the
# bottom of a 4-inch usable interior.
HORIZONTAL_Z = BOTTOM + INNER_DEPTH * (1.5 / 4.0)

# Bottom compartment is divided into two equal sections.
BOTTOM_DIVIDER_X = LEFT + INNER_WIDTH / 2

# Top compartment is divided into three equal sections.
TOP_DIVIDER_X1 = LEFT + INNER_WIDTH / 3
TOP_DIVIDER_X2 = LEFT + INNER_WIDTH * 2 / 3


# ---------------------------------------------------------
# Interior walls
# ---------------------------------------------------------

# Horizontal wall spanning the full interior width.
wall(
    LEFT, RIGHT,
    HORIZONTAL_Z - T/2,
    HORIZONTAL_Z + T/2
)

# Bottom vertical wall, centered horizontally.
wall(
    BOTTOM_DIVIDER_X - T/2,
    BOTTOM_DIVIDER_X + T/2,
    BOTTOM,
    HORIZONTAL_Z - T/2
)

# Top vertical wall 1 (at one-third of interior width).
wall(
    TOP_DIVIDER_X1 - T/2,
    TOP_DIVIDER_X1 + T/2,
    HORIZONTAL_Z + T/2,
    TOP
)

# Top vertical wall 2 (at two-thirds of interior width).
wall(
    TOP_DIVIDER_X2 - T/2,
    TOP_DIVIDER_X2 + T/2,
    HORIZONTAL_Z + T/2,
    TOP
)

obj=['mtllib Token_Tray.mtl','o Leaf_And_Let_Die_Token_Tray']
obj += ['v %.6f %.6f %.6f' % v for v in verts]
obj += ['vt %.6f %.6f' % t for t in uv]
obj += ['usemtl Tray']
obj += ['f ' + ' '.join(f'{v}/{t}' for v,t in zip(vs,ts)) for vs,ts in faces]
(out / 'Token_Tray.obj').write_text('\n'.join(obj) + '\n')
(out / 'Token_Tray.mtl').write_text('newmtl Tray\nKd 1.0 1.0 1.0\nmap_Kd Token_Tray_Texture.png\n')

# Each token type now has ONE X/Z anchor.  All copies are stacked vertically
# above that anchor; no separate 3x3 positions are necessary.
# X and Z are horizontal TTS world axes.  Y is height above the table.
bee_x = -2.2
bee_z = +1.75

worm_x = -0.25
worm_z = +1.75

sunlight_x = +1.868
sunlight_z = +2

water_x = -1.75
water_z = -1.074

mulch_x = +1.55
mulch_z = -1.074

TOKEN_STACKS = {
    'Bee':      (bee_x, bee_z),
    'Worm':     (worm_x, worm_z),
    'Sunlight': (sunlight_x, sunlight_z),
    'Water':    (water_x, water_z),
    'Mulch':    (mulch_x, mulch_z),
}

# Common settings for ALL token types; adjust STACK_STEP if the tiles
# overlap or sit too far apart vertically in Tabletop Simulator.
STACK_BASE_Y = 1.05 + H + 0.16
STACK_STEP = 0.20
TOKENS_PER_STACK = 9
TRAY_CENTER_X = 23.5


def stack(objects, x, z, first_guid, destination):
    """Add one vertical stack of loose TTS objects to destination.

    All tokens have identical X/Z coordinates; successive objects are
    placed STACK_STEP higher on Y.  Return the next available GUID number.
    """
    for level, obj in enumerate(objects):
        d = copy.deepcopy(obj)
        d['GUID'] = f'f1{first_guid + level:04x}'
        d['Transform']['posX'] = TRAY_CENTER_X + x
        d['Transform']['posY'] = STACK_BASE_Y + level * STACK_STEP
        d['Transform']['posZ'] = z
        d['Transform']['rotY'] = 180
        destination.append(d)
    return first_guid + len(objects)

if args.save.exists():
    data = json.loads(args.save.read_text())
    main = next((o for o in data['ObjectStates'] if o.get('Nickname','').startswith('Game Box')), None)
    bag = next((o for o in main.get('ContainedObjects', []) if o.get('Nickname') == 'Token Tray - Critters and Nutrients'), None) if main else None
    mesh = f'{args.tts_root}/generated/token_tray/Token_Tray.obj'
    tex = f'{args.tts_root}/generated/token_tray/Token_Tray_Texture.png'
    tray_guid = 'f0a931'
    token_guids = {f'f1{n:04x}' for n in range(45)}
    remove = {tray_guid} | token_guids
    data['ObjectStates'] = [o for o in data['ObjectStates'] if o.get('GUID') not in remove]
    item = {'GUID': tray_guid, 'Name': 'Custom_Model',
        'Transform': {'posX': 23.5, 'posY': 1.05, 'posZ': 0, 'rotX': 0, 'rotY': 270, 'rotZ': 0, 'scaleX': 1, 'scaleY': 1, 'scaleZ': 1},
        'Nickname': 'Token Tray — Physical Test',
        'Description': '4.5-inch square Token Tray, 1-inch walls. Bee, Worm, Sunlight above; Water and Mulch below.',
        'GMNotes': 'Generated from assets/trays/Tray_ Critters, Tokens.jpg',
        'ColorDiffuse': {'r':1,'g':1,'b':1}, 'Locked': False, 'Grid': False, 'Snap': False,
        'LuaScript': '', 'LuaScriptState': '', 'XmlUI': '',
        'CustomMesh': {'MeshURL': mesh, 'DiffuseURL': tex, 'NormalURL': '',
                       'ColliderURL': mesh, 'Convex': False, 'MaterialIndex': 0, 'Type': 0, 'CastShadows': True}}
    data['ObjectStates'].append(item)
    if bag:
        by_name = {}
        for obj in bag.get('ContainedObjects', []):
            by_name.setdefault(obj.get('Nickname'), []).append(obj)
        guid_index = 0
        for name, (x, z) in TOKEN_STACKS.items():
            objs = by_name.get(name, [])
            if len(objs) != TOKENS_PER_STACK:
                raise ValueError(f'{name}: expected {TOKENS_PER_STACK} items but found {len(objs)}')
            guid_index = stack(objs, x, z, guid_index, data['ObjectStates'])
    (out / 'Token_Tray_Test_Save.json').write_text(json.dumps(data, indent=2) + '\n')
    print('Written model, texture and updated save in', out)
else:
    print('Written OBJ and texture; no input save supplied')
