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

Research baselines, overrides, and historical market configurations likewise
live under the player-count stream that produced/validated them:

- `research/2p/` — reserved for 2-player research artifacts
- `research/3p/` — reserved for 3-player research artifacts
- `research/4p/` — existing historical research artifacts

The current accepted working Plant and Round research baselines therefore live
at:

- `research/4p/resync/resync-current.csv`
- `research/4p/resync/round-resync-current.csv`

This directory organization records provenance. It does **not** imply that a
4-player-derived game rule is intended only for four-player games. Cross-player
validation can later justify copying/promoting an equivalent baseline into a
2p/3p research stream or replacing this organization with a deliberately shared
validated baseline.
