# Learned Battle Support Policy

This research policy learns only the high-level Battle Step-5 choice. It does not learn the first or final ordinary Battle Main logic, Buy, Cultivation Main, die placement, or effect-internal targets.

## Decision boundary

The engine enumerates legal `BattleTurnAction` candidates first. The learner scores only those legal candidates. Candidates include fully formed Water, Mulch, Worm flip/heal, Butterfly, Wisp, Bee/Worm placement, Sunlight-funded Main Actions, and the normal Final Main that ends Support timing.

For Sunlight, the candidate includes the funded ordinary Main (`DRAW`, a stable Plant ID, or Round effect slot), so the policy can learn whether consuming Sunlight is worthwhile for that concrete extra Main. Lower-level choices inside the funded action remain in the existing Human/effect policies.

## Visible features

The v1 linear model uses only `BattleSupportObservation` / `DecisionContext`: Battle number, Battles remaining, final-Battle status, pass number, token holdings, face-up Plants, Hand/dice power, current visible Strike-row status, Wound exposure, opponents already done, action type, and whether the candidate matches Human Baseline's reference choice. Effective Plant and Round effects are represented through stable named features, so Plant and Round research overrides are visible to the learner.

No mutable `Game`, hidden Wisp identities of opponents, hidden Round cards, RNG, or deck internals are exposed.

The Human-reference-match feature is advisory only. The learner may disagree with Human Baseline; Human still supplies lower-level targeting.

## Training

```bash
bin/train_battle_support_policy \
  --grove-pattern random \
  --plant-overrides data/research/4p/resync/resync-current.csv \
  --round-overrides data/research/4p/resync/round-resync-current.csv \
  --players 4 \
  --rounds 3/2/2 \
  --generations 8 \
  --population 10 \
  --games 50 \
  --evolution-seed 53000 \
  --seed 63000 \
  --strategy-seed 73000 \
  --output output/ai/battle-support-policy-v1-trained.weights
```

Only the affected seat's Battle Support policy is learned. The affected seat rotates by sample. Buy, Cultivation Main, normal Battle Main, and all lower-level targets remain Human Baseline.

## Held-out evaluation

```bash
bin/evaluate_battle_support_policy \
  --weights output/ai/battle-support-policy-v1-trained.weights \
  --grove-pattern random \
  --plant-overrides data/research/4p/resync/resync-current.csv \
  --round-overrides data/research/4p/resync/round-resync-current.csv \
  --games 300
```

Control and intervention use matched Grove/mechanical/strategy seeds. Reporting includes Support-action frequencies, Final-Main/proceed frequency, Sunlight opportunity/use rate and funded Main type, immediate/decisive Sunlight Strike contribution metrics already available in `GameSummary`, Battle/total VP, wounds, token economy, and held-out win share.

The evaluator highlights extreme action-use patterns (for example near-always or never-used Supports) as designer-review signals only. They are not labeled exploits without controlled follow-up.

## Persistence

`data/ai/4p/battle-support-policy-v1.weights` is the zero-weight seed. Saved trained files include stable feature names, training provenance, and a Plant-card manifest. Evaluation validates the manifest against the current canonical Plant catalog before use.
