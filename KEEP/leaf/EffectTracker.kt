package dugsolutions.leaf.player.components

import dugsolutions.leaf.components.GameCard

/**
 * Information to track effects across processing during a single turn
 */
class EffectTracker {
    // Damage-related effects
    var incomingDamage: Int = 0
    var thornDamage: Int = 0
    var deflectDamage: Int = 0
    var cardsReused: MutableList<GameCard> = mutableListOf()
    var pipModifier: Int = 0

    // Reset all values
    fun clear() {
        incomingDamage = 0
        thornDamage = 0
        deflectDamage = 0
        pipModifier= 0
        cardsReused.clear()
    }

} 
