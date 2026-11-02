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

## Python research experiments

Python research runners use `leaf_experiments.timing` rather than duplicating
timing code in each experiment.

The shared Python timing layer provides two levels of feedback:

1. **Experiment-run timing** — every train/evaluate step immediately prints
   `Run X of Y`, the local clock, measured run duration, elapsed time,
   estimated remaining work, and an estimated finish time for the **entire
   experiment**. Resume skips are excluded from both the timing averages and
   the remaining-work estimate. Timing samples are tracked by operation and
   player count (for example `eval-4p`), with the broader operation type used
   only as a fallback when no exact timing sample exists yet.
2. **Silent-process heartbeat** — if a Gradle/Kotlin child process produces no
   output, the parent still prints a `PROCESS_PROGRESS` heartbeat every 60
   seconds with that process's elapsed wall-clock time. This prevents a healthy
   but quiet process from appearing hung.

Example:

```text
Run 3 of 30 starting: Train 2p learner 2/5
Clock: 2026-10-08 12:18:03 PM
Estimated remaining: 02:11:42; ETA Thu Oct 08 02:29 PM
...
PROCESS_PROGRESS command=train_buy_policy elapsed=00:01:00 clock=12:19:03 PM (child process still running)
...
PROCESS_COMPLETE command=train_buy_policy elapsed=00:07:41 exit=0
RUN_COMPLETE 3/30 took 00:07:41; elapsed=00:20:35
OVERALL_PROGRESS runs=3/30 elapsed=00:20:35 remainingWork=19; estRemaining=02:04:17; estimatedExperimentFinish=Thu Oct 08 02:32 PM
```

Output is explicitly flushed/line-buffered so it remains live when the user
runs an experiment through `|& tee ...`.

The heartbeat interval is operational only and cannot affect experiment
results. It defaults to 60 seconds and can be changed for a run with:

```bash
LEAF_PROGRESS_HEARTBEAT_SECONDS=30 bin/research_python -m ...
```

`remainingWork` counts only operations that were unfinished when the run began.
A train/evaluate step that already has a valid resume marker does not consume
ETA budget merely because it still appears in the `runs=X/Y` bookkeeping. This
prevents many instant resume skips from making the overall ETA unrealistically
optimistic.

## Mandatory orchestration timing for new research experiments

New Python research orchestrators should define their long-running operations as
`ExperimentStep` values and execute them through
`leaf_experiments.timing.ExperimentPlanRunner`.

This is the highest common layer that can correctly own experiment ETA.  The
single-game engine is too low-level: it knows how to run one game, but it does
not know how many training/evaluation operations remain or which categories
have different runtimes.  Conversely, individual experiment scripts are too
high-level if they hand-roll their own timers, because timing can be forgotten.

`ExperimentPlanRunner` therefore makes timing part of orchestration itself:

- every declared long-running step gets `Run X of Y`, elapsed time and ETA;
- training/evaluation categories retain separate duration estimates;
- resume skips are excluded from ETA work;
- child-process heartbeat remains supplied by `run_streaming_process`;
- experiment scripts should not implement their own timing loops.

Legacy Bash experiments may still use `bin/lib/experiment_run_timing.sh`; they
have not all been migrated.  New research experiments should use the Python
plan runner so timing is not optional.
