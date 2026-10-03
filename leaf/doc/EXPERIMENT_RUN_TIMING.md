# Experiment Run Timing

Multi-run experiment shell scripts use `bin/lib/experiment_run_timing.sh` for
progress timing.

The existing progress message remains:

```text
Run X of Y starting: <intention>
```

Beginning with the second run, the script first prints a timing line such as:

```text
Previous run took 12m 41s; average 11m 58s/run; estimated remaining 1h 47m 42s; ETA Fri Oct 03 01:04 PM
```

The estimate is intentionally simple and transparent:

- each completed non-skipped run contributes its wall-clock duration;
- the arithmetic mean of measured run durations is used for the estimate;
- that mean is multiplied by the number of remaining run slots, including the
  run about to start;
- the resulting duration is added to the current local clock time for the ETA;
- resume-mode entries beginning with `SKIP ` do not enter the timing average.

This is an operational estimate, not a simulation statistic. Training and
held-out evaluation can take different amounts of time, so the ETA generally
gets more useful as more runs finish.
