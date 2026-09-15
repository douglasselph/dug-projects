package dugsolutions.leaf.v35.player.decision.baseline.card.plant.root

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter

object RootAppreciationBaseline : ConfiguredCardScorer(
    cardNames = setOf("Root_07_02"),
    effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
    cultivationPlayBase = 60,
    battlePlayBase = 60
), BaselineInfluencer {
    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> = buildList {
        if (DecisionTag.ACQUIRE_WORM in candidate.tags) {
            add(
                ScoreAdjustment(
                    +35,
                    "Root Appreciation makes Worm acquisition more valuable"
                )
            )
        }

        if (
            DecisionTag.SPEND_WORM in candidate.tags &&
            context.self.board.wormValue <= Critter.WORM.baseValue
        ) {
            add(
                ScoreAdjustment(
                    -20,
                    "Preserve an unboosted Worm for Root Appreciation"
                )
            )
        }
    }
}
