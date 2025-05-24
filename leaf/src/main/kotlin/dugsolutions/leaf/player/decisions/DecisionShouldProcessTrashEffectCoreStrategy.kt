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
 *    - If a card effect target's a die and there is not available die then do not trash.
 *    - If a card effect target's a die, and the only die available is one that allows a match effect, do not trash.
 *    - If card has no effects (primary or match), always trash
 * 3. Then Trash trigger is determined by:
 *    - Card-specific trigger in trashTriggerCard map. This allows an AI like behavior to determine the right time to trash a card.
 *        The failure here, is context is normally important, but for the purposes of this engine perhaps can be ignored.
 *    - Falls back to general trigger (default 3) if no specific trigger
 */
class DecisionShouldProcessTrashEffectCoreStrategy : DecisionShouldProcessTrashEffect {

    companion object {
        private const val TRASH_TRIGGER = 3
    }

    private val trackMap = mutableMapOf<CardID, Int>()

    // region public

    val trashTriggerCard = mutableMapOf<CardID, Int>()
    var trashTriggerGeneral = TRASH_TRIGGER

    override fun invoke(card: GameCard): DecisionShouldProcessTrashEffect.Result {
        // Only certain TrashEffects apply toward being able to trash here.
        when (card.trashEffect) {
            CardEffect.GAIN_FREE_ROOT,
            CardEffect.GAIN_FREE_CANOPY,
            CardEffect.GAIN_FREE_VINE,
            CardEffect.UPGRADE_ANY_RETAIN,
            CardEffect.UPGRADE_ANY,
            CardEffect.UPGRADE_D4,
            CardEffect.UPGRADE_D6,
            CardEffect.UPGRADE_D4_D6 -> {
            }

            else -> return DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED
        }
        // If card has no effects, always trash
        if (card.primaryEffect == null && card.matchEffect == null) {
            return DecisionShouldProcessTrashEffect.Result.TRASH
        }
        // Get the current count for this card, defaulting to 0 if not seen before
        val currentCount = trackMap.getOrDefault(card.id, 0)

        // Determine the trigger threshold for this specific card
        val triggerThreshold = trashTriggerCard[card.id] ?: trashTriggerGeneral

        // Increment the count for this card
        trackMap[card.id] = currentCount + 1

        // Check if we've reached or exceeded the trigger threshold
        return if (currentCount + 1 >= triggerThreshold) {
            DecisionShouldProcessTrashEffect.Result.TRASH
        } else {
            DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH
        }
    }

    /**
     * Call this in-between games to reset the tracking of seen cards.
     */
    override fun reset() {
        trackMap.clear()
    }

    // endregion public

}
