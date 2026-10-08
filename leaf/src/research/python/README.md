# Leaf & Let Die research Python tooling

This source tree contains Python tooling for simulation research, reporting, and experiment orchestration. It is deliberately separate from the Kotlin game engine and simulation source sets.

## Layout

```text
src/research/python/
  leaf_experiments/     Python package
  tests/                pytest tests
    fixtures/           small deterministic test fixtures
  pytest.ini            pytest configuration
```

## No installation is required

Repository `bin/...` scripts should invoke Python through:

```bash
bin/research_python -m leaf_experiments.some_module ...
```

`bin/research_python` prepends `src/research/python` to `PYTHONPATH`, so the package does not need to be installed globally or into the user's site-packages.

For modules that deserve a stable human-facing command, keep a tiny wrapper in `bin/`. The included smoke example is:

```bash
bin/research_python_smoke --echo hello
```

## Running tests

From the repository root:

```bash
bin/research_python -m pytest -c src/research/python/pytest.ini src/research/python/tests
```

Only Python and pytest are required. Runtime research modules should prefer the Python standard library unless a later task establishes a justified dependency.
