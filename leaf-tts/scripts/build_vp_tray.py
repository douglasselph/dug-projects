#!/usr/bin/env python3
"""Build a textured open VP tray OBJ and TTS test save from an existing save.
Run from leaf-tts root: python3 scripts/build_vp_tray.py --save TS_Save_15.json
This standalone version defaults to the companion input paths for ease of testing.
"""
import argparse, copy, json, math
from pathlib import Path
from PIL import Image, ImageOps

p=argparse.ArgumentParser(); p.add_argument('--save',type=Path,default=Path('TS_Save_15.json'));p.add_argument('--assets',type=Path,default=Path('assets/trays'));p.add_argument('--output',type=Path,default=Path('generated/vp_tray'));p.add_argument('--tts-root',default=None); args=p.parse_args()
if args.tts_root is None: args.tts_root='Z:'+str(Path.cwd().resolve())
out=args.output;out.mkdir(parents=True,exist_ok=True)
art=Image.open(args.assets/'Tray_ VP.jpg').convert('RGB'); art=ImageOps.mirror(art); grass=Image.open(args.assets/'grassy_field.png').convert('RGB')
# A 2x2 atlas: upper-left = floor art, upper-right = grass for walls.
# Lower halves are repeated to avoid edge bleed at mipmap levels.
S=1024; a=Image.new('RGB',(S,S)); a.paste(art.resize((512,512),Image.Resampling.LANCZOS),(0,0));
g=ImageOps.fit(grass,(512,512));a.paste(g,(512,0));a.paste(g,(0,512));a.paste(g,(512,512))
a.save(out/'VP_Tray_Texture.png')
# We use 1 TTS world unit ~= 1 physical inch, consistent with scaled cards.
# Artwork aspect ratio determines the width/depth; adjustable dimensions.
# Physical dimensions, converted using the full-game builder calibration.
# 3.06 TTS units / 3.5 inch card * 1.67 card/dice correction.
UPI=3.06/3.5*1.67
W=4.5*UPI; D=4.5*UPI; H=1.0*UPI; T=.12*UPI; FLOOR=.12*UPI
# The 2 regions are divided where the line appears in the artwork (y=205/414).
DIVX=0.0  # North–South divider, separating left/right compartments
verts=[];uv=[];faces=[]
def quad(points, uvcoords):
    vi=len(verts)+1;ti=len(uv)+1;verts.extend(points);uv.extend(uvcoords)
    faces.append((tuple(range(vi,vi+4)),tuple(range(ti,ti+4))))
def box(x0,x1,y0,y1,z0,z1, floor_top=False):
    # Each quad has outward-pointing normals for backface visibility/collisions.
    # UV coordinates have OBJ bottom-left origin, opposite Pillow's top-left.
    grassuv=[(.52,.98),(.98,.98),(.98,.52),(.52,.52)]
    flooruv=[(.01,.99),(.49,.99),(.49,.51),(.01,.51)]
    q=[
       ([(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],flooruv if floor_top else grassuv),
       ([(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)],grassuv),
       ([(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(x1,y0,z0)],grassuv),
       ([(x1,y0,z1),(x1,y1,z1),(x0,y1,z1),(x0,y0,z1)],grassuv),
       ([(x0,y0,z1),(x0,y1,z1),(x0,y1,z0),(x0,y0,z0)],grassuv),
       ([(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)],grassuv)]
    for pts,uvs in q:quad(pts,uvs)
# Single floor plus four exterior walls and a central separator.
box(-W/2,W/2,0,FLOOR,-D/2,D/2,True)
box(-W/2,-W/2+T,FLOOR,H,-D/2,D/2)
box(W/2-T,W/2,FLOOR,H,-D/2,D/2)
box(-W/2+T,W/2-T,FLOOR,H,-D/2,-D/2+T)
box(-W/2+T,W/2-T,FLOOR,H,D/2-T,D/2)
box(DIVX-T/2,DIVX+T/2,FLOOR,H,-D/2+T,D/2-T)
obj=['mtllib VP_Tray.mtl','o Leaf_And_Let_Die_VP_Tray']
obj+=['v %.6f %.6f %.6f'%v for v in verts];obj+=['vt %.6f %.6f'%t for t in uv]
obj+=['usemtl Tray'];obj+=['f '+' '.join(f'{v}/{t}' for v,t in zip(vs,ts)) for vs,ts in faces]
(out/'VP_Tray.obj').write_text('\n'.join(obj)+'\n')
(out/'VP_Tray.mtl').write_text('newmtl Tray\nKd 1.0 1.0 1.0\nmap_Kd VP_Tray_Texture.png\n')
if args.save.exists():
 data=json.loads(args.save.read_text())
 main=next((o for o in data['ObjectStates'] if o.get('Nickname','').startswith('Game Box')), None)
 vp=next((o for o in main.get('ContainedObjects',[]) if o.get('Nickname')=='VP Tray'),None) if main else None
 mesh=f'{args.tts_root}/generated/vp_tray/VP_Tray.obj'
 tex=f'{args.tts_root}/generated/vp_tray/VP_Tray_Texture.png'
 # Idempotent: replace the previous physical test tray and its deck copies.
 data['ObjectStates']=[o for o in data['ObjectStates'] if o.get('GUID') not in ('f0a901','f0a902','f0a903')]
 item={'GUID':'f0a901','Name':'Custom_Model',
 'Transform':{'posX':7.5,'posY':1.05,'posZ':0,'rotX':0,'rotY':90,'rotZ':0,'scaleX':1,'scaleY':1,'scaleZ':1},
 'Nickname':'VP Tray — Physical Test',
 'Description':'4.5-inch square VP tray, 1-inch walls. North–South divider. VP artwork floor; grass walls.',
 'GMNotes':'Generated from assets/trays/Tray_ VP.jpg',
 'ColorDiffuse':{'r':1,'g':1,'b':1},'Locked':False,'Grid':False,'Snap':False,
 'LuaScript':'','LuaScriptState':'','XmlUI':'',
 'CustomMesh':{'MeshURL':mesh,'DiffuseURL':tex,'NormalURL':'',
               'ColliderURL':mesh,'Convex':False,'MaterialIndex':0,'Type':0,'CastShadows':True}}
 data['ObjectStates'].append(item)
 if vp:
  for i,deck in enumerate(vp['ContainedObjects'][:2]):
   d=copy.deepcopy(deck)
   d['GUID']='f0a90'+str(i+2)
   d['Transform']['posX']=7.5
   d['Transform']['posY']=1.05+H+0.35
   d['Transform']['posZ']=(-D/4 if i==0 else D/4)
   d['Transform']['rotY']=0
   data['ObjectStates'].append(d)
 (out/'VP_Tray_Test_Save.json').write_text(json.dumps(data,indent=2)+'\n')
 print('Written model, texture and updated save in',out)
else:print('Written OBJ and texture; no input save supplied')
