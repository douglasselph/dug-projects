#!/usr/bin/env python3
"""Build a textured open VP tray OBJ and TTS test save from an existing save.
Run from leaf-tts root: python3 scripts/build_vp_tray.py --save TS_Save_15.json
This standalone version defaults to the companion input paths for ease of testing.
"""
import argparse, copy, json, math
from pathlib import Path
from PIL import Image, ImageOps

p=argparse.ArgumentParser(); p.add_argument('--save',type=Path,default=Path('TS_Save_15.json'));p.add_argument('--assets',type=Path,default=Path('assets/trays'));p.add_argument('--output',type=Path,default=Path('generated/vp_tray'));p.add_argument('--tts-root',default='Z:/home/dug/src/dug-projects/leaf-tts'); args=p.parse_args()
out=args.output;out.mkdir(parents=True,exist_ok=True)
art=Image.open(args.assets/'Tray_ VP.jpg').convert('RGB'); grass=Image.open(args.assets/'grassy_field.png').convert('RGB')
# A 2x2 atlas: upper-left = floor art, upper-right = grass for walls.
# Lower halves are repeated to avoid edge bleed at mipmap levels.
S=1024; a=Image.new('RGB',(S,S)); a.paste(art.resize((512,512),Image.Resampling.LANCZOS),(0,0));
g=ImageOps.fit(grass,(512,512));a.paste(g,(512,0));a.paste(g,(0,512));a.paste(g,(512,512))
a.save(out/'VP_Tray_Texture.png')
# We use 1 TTS world unit ~= 1 physical inch, consistent with scaled cards.
# Artwork aspect ratio determines the width/depth; adjustable dimensions.
W=3.9; D=W*art.height/art.width; H=1.0; T=.12; FLOOR=.12
# The 2 regions are divided where the line appears in the artwork (y=205/414).
DIVZ=-D/2+D*(205/art.height)
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
box(-W/2+T,W/2-T,FLOOR,H,DIVZ-T/2,DIVZ+T/2)
obj=['mtllib VP_Tray.mtl','o Leaf_And_Let_Die_VP_Tray']
obj+=['v %.6f %.6f %.6f'%v for v in verts];obj+=['vt %.6f %.6f'%t for t in uv]
obj+=['usemtl Tray'];obj+=['f '+' '.join(f'{v}/{t}' for v,t in zip(vs,ts)) for vs,ts in faces]
(out/'VP_Tray.obj').write_text('\n'.join(obj)+'\n')
(out/'VP_Tray.mtl').write_text('newmtl Tray\nKd 1.0 1.0 1.0\nmap_Kd VP_Tray_Texture.png\n')
if args.save.exists():
 data=json.loads(args.save.read_text());main=next(o for o in data['ObjectStates'] if o.get('Nickname','').startswith('Game Box'))
 vp=next(o for o in main['ContainedObjects'] if o.get('Nickname')=='VP Tray')
 mesh=f'{args.tts_root}/generated/vp_tray/VP_Tray.obj';tex=f'{args.tts_root}/generated/vp_tray/VP_Tray_Texture.png'
 item={'GUID':'f0a901','Name':'Custom_Model','Transform':{'posX':7.5,'posY':1.05,'posZ':0,'rotX':0,'rotY':0,'rotZ':0,'scaleX':1,'scaleY':1,'scaleZ':1},'Nickname':'VP Tray — Physical Test','Description':'Open 1-inch-wall tray. Grassy walls, VP art on floor. Two compartments.', 'GMNotes':'Generated from assets/trays/Tray_ VP.jpg', 'ColorDiffuse':{'r':1,'g':1,'b':1},'Locked':True,'Grid':False,'Snap':False,'LuaScript':'','LuaScriptState':'','XmlUI':'','CustomMesh':{'MeshURL':mesh,'DiffuseURL':tex,'NormalURL':'','ColliderURL':mesh,'Convex':False,'MaterialIndex':0,'Type':0,'CastShadows':True}}
 data['ObjectStates'].append(item)
 for i,deck in enumerate(vp['ContainedObjects']):
  d=copy.deepcopy(deck);d['GUID']='f0a90'+str(i+2);d['Transform']['posX']=7.5;d['Transform']['posY']=1.32;d['Transform']['posZ']=(-D/4 if i==0 else D/4);d['Transform']['rotY']=0
  data['ObjectStates'].append(d)
 data['SaveName']=data.get('SaveName','Leaf & Let Die')+' — VP Tray Test'
 (out/'VP_Tray_Test_Save.json').write_text(json.dumps(data,indent=2))
 print('Written tray mesh, texture and test save in',out)
else:print('Written OBJ and texture; no save provided')
