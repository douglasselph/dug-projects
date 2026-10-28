"""Versioned rulebook selection and in-place TTS Game Box Sack update."""
import re
from pathlib import Path

RULEBOOK_GUID = 'f0d002'
PATTERN = re.compile(r'^Leaf_and_Let_Die_v(\d+)-(\d+)\.pdf$')


def latest_rulebook(docs=Path('docs')):
    candidates = []
    for path in docs.glob('Leaf_and_Let_Die_v*.pdf'):
        match = PATTERN.fullmatch(path.name)
        if match:
            candidates.append(((int(match[1]), int(match[2])), path))
    if not candidates:
        raise FileNotFoundError(f'No Leaf_and_Let_Die_vNN-NN.pdf rulebook found in {docs}')
    return max(candidates, key=lambda pair: pair[0])[1]


def update_rulebook(sack, pdf_path=None, tts_root=None):
    """Update existing PDF only, preserving GUID, transform, page and other Sack items."""
    path = Path(pdf_path) if pdf_path else latest_rulebook()
    if not path.is_file():
        raise FileNotFoundError(f'Missing rulebook PDF: {path}')
    match = PATTERN.fullmatch(path.name)
    if not match:
        raise ValueError(f'Expected versioned rulebook name Leaf_and_Let_Die_vNN-NN.pdf: {path}')
    contents = sack.get('ContainedObjects', [])
    matches = [obj for obj in contents if obj.get('GUID') == RULEBOOK_GUID]
    if len(matches) != 1 or matches[0].get('Name') != 'Custom_PDF':
        raise ValueError(f'Expected exactly one Custom_PDF {RULEBOOK_GUID} in Game Box Sack; found {len(matches)}. Save not written.')
    if tts_root is None:
        tts_root = 'Z:' + str(Path.cwd().resolve())
    pdf = matches[0]
    pdf['Nickname'] = f'Leaf & Let Die — Rulebook v{int(match[1])}.{int(match[2]):02d}'
    pdf['Description'] = 'START HERE: Take out and read this rulebook to set up and learn Leaf & Let Die.'
    pdf.setdefault('CustomPDF', {})['PDFUrl'] = f'{tts_root.rstrip("/")}/{path.as_posix()}'
    return path
