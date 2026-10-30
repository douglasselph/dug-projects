#!/usr/bin/env python3
"""Write a copy of the staged TTS save with concise, versioned SaveName."""
import json
import os
import sys
from pathlib import Path
from save_release import name_local

def main():
    src, dest = map(Path, sys.argv[1:3])
    data = json.loads(src.read_text(encoding='utf-8'))
    data['SaveName'] = name_local(data)
    dest.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('Local TTS display name:', data['SaveName'])

if __name__ == '__main__':
    main()
