package dugsolutions.leaf.v35.player.decision.reward

import dugsolutions.leaf.v35.tokens.Critter

/**
 * Deterministic baseline reward policy.
 *
 * If both Bee and Worm are legal, choose whichever normal type the player
 * currently owns fewer of. Bee wins ties.
 *
 * Boosted Bees/Worms count as their normal type when comparing holdings.
 * If the normal Bee/Worm pair is not both available, choose the first legal
 * option supplied by gameplay code.
 */
class BaselineRewardStrategy : RewardStrategy {

    override fun chooseCritter(
        request: ChooseCritterRequest
    ): Critter {
        val choices = request.legalChoices

        val hasBee = Critter.BEE in choices
        val hasWorm = Critter.WORM in choices

        if (hasBee && hasWorm) {
            val beeCount = request.ownedCritters.count {
                it.normal == Critter.BEE
            }
            val wormCount = request.ownedCritters.count {
                it.normal == Critter.WORM
            }

            return if (beeCount <= wormCount) {
                Critter.BEE
            } else {
                Critter.WORM
            }
        }

        return choices.first()
    }
}
