package dugsolutions.leaf.v35.player.decision.reward

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Immutable information needed to choose a Critter reward.
 *
 * Gameplay code is responsible for supplying only legal reward choices.
 * The strategy chooses; it does not mutate Player or Grove state.
 */
class ChooseCritterRequest(
    legalChoices: List<Critter>,
    ownedCritters: List<Critter>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalChoices: List<Critter> = legalChoices.toList()
    val ownedCritters: List<Critter> = ownedCritters.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Critter decision requires at least one legal choice"
        }
    }
}

interface RewardStrategy {

    fun chooseCritter(
        request: ChooseCritterRequest
    ): Critter
}
