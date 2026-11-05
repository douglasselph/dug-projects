package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.game.round.battle.ThreeEngagementRoundExecutor

/** Historical simulation convenience entry point; the executor now lives in main. */
fun configuredSimulationRoundCoordinator(
    cultivation: RoundExecutor,
    legacyBattle: BattleRound
): RoundCoordinator = RoundCoordinator(
    cultivation = cultivation,
    battle = legacyBattle,
    threeEngagements = ThreeEngagementRoundExecutor(legacyBattle)
)

/** Compatibility wrapper for existing simulation launchers. */
fun configuredSimulationGameRunner(
    cultivation: RoundExecutor,
    legacyBattle: BattleRound
): GameRunner = GameRunner(configuredSimulationRoundCoordinator(cultivation, legacyBattle))
