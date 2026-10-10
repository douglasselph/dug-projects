# Flower 14/17 Coordinated Expert-Context Overnight Reconfirmation

Purpose: determine whether the near-dead expert demand for 2p Flower 17 and 4p Flower 14 survives when the important surrounding strategic systems are allowed to adapt together.

Adaptive families: Buy, Cultivation Main, Plant Effect/Targeting, Battle Support. Battle Main remains Human Baseline because its current learner is not yet trusted. Other policy families remain Human Baseline.

Method: iterative best response, not a Cartesian product of populations. Each replicate begins from a different certified Buy learner. Cycle 1 trains Cultivation Main from zero around that Buy learner, Plant Effect from zero around Buy+Cultivation, Battle Support from zero around Buy+Cultivation+Plant, then trains a fresh Buy policy from zero around all three learned companions. Later cycles warm-start each family and retrain it against the latest other three policies in the order Cultivation -> Plant -> Battle Support -> Buy.

Defaults are deliberately overnight-sized: 2p and 4p, 3 independent replicates, 3 coordination cycles, 8 generations, population 10, 60 games/policy. Intermediate cycles get 1000 held-out matched samples; final cycles get 4000. All long operations use the shared ExperimentPlanRunner timing/ETA layer and the experiment is resume-safe.

Primary evidence is Flower selection when legal, tracked by exact card and tier across cycles and independent replicates. If the problematic tiers remain near zero across final replicates, confidence rises sharply that the effect is not a single Human-policy artifact. If they revive materially and repeatedly, the earlier Buy-only result was context-dependent.
