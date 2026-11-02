# Flower 14/17 Expert-Buy "Why" Plan

## Decision standard

The primary balance authority remains Human Baseline play. Learned Buy is a secondary expert-play stress test: a Flower tier becomes concerning when successful expert Buy policies almost never select it even when it is legal to buy.

The purpose of this work is not to make expert and Human purchase frequencies equal. It is to determine whether a sub-1% expert rate is a robust expert judgment about the card/cost package or an interaction with one fixed Human Baseline decision family.

## Causal-isolation ladder

Run these experiments in order. Do not switch every policy to learned at once, because that can show that the result changes without identifying which Human Baseline decision family caused the change.

### 0. Human-only reference

Measure Flower 14/17 use with every policy Human Baseline. This is the primary design reference.

### 1. Buy-only expert screen — already established

Learn Buy while all surrounding decisions remain Human Baseline. Measure purchase rate and selection-when-legal. The current <1% cases are expert-screen flags, not automatic redesign verdicts.

### 2. Cultivation Main context ablation — implemented by `bin/experiment_flower_tier_why`

**Fresh-start requirement:** Cultivation Main training starts from zero weights. The experiment intentionally passes a nonexistent input path; the trainer converts that to `LearnedCultivationMainWeights.zeros()` and then prepares the zero policy against the current Plant catalog/effective costs. No frozen `data/ai/.../cultivation-main-policy-v1.weights` file is used.

The default output root is `output/experiments/flower-tier-why-zero-init` so an older partial run cannot be accidentally resumed.

For each certified Buy learner:

1. Keep the same frozen Buy learner.
2. Train Cultivation Main with that Buy learner active.
3. Evaluate the same Buy learner twice on matched seeds and Groves:
   - learned Buy + Human Cultivation Main;
   - learned Buy + learned Cultivation Main.
4. Compare Flower 14/17 selection when legal.

If the tier revives, Human Cultivation Main is a causal contributor. If it remains near zero, Human Cultivation Main is not a sufficient explanation.

### 3. Plant Effect / Targeting context ablation

Repeat the same one-family replacement with Plant Effect / Targeting. This tests whether Flowers are being rejected because the Human Baseline does not exploit their effects well enough after purchase.

### 4. Battle-context ablation

Because player count directly changes Battle, test trusted Battle policy families one at a time, beginning with Battle Support. Battle Main should wait until its learner is trustworthy. A strong 2p-vs-4p Flower difference that moves under Battle-policy replacement would directly identify Battle usage/value as the source.

### 5. Retrain Buy inside the identified context

A frozen Buy learner was trained against Human Baseline surroundings. If an ablation materially changes Flower demand, retrain Buy with that altered policy context active. This distinguishes a transient mismatch from a stable joint strategy.

### 6. Multi-policy / coordinated expert confirmation

Only after one-family ablations identify the important interactions, train a small coordinated set such as Buy + Cultivation Main + Plant Effect. Use this as a robustness confirmation, not as the first diagnostic, because simultaneous changes are harder to attribute.

### 7. Independent-seed replication

Repeat the decisive experiment with fresh evaluation seeds. For a redesign-level conclusion, also consider fresh training seeds. Large effects should reproduce qualitatively.

## Interpretation threshold

A Flower card/tier is a strong redesign candidate only when:

- Human Baseline still says the card is reasonable enough to be part of normal play;
- multiple successful Buy learners put it below roughly 1% selection when legal;
- the low rate survives the relevant one-family context ablations;
- it remains low after Buy is retrained in the important context; and
- the result replicates on independent seeds.

If the card revives when a particular surrounding Human policy is replaced, diagnose that interaction rather than automatically changing the physical card.
