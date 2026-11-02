# Human Baseline — Simple Die-Value Plant Decision Specification

Status: **proposed for designer review; not yet approved.**

This document is the Step 11 gate. It does not authorize scorer changes. Each card below should be reviewed as an ordinary-human decision specification before calibration constants or behavior are changed. Shared Creature Refresh and Phase-Preservation logic remains shared; card scorers should describe the immediate opportunity, not reimplement those concepts.

## Shared certification standard

For every card, certify: (1) the engine effect matches the card, (2) Human Baseline selects a sensible target, (3) activation timing is sensible, (4) immediate value is compared with Draw and other legal actions, (5) Creature Refresh Value applies, (6) Phase-Preservation Value applies when Battle use is materially stronger, and (7) targeted observations establish empirical ordering across meaningful states. Buy-threshold gains matter during Cultivation when an immediate die improvement crosses an available purchase tier.

## Root_05_01 — Root Double Down

**Mechanical behavior:** raise one die to twice its showing, capped by that die's maximum.

**Proposed ordinary-human behavior:** prefer the die with the largest actual gain, not simply the largest current die. A high-sided die with room to double should dominate a capped or already-high die. During Cultivation, crossing a Buy tier should increase activation value. Compare the best actual doubling gain against ordinary Draw. Shared Refresh applies. Preserve for imminent Battle when its expected tactical value there is materially better and Refresh will not restore it.

**Empirical ordering to test:** larger uncapped gain > smaller/capped gain; Buy-tier crossing > same raw gain without crossing; useful refresh can reverse a close Draw comparison; Battle-next preservation can reverse a close Cultivation use.

## Root_05_02 — Root Four More

**Mechanical behavior:** raise one die by up to +4, capped at its maximum.

**Approved earlier direction to retain:** value actual +4 gain, Buy-threshold consequences and relative value versus Draw. Shared Refresh makes use more attractive, including weaker two-action refresh anticipation. Battle-next preservation reduces Cultivation use when Refresh is not imminent. Step 6 provenance measures the affected die's later Battle contribution rather than crediting the whole die to the card.

**Empirical ordering to test:** actual +4 > capped smaller gain; tier-crossing > no crossing; immediate refresh > no refresh; Battle-next/no-refresh suppresses use; Battle-next/refresh removes much of that suppression.

## Root_05_03 — Root on a Roll

**Mechanical behavior:** reroll one die until it shows at least 3; roll rewards are ignored.

**Proposed ordinary-human behavior:** primarily rescue dice showing 1 or 2. Prefer the target with the stronger expected improvement, with die size relevant because the post-condition distribution differs by die. A die already showing 3+ normally provides no useful immediate reason to activate this effect. During Cultivation, account for Buy-tier consequences of the expected improvement without pretending the reroll result is known. Shared Refresh can justify otherwise marginal use. Preserve before Battle only to the extent the card has a credible low-die rescue opportunity there; do not preserve merely because Battle is next.

**Empirical ordering to test:** 1 generally > 2 as an opportunity on comparable dice; larger expected gain > smaller; no 1/2 target should usually lose to Draw unless Refresh value is substantial.

## Root_09_02 — Root Cause

**Mechanical behavior:** flip one owned die to its opposite face.

**Approved earlier direction to retain:** score actual flip gain. D20=1 is a very large immediate opportunity. Select the die with the best positive flip gain and account for Buy-tier crossings. Compare with Draw. Shared Refresh applies. Because Battle use can be especially powerful, Battle-next preservation should be meaningful when Refresh is not expected.

**Empirical ordering to test:** D20=1 near the top of immediate opportunities; positive large flip gain > small gain; harmful/zero flips should not be preferred; preservation can suppress merely moderate Cultivation opportunities but should not blindly suppress exceptional immediate value.

## Root_09_03 — Root Kindred

**Mechanical behavior:** set one die to match another die's showing, limited by the target die's own maximum.

**Proposed ordinary-human behavior:** evaluate source-target pairs by actual target gain. Prefer a low target that can copy a high visible value; never treat the source's value as fully obtainable when the target's sides cap it. During Cultivation, reward Buy-tier crossings. Compare best pair gain with Draw. Shared Refresh applies. Battle-next preservation should depend on whether the visible Cultivation pair is weaker than the plausible tactical Battle value, not on a blanket card-specific bonus.

**Empirical ordering to test:** larger realizable pair gain > smaller; capped copy valued at realized value; tier crossing matters; poor pair availability should make Draw competitive.

## Vine_09_01 — Low & Behold

**Mechanical behavior:** choose a die tied for your lowest showing and set it to that die's maximum.

**Proposed ordinary-human behavior:** value the actual gain from lowest showing to that die's maximum. When several dice share the lowest showing, prefer the eligible die with the greatest gain (usually the higher-sided die). Do not value an attractive non-lowest die because it is mechanically ineligible. During Cultivation, include Buy-tier consequences. Shared Refresh applies. Preserve for Battle only when its likely tactical use exceeds the current opportunity and Refresh will not restore it.

**Empirical ordering to test:** among equally-low dice, higher actual max gain > lower; low D20/D12 states should be very attractive; weak gain should allow Draw to win.

## Vine_09_03 — Rise and Vine

**Mechanical behavior:** raise all owned dice by +2, each capped at its maximum.

**Proposed ordinary-human behavior:** value the sum of actual gains across all eligible dice, so both hand size and headroom matter. Do not use a naive `2 × dice count` when dice are near maximum. During Cultivation, Buy-tier consequences should consider the resulting total purchasing power. Shared Refresh applies. Battle-next preservation should become stronger when several dice are likely to benefit tactically in Battle, but immediate large multi-die Cultivation value can still justify use.

**Empirical ordering to test:** more total headroom > less; several useful dice > one useful die; capped dice contribute little/zero; Buy-tier crossing and Refresh can change close choices.

## Vine_11_02 — Saplink Trellis

**Mechanical behavior:** raise one die by +1 for each grafted Root or Vine, capped by the die maximum.

**Proposed ordinary-human behavior:** first compute the current Root/Vine count, then value the best *actual* realizable die gain after capping. A large Creature count should not create fictitious value if every die is near maximum. During Cultivation, account for Buy-tier crossing. Shared Refresh applies. Phase preservation should reflect the same current scaling plus Battle tactical opportunity rather than a separate bespoke rule.

**Empirical ordering to test:** more Root/Vine grafts increase value only when target headroom exists; high-headroom target > capped target; tier crossing matters.

## Vine_11_04 — Vine's the Limit

**Mechanical behavior:** set one die up to D12 to its maximum; D20 is not an eligible target.

**Proposed ordinary-human behavior:** among eligible D4–D12 dice, prefer the largest actual gain to maximum. Never score a D20 as a target. A low D12 should be an especially strong opportunity. During Cultivation, account for Buy-tier crossings and compare with Draw. Shared Refresh applies. Battle-next preservation is appropriate when current gain is modest and a stronger tactical opportunity is plausible; exceptional current gain may justify immediate use.

**Empirical ordering to test:** low D12/D10 with large headroom > smaller gain; D20 excluded; tier crossing increases value; Refresh/preservation can resolve close choices.

## Shared-logic promotion candidates

The review already suggests three reusable concepts rather than nine bespoke implementations: **actual realizable die gain after caps/eligibility**, **Buy-threshold value from the realized post-effect Hand**, and **expected-value handling for stochastic transforms** such as Root on a Roll. Creature Refresh and Phase-Preservation are already shared and should remain so.

No scorer constants or behavior for these Step 11 cards should be changed until this specification is reviewed and approved.
