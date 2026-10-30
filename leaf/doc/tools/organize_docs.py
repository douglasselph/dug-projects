#!/usr/bin/env python3
"""Conservatively archive unreferenced milestone notes from doc/.

Preview: python3 bin/organize_docs.py
Apply:   python3 bin/organize_docs.py --apply
Undo:    python3 bin/organize_docs.py --undo
"""
import argparse
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / 'doc'
MANIFEST = DOC / 'archive' / 'doc-moves-manifest.json'
TEXT_EXTS = {'.md','.sh','.py','.kt','.kts','.json','.yaml','.yml','.txt','.csv','.gradle','.properties'}
SKIP_DIRS = {'.git','.gradle','build','output','.idea','.venv','node_modules','archive','legacy'}

def destination(name):
    if name.startswith('C04_'):
        return 'archive/c04'
    if name.startswith(('TASK_5B','TASK5_','TASK4_')):
        return 'archive/implementation'
    if name.startswith(('B01_','B02_','B03_','B04_','B05_','B06_','B07_','B08_')):
        return 'archive/sunlight-migration'
    if name.startswith(('PHASE_C1_','PHASE_C2_','PHASE_C3_','PHASE_C4_','PHASE_C5_')):
        return 'archive/research-infrastructure'
    return None

def text_files():
    for p in ROOT.rglob('*'):
        if not p.is_file() or p.suffix.lower() not in TEXT_EXTS:
            continue
        if any(part in SKIP_DIRS for part in p.relative_to(ROOT).parts[:-1]):
            continue
        if p.stat().st_size > 2_000_000 or p.name == 'organize_docs.py':
            continue
        yield p

def git_move(src, dest):
    tracked = subprocess.run(['git','ls-files','--error-unmatch',str(src.relative_to(ROOT))], cwd=ROOT,
                             stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
    if tracked:
        subprocess.run(['git','mv',str(src.relative_to(ROOT)),str(dest.relative_to(ROOT))], cwd=ROOT,check=True)
    else:
        shutil.move(str(src),str(dest))

def undo():
    if not MANIFEST.exists():
        raise SystemExit('No move manifest exists; nothing to undo.')
    entries = json.loads(MANIFEST.read_text())['moves']
    for item in reversed(entries):
        src = ROOT / item['to']; dest = ROOT / item['from']
        if not src.exists() or dest.exists():
            raise SystemExit(f'Cannot safely undo {src}: source missing or destination exists')
        git_move(src,dest)
        print('RESTORED', item['from'])
    MANIFEST.unlink()
    print('Undo complete; historical directories are left in place.')

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    mode=ap.add_mutually_exclusive_group()
    mode.add_argument('--apply',action='store_true',help='Move eligible files and save undo manifest')
    mode.add_argument('--undo',action='store_true',help='Undo moves from manifest')
    args=ap.parse_args()
    if args.undo:
        undo(); return
    if MANIFEST.exists():
        raise SystemExit('Existing move manifest; use --undo or commit/reconcile before another run.')
    # Preserve markdown notes if any other source, script or document names them.
    corpus=[]
    for p in text_files():
        try: corpus.append((p,p.read_text(errors='replace')))
        except OSError: pass
    candidates=[]; skipped=[]
    for src in sorted(DOC.glob('*.md')):
        folder=destination(src.name)
        if not folder: continue
        references=[str(p.relative_to(ROOT)) for p,t in corpus if p!=src and src.name in t]
        if references:
            skipped.append((src.name,references[:3])); continue
        dest=DOC / folder / src.name
        if dest.exists():
            skipped.append((src.name,['destination already exists']));continue
        candidates.append((src,dest))
    print(f'Eligible for archival: {len(candidates)}; kept due to references/collision: {len(skipped)}')
    for src,dest in candidates:
        print(('MOVE ' if args.apply else 'WOULD MOVE ') + str(src.relative_to(ROOT))+' -> '+str(dest.relative_to(ROOT)))
    for name,refs in skipped:
        print('KEEP (referenced):',name, 'by', ', '.join(refs))
    if not args.apply:
        print('\nPreview only. Run with --apply to move files. No documents are deleted.')
        return
    if not candidates:
        print('Nothing to move.'); return
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    moves=[]
    for src,dest in candidates:
        dest.parent.mkdir(parents=True,exist_ok=True)
        git_move(src,dest)
        moves.append({'from':str(src.relative_to(ROOT)),'to':str(dest.relative_to(ROOT))})
    MANIFEST.write_text(json.dumps({'schema':1,'moves':moves},indent=2)+'\n')
    print(f'\nMoved {len(moves)} files. Undo: python3 bin/organize_docs.py --undo')
    print('Review git status and run the test suite before committing.')

if __name__=='__main__': main()
