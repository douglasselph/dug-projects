Step 10 + Step 11 gate

Step 10 implements optional targeted counterfactual mode for Root_05_02 and Flower_17_02 only.
Step 11 adds the proposed ordinary-human decision specification for the simple die-value Plant set. It intentionally does NOT change those card scorers yet; designer approval is the gate before behavior changes.

Suggested checks:
  ./gradlew compileSimulationKotlin --no-daemon
  bin/targeted-decision-calibration --target=Root_05_02 --games=20 --counterfactual=true
  bin/targeted-decision-calibration --target=Flower_17_02 --games=20 --counterfactual=true
