#!/usr/bin/env python3
"""Generate two-sided alpha-contour OBJ models for Water and Mulch tokens.
Requires: Pillow, numpy, opencv-python-headless, shapely.
Re-run when the Water or Mulch artwork changes.

This version corrects the orientation so that:
- the front face reads normally (not mirrored)
- the back face reads normally when the token is flipped
"""
from pathlib import Path
from PIL import Image
import numpy as np
import cv2
from shapely.geometry import Polygon
from shapely.ops import triangulate
import argparse


def generate(source: Path, obj_path: Path, atlas_path: Path):
    im = Image.open(source).convert('RGBA')
    w, h = im.size
    alpha = np.array(im.getchannel('A'))
    contours, _ = cv2.findContours((alpha > 24).astype('uint8'), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        raise ValueError(f'No opaque pixels: {source}')

    contour = max(contours, key=cv2.contourArea)
    contour = cv2.approxPolyDP(contour, epsilon=0.85, closed=True).reshape(-1, 2)
    polygon = Polygon(contour).buffer(0)
    if not polygon.is_valid or polygon.is_empty:
        raise ValueError(f'Invalid outline in {source}')
    if polygon.geom_type == 'MultiPolygon':
        polygon = max(polygon.geoms, key=lambda p: p.area)

    tris = [t for t in triangulate(polygon) if polygon.covers(t.representative_point())]
    coords = list(polygon.exterior.coords)[:-1]

    # TTS's rendering/orientation of custom-model UVs makes the visible front appear
    # horizontally mirrored relative to the source image. To compensate, pre-mirror the
    # atlas front half. The visible back appears with the same orientation as its atlas
    # half when flipped, so keep the back half as the original art.
    front_im = im.transpose(Image.FLIP_LEFT_RIGHT)
    back_im = im

    atlas = Image.new('RGBA', (w * 2, h + 4), (180, 165, 130, 255))
    atlas.paste(front_im, (0, 0))
    atlas.paste(back_im, (w, 0))
    atlas_path.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(atlas_path)

    obj_path.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        '# Two-sided cutout token, front and back independently UV mapped',
        'o Token',
    ]
    count_v = 0

    def emit_vertex(px, py, top):
        nonlocal count_v
        x = (px / w) - 0.5
        z = 0.5 - (py / h)
        y = 0.027 if top else -0.027
        lines.append(f'v {x:.7f} {y:.7f} {z:.7f}')
        count_v += 1
        # Map into left (front) or right (back) atlas half.
        u = (px / (w * 2.0)) + (0.0 if top else 0.5)
        v = 1.0 - (py / (h + 4.0))
        lines.append(f'vt {u:.7f} {v:.7f}')
        return count_v

    for tri in tris:
        pts = list(tri.exterior.coords)[:3]
        # top face
        ix = [emit_vertex(x, y, True) for x, y in pts]
        lines.append('f ' + ' '.join(f'{n}/{n}' for n in (ix[0], ix[2], ix[1])))
        # bottom face
        ix = [emit_vertex(x, y, False) for x, y in pts]
        lines.append('f ' + ' '.join(f'{n}/{n}' for n in (ix[0], ix[1], ix[2])))

    # Side walls: use the brown gutter area; exact UVs there are not important.
    for a, b in zip(coords, coords[1:] + coords[:1]):
        ix = [emit_vertex(*a, True), emit_vertex(*b, True), emit_vertex(*b, False), emit_vertex(*a, False)]
        lines.append('f ' + ' '.join(f'{i}/{i}' for i in (ix[0], ix[1], ix[2])))
        lines.append('f ' + ' '.join(f'{i}/{i}' for i in (ix[0], ix[2], ix[3])))

    obj_path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'{source.name}: {len(coords)} contour points, {len(tris)} front/back triangles')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--project', type=Path, default=Path(__file__).resolve().parent.parent)
    args = parser.parse_args()
    for name in ('Water', 'Mulch'):
        generate(args.project / 'assets/tokens' / f'Token_ {name}.png',
                 args.project / 'models/token_shapes' / f'{name.lower()}.obj',
                 args.project / 'models/token_shapes' / f'{name.lower()}_two_sided.png')
