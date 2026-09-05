package dugsolutions.leaf.v35.player.decision.mechanical.reward

import dugsolutions.leaf.v35.player.decision.reward.*

import dugsolutions.leaf.v35.tokens.Critter

/**
 * Deterministic mechanical control reward policy.
 *
 * If both Bee and Worm are legal, choose whichever type the player currently
 * owns fewer of. Bee wins ties. If both are not available, choose the first
 * legal option supplied by gameplay code.
 */
class MechanicalRewardStrategy : RewardStrategy {

    override fun chooseCritter(
        request: ChooseCritterRequest
    ): Critter {
        val choices = request.legalChoices

        val hasBee = Critter.BEE in choices
        val hasWorm = Critter.WORM in choices

        if (hasBee && hasWorm) {
            val beeCount = request.ownedCritters.count { it == Critter.BEE }
            val wormCount = request.ownedCritters.count { it == Critter.WORM }

            return if (beeCount <= wormCount) {
                Critter.BEE
            } else {
                Critter.WORM
            }
        }

        return choices.first()
    }
}
