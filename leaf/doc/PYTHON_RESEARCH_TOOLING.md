# Python research tooling

Leaf & Let Die research automation is beginning to move application/reporting logic out of large Bash scripts and into testable Python modules.

The Python research source root is:

```text
src/research/python/
```

The importable package is:

```text
leaf_experiments
```

## Invocation model

Do **not** require a global package installation. Repository commands should invoke modules through:

```bash
bin/research_python -m leaf_experiments.<module> [arguments]
```

`bin/research_python` discovers the repository relative to itself and prepends `src/research/python` to `PYTHONPATH` before launching `python3`.

Small stable user-facing commands may remain in `bin/` as thin wrappers. They should contain no research/business logic beyond selecting the Python module and forwarding arguments.

Example:

```bash
bin/research_python_smoke --echo hello
```

## Tests

The Python test suite uses pytest and lives at:

```text
src/research/python/tests/
```

Run it from the repository root with:

```bash
bin/research_python -m pytest -c src/research/python/pytest.ini src/research/python/tests
```

The initial smoke suite verifies:

- the package imports correctly;
- fixture files are readable;
- the repository root is resolved correctly;
- the generic launcher works from the repository root;
- executable Python modules receive CLI arguments;
- an existing `PYTHONPATH` is preserved after the research source path is prepended.

## Dependencies

Production research modules should prefer the Python standard library. pytest is the only test dependency introduced by this skeleton.

No Gradle configuration is required for Python tooling, and Python tests must not implicitly invoke Gradle.
