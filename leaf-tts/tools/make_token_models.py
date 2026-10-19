#!/usr/bin/env python3
"""Generate two-sided alpha-contour OBJ models for Water and Mulch tokens.
Requires: Pillow, opencv-python, shapely. Re-run when the art changes.
"""
from pathlib import Path
from PIL import Image
import numpy as np
import cv2
from shapely.geometry import Polygon
from shapely.ops import triangulate
import argparse


def generate(source, obj_path, atlas_path):
    im = Image.open(source).convert('RGBA')
    w,h=im.size
    alpha=np.array(im.getchannel('A'))
    contours,_=cv2.findContours((alpha>24).astype('uint8'),cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
    if not contours:raise ValueError(f'No opaque pixels: {source}')
    contour=max(contours,key=cv2.contourArea)
    contour=cv2.approxPolyDP(contour,epsilon=0.85,closed=True).reshape(-1,2)
    polygon=Polygon(contour).buffer(0)
    if not polygon.is_valid or polygon.is_empty:raise ValueError('Invalid outline')
    # Binary outlines normally have one exterior; use biggest if buffer repaired it.
    if polygon.geom_type=='MultiPolygon': polygon=max(polygon.geoms,key=lambda p:p.area)
    tris=[t for t in triangulate(polygon) if polygon.covers(t.representative_point())]
    coords=list(polygon.exterior.coords)[:-1]
    atlas=Image.new('RGBA',(w*2,h+4),(180,165,130,255))
    atlas.paste(im,(0,0));atlas.paste(im,(w,0))
    atlas_path.parent.mkdir(parents=True,exist_ok=True)
    atlas.save(atlas_path)
    obj_path.parent.mkdir(parents=True,exist_ok=True)
    lines=['# Two-sided cutout token, front and back independently UV mapped', 'o Token']
    count_v=0;count_vt=0
    def emit_vertex(px,py,top):
        nonlocal count_v,count_vt
        x=(px/w)-.5; z=.5-(py/h); y=.027 if top else -.027
        lines.append(f'v {x:.7f} {y:.7f} {z:.7f}');count_v+=1
        # top: source orientation; bottom: invert V to keep text readable when flipped
        u=(px/(w*2)) + (0 if top else .5)
        v=(1-(py/(h+4))) if top else ((py+4)/(h+4))
        lines.append(f'vt {u:.7f} {v:.7f}');count_vt+=1
        return count_v
    for tri in tris:
        pts=list(tri.exterior.coords)[:3]
        for top in (True,False):
            ix=[emit_vertex(x,y,top) for x,y in pts]
            if top: indices=(ix[0],ix[2],ix[1])
            else: indices=(ix[0],ix[1],ix[2])
            lines.append('f '+' '.join(f'{n}/{n}' for n in indices))
    # Side faces close the mesh. UVs use a solid brown gutter in the atlas.
    for a,b in zip(coords,coords[1:]+coords[:1]):
        ix=[emit_vertex(*a,True),emit_vertex(*b,True),emit_vertex(*b,False),emit_vertex(*a,False)]
        lines.extend(['f '+' '.join(f'{i}/{i}' for i in (ix[0],ix[1],ix[2])),
                      'f '+' '.join(f'{i}/{i}' for i in (ix[0],ix[2],ix[3]))])
    obj_path.write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print(f'{source.name}: {len(coords)} contour points, {len(tris)} front/back triangles')

if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--project',type=Path,default=Path(__file__).resolve().parent.parent)
    a=p.parse_args()
    for name in ('Water','Mulch'):
        generate(a.project/'assets/tokens'/f'Token_ {name}.png',
                 a.project/'models/token_shapes'/f'{name.lower()}.obj',
                 a.project/'models/token_shapes'/f'{name.lower()}_two_sided.png')
