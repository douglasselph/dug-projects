# Data layout

The `data` tree separates player-count-neutral game assets from research artifacts
whose evidence depends on player count.

## Player-count-neutral data

These files describe the game itself and are not learned policies:

- `Cultivation_Cards - Root.csv`
- `Cultivation_Cards - VF.csv`
- `Turn_Cards.csv`
- `Wisp_Cards.csv`
- `icon/`
- `images/`

## Learned AI policies

Learned/frozen policy weights live under the player count on which they were
trained and evaluated:

- `ai/2p/` — reserved for promoted 2-player policies
- `ai/3p/` — reserved for promoted 3-player policies
- `ai/4p/` — current historical/promoted 4-player policies

A policy must not be treated as a player-count-neutral expert policy. Keep the
player count explicit in its path even if a policy later proves portable.

## Research data

Game definitions and experiments that change the game rules or shared card
market are player-count-neutral, regardless of which player count first tested
them:

- `research/resync/resync-current.csv` — current Plant-card research baseline
- `research/resync/round-resync-current.csv` — current Round-card research baseline
- `research/resync/resync-v01.csv` — historical Plant baseline snapshot
- `research/plant-overrides/` — experimental Plant-card variants
- `research/round-overrides/` — experimental Round-card variants
- `research/market-configurations/` — proposed shared-market configurations

The canonical data CSVs above remain separate from these opt-in research
overrides. Scripts must explicitly request research overrides; this refactor
does not promote them to canonical defaults.

Player-count-specific research results and learned policies may remain under
`research/2p/`, `research/3p/`, or `research/4p/` when appropriate. A game-rule
CSV must not be copied between those trees: one shared definition applies to
all supported player counts.

Previously trained policy weight files are deliberately left unchanged,
including provenance fields that mention the historical `4p` baseline path.
Those fields document training history, not a live game-definition path.
