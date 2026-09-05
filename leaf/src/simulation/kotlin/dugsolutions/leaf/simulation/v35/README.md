# v35 Simulation Layer

The v35 decision/simulation architecture has distinct purposes:

1. **Mechanical Control (Level 0, core)** — deterministic, legal, deliberately
   naive behavior for engine tests and a bottom benchmark.
2. **Human Baseline (Level 1, core)** — the canonical simulation control group,
   intended to model simple, reasonable experienced-human behavior. Its
   separate director/strategy types are established; priority/context scoring
   is implemented in the next stages.
3. **Tactical (Level 2, simulation)** — short-horizon improved play.
4. **Strategic (Level 3, simulation)** — broader plans/opponent-aware play.
5. **Learned/Adaptive (Level 4, simulation)** — trained/persisted behavior.

Integration-test scripted strategies remain under `src/integration` and should
normally fall back to **Mechanical Control**, never Human Baseline. This keeps
engine tests deterministic as Human Baseline evolves.
