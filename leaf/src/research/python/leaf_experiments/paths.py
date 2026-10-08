"""Shared repository-path helpers for research tooling."""

from __future__ import annotations

from pathlib import Path


PROJECT_MARKERS = ("settings.gradle.kts", "build.gradle.kts")


def find_project_root(start: Path | str | None = None) -> Path:
    """Return the Leaf & Let Die repository root.

    The root is the nearest directory at or above *start* containing both
    Gradle project markers.  When *start* is omitted, discovery starts from
    this module's location, making the helper independent of the caller's
    current working directory.
    """

    candidate = Path(start).resolve() if start is not None else Path(__file__).resolve()
    if candidate.is_file():
        candidate = candidate.parent

    for directory in (candidate, *candidate.parents):
        if all((directory / marker).is_file() for marker in PROJECT_MARKERS):
            return directory

    markers = ", ".join(PROJECT_MARKERS)
    raise RuntimeError(f"Could not find project root containing: {markers}")


def research_python_root(project_root: Path | str | None = None) -> Path:
    """Return ``src/research/python`` for the current repository."""

    root = Path(project_root).resolve() if project_root is not None else find_project_root()
    return root / "src" / "research" / "python"


def fixture_path(name: str, project_root: Path | str | None = None) -> Path:
    """Return a path in the Python research test-fixture directory."""

    return research_python_root(project_root) / "tests" / "fixtures" / name
