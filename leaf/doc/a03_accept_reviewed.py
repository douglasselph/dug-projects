#!/usr/bin/env python3
"""Accept ONLY explicitly reviewed A03 protected changes into the baseline.

Run from anywhere in the leaf checkout:
  python3 doc/a03_accept_reviewed.py --approve-path bin/example.py \
    --reason 'Reviewed addition for feature XYZ' --accept

Without --accept, show the pending changes and leave the manifest untouched.
Never use this script to approve changes that have not been independently reviewed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path


def run_checker(root: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(root / 'bin/a03_verify_boundaries.py'), *args],
        cwd=root, text=True, capture_output=True,
    )


def fingerprint(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--approve-path', action='append', default=[], help='Reviewed protected path (repeatable)')
    parser.add_argument('--reason', help='Meaningful review justification')
    parser.add_argument('--accept', action='store_true', help='Explicitly install reviewed baseline proposal')
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    sys.path.insert(0, str(root / 'bin'))
    import a03_verify_boundaries as a03
    manifest_path = root / a03.MANIFEST_PATH
    try:
        manifest = a03.read_manifest(manifest_path)
        approvals = {a03.validate_path(path) for path in args.approve_path}
        if approvals and (not args.reason or len(args.reason.strip()) < 10):
            raise ValueError('Explicit approvals require --reason of at least 10 characters')
        current, changes, blocked, unused = a03.check(root, manifest, approvals)
        print('A03 pending protected changes:')
        if not changes:
            print('  none')
            result = run_checker(root)
            print(result.stdout, end='')
            return result.returncode
        for item in changes:
            print(f'  {"APPROVED" if item.path in approvals else "BLOCKED"} {item.status} {item.path}')
        if unused:
            raise ValueError('Approved paths are not changed: ' + ', '.join(unused))
        if blocked:
            raise ValueError('Unapproved protected changes exist; baseline unchanged')
        if not args.accept:
            print('DRY RUN only. To accept after review, append --accept.')
            return 0
        # Produce a real A03 proposal first; do not duplicate its hashing/format rules.
        with tempfile.TemporaryDirectory(prefix='leaf-a03-review-') as temp:
            proposal = Path(temp) / 'proposed.json'
            cmd = []
            for path in sorted(approvals):
                cmd.extend(['--approve-path', path])
            result = run_checker(root, *cmd, '--reason', args.reason,
                                 '--propose-baseline', str(proposal))
            print(result.stdout, end='')
            if result.returncode:
                raise RuntimeError('A03 refused proposal: ' + result.stderr.strip())
            proposed = a03.read_manifest(proposal)
            expected = dict(manifest['protected_files'])
            for item in changes:
                if item.status == 'REMOVED':
                    expected.pop(item.path)
                else:
                    expected[item.path] = current[item.path]
            if proposed['protected_files'] != expected:
                raise RuntimeError('Proposal differs beyond explicitly reviewed files')
            old_bytes = manifest_path.read_bytes()
            if proposed['previous_baseline_digest'] != hashlib.sha256(old_bytes).hexdigest():
                raise RuntimeError('Proposal no longer matches baseline being accepted')
            # Atomic replace; keep dated-by-content backup outside protected scope.
            backup = manifest_path.with_name(manifest_path.name + '.backup-' + fingerprint(manifest_path)[:12])
            if not backup.exists():
                backup.write_bytes(old_bytes)
            fd, temp_name = tempfile.mkstemp(prefix='.a03-manifest-', dir=manifest_path.parent)
            try:
                with os.fdopen(fd, 'wb') as out:
                    out.write(proposal.read_bytes())
                os.replace(temp_name, manifest_path)
            finally:
                if os.path.exists(temp_name):
                    os.unlink(temp_name)
            result = run_checker(root)
            print(result.stdout, end='')
            if result.returncode:
                manifest_path.write_bytes(old_bytes)
                raise RuntimeError('A03 verification failed after installation; original manifest restored')
            print('A03 BASELINE ACCEPTED: exact reviewed paths only.')
            print('Backup:', backup.relative_to(root))
            print('Commit: git add doc/A03_PROTECTED_BASELINE.json')
            return 0
    except (ValueError, OSError, RuntimeError, json.JSONDecodeError) as exc:
        print('A03 ACCEPT FAIL:', exc, file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
