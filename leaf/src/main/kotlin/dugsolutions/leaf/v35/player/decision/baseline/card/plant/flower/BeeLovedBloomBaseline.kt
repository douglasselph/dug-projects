package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter

object BeeLovedBloomBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_14_01"),
    effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
    cultivationPlayBase = 68,
    battlePlayBase = 68
), BaselineInfluencer {
    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> = buildList {
        if (DecisionTag.ACQUIRE_BEE in candidate.tags) {
            val existingBeeBonus = minOf(10, context.self.board.bees * 3)
            add(
                ScoreAdjustment(
                    +30 + existingBeeBonus,
                    "Bee-loved Bloom makes additional Bees more valuable"
                )
            )
        }

        if (
            DecisionTag.SPEND_BEE in candidate.tags &&
            context.self.board.beeValue <= Critter.BEE.baseValue
        ) {
            add(
                ScoreAdjustment(
                    -20,
                    "Preserve an unboosted Bee for Bee-loved Bloom"
                )
            )
        }
    }
}
