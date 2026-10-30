# Candidate B / Checkpoint 3: locked-Strike reroll target repair

## Failure reproduced from reported stack trace

Mechanical Control, 2 players, Battle 2, sample 37 of the standard 100-sample
run, mechanical seed `2028340345`, strategy seed `2028341585`, Wisp seed
`2028342937`.

`DrawEffectHandler.execute` for `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS`
(Root on a Roll) selected its target from the actor's *entire* Hand. In a
row-by-row Battle the Hand continues to hold committed dice from already
resolved rows. The shared Die's new locked-row guard then correctly rejected
`RollResolver.roll` on the chosen die.

## Correction

Use `battleHandChoices(request)` for effect targets during Battle, and normal
`handChoices(actor)` in Cultivation. The existing Battle helper already limits
eligible dice to the actor's live dice placed in **open** Strike rows.

The same shared filter is used for:

- Root on a Roll: repeat rerolls until 3+, no Roll Rewards;
- Root Recall's Battle reroll branches;
- Bursting Blossom's target to Raise +1 before drawing;
- current Reap What You Roll's eligibility to discard a Battle die.

Root on a Roll and Root Recall are no longer offered as legal Battle actions
when the player has no dice in an unresolved Strike. Canonical v37.03 does not
lock any row before all Battle actions conclude, so its ordinary legal targets
remain unchanged. A die in a locked Strike must **never** be unlocked or
mutated to let an effect succeed.

## Verification

A03 protected manifest reviewed for a single change to DrawEffectHandler;
A03 guard passes with zero unexpected changes; 19 Python A03 tests pass.

New JUnit tests cover Root on a Roll selecting only an open die (including a
strategy that would prefer the locked die), the no-open-dice legal-action gate,
Root Recall, and Bursting Blossom.

The full Kotlin test suite and end-to-end sample have **not** been executed in
the local offline environment because required Gradle dependencies are absent.

### Replay the precise failed sample (only one paired Battle)

The batch runner starts samples from index zero. For the original sample index
37, the following base seeds have already been shifted so its index-zero seeds
match that exact failing sample:

```bash
bash doc/run_battle_pacing_ab.sh \
  --samples 1 --players 2 --battles 2 --policy mechanical \
  --seed 2026140333 --strategy-seed 2026141481 --wisp-seed 2026142777 \
  --out output/experiments/battle-pacing/replay-sample37-fixed
```

Output directories are create-new; use a new path for every rerun. The
original incomplete `mechanical-100` is **not** an accepted result.
