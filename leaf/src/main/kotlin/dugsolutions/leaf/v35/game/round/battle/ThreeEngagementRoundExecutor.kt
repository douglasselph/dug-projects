package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.simulation.v35.experiment.battlepacing.HumanGuidedEngagementPolicy
import dugsolutions.leaf.simulation.v35.experiment.battlepacing.ThreeEngagementBattle
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Adopted v38 Battle executor, available with only the main source set on the classpath. */
class ThreeEngagementRoundExecutor(private val battleRound: BattleRound) : RoundExecutor {
    override fun execute(game: Game, roundCard: RoundCard) {
        battleRound.executeOpeningDraw(game)
        val placement = battleRound.executeRankAndPlace(game)
        ThreeEngagementBattle(battleRound).execute(
            game, placement.battleState, HumanGuidedEngagementPolicy(game)
        )
    }
}
