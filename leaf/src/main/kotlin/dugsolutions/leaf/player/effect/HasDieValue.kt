package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.HandItem

/**
 * Checks if a specific die value exists in the player's hand items
 */
class HasDieValue {
    operator fun invoke(handItems: List<HandItem>, targetValue: Int): Boolean {
        return handItems.any { item ->
            when (item) {
                is HandItem.Card -> false
                is HandItem.Dice -> item.die.value == targetValue
            }
        }
    }
} 