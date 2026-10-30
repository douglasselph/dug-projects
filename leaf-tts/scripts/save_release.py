"""Read the rulebook revision and apply predictable display names to TTS saves."""
import re
from pathlib import Path

RULEBOOK_GUID = 'f0d002'
REV = re.compile(r'Leaf_and_Let_Die_v(\d+)-(\d+)\.pdf(?:\?.*)?$', re.I)


def revision(data):
    found = []
    def walk(o):
        if isinstance(o, dict):
            if o.get('GUID') == RULEBOOK_GUID:
                found.append(o)
            for value in o.values():
                walk(value)
        elif isinstance(o, list):
            for item in o:
                walk(item)
    walk(data.get('ObjectStates', []))
    if len(found) != 1:
        raise ValueError(f'Expected one rulebook GUID {RULEBOOK_GUID}, found {len(found)}')
    url = found[0].get('CustomPDF', {}).get('PDFUrl', '')
    match = REV.search(url)
    if not match:
        raise ValueError('Rulebook PDF URL has no vNN-NN revision; cannot label release safely')
    return f'v{int(match[1])}.{int(match[2]):02d}'


def name_local(data):
    return f'LOCAL {revision(data)}'


def name_cloud(data):
    return f'Leaf & Let Die — Blind Playtest {revision(data)} (Cloud)'
