# Limited Asset Provenance

This research seam deliberately tracks only the first two causal questions needed by Human Baseline calibration.

## Forget-Me-Not

When `Flower_17_02` selects a Discard die, the game-local provenance ledger assigns that exact live die a stable per-game asset ID before it moves. It records the die sides and an approximate natural recycle distance: remaining Supply dice plus lower-sided (and earlier equal-sided) Discard dice plus the selected die's own draw. Because Forget-Me-Not makes the die available immediately, that count is also the estimated number of draws accelerated.

The same asset ID can then be observed when the die reaches Battle, is placed on the Battle Grid, and appears in the Strike contribution ledger. Research can therefore distinguish retrieving a strong die from actually accelerating it into Battle and from contributing to a winning or decisive Strike.

## Root Four More contrast

For `RAISE_DIE_PLUS_4`, provenance records the exact live die, before/after value and actual increase. If that die is on the Battle Grid, the asset ID flows into the Strike contribution ledger, allowing the immediate effect record to be joined to winner decisiveness, Wound decisiveness and associated Battle VP.

This does not claim that the entire final die value was caused by Root Four More. The effect provenance records only its actual value change; the Strike ledger separately records the final die contribution. That distinction prevents the research layer from overstating causality.

## Boundary

This is not a general causal graph. No generic parent/child effect tree, Mulch lineage, Compost lineage, arbitrary resource lineage or retrospective inference is introduced. The game never consults provenance when making decisions, and provenance consumes no RNG.
