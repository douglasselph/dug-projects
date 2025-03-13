package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard

/**
 * Strategy for determining when to process a card's trash effect.
 *
 * Algorithm:
 * 1. For each card, track how many times it has been seen
 * 2. When a card is seen:
 *    - If card has no effects (primary or match), always trash
 *    - Otherwise, check if we've seen it enough times to trigger trash
 * 3. Trash trigger is determined by:
 *    - Card-specific trigger in trashTriggerCard map
 *    - Falls back to general trigger (default 3) if no specific trigger
 *
 * Example:
 * ```kotlin
 * val strategy = DecisionShouldProcessTrashEffectCoreStrategy()
 *
 * // Set specific trigger for a card
 * strategy.trashTriggerCard[cardId] = 2  // Trash after 2nd sight
 *
 * // Change general trigger
 * strategy.trashTriggerGeneral = 4  // Default to 4th sight
 *
 * // First sight of card
 * strategy(card)  // Returns false
 *
 * // Second sight of card
 * strategy(card)  // Returns true if card-specific trigger is 2
 *
 * // Third sight of card
 * strategy(card)  // Returns true if using general trigger (3)
 * ```
 *
 * Note: Call reset() between games to clear tracking.
 */
class DecisionShouldProcessTrashEffectCoreStrategy : DecisionShouldProcessTrashEffect {

    companion object {
        private const val TRASH_TRIGGER = 3
    }

    private val trackMap = mutableMapOf<CardID, Int>()

    // region public

    val trashTriggerCard = mutableMapOf<CardID, Int>()
    var trashTriggerGeneral = TRASH_TRIGGER

    override fun invoke(card: GameCard): Boolean {
        // Only certain TrashEffects apply toward being able to trash here.
        when (card.trashEffect) {
            CardEffect.GAIN_FREE_ROOT,
            CardEffect.GAIN_FREE_CANOPY,
            CardEffect.GAIN_FREE_VINE,
            CardEffect.UPGRADE_ANY_RETAIN,
            CardEffect.UPGRADE_ANY,
            CardEffect.UPGRADE_D4,
            CardEffect.UPGRADE_D6,
            CardEffect.UPGRADE_D4_D6 -> {}

            else -> return false
        }
        // If card has no effects, always trash
        if (card.primaryEffect == null && card.matchEffect == null) {
            return true
        }
        // Get the current count for this card, defaulting to 0 if not seen before
        val currentCount = trackMap.getOrDefault(card.id, 0)

        // Determine the trigger threshold for this specific card
        val triggerThreshold = trashTriggerCard[card.id] ?: trashTriggerGeneral

        // Increment the count for this card
        trackMap[card.id] = currentCount + 1

        // Check if we've reached or exceeded the trigger threshold
        return currentCount + 1 >= triggerThreshold
    }

    /**
     * Call this in-between games to reset the tracking of seen cards.
     */
    override fun reset() {
        trackMap.clear()
    }

    // endregion public

}
