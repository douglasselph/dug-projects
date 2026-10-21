"""Maintain the prototype photograph PDF inside the Game Box Sack.

Shared by both the box generator and the existing-save layout updater.
"""
from pathlib import Path

PDF_GUID = 'f0d003'
PDF_PATH = Path('docs/Box Illustrations.pdf')
PDF_TITLE = 'Leaf & Let Die — Prototype Box Photos'
PDF_DESCRIPTION = (
    'PHOTO GUIDE: See how the actual Leaf & Let Die prototype is packed and arranged. '
    'These pictures clarify the physical Game Box, trays, and especially the Player '
    'Supply and Discard boxes, which Tabletop Simulator cannot reproduce exactly. '
    'This is a visual reference, not game rules. Read the Rulebook to learn the game.'
)
PDF_GM_NOTES = 'Local prototype photo PDF; remote players need a hosted HTTPS URL.'


def add_box_illustrations(sack, *, tts_root=None, require_file=True):
    """Upsert exactly one PDF in the Sack, preserving all other contents.

    The save is modified in memory only. Missing PDF fails before caller writes.
    """
    if require_file and not PDF_PATH.is_file():
        raise FileNotFoundError(
            f'Missing {PDF_PATH}. Put Box Illustrations.pdf under docs/ '
            'before rebuilding this save.'
        )
    if tts_root is None:
        tts_root = 'Z:' + str(Path.cwd().resolve())
    pdf_url = f'{tts_root}/{PDF_PATH.as_posix()}'
    contents = sack.setdefault('ContainedObjects', [])
    existing = [o for o in contents if o.get('GUID') == PDF_GUID]
    if len(existing) > 1:
        raise ValueError(f'Duplicate prototype PDF objects with GUID {PDF_GUID}')
    if existing:
        pdf = existing[0]
    else:
        pdf = {
            'GUID': PDF_GUID, 'Name': 'Custom_PDF',
            'Transform': {
                'posX': 0, 'posY': 1.2, 'posZ': 0,
                'rotX': 0, 'rotY': 180, 'rotZ': 0,
                'scaleX': 4.0, 'scaleY': 1, 'scaleZ': 4.0,
            },
            'ColorDiffuse': {'r': 1, 'g': 1, 'b': 1},
            'Locked': False, 'Grid': False, 'Snap': False,
            'LuaScript': '', 'LuaScriptState': '', 'XmlUI': '',
        }
        contents.append(pdf)
    pdf['Name'] = 'Custom_PDF'
    pdf['Nickname'] = PDF_TITLE
    pdf['Description'] = PDF_DESCRIPTION
    pdf['GMNotes'] = PDF_GM_NOTES
    pdf['CustomPDF'] = {
        'PDFUrl': pdf_url, 'PDFPassword': '', 'PDFPage': 0, 'PDFPageOffset': 0,
    }
    return pdf
