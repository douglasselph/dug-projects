package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Simulation-layer bridge for the adopted v38 scheduler; main source stays policy-agnostic. */
class ThreeEngagementRoundExecutor(private val legacyBattle: BattleRound) : RoundExecutor {
    override fun execute(game: Game, roundCard: RoundCard) {
        legacyBattle.executeOpeningDraw(game)
        val rank = legacyBattle.executeRankAndPlace(game)
        ThreeEngagementBattle(legacyBattle).execute(
            game, rank.battleState, HumanGuidedEngagementPolicy(game)
        )
    }
}

/** Use this in a normal simulation's coordinator when selecting THREE_ENGAGEMENTS. */
fun configuredSimulationRoundCoordinator(
    cultivation: RoundExecutor,
    legacyBattle: BattleRound
): RoundCoordinator = RoundCoordinator(
    cultivation = cultivation,
    battle = legacyBattle,
    threeEngagements = ThreeEngagementRoundExecutor(legacyBattle)
)

/** Construct a full normal-game runner using the selectable simulation Battle engine. */
fun configuredSimulationGameRunner(
    cultivation: RoundExecutor,
    legacyBattle: BattleRound
): dugsolutions.leaf.v35.game.GameRunner = dugsolutions.leaf.v35.game.GameRunner(
    configuredSimulationRoundCoordinator(cultivation, legacyBattle)
)

/** Discovered automatically by the production coordinator when simulation is on the classpath. */
class SimulationBattleStructureProvider : dugsolutions.leaf.v35.game.round.BattleStructureProvider {
    override fun create(legacyBattle: RoundExecutor): RoundExecutor =
        ThreeEngagementRoundExecutor(legacyBattle as BattleRound)
}
