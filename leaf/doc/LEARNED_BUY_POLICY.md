# Learned Buy Policy

The learned Buy work is a research tool for discovering simple purchase
preferences that may expose dominant cards, costs, timing windows, or broader
balance problems. It is not a replacement for Human Baseline. Only
`choosePurchase` is learned; all other decisions remain Human Baseline and the
Human Baseline payment strategy pays for the selected purchase.

## Current feature model

The scorer is linear: each legal Buy action is converted to named features and
the feature weights are summed. This deliberately keeps learned behavior
inspectable.

The feature vocabulary contains several levels of Plant knowledge:

- general purchase/type features such as Plant, Root, Vine, and Flower;
- continuous Plant cost plus exact-cost features such as `ACTION_COST_9`;
- Cultivation-block interactions for important action/type/cost features;
- stable card-identity features such as `CARD_Vine_09_01`.

Card identity follows the stable card ID, not the display title or current card
effect. This allows a design to move through balancing changes without losing
the ability to refer to the same conceptual card. Card-specific stage
interactions are intentionally not generated yet; that would greatly increase
the feature count and can be added later when evidence justifies it.

A Buy stage is derived from completed Battle rounds. Thus the first Cultivation
block is Stage 1, the block after the first Battle is Stage 2, and so on, with
longer structures collapsed into Stage 4+. This lets patterns such as `3/2/2`
and `2/2/3` share the same semantic stage boundaries while retaining continuous
game-progress information separately.

## CardDataFiles are authoritative

The Plant CSV files loaded through `common/CardDataFiles` remain the single
source of truth. There is no second hand-maintained card manifest.

At runtime the normal card loader produces the canonical Plant objects. The AI
layer generates `PlantCardManifest` from those parsed objects. A deterministic
strategic fingerprint records the card ID and strategically meaningful fields;
presentation-only properties such as artwork are not intended to invalidate a
trained policy.

`cardManifestFormatVersion` belongs to the AI manifest algorithm, not to the
CSV files. A designer does **not** increment it after editing a card. It changes
only if the program changes what fields/normalization constitute a manifest
fingerprint.

## What happens when a Plant CSV changes

Edit the CSV normally, then run the regression suite before doing research:

```bash
./gradlew test integrationTest simulationCheck
```

The unit tests include a guard that compares the checked-in AI card-feature
schema with the current `CardDataFiles` catalog. Adding or removing a Plant can
therefore expose missing/stale `CARD_<id>` features instead of silently letting
the AI schema drift.

Changes have different meanings:

1. **Display/presentation changes** should not require learned strategy changes
   unless they alter a field deliberately included in the strategic manifest.
2. **A new or removed card ID** changes the identity feature catalog. Update the
   checked-in untrained/current policy schema and tests before training.
3. **A strategic definition changes** (for example cost, effect, or scoring)
   changes that card's manifest fingerprint. An old trained policy is then
   historical evidence for a different card environment.
4. **A card is deliberately moved to another stable ID** (for example a design
   moves from `Vine_07_01` to `Vine_09_01`) is a migration decision. Do not make
   code guess that two IDs are the same card. Decide explicitly whether the old
   identity weight should be transferred, reset, or merely retained for
   comparison.

A trained policy whose stored manifest does not match the current strategic
catalog fails with `LearnedPolicyCardManifestMismatchException` rather than
silently participating in new research. Its message identifies changed, added,
or removed cards and tells the operator to migrate/reset affected identity
knowledge and retrain. **Do not change the CSV merely to make an old policy
load; the CSV describes the current game.**

Old generic knowledge (for example a preference for Vines or D12s) can still be
a useful starting point after a card change, but a migrated policy must not be
presented as validated for the new game until it has been retrained and tested.

## Weight-file provenance

The checked-in `data/ai/buy-policy-v1.weights` is the deliberate input policy.
The current persistence format records training status and, for trained output,
research provenance including round pattern, Grove, generations, games per
candidate, population/mutation settings, RNG seeds, fitness, and the Plant
manifest snapshot/fingerprints.

This provenance answers two separate questions:

- *How was this policy trained?*
- *What Plant definitions existed when it was trained?*

The manifest is a historical snapshot. It is generated from the current CSVs
when a policy is prepared/trained; it is not another source of game rules.

## Training terminology

The trainer uses evolutionary terminology. The terms are simpler than they may
first sound:

- **Policy** — one complete candidate set of Buy weights. A policy is one
  possible Buy strategy. For example, one policy may strongly favor D12s and
  `Vine_09_01`, while another policy has different weights and therefore makes
  different purchases.
- **Population** — all candidate policies being compared in one generation. A
  population of 8 means eight different candidate Buy strategies are evaluated.
- **Generation** — one complete evaluate/select/reproduce cycle. Every policy in
  the population plays its controlled cohort of games, fitness is measured, the
  better policies are retained, and mutated descendants are created for the next
  generation. The term is used in the biological/evolutionary sense.
- **Fitness** — the number used to compare candidate policies. The current
  trainer uses the affected learned role's mean win share over its training
  cohort. Fitness is training/selection evidence, not held-out proof that the
  policy generalizes.
- **Elite** — one of the best policies copied unchanged into the next generation.
  Elitism prevents a good candidate from being lost merely because its children
  mutate badly.
