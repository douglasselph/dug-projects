# R1 — First question-driven card-effect experiment

## Question and limitations

**Design question:** Does allowing Sapping Snapdragon to choose a *different* Battle die
make the effect too powerful, or make the card more useful?

R1 implements a **reproducible exploratory A/B comparison**, **not** a conclusive
card-balance decision. Both conditions use **Human Baseline for every seat**;
there is **no learning, force-buy incentive, or countersearch** at this stage.
Each game has the same nine-card Grove and identical cost, quantity, VP,
Rounds and Wisps; only one typed Plant-effect mapping changes.

- **Original:** `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW`.
  Select a Battle die, raise it +2 (capped), reduce opposing dice in its
  Strike Row by up to 2 (not below 1), and raise that same die by the total
  **actual** reduction (capped).
- **Different-die:** `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW`.
  Raise one Battle die +2; choose **another distinct** placed Battle die.
  Reduce opponents in **that other die's** Strike Row and raise the **other die**
  by the actual amount reduced. Requires at least two eligible Battle dice;
  there is no fallback to the same die. Cultivation +2 remains unchanged.

This description follows the two existing executable Kotlin effects and tests,
not an unconfirmed change to the printed card. R1 does **not** propose that
one version is necessarily stronger than the other; having to divide the two
raises between dice and requiring two dice could make the variant weaker.

## Locked default nine-card Grove

One each of Root Four More (Root 5), Root Loot (Root 7), Root Kindred (Root 9),
Shift Happens (Vine 7), Low & Behold (Vine 9), Vine's the Limit (Vine 11),
Sapping Snapdragon (Flower 11), Bloom Backflip (Flower 14), and Queen's Blossom
(Flower 17). The program validates their canonical IDs, card types and costs;
refuse to proceed if the catalog differs. To research other Groves, explicitly
pass nine card IDs using `--grove=`, preserving slot order and Sapping in Flower 11.
The Round setup is the production `GameRoundSetup.standard()` and Wisp/Round
catalogs are unchanged and hashed. There are no learned weights in R1.

## Running on Ubuntu (from the top of `leaf/`)

```bash
# Minimal end-to-end pilot (4 games per condition, each in a separate JVM):
python3 bin/r1_sapping_compare --games 4 --chunk 2 --players 2 \
  |& tee ~/Downloads/r1-sapping-pilot.log

# Larger baseline exploration only after checking that Sapping is actually used:
python3 bin/r1_sapping_compare --games 250 --chunk 25 --players 2 \
  |& tee ~/Downloads/r1-sapping-250.log
```

The wrapper compiles **once**, then launches one **persistent JVM per condition**
through `bin/lib/run_simulation_java`, respecting
`doc/SIMULATION_LAUNCH_ARCHITECTURE.md`. Each condition independently initializes
and closes its game catalogs/Koin state. No Gradle process is started per game
or per chunk. Progress includes the current chunk and an **overall** estimate
for the two conditions after at least one chunk is measured.

Results go to `output/experiments/r1-sapping-effect-comparison/<fingerprint>/`.
Re-running the identical command validates and skips completed chunks, and the
wrapper creates a single `<fingerprint>.tgz` archive outside the result folder.
The manifest fingerprints relevant Kotlin sources, input files, runner, Grove,
seeds, player count, chunk size and both effect names. Changing code or catalog
will produce a new run ID; never merge old results into a new rules version.

Each TSV row corresponds to one player in one completed game, with named Plants
purchased, named grafted Plants at game end, cycling dice shape plus separately
stored/pending Mulch dice, scores, Strike VP,
Sapping purchases and Cultivation/Battle activations. Each condition logs its
actual chunk progress; `paired-seat-outcomes.tsv` joins conditions by the same
sample and seat. `assessment.json` and `ASSESSMENT.md` summarize counts and
bootstrap uncertainty on game-level differences.

## What to conclude

- If there are **no different-die Battle activations**, the experiment has **not
  tested its actual effect in play**. No win-rate inference is meaningful.
- If a version is exercised, the A/B changes in VP, activation and purchases
  are useful exploratory signals, not proof of over/underpower.
- **All seats have the same effect** in a given condition: average all-seat win
  share is mechanically `1 / players`. We must **never** report S2 minus S1
  as a causal change in one player's chance of winning.
- The win share of players *who bought* Sapping is **selection-confounded**:
  they differ in dice, initial luck, market opportunities and decisions.
- Identical initial random seeds do not guarantee identical random events
  once choices and execution diverge; matched-seed per-game differences must
  be interpreted accordingly.
- The honest R1 assessment is **Investigate**, possibly because the effect
  was not exercised. A "No concern demonstrated" or "Strong balance concern"
  requires R2 strategy discovery and R3 independent competitive countersearch.

**Stop/go:** Before a longer run, inspect whether both effects actually
execute and whether baseline ownership is sufficient. If exposure is small,
R2 must deliberately search for strategies using the card; blindly increasing
R1 sample count is usually inefficient.

Note that `owned_dice` in the row schema counts dice cycling through supply, hand
and discard; `mulch_stored_dice` accounts separately for held/pending Mulch.
The per-game final shapes are snapshots, **not** complete per-die provenance.
Detailed acquisition/upgrade/trash histories are the existing D4–D7 tracker
facility to integrate during R2 as needed.
