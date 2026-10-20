#!/usr/bin/env python3
"""Build four player-box assemblies and stage them in a TTS save.

Uses the hand-adjusted Blue player-box layout as the master layout, then clones it
for Green, Red, and Yellow by shifting the full assembly to the right.
"""
import argparse, copy, json
from pathlib import Path
from PIL import Image, ImageOps

p = argparse.ArgumentParser()
p.add_argument('--save', type=Path, default=Path('source/TS_Save_latest.json'))
p.add_argument('--assets-player', type=Path, default=Path('assets/player'))
p.add_argument('--assets-trays', type=Path, default=Path('assets/trays'))
p.add_argument('--output', type=Path, default=Path('generated/player_box'))
p.add_argument('--tts-root', default=None)
args = p.parse_args()
if args.tts_root is None:
    args.tts_root = 'Z:' + str(Path.cwd().resolve())
out = args.output
out.mkdir(parents=True, exist_ok=True)

UPI = 3.06 / 3.5 * 1.67
BASE_LONG = 4.625 * UPI
BASE_WIDE = 3.5 * UPI
LID_LONG = 4.75 * UPI
LID_WIDE = 3.5 * UPI
WALL_H = 0.5 * UPI
T = 0.12 * UPI
FLOOR = 0.12 * UPI

# Clone the approved Blue layout for the other 3 player boxes.
# This is the center-to-center spacing of the trays in world X.
PLAYER_BOX_X_SPACING = BASE_WIDE + 0.35
PLAYER_COLORS = ['Blue', 'Green', 'Red', 'Yellow']
BASE_GUID_START = int('f0c001', 16)
OBJECTS_PER_PLAYER = 12

BASE_STEM = 'Player Box Supply'
LID_STEM = 'Placard_ Discard Bin'


def artwork_path(root, stem):
    for ext in ('.png', '.jpg'):
        path = root / (stem + ext)
        if path.is_file():
            return path
    raise FileNotFoundError(f'Missing {stem}.png or {stem}.jpg in {root}')


def build_atlas(main_art_path, output_png, wall_art_path):
    """Texture atlas: floor art in upper-left; wall art in the other quadrants."""
    art = ImageOps.mirror(Image.open(main_art_path).convert('RGB'))
    wall = Image.open(wall_art_path).convert('RGB')
    atlas = Image.new('RGB', (1024, 1024))
    atlas.paste(art.resize((512, 512), Image.Resampling.LANCZOS), (0, 0))
    g = ImageOps.fit(wall, (512, 512))
    for at in ((512, 0), (0, 512), (512, 512)):
        atlas.paste(g, at)
    atlas.save(output_png)


