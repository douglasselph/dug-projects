# Expert-vs-expert original Compost intervention

Install this patch at repository root. Use the *same* learned Cultivation Main and Buy weights for **every seat**. Strong policies should have been trained for the original-rule environment; using weights trained on immediate-use Compost invalidates the expert inference.

The experiment uses two players by default and the 3/2/2 round schedule. Each starting seed runs an all-expert control and branches which force the affected expert to Compost 1, 2, ... N times. A force occurs only when original Compost is legal and the expert's natural choice is something else. If fewer than N such opportunities arise, `actual_forced` will be less; the headline summary excludes those incomplete-count rows.

## Commands

```bash
./gradlew compileSimulationKotlin
MAIN_WEIGHTS=path/to/original-rule-main.weights BUY_WEIGHTS=path/to/buy.weights PREFLIGHT_ONLY=1 bin/experiment_expert_forced_compost
GAMES=10 MAX_FORCED=2 CHRONICLES=3 MAIN_WEIGHTS=path/to/original-rule-main.weights BUY_WEIGHTS=path/to/buy.weights OUTPUT_ROOT=output/experiments/expert-forced-compost-smoke bin/experiment_expert_forced_compost
nohup env GAMES=1000 MAX_FORCED=3 CHRONICLES=36 MAIN_WEIGHTS=path/to/original-rule-main.weights BUY_WEIGHTS=path/to/buy.weights bin/experiment_expert_forced_compost > expert-compost.log 2>&1 &
```

Use `PLAYERS=4` for a four-player extension. `DELAY_SECONDS` postpones execution. `SEED` changes held-out seeds. The launcher writes an original Compost override CSV; it does not modify canonical card data.

The final archive is `output/experiments/expert-forced-compost/expert-forced-compost-results.tar.gz`. Upload it for analysis. `pairs.tsv` reports forced-minus-control VP, Battle VP, Plant VP, win share, D4–D20 inventory, and the number of interventions actually applied. `cases/` holds full paired Chronicles and decision ledgers for narrative reconstruction.

## Scope and limitations

All players use the **same expert Main and Buy policies**. Other decisions remain identical baseline subpolicies, which is symmetric but not a fully optimized all-module expert. A policy trained primarily against humans may play differently against another copy of itself; this experiment measures that matchup and should be described as such. The affected seat rotates across games. The no-force control and each forced branch start with identical seeds. Branching can shift subsequent RNG consumption, so a single large VP swing is not proof of Compost's causal influence. Aggregate effects should include uncertainty estimates and, ideally, multiple independently trained policies.

The compulsory choices are opportunistic: the first N legal chances the expert would decline Compost, not necessarily predetermined rounds. For 2 or 3 interventions the later forced opportunities can change due to earlier intervention. `control_compost_selected` records natural expert uses (if any). No separate human policy appears in this experiment.

Compilation was not completed in the sandbox due to time limit. Run compile and a small smoke test before your overnight batch.
