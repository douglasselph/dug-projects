# Battle structure selection (simulation)

Plant-card overrides remain in `data/research/resync/resync-current.csv`. The game-level
Battle structure belongs in `data/research/resync/game-rules-current.properties`:

```
battle.structure=CANONICAL
# or THREE_ENGAGEMENTS
```

New `GameConfig` instances read the properties file by default. The production
round coordinator locates the Candidate B adapter when the simulation source set is
on the runtime classpath. If the simulation source set is missing, selecting B fails
clearly rather than silently falling back to A. Explicitly supplied `battleStructure`
in `GameConfig` overrides the file for one game. A setting change affects new games,
not already-created games. Production-only executables cannot use Candidate B;
select CANONICAL there. The experiment runner still explicitly executes A and B.

The A/B comparison deliberately invokes `BattleRound` and `ThreeEngagementBattle`
explicitly; it does not consult this properties file and remains independent.

No rule changes, market resync edits, or research-control changes are made here.