def build_open_box(prefix, long_axis, wide_axis, texture_png):
    vertices, uvs, faces = [], [], []

    def quad(points, coords):
        vi = len(vertices) + 1
        ti = len(uvs) + 1
        vertices.extend(points)
        uvs.extend(coords)
        faces.append((tuple(range(vi, vi + 4)), tuple(range(ti, ti + 4))))

    def box(x0, x1, y0, y1, z0, z1, floor_top=False):
        wall_uv = [(.52, .98), (.98, .98), (.98, .52), (.52, .52)]
        floor_uv = [(.01, .99), (.49, .99), (.49, .51), (.01, .51)]
        quads = [
            ([(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)], floor_uv if floor_top else wall_uv),
            ([(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)], wall_uv),
            ([(x0, y0, z0), (x0, y1, z0), (x1, y1, z0), (x1, y0, z0)], wall_uv),
            ([(x1, y0, z1), (x1, y1, z1), (x0, y1, z1), (x0, y0, z1)], wall_uv),
            ([(x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (x0, y0, z0)], wall_uv),
            ([(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], wall_uv),
        ]
        for ps, ts in quads:
            quad(ps, ts)

    W = long_axis
    D = wide_axis

    # Base structural floor (wall texture), with the visible artwork inset by wall thickness.
    box(-W / 2, W / 2, 0, FLOOR, -D / 2, D / 2, False)
    floor_uv = [(.01, .99), (.49, .99), (.49, .51), (.01, .51)]
    art_y = FLOOR + 0.001
    quad(
        [(-W / 2 + T, art_y, -D / 2 + T),
         (-W / 2 + T, art_y, D / 2 - T),
         (W / 2 - T, art_y, D / 2 - T),
         (W / 2 - T, art_y, -D / 2 + T)],
        floor_uv,
    )

    # Outer walls only. (No interior walls on the player trays.)
    box(-W / 2, -W / 2 + T, FLOOR, WALL_H, -D / 2, D / 2)
    box(W / 2 - T, W / 2, FLOOR, WALL_H, -D / 2, D / 2)
    box(-W / 2 + T, W / 2 - T, FLOOR, WALL_H, -D / 2, -D / 2 + T)
    box(-W / 2 + T, W / 2 - T, FLOOR, WALL_H, D / 2 - T, D / 2)

    obj = [f'mtllib {prefix}.mtl', f'o {prefix}']
    obj += ['v %.6f %.6f %.6f' % v for v in vertices]
    obj += ['vt %.6f %.6f' % uv for uv in uvs]
    obj += ['usemtl Tray']
    obj += ['f ' + ' '.join(f'{v}/{t}' for v, t in zip(vs, ts)) for vs, ts in faces]
    (out / f'{prefix}.obj').write_text('\n'.join(obj) + '\n')
    (out / f'{prefix}.mtl').write_text(f'newmtl Tray\nKd 1 1 1\nmap_Kd {texture_png.name}\n')


# Exact hand-adjusted layout from the supplied TS save (Blue player box only).
BLUE_LAYOUT = {
    "f0c001": {"posX": 6.81899548, "posY": 1.68246937, "posZ": -8.02994, "rotX": 359.9475, "rotY": 269.947754, "rotZ": 359.9694, "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0},
    "f0c002": {"posX": 6.76924372, "posY": 0.9583365, "posZ": -8.307693, "rotX": 0.0130117172, "rotY": 269.915558, "rotZ": 0.0358799063, "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0},
    "f0c003": {"posX": 5.568294, "posY": 2.642262, "posZ": -5.959592, "rotX": 18.55305, "rotY": 354.5348, "rotZ": 359.529968, "scaleX": 1.0999999, "scaleY": 1.0999999, "scaleZ": 1.0999999},
    "f0c004": {"posX": 5.61921024, "posY": 2.645367, "posZ": -8.591124, "rotX": 18.6590385, "rotY": 231.880371, "rotZ": 0.0241459962, "scaleX": 1.0999999, "scaleY": 1.0999999, "scaleZ": 1.0999999},
    "f0c005": {"posX": 5.57703733, "posY": 2.64208436, "posZ": -6.788144, "rotX": 18.6582565, "rotY": 54.32418, "rotZ": 119.994362, "scaleX": 1.0999999, "scaleY": 1.0999999, "scaleZ": 1.0999999},
    "f0c006": {"posX": 7.788634, "posY": 2.738474, "posZ": -6.78994942, "rotX": 0.5125502, "rotY": 7.225498, "rotZ": 359.9557, "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0},
    "f0c007": {"posX": 7.666749, "posY": 2.73806167, "posZ": -5.445283, "rotX": 89.94066, "rotY": 338.859039, "rotZ": 0.0, "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0},
    "f0c008": {"posX": 7.52030325, "posY": 2.741731, "posZ": -8.102461, "rotX": 359.9577, "rotY": 291.906158, "rotZ": 89.94672, "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0},
    "f0c009": {"posX": 6.67648172, "posY": 3.23009634, "posZ": -7.619446, "rotX": 359.786224, "rotY": 180.0605, "rotZ": 6.181936, "scaleX": 2.961371, "scaleY": 1.0, "scaleZ": 2.920114},
    "f0c00a": {"posX": 5.68928432, "posY": 3.44991875, "posZ": -5.74026251, "rotX": 359.778748, "rotY": 180.019562, "rotZ": 6.18587875, "scaleX": 0.859436, "scaleY": 1.0, "scaleZ": 0.866909},
    "f0c00b": {"posX": 5.69062376, "posY": 3.457825, "posZ": -7.72937965, "rotX": 359.784424, "rotY": 180.059723, "rotZ": 6.182064, "scaleX": 0.859436, "scaleY": 1.0, "scaleZ": 0.866909},
    "f0c00c": {"posX": 7.436798, "posY": 3.263788, "posZ": -6.529908, "rotX": 359.783417, "rotY": 180.0248, "rotZ": 6.18796, "scaleX": 1.630161, "scaleY": 1.0, "scaleZ": 1.825071},
}


def blue_guid_for_index(n):
    return f'{BASE_GUID_START + n:06x}'


def shifted_pose(blue_guid, dx):
    pose = dict(BLUE_LAYOUT[blue_guid])
    pose['posX'] += dx
    return pose


def make_mesh_object(guid, nickname, description, gmnotes, mesh_name, diffuse_name):
    return {
        'GUID': guid,
        'Name': 'Custom_Model',
        'Transform': {'posX': 0, 'posY': 0, 'posZ': 0, 'rotX': 0, 'rotY': 0, 'rotZ': 0, 'scaleX': 1, 'scaleY': 1, 'scaleZ': 1},
        'Nickname': nickname,
        'Description': description,
        'GMNotes': gmnotes,
        'ColorDiffuse': {'r': 1, 'g': 1, 'b': 1},
        'Locked': False,
        'Grid': False,
        'Snap': False,
        'LuaScript': '',
        'LuaScriptState': '',
        'XmlUI': '',
        'CustomMesh': {
            'MeshURL': f'{args.tts_root}/generated/player_box/{mesh_name}.obj',
            'DiffuseURL': f'{args.tts_root}/generated/player_box/{diffuse_name}.png',
            'NormalURL': '',
            'ColliderURL': f'{args.tts_root}/generated/player_box/{mesh_name}.obj',
            'Convex': False,
            'MaterialIndex': 0,
            'Type': 0,
            'CastShadows': True,
        },
    }


def first_or_none(items):
    return items[0] if items else None


def clone_component(template, guid):
    obj = copy.deepcopy(template)
    obj['GUID'] = guid
    return obj


def add_player_box(data, main, color, player_index):
    bag_name = f'{color} Player Components'
    bag = next((o for o in main.get('ContainedObjects', []) if o.get('Nickname') == bag_name), None)
    if bag is None:
        raise ValueError(f'Missing bag: {bag_name}')

    dx = player_index * PLAYER_BOX_X_SPACING
    guid_base = BASE_GUID_START + player_index * 0x100

    # Base tray and lid
    base_guid = f'{guid_base + 0:06x}'
    lid_guid = f'{guid_base + 1:06x}'
    base_obj = make_mesh_object(
        base_guid,
        f'{color} Player Supply Tray — Physical Test',
        f'Open player-supply base tray for the {color.lower()} player box.',
        'Generated from assets/player/Player Box Supply',
        'Player_Box_Base',
        'Player_Box_Base_Texture',
    )
    lid_obj = make_mesh_object(
        lid_guid,
        f'{color} Discard Bin Lid — Physical Test',
        f'Discard-bin cover for the {color.lower()} player box.',
        'Generated from assets/player/Placard_ Discard Bin',
        'Player_Box_Lid',
        'Player_Box_Lid_Texture',
    )
    base_obj['Transform'].update(shifted_pose('f0c001', dx))
    lid_obj['Transform'].update(shifted_pose('f0c002', dx))
    data['ObjectStates'].extend([base_obj, lid_obj])

    contained = bag.get('ContainedObjects', [])
    by_name = {}
    for o in contained:
        by_name.setdefault(o.get('Nickname'), []).append(o)

    next_guid = guid_base + 2

    # D4s -> f0c003, f0c004, f0c005
    for blue_guid, template in zip(('f0c003', 'f0c004', 'f0c005'), by_name.get('D4', [])):
        obj = clone_component(template, f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose(blue_guid, dx))
        data['ObjectStates'].append(obj)
        next_guid += 1

    # D6s -> f0c006, f0c007, f0c008
    for blue_guid, template in zip(('f0c006', 'f0c007', 'f0c008'), by_name.get('D6', [])):
        obj = clone_component(template, f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose(blue_guid, dx))
        data['ObjectStates'].append(obj)
        next_guid += 1

    core = first_or_none(by_name.get(f'{color} Plant Core', []))
    if core is not None:
        obj = clone_component(core, f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose('f0c009', dx))
        data['ObjectStates'].append(obj)
        next_guid += 1

    actions = by_name.get(f'{color} Main Action', [])
    if len(actions) >= 1:
        obj = clone_component(actions[0], f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose('f0c00a', dx))
        data['ObjectStates'].append(obj)
        next_guid += 1
    if len(actions) >= 2:
        obj = clone_component(actions[1], f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose('f0c00b', dx))
        data['ObjectStates'].append(obj)
        next_guid += 1

    gnome = first_or_none(by_name.get(f'{color} Gnome', []))
    if gnome is not None:
        obj = clone_component(gnome, f'{next_guid:06x}')
        obj['Transform'].update(shifted_pose('f0c00c', dx))
        data['ObjectStates'].append(obj)
        next_guid += 1


# Build textures/meshes once.
base_art = artwork_path(args.assets_player, BASE_STEM)
lid_art = artwork_path(args.assets_player, LID_STEM)
base_tex = out / 'Player_Box_Base_Texture.png'
lid_tex = out / 'Player_Box_Lid_Texture.png'
build_atlas(base_art, base_tex, args.assets_trays / 'grassy_field.png')
build_atlas(lid_art, lid_tex, lid_art)
build_open_box('Player_Box_Base', BASE_LONG, BASE_WIDE, base_tex)
build_open_box('Player_Box_Lid', LID_LONG, LID_WIDE, lid_tex)

if not args.save.is_file():
    raise FileNotFoundError(f'Input TTS save not found: {args.save}')
data = json.loads(args.save.read_text())
main = next((o for o in data.get('ObjectStates', []) if o.get('Nickname', '').startswith('Game Box')), None)
if main is None:
    raise ValueError('No Game Box found in input TTS save')

# Remove any previously generated player-box objects for all 4 players.
remove_guids = set()
for player_index in range(len(PLAYER_COLORS)):
    guid_base = BASE_GUID_START + player_index * 0x100
    for i in range(OBJECTS_PER_PLAYER):
        remove_guids.add(f'{guid_base + i:06x}')
data['ObjectStates'] = [o for o in data['ObjectStates'] if o.get('GUID') not in remove_guids]

# Generate the 4 player boxes.
for player_index, color in enumerate(PLAYER_COLORS):
    add_player_box(data, main, color, player_index)

(out / 'Player_Box_Test_Save.json').write_text(json.dumps(data, indent=2) + '\n')
print('Written', out / 'Player_Box_Test_Save.json')
