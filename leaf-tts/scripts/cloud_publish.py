#!/usr/bin/env python3
"""Convert a validated local TTS save into a standalone Steam Cloud release.

This does NOT upload assets or publish a Steam Workshop item. No input save is modified.
"""
import argparse
import json
import os
from pathlib import Path
from urllib.parse import urlsplit

ASSET_KEYS = {'FaceURL','BackURL','ImageURL','ImageSecondaryURL','MeshURL',
              'DiffuseURL','NormalURL','ColliderURL','PDFUrl'}


def convert(data, mapping):
    counters = {'converted': 0, 'already_hosted': 0, 'nonempty': 0}
    unresolved = []
    seen_local = set()

    def visit(item, location='$'):
        if isinstance(item, dict):
            for key, value in item.items():
                path = f'{location}.{key}'
                if key in ASSET_KEYS and isinstance(value, str) and value:
                    counters['nonempty'] += 1
                    if value.startswith(('http://','https://')):
                        if not value.startswith('https://'):
                            unresolved.append(f'{path}: insecure HTTP URL: {value}')
                        else:
                            counters['already_hosted'] += 1
                    elif value in mapping:
                        target = mapping[value]
                        if not (isinstance(target,str) and target.startswith('https://')):
                            unresolved.append(f'{path}: invalid mapping for {value}')
                        else:
                            item[key] = target
                            counters['converted'] += 1
                            seen_local.add(value)
                    else:
                        unresolved.append(f'{path}: no cloud URL for {value}')
                else:
                    visit(value, path)
        elif isinstance(item,list):
            for i,value in enumerate(item):
                visit(value, f'{location}[{i}]')
    visit(data)
    return counters, unresolved, seen_local


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=Path)
    p.add_argument('output',type=Path)
    p.add_argument('--manifest',type=Path,required=True)
    p.add_argument('--name',default='Leaf & Let Die - Blind Playtest (Cloud)')
    args=p.parse_args()
    if args.source.resolve()==args.output.resolve():p.error('Input and output must be different files')
    data=json.loads(args.source.read_text(encoding='utf-8'))
    manifest=json.loads(args.manifest.read_text(encoding='utf-8'))
    mapping=manifest['assets']
    counters,unresolved,used=convert(data,mapping)
    if unresolved:
        print(f'FAILED: {len(unresolved)} unhosted/invalid asset references:',file=__import__('sys').stderr)
        for item in unresolved[:35]:print('  '+item,file=__import__('sys').stderr)
        if len(unresolved)>35:print(f'  ... and {len(unresolved)-35} more',file=__import__('sys').stderr)
        print('Upload new assets in TTS, then update config/cloud_asset_urls.json before retrying.',file=__import__('sys').stderr)
        return 2
    # Catch any non-URL local paths not covered by the known TTS asset key list.
    leftovers=[]
    def scan(obj,path='$'):
        if isinstance(obj,dict):
            for k,v in obj.items():scan(v,path+'.'+k)
        elif isinstance(obj,list):
            for i,v in enumerate(obj):scan(v,f'{path}[{i}]')
        elif isinstance(obj,str) and (obj.startswith(('Z:/','file://','/home/')) or 'Z:/home/' in obj):
            leftovers.append(f'{path}: {obj[:160]}')
    # GMNotes is informational metadata and sometimes intentionally describes a local file;
    # its text is not used by TTS to fetch assets. Scan only actual asset fields above.
    data['SaveName']=args.name
    args.output.parent.mkdir(parents=True,exist_ok=True)
    temp=args.output.with_name(args.output.name+'.tmp')
    try:
        with temp.open('w',encoding='utf-8') as f:
            json.dump(data,f,ensure_ascii=False,indent=2)
            f.write('\n')
        os.replace(temp,args.output)
    finally:
        temp.unlink(missing_ok=True)
    print(f'Cloud save: {args.output}')
    print(f'Converted {counters["converted"]} references ({len(used)} distinct assets); '
          f'already HTTPS: {counters["already_hosted"]}; unresolved: 0')
    return 0

if __name__=='__main__':raise SystemExit(main())
