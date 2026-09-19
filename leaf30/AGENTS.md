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
