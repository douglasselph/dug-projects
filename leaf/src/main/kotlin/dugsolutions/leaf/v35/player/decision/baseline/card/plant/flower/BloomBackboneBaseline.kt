package dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object BloomBackboneBaseline : ConfiguredCardScorer(
    cardNames = setOf("Flower_11_02"),
    effect = GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
    cultivationPlayBase = 70,
    battlePlayBase = 70
), BaselineInfluencer {
    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> = buildList {
        if (DecisionTag.ACQUIRE_VINE in candidate.tags || DecisionTag.ACQUIRE_FLOWER in candidate.tags) {
            add(
                ScoreAdjustment(
                    +10,
                    "Bloom Backbone makes additional Vines and Flowers more valuable"
                )
            )
        }
    }
}
