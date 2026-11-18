# Project Instructions

## Kotlin Style

When editing or creating Kotlin files, place `companion object` near the top of the class body, before normal properties and functions.

Prefer this order:

1. Class declaration
2. `companion object`
3. Constructor-injected properties and private properties
4. Public properties
5. Public functions
6. Private helper functions

Do not place `companion object` at the bottom unless the existing file strongly establishes that style or Kotlin syntax requires a different structure.

## Delivering source changes (required across conversations)

For **every** changed-files ZIP, assume the user is already in the repository's top-level `leaf/` directory when extracting. ZIP entry paths **must be relative to that directory** (`src/...`, `doc/...`, `README.md`, `TASKS.md`, etc.), **never prefixed by `leaf/`**. Do not create a nested `leaf/leaf/` directory. Give the unzip command in this form, with the archive in Downloads:

```bash
unzip -o ~/Downloads/<changed-files.zip>
```

Inspect and verify the ZIP's entry list before delivery. See the "Operational and developer workflow requirements" section of `README.md`. Carry this convention forward in every new coding conversation.
