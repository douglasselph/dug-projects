"""Small executable used to verify the research Python environment."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Sequence

from .paths import find_project_root


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Leaf & Let Die research Python smoke check")
    parser.add_argument("--echo", default=None, help="Print the supplied value")
    parser.add_argument(
        "--show-project-root",
        action="store_true",
        help="Print the discovered repository root",
    )
    parser.add_argument(
        "--read-file",
        type=Path,
        default=None,
        help="Read a UTF-8 text file and print its contents",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    if args.echo is not None:
        print(args.echo)

    if args.show_project_root:
        print(find_project_root())

    if args.read_file is not None:
        print(args.read_file.read_text(encoding="utf-8"), end="")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
