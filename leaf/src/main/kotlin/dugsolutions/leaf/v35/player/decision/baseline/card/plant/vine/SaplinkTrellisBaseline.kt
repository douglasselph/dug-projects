package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object SaplinkTrellisBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_11_02"),
    effect = GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
    cultivationPlayBase = 65,
    battlePlayBase = 65
), BaselineInfluencer {
    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> = buildList {
        if (DecisionTag.ACQUIRE_ROOT in candidate.tags || DecisionTag.ACQUIRE_VINE in candidate.tags) {
            add(
                ScoreAdjustment(
                    +10,
                    "Saplink Trellis makes additional Roots and Vines more valuable"
                )
            )
        }
    }
}