- **Mutation** — a small random change to some weights when producing a child
  policy. Gaussian mutation is currently used.
- **Champion** — the best policy found by the training run. It is a candidate for
  later evaluation, not automatically the new checked-in policy.
- **Training cohort** — the controlled games used to measure fitness. Candidates
  within a generation receive the same underlying seed cohort and the learned
  role rotates seats so luck and seat position interfere less with comparisons.
- **Held-out evaluation** — new games/seeds that were not used to select the
  champion. This is the important follow-up test for whether the learned strategy
  generalizes rather than merely fitting peculiarities of its training games.

For example, the default settings use 5 generations, a population of 8 policies,
and 20 games per policy. One generation therefore evaluates `8 × 20 = 160`
policy-games, and five generations evaluate `5 × 8 × 20 = 800` policy-games. A
**policy-game** simply means one game used to evaluate one candidate policy.
Those 800 games are not 800 independent confirmations of the final champion: the
trainer is actively using them to search for that champion. Independent evidence
comes later from held-out evaluation.

A simplified generation looks like this:

```text
8 candidate policies
        ↓
each plays the same controlled 20-game cohort
        ↓
measure each policy's fitness
        ↓
keep the elites / best candidates
        ↓
create mutated children
        ↓
next generation
```

## Training workflow

Before training, make the full relevant regression gate green:

```bash
./gradlew test integrationTest simulationCheck
```

Then use the small default run as an end-to-end smoke test:

```bash
bin/train_buy_policy
```

The trainer currently uses `FirstGameDefault`, standard `3/2/2`, deterministic
mechanical and strategy seed cohorts, and seat rotation. Every candidate in a
generation receives the same controlled cohort. Evolution has its own RNG.
Elites survive unchanged and children receive Gaussian mutations.

The default run is intentionally small and should establish that the pipeline
works, not that a policy is good. Inspect both console output and:

```text
output/ai/buy-policy-v1-trained.weights
```

Confirm that weights evolved, provenance and manifest data are present, and
card-identity/exact-cost/stage features are changing. Do **not** automatically
copy this file over `data/ai/buy-policy-v1.weights`.

After a training smoke run produces a champion, evaluate it before promotion:

```bash
bin/evaluate_buy_policy --input output/ai/buy-policy-v1-trained.weights
```

The M3-G1 evaluator defaults to 1000 **matched samples** (2000 complete games):
one Human Baseline control and one learned-Buy game per sample. The pair shares
mechanical/strategy seeds and affected seat, while the affected role rotates
through all four seats. Default evaluation seeds start at 161000/171000 and the
evaluator refuses ranges that overlap the training cohorts recorded in policy
provenance. It reports win-share/VP deltas, seat effects, end-state development,
and Buy behavior by Plant cost, Plant type, individual Plant identity, and die
size. The default evaluator remains on `FirstGameDefault` and `3/2/2`. To test Grove
generalization without confounding the matched comparison, pass `--grove CODE`.
Every zero in the nine-position code is resolved using a dedicated Grove RNG
once per matched sample, and that exact concrete Grove is then shared by the
CONTROL and LEARNED games. Thus `--grove 000100000` keeps `Vine_07_01` fixed
while exposing the frozen policy to a newly resolved set of the other eight
Plants on each sample. `--random-grove` is equivalent to `--grove 000000000`.
The Grove RNG defaults to seed 181000 and can be changed with `--grove-seed`; it
does not consume the game's mechanical or strategy RNG streams.

A no-purchase (`Done`) choice is not currently a production Chronicle purchase
event, so M3-G1 does not change decision tracing just to report Done-by-stage.
That metric can be added later with an explicit compact observation seam.

For a larger explicitly requested run:

```bash
bin/train_buy_policy --generations 20 --population 16 --games 100
```

Training fitness is selection data, not independent evidence. The next research
step for a promising champion is held-out evaluation using new mechanical and
strategy seeds, with seat rotation. Future evaluation should also test Grove
and round-structure generalization where appropriate. Only after such evidence
should a trained output be deliberately promoted as a new checked-in starting
policy.

## Interpreting learned weights

The useful result is not merely a higher fitness number. The linear model lets
us separate possible explanations. For example, a purchase can score well
because Plants in general are useful, Vines are useful, cost 9 is useful at a
particular stage, or `Vine_09_01` itself has an unusually large identity
weight. A large card-specific residual is a useful trigger for a controlled
Card-Break/Dominant-Card experiment; it is not by itself proof that the card is
broken.

Likewise, changing one aggregate is not a rules-change justification. Preserve
the project's research discipline: matched controls where possible, controlled
seeds, seat rotation, adequate complete-game samples, compact summaries, and
selected Chronicle reruns before changing the game.

## Future direction

The intended sequence is to keep learned strategy discovery and causal game
experiments separate. Buy evolution can discover candidate preferences;
held-out evaluation establishes whether they generalize; then controlled card,
cost, timing, or strategy experiments investigate *why*. Later work can add
explicit policy migration helpers, broader Grove/round training cohorts,
self-play or champion opponents, and carefully chosen interaction features
without turning the first model into an opaque high-dimensional learner.
