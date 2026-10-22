# Shared research inputs

The `resync/`, `plant-overrides/`, `round-overrides/`, and
`market-configurations/` trees describe one shared game, not one set per
player count. Research runs for 2, 3, or 4 players use these same inputs.

The `2p/`, `3p/`, and `4p/` subdirectories are reserved for legitimately
player-count-specific outputs and research artifacts. Frozen learned AI
policies remain under `data/ai/<count>p/`.

The shared `resync-current.csv` and `round-resync-current.csv` files are
opt-in research baselines; they are not silently applied by the core engine.
