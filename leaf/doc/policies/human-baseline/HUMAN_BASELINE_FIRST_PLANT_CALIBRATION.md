# Human Baseline — First Plant Calibration Set

This checkpoint calibrates Root Four More (`Root_05_02`), Root Cause (`Root_09_02`), Forget-Me-Not (`Flower_17_02`), and Queen's Blossom (`Flower_17_04`). Numeric values are provisional calibration constants, not designer-approved final values.

## Shared timing logic

Plant activation now applies shared Creature Refresh Value and Phase-Preservation Value. An activation that consumes the last face-up Plant receives a refresh benefit based on the cards restored; a two-face-up-card state receives a smaller anticipatory benefit. When Battle is next, a Plant also receives a preservation penalty. Immediate refresh cancels that penalty and a two-action refresh opportunity reduces it. The size of preservation pressure uses the card's Battle-versus-Cultivation base premium, so high-Battle-value cards can be preserved more strongly without duplicating timing code in each scorer.

## Calibration set

- Root Four More retains immediate +4 and Buy-tier valuation. It has a modest Battle premium and participates in shared refresh/preservation. Step 6 provenance remains responsible for attributing its immediate Battle effect to the affected die and Strike contribution.
- Root Cause remains the positive control for immediate opposite-face gain; e.g. a D20 showing 1 exposes a +19 gain through the existing die-value heuristic. It has a larger Battle premium and therefore stronger preservation before Battle unless refresh offsets it.
- Forget-Me-Not now values both die quality and visible future availability. D20/D12 are strongly attractive, D10/D8 remain meaningful, and natural recycle distance plus imminent Battle can increase the value of recovery. Target scoring uses the same visible recycle-distance model. Step 6 provenance measures actual acceleration and later Battle contribution.
- Queen's Blossom retains its strong two-die immediate value and receives the largest Battle premium in this first set. Shared preservation therefore makes it easier for a useful lower-value Plant to be consumed while Queen's Blossom is saved for imminent Battle; refresh reduces/cancels that pressure.

No general game-tree search or card-specific long-range planner was added.
