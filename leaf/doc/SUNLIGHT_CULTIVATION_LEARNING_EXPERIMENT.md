# Learned Cultivation-Main Sunlight Experiment

`bin/experiment_sunlight_cultivation_learning` tests whether independently
trained Cultivation Main policies discover `GAIN_SUNLIGHT_TOKEN` as a healthy
contextual action or as a near-mandatory rule.

The experiment deliberately isolates one learned decision family:

- Buy: Human Baseline
- Cultivation Main: learned for the affected rotating role
- Battle Main: Human Baseline
- Battle Support: Human Baseline
- all lower-level effect targets: Human Baseline

It explicitly supplies both:

- `data/research/resync/resync-current.csv`
- `data/research/round-overrides/sunlight-token.csv`

No core runner silently loads either research configuration.

## Default screen

The default batch trains three independent learners with the same random-Grove
schedule, different evolution/mechanical/strategy training seeds, and then
evaluates all three on exactly the same held-out Grove/mechanical/strategy
cohort. The default held-out size is 300 matched samples per learner.

Every evaluation contains its own matched Human Cultivation Main control.

## Mandatory-action check

For Sunlight opportunities, the held-out evaluator reports take rates:

- overall;
- when Sunlight is already held;
- when Battle is next;
- when Battle is not next;
- by Battles remaining;
- when the certified Human Baseline preferred another legal Main Action.

The last category is an operational **strong competing-action proxy**. It does
not claim that the Human preference is optimal. It asks whether the learned
policy still takes Sunlight in states where the calibrated Human Baseline would
rather spend the same Main Action on something else.

Final VP and win-share values attached to opportunity buckets are descriptive
associations, not causal estimates.

The batch creates an experiment-level `mandatory-action-summary.txt`. Its
review flag requires all independent learners to reach a near-universal overall
take rate, a material mean held-out advantage, and persistent high take rates
in both Battle-distant and Human-preferred-competitor contexts. The thresholds
are screening conventions, not game-balance rules, and a flag is never an
automatic conclusion that Sunlight is overpowered.

## Run

```bash
bin/experiment_sunlight_cultivation_learning
```

The script prints `Run X of Y starting: ...` before every training/evaluation
run and produces one `sunlight-cultivation-learning-results.tar.gz` containing
weights, manifests, exact research configurations, logs, and the compact
mandatory-action summary.
