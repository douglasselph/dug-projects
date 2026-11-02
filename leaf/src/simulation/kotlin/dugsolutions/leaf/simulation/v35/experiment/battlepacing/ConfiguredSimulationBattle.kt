package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Simulation-only bridge: production source never depends on experimental policy classes. */
class ThreeEngagementRoundExecutor(private val canonical: BattleRound) : RoundExecutor {
    override fun execute(game: Game, roundCard: RoundCard) {
        canonical.executeOpeningDraw(game)
        val rank = canonical.executeRankAndPlace(game)
        ThreeEngagementBattle(canonical).execute(
            game, rank.battleState, HumanGuidedEngagementPolicy(game)
        )
    }
}

/** Use this in a normal simulation's coordinator when selecting THREE_ENGAGEMENTS. */
fun configuredSimulationRoundCoordinator(
    cultivation: RoundExecutor,
    canonicalBattle: BattleRound
): RoundCoordinator = RoundCoordinator(
    cultivation = cultivation,
    battle = canonicalBattle,
    threeEngagements = ThreeEngagementRoundExecutor(canonicalBattle)
)

/** Construct a full normal-game runner using the selectable simulation Battle engine. */
fun configuredSimulationGameRunner(
    cultivation: RoundExecutor,
    canonicalBattle: BattleRound
): dugsolutions.leaf.v35.game.GameRunner = dugsolutions.leaf.v35.game.GameRunner(
    configuredSimulationRoundCoordinator(cultivation, canonicalBattle)
)

/** Discovered automatically by the production coordinator when simulation is on the classpath. */
class SimulationBattleStructureProvider : dugsolutions.leaf.v35.game.round.BattleStructureProvider {
    override fun create(canonical: RoundExecutor): RoundExecutor =
        ThreeEngagementRoundExecutor(canonical as BattleRound)
}
