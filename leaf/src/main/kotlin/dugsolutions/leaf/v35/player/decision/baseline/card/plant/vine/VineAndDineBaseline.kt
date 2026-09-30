package dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.ConfiguredCardScorer
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

object VineAndDineBaseline : ConfiguredCardScorer(
    cardNames = setOf("Vine_09_04"),
    effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
    cultivationPlayBase = 45,
    battlePlayBase = 45
), BaselineInfluencer {
    override val influencers: List<BaselineInfluencer>
        get() = listOf(this)

    override fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> = buildList {
        val availableCritters = context.self.board.bees + context.self.board.worms
        val acquiresUsableCritter =
            DecisionTag.ACQUIRE_BEE in candidate.tags || DecisionTag.ACQUIRE_WORM in candidate.tags
        val spendsUsableCritter =
            DecisionTag.SPEND_BEE in candidate.tags || DecisionTag.SPEND_WORM in candidate.tags
        val spendsForThisCard = DecisionTag.SPEND_CRITTER_FOR_VINE_AND_DINE in candidate.tags

        if (availableCritters == 0 && acquiresUsableCritter) {
            add(
                ScoreAdjustment(
                    +15,
                    "Vine and Dine needs a Bee or Worm available"
                )
            )
        }

        if (availableCritters <= 1 && spendsUsableCritter && !spendsForThisCard) {
            add(
                ScoreAdjustment(
                    -25,
                    "Preserve the last Bee or Worm for Vine and Dine"
                )
            )
        }
    }
}
