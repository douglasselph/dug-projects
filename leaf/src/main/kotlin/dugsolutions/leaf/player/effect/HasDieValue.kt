package dugsolutions.leaf.player.effect

import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.Die

/**
 * Checks if a specific die value exists in the player's hand items
 */
class HasDieValue {
    operator fun invoke(handItems: List<HandItem>, targetValue: Int): Die? {
        var bestChoice: Die? = null
        handItems.forEach { item ->
            if (item is HandItem.aDie) {
                if (item.die.value == targetValue) {
                    bestChoice = bestChoice?.let { if (item.die.sides < it.sides) item.die else it } ?: item.die
                }
            }
        }
        return bestChoice
    }
} 
