from __future__ import annotations

import os
import subprocess
from pathlib import Path

import leaf_experiments
from leaf_experiments.paths import find_project_root, fixture_path, research_python_root


def test_package_imports() -> None:
    assert leaf_experiments.__version__ == "0.1.0"


def test_fixture_file_can_be_read() -> None:
    fixture = fixture_path("smoke.txt")
    assert fixture.read_text(encoding="utf-8") == "leaf-research-python-fixture\n"


def test_project_root_resolution() -> None:
    root = find_project_root()
    assert (root / "settings.gradle.kts").is_file()
    assert (root / "build.gradle.kts").is_file()
    assert research_python_root(root) == root / "src" / "research" / "python"


def test_package_imports_from_repository_root_via_launcher() -> None:
    root = find_project_root()
    completed = subprocess.run(
        [
            str(root / "bin" / "research_python"),
            "-c",
            "import leaf_experiments; print(leaf_experiments.__version__)",
        ],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    assert completed.stdout.strip() == "0.1.0"


def test_executable_module_receives_cli_arguments() -> None:
    root = find_project_root()
    completed = subprocess.run(
        [str(root / "bin" / "research_python_smoke"), "--echo", "hello-from-test"],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    assert completed.stdout.strip() == "hello-from-test"


def test_launcher_preserves_existing_pythonpath() -> None:
    root = find_project_root()
    env = os.environ.copy()
    env["PYTHONPATH"] = "/tmp/existing-python-path"
    completed = subprocess.run(
        [
            str(root / "bin" / "research_python"),
            "-c",
            "import os; print(os.environ['PYTHONPATH'])",
        ],
        cwd=root,
        env=env,
        check=True,
        capture_output=True,
        text=True,
    )
    parts = completed.stdout.strip().split(os.pathsep)
    assert str(root / "src" / "research" / "python") == parts[0]
    assert "/tmp/existing-python-path" in parts[1:]
