# Human Baseline — Gaining Sunlight

`GAIN_SUNLIGHT_TOKEN` asks a Cultivation player to spend one of the two normal
Build Main Actions now in order to bank one extra Main Action that can later be
used as a Battle Support action.

The Human Baseline treats this as a prospective capacity decision, not as an
exact expected-value calculation.  Its priority remains on the same scale as
Draw, Plant activation, and the other Round effect, so the existing action
chooser still decides which Main Action wins the current opportunity cost.

## Visible information used

The scorer uses only ordinary public information already present in
`DecisionContext`:

- whether any Battle remains;
- how many Battles remain;
- how many public round positions away the next Battle is;
- how many Sunlight tokens the player already holds;
- the calibrated Battle-role/base values of the player's currently face-up
  Plants.

When Battle is next, the scorer looks specifically for a *third* face-up Plant
with meaningful Battle value.  Two normal Main Actions already exist, so a
third attractive Battle Plant is a shallow, visible indication that extra Main
Action capacity could unlock an action the ordinary budget cannot comfortably
cover.  This is intentionally a role/base observation: it does not predict
future Strike rows or targets.

## Components

The current calibration is deliberately interpretable:

- base future-action value: `42`;
- future Battles: `+4` per remaining Battle, capped at `+12`;
- Battle next: `+8`;
- Battle one round away: `+4`;
- existing Sunlight: `-12` per token;
- if stored Sunlight already covers the number of remaining Battles: an
  additional `-8` diminishing-return adjustment;
- when Battle is next, visible value from a third face-up Battle Plant can add
  up to `+12` based on its calibrated Battle base.

If no Battle remains, the final priority is `0`.

The numbers are Human Baseline calibration values, not claims about Sunlight's
objective game balance.

## Intended behavior

Representative cases are:

- no future Battle -> decline Sunlight;
- Battle next, no Sunlight, several strong face-up Battle Plants -> strong
  reason to bank Sunlight;
- several Sunlights already banked for one remaining Battle -> very low
  marginal value;
- Battle distant with no obvious extra Plant action -> modest prospective
  value rather than an automatic choice;
- excellent immediate Plant/Draw opportunity -> that action can outrank
  Sunlight because the existing Main-action chooser compares the scores;
- weak immediate alternative plus imminent Battle -> Sunlight can win;
- several Battles remain and no Sunlight is held -> Sunlight retains useful
  general option value.

## Information intentionally not used

The scorer does **not**:

- simulate unknown future rolls;
- predict exact future Strike totals;
- search a future game tree;
- assume a future Plant purchase;
- inspect hidden information;
- optimize itself against final win rate.

This makes the scorer an ordinary competent-human estimate of future action
capacity.  Learned Cultivation Main policies can later test whether stronger
strategy systematically disagrees with this estimate.
