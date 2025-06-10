package dugsolutions.leaf.player.domain

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard

/**
 * Represents an effect that can be applied to a player.
 * These effects are the result of processing a CardEffect and represent
 * concrete actions that will modify the player's state.
 */
sealed class AppliedEffect {

    data object FlourishOverride : AppliedEffect()

    data class MarketBenefit(
        val type: FlourishType? = null,
        val costReduction: Int = 0,
        val isFree: Boolean = false,
    ) : AppliedEffect() {

        override fun toString(): String {
            return if (isFree) {
                "$type-FREE"
            } else "$type-$costReduction"
        }
    }

    data class TrashIfNeeded(
        val card: GameCard
    ) : AppliedEffect()

} 
