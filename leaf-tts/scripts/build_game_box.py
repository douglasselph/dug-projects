#!/usr/bin/env python3
"""Stage the TTS Game Box placard, streamline its bag, and include the rulebook PDF.

Designed to be run after tray/player-box rebuilds. Existing table objects are preserved.
"""
import argparse
import copy
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from box_illustrations import add_box_illustrations, PDF_GUID as ILLUSTRATIONS_GUID

parser = argparse.ArgumentParser()
parser.add_argument('--save', type=Path, default=Path('output/TS_Save_latest.json'))
parser.add_argument('--assets-box', type=Path, default=Path('assets/box'))
parser.add_argument('--rules-pdf', type=Path, default=Path('docs/Leaf_and_Let_Die_v37-01.pdf'))
parser.add_argument('--output', type=Path, default=Path('generated/game_box'))
parser.add_argument('--tts-root', default=None)
args = parser.parse_args()
if args.tts_root is None:
    args.tts_root = 'Z:' + str(Path.cwd().resolve())
args.output.mkdir(parents=True, exist_ok=True)

UPI = 3.06 / 3.5 * 2.67
BOARD_W = 8.5 * UPI
BOARD_H = 11.0 * UPI
BOARD_THICK = 0.10 * UPI
# TTS Custom PDF native dimensions depend on the PDF object; fine-tune in TTS.
PDF_SCALE = 4.0
BOX_X = -18.0
BOX_Y = 1.05
BOX_Z = 0.0
BOX_GUID = 'f0d001'
PDF_GUID = 'f0d002'
BOX_DESC = (
    'GAME BOX SIMULATION: This arrangement approximates the experience of opening '
    'a retail copy of Leaf & Let Die. Tabletop Simulator has some limitations: '
    'in particular, the player component boxes and their contents are represented '
    'as separate simulated pieces rather than precisely nested physical packaging.'
)
REMAINING_NAMES = {
    'Plant Card Catalog',
    'Round Cards - All 18',
    'Battle Glade',
    'Cultivation / Battle Round Helper',
    'Common Rules / Helpers & Tokens',
    'Grove Setup Reference',
}
EXPECTED_COUNTS = {
    'Plant Card Catalog': 1,
    'Round Cards - All 18': 1,
    'Battle Glade': 1,
    'Cultivation / Battle Round Helper': 2,
    'Common Rules / Helpers & Tokens': 2,
    'Grove Setup Reference': 1,
}


def image_path(root):
    for name in ('cover_main.png', 'cover_main.jpg', 'cover_main.jpeg'):
        candidate = root / name
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(f'Missing assets/box/cover_main.png or .jpg or .jpeg in {root}')


def font_at(size, bold=False):
    paths = (
        ['/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', '/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf']
        if bold else
        ['/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', '/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf']
    )
    for fontfile in paths:
        if Path(fontfile).is_file():
            return ImageFont.truetype(fontfile, size)
    return ImageFont.load_default()


def wrap(draw, text, font, width):
    lines = []
    for paragraph in text.split('\n'):
        words = paragraph.split()
        line = ''
        for word in words:
            candidate = f'{line} {word}'.strip()
            if draw.textbbox((0, 0), candidate, font=font)[2] <= width or not line:
                line = candidate
            else:
                lines.append(line)
                line = word
        lines.append(line)
    return lines


