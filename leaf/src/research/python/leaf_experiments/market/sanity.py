"""Current-market sanity-check entry point using the shared market runner."""

from __future__ import annotations

from typing import Sequence

from .confirmation import SANITY_PROFILE, main_for_profile


def main(argv: Sequence[str] | None = None) -> int:
    return main_for_profile(SANITY_PROFILE, argv)


if __name__ == "__main__":
    raise SystemExit(main())
