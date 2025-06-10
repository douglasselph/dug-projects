package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.domain.CardID

/**
 * Compute the bonus granted based on the contributing flowers
 */
class FloralBonusCount {

    operator fun invoke(cardIds: List<CardID>, bloomFlowerCardID: CardID): Int {
        val matchingCount = cardIds.count { it == bloomFlowerCardID }
        val uniqueNonMatchingCount = cardIds.filter { it != bloomFlowerCardID }.distinct().size
        return matchingCount + uniqueNonMatchingCount
    }

}
