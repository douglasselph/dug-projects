package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.player.domain.HandItem

/**
 * Checks if a specific flourish type exists in the player's hand items
 */
class HasFlourishType(
    private val cardManager: CardManager
) {
    operator fun invoke(handItems: List<HandItem>, targetType: FlourishType): Boolean {
        return handItems.any { item ->
            when (item) {
                is HandItem.aCard -> cardManager.getCard(item.card.id)?.type == targetType
                is HandItem.aDie -> false
            }
        }
    }
} 
