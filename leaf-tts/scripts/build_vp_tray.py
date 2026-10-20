#!/usr/bin/env python3
"""Build a textured open VP tray OBJ and TTS test save from an existing save."""
import argparse, copy, json
from pathlib import Path
from PIL import Image, ImageOps

p=argparse.ArgumentParser()
p.add_argument('--save',type=Path,default=Path('source/TS_Save_latest.json'))
p.add_argument('--assets',type=Path,default=Path('assets/trays'))
p.add_argument('--output',type=Path,default=Path('generated/vp_tray'))
p.add_argument('--tts-root',default=None)
args=p.parse_args()
if args.tts_root is None:
    args.tts_root='Z:'+str(Path.cwd().resolve())
out=args.output; out.mkdir(parents=True,exist_ok=True)
art_path = next((args.assets / f'Tray_ VP{ext}' for ext in ('.png', '.jpg') if (args.assets / f'Tray_ VP{ext}').is_file()), None)
if art_path is None:
    raise FileNotFoundError('Missing tray artwork: Tray_ VP.png or Tray_ VP.jpg in ' + str(args.assets))
art = Image.open(art_path).convert('RGB')
art=ImageOps.mirror(art)
grass=Image.open(args.assets/'grassy_field.png').convert('RGB')
S=1024
atlas=Image.new('RGB',(S,S))
atlas.paste(art.resize((512,512),Image.Resampling.LANCZOS),(0,0))
g=ImageOps.fit(grass,(512,512))
atlas.paste(g,(512,0)); atlas.paste(g,(0,512)); atlas.paste(g,(512,512))
atlas.save(out/'VP_Tray_Texture.png')
UPI=3.06/3.5*1.67
W=4.5*UPI; D=4.5*UPI; H=1.0*UPI; T=.12*UPI; FLOOR=.12*UPI
DIVX=0.0
verts=[]; uv=[]; faces=[]
def quad(points, uvcoords):
    vi=len(verts)+1; ti=len(uv)+1; verts.extend(points); uv.extend(uvcoords)
    faces.append((tuple(range(vi,vi+4)),tuple(range(ti,ti+4))))
def box(x0,x1,y0,y1,z0,z1, floor_top=False):
    grassuv=[(.52,.98),(.98,.98),(.98,.52),(.52,.52)]
    flooruv=[(.01,.99),(.49,.99),(.49,.51),(.01,.51)]
    quads=[
       ([(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],flooruv if floor_top else grassuv),
       ([(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)],grassuv),
       ([(x0,y0,z0),(x0,y1,z0),(x1,y1,z0),(x1,y0,z0)],grassuv),
       ([(x1,y0,z1),(x1,y1,z1),(x0,y1,z1),(x0,y0,z1)],grassuv),
       ([(x0,y0,z1),(x0,y1,z1),(x0,y1,z0),(x0,y0,z0)],grassuv),
       ([(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)],grassuv)]
    for pts,uvs in quads:
        quad(pts,uvs)
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
box(-W/2,-W/2+T,FLOOR,H,-D/2,D/2)
box(W/2-T,W/2,FLOOR,H,-D/2,D/2)
box(-W/2+T,W/2-T,FLOOR,H,-D/2,-D/2+T)
box(-W/2+T,W/2-T,FLOOR,H,D/2-T,D/2)
box(DIVX-T/2,DIVX+T/2,FLOOR,H,-D/2+T,D/2-T)
obj=['mtllib VP_Tray.mtl','o Leaf_And_Let_Die_VP_Tray']
obj += ['v %.6f %.6f %.6f' % v for v in verts]
obj += ['vt %.6f %.6f' % t for t in uv]
obj += ['usemtl Tray']
obj += ['f ' + ' '.join(f'{v}/{t}' for v,t in zip(vs,ts)) for vs,ts in faces]
(out/'VP_Tray.obj').write_text('\n'.join(obj)+'\n')
(out/'VP_Tray.mtl').write_text('newmtl Tray\nKd 1.0 1.0 1.0\nmap_Kd VP_Tray_Texture.png\n')
if args.save.exists():
    data=json.loads(args.save.read_text())
    main=next((o for o in data['ObjectStates'] if o.get('Nickname','').startswith('Game Box')), None)
    vp=next((o for o in main.get('ContainedObjects',[]) if o.get('Nickname')=='VP Tray'),None) if main else None
    mesh=f'{args.tts_root}/generated/vp_tray/VP_Tray.obj'
    tex=f'{args.tts_root}/generated/vp_tray/VP_Tray_Texture.png'
    data['ObjectStates']=[o for o in data['ObjectStates'] if o.get('GUID') not in ('f0a901','f0a902','f0a903')]
    item={'GUID':'f0a901','Name':'Custom_Model',
      'Transform':{'posX':7.5,'posY':1.05,'posZ':0,'rotX':0,'rotY':270,'rotZ':0,'scaleX':1,'scaleY':1,'scaleZ':1},
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
            d['Transform']['posZ']=(D/4 if i==0 else -D/4)
            d['Transform']['rotY']=180
            data['ObjectStates'].append(d)
    (out/'VP_Tray_Test_Save.json').write_text(json.dumps(data,indent=2)+'\n')
    print('Written model, texture and updated save in',out)
else:
    print('Written OBJ and texture; no input save supplied')
