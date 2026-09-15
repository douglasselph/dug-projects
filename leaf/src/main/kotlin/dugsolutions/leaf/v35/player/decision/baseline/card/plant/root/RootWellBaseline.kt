package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object RootWellBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_05_04"),
    effect = GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
    cultivationPlayBase = 40,
    battlePlayBase = 65
), BaselineInfluencer {
    private const val USEFUL_WATER_RESERVE = 2

    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> {
        if (DecisionTag.ACQUIRE_WATER !in candidate.tags) return emptyList()

        val missing = (USEFUL_WATER_RESERVE - context.self.board.water).coerceAtLeast(0)
        if (missing == 0) return emptyList()

        return listOf(
            ScoreAdjustment(
                +15 * missing,
                "Root Well benefits from keeping Water available for Battle rerolls"
            )
        )
    }
}
