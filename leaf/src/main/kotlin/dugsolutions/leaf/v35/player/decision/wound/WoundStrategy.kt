package dugsolutions.leaf.v35.player.decision.wound

import dugsolutions.leaf.v35.player.creature.CreatureCard

/**
 * One legal way to resolve a Wound.
 *
 * Gameplay/Wound resolution computes which choices are legal under the current
 * Flip It or Snip It rule. The strategy only selects one offered choice.
 */
sealed interface WoundChoice {

    val card: CreatureCard

    data class Flip(
        override val card: CreatureCard
    ) : WoundChoice

    data class Snip(
        override val card: CreatureCard
    ) : WoundChoice
}

class ChooseWoundRequest(
    legalChoices: List<WoundChoice>
) {
    val legalChoices: List<WoundChoice> =
        legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Wound decision requires at least one legal choice"
        }
    }
}

interface WoundStrategy {

    fun choose(
        request: ChooseWoundRequest
    ): WoundChoice
}