def build_board_art(path):
    # 8.5:11 background; the cover is centered and fitted by HEIGHT only,
    # preserving original aspect ratio without distorting the image.
    img_w, img_h = 1020, 1320
    canvas = Image.new('RGB', (img_w, img_h), (212, 215, 217))
    cover = Image.open(path).convert('RGB')
    desired_height = int(img_h * 0.80)
    scaled_width = round(cover.width * desired_height / cover.height)
    # Do not crop or stretch; if source is unusually wide, fit it inside.
    ratio = min(1.0, (img_w - 40) / scaled_width)
    target_w = max(1, round(scaled_width * ratio))
    target_h = max(1, round(desired_height * ratio))
    cover = cover.resize((target_w, target_h), Image.Resampling.LANCZOS)
    x = (img_w - target_w) // 2
    y = int(img_h * 0.015)
    canvas.paste(cover, (x, y))
    draw = ImageDraw.Draw(canvas)
    title = 'GAME BOX SIMULATION'
    title_font = font_at(25, True)
    body_font = font_at(20)
    footer_top = max(y + target_h + 16, int(img_h * 0.825))
    draw.text((img_w // 2, footer_top), title, anchor='mt', fill=(45, 49, 53), font=title_font)
    explanation = (
        'A simulation of what players encounter when opening the game. '
        'TTS packaging is approximate; especially the nested player boxes.'
    )
    for i, line in enumerate(wrap(draw, explanation, body_font, img_w - 100)):
        draw.text((img_w // 2, footer_top + 43 + i * 27), line, anchor='mt', fill=(50, 54, 58), font=body_font)
    texture_path = args.output / 'Game_Box_Board.png'
    canvas.save(texture_path)
    return texture_path


def build_model():
    # A genuinely flat rectangular board, not another open tray.
    w = BOARD_W / 2
    h = BOARD_H / 2
    t = BOARD_THICK
    vertices = [
        (-w, t, -h), (-w, t, h), (w, t, h), (w, t, -h),
        (-w, 0, -h), (-w, 0, h), (w, 0, h), (w, 0, -h),
    ]
    top_uv = [(0, 1), (0, 0), (1, 0), (1, 1)]
    edge_uv = [(0.02, 0.02), (0.02, 0.03), (0.98, 0.03), (0.98, 0.02)]
    quad_index = [(1, 2, 3, 4), (8, 7, 6, 5), (5, 6, 2, 1),
                  (4, 3, 7, 8), (1, 4, 8, 5), (2, 6, 7, 3)]
    lines = ['mtllib Game_Box_Board.mtl', 'o Leaf_And_Let_Die_Game_Box_Board']
    lines.extend('v %.6f %.6f %.6f' % xyz for xyz in vertices)
    for uvs in [top_uv] + [edge_uv] * 5:
        lines.extend('vt %.6f %.6f' % uv for uv in uvs)
    lines.append('usemtl Board')
    for i, inds in enumerate(quad_index):
        lines.append('f ' + ' '.join(f'{v}/{4*i+j+1}' for j, v in enumerate(inds)))
    (args.output / 'Game_Box_Board.obj').write_text('\n'.join(lines) + '\n')
    (args.output / 'Game_Box_Board.mtl').write_text('newmtl Board\nKd 1 1 1\nmap_Kd Game_Box_Board.png\n')


def game_box_object():
    return {
        'GUID': BOX_GUID, 'Name': 'Custom_Model',
        'Transform': {'posX': BOX_X, 'posY': BOX_Y, 'posZ': BOX_Z,
                      'rotX': 0, 'rotY': 180, 'rotZ': 0,
                      'scaleX': 1, 'scaleY': 1, 'scaleZ': 1},
        'Nickname': 'Leaf & Let Die — Game Box Simulation',
        'Description': BOX_DESC,
        'GMNotes': '8.5 x 11 inch game box simulation; no walls.',
        'ColorDiffuse': {'r': 1, 'g': 1, 'b': 1},
        'Locked': False, 'Grid': False, 'Snap': False,
        'LuaScript': '', 'LuaScriptState': '', 'XmlUI': '',
        'CustomMesh': {
            'MeshURL': f'{args.tts_root}/generated/game_box/Game_Box_Board.obj',
            'DiffuseURL': f'{args.tts_root}/generated/game_box/Game_Box_Board.png',
            'NormalURL': '', 'ColliderURL': f'{args.tts_root}/generated/game_box/Game_Box_Board.obj',
            'Convex': True, 'MaterialIndex': 0, 'Type': 0, 'CastShadows': True,
        },
    }


def pdf_item():
    return {
        'GUID': PDF_GUID, 'Name': 'Custom_PDF',
        'Transform': {'posX': 0, 'posY': 1.2, 'posZ': 0,
                      'rotX': 0, 'rotY': 180, 'rotZ': 0,
                      'scaleX': PDF_SCALE, 'scaleY': 1, 'scaleZ': PDF_SCALE},
        'Nickname': 'Leaf & Let Die — Rulebook v37.01',
        'Description': 'Full rulebook PDF. Take this object out of the Game Box bag to read and turn pages.',
        'GMNotes': 'Local PDF; use a hosted public HTTPS PDF URL to share with other computers.',
        'ColorDiffuse': {'r': 1, 'g': 1, 'b': 1},
        'Locked': False, 'Grid': False, 'Snap': False,
        'LuaScript': '', 'LuaScriptState': '', 'XmlUI': '',
        'CustomPDF': {'PDFUrl': f'{args.tts_root}/{args.rules_pdf.as_posix()}',
                      'PDFPassword': '', 'PDFPage': 0, 'PDFPageOffset': 0},
    }


if not args.save.is_file():
    raise FileNotFoundError(f'Missing input save: {args.save}')
# Rules must exist on the user's computer for TTS to load them.
if not args.rules_pdf.is_file():
    raise FileNotFoundError(f'Missing rulebook: {args.rules_pdf}. Put the PDF in docs/ first.')

build_board_art(image_path(args.assets_box))
build_model()

data = json.loads(args.save.read_text())
main = next((o for o in data.get('ObjectStates', []) if o.get('Nickname', '').startswith('Game Box')), None)
if main is None:
    raise ValueError('Game Box bag missing in input save')
old_contents = main.get('ContainedObjects', [])
# The Catalog now lives on the placard, not in the Game Box Sack.  Handle
# fresh saves and saves already converted by rebuild_layout.sh alike.
CATALOG_GUID = 'b000fc'
CATALOG_POS = {'posX': -20.2, 'posY': 1.05, 'posZ': -6.83,
               'rotX': 0, 'rotY': 180, 'rotZ': 0,
               'scaleX': 1, 'scaleY': 1, 'scaleZ': 1}
external_catalog = next((o for o in data['ObjectStates'] if o.get('GUID') == CATALOG_GUID), None)
internal_catalog = next((o for o in old_contents if o.get('GUID') == CATALOG_GUID), None)
if external_catalog is None and internal_catalog is not None:
    external_catalog = copy.deepcopy(internal_catalog)
    external_catalog['Transform'] = dict(CATALOG_POS)
    data['ObjectStates'].append(external_catalog)

# Keep the Sack's real contents and replace only the generated PDF.
# Don't reinsert the catalog after rebuild_layout.sh has moved it out.
PDF_GUID_ILLUSTRATIONS = ILLUSTRATIONS_GUID
main['ContainedObjects'] = [copy.deepcopy(o) for o in old_contents
    if (o.get('Nickname') in REMAINING_NAMES or o.get('GUID') == 'b0010f')
    and o.get('GUID') not in (CATALOG_GUID, PDF_GUID, PDF_GUID_ILLUSTRATIONS)]
main['ContainedObjects'].append(pdf_item())
add_box_illustrations(main, tts_root=args.tts_root)
main['Description'] = (
    'Store components not currently needed on the table here. '
    'Take out and read the Rulebook first. The other materials in this Sack '
    'are also part of the physical game box; the items on the placard '
    'represent components unpacked onto the table.'
)

# Safe and repeatable: replace existing generated placard only.
data['ObjectStates'] = [o for o in data['ObjectStates'] if o.get('GUID') != BOX_GUID]
data['ObjectStates'].append(game_box_object())

# Preserve both externally arranged Round stacks even on a standalone box rebuild.
from round_stack_layout import arrange_round_stacks
round_positions = json.loads(Path('config/round_stack_layout.json').read_text())
arrange_round_stacks(data, round_positions)

path = args.output / 'Game_Box_Test_Save.json'
path.write_text(json.dumps(data, indent=2) + '\n')
print('Written', path)
print('Game Box Sack items:', len(main['ContainedObjects']))
print('Catalog on placard:', external_catalog is not None)
print('Board size: %.3f x %.3f TTS units' % (BOARD_W, BOARD_H))
print('Rulebook:', args.rules_pdf)
print('Prototype photographs: docs/Box Illustrations.pdf')
