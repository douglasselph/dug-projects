package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.CardID
import kotlin.math.ceil

/**
 * Compute the bonus granted based on the contributing flowers
 */
class FloralBonusCount {

    operator fun invoke(cardIds: List<CardID>, bloomFlowerCardID: CardID): Int {
        val matchingCount = cardIds.count { it == bloomFlowerCardID }
        val uniqueNonMatchingCount = cardIds.filter { it != bloomFlowerCardID }.size
        return matchingCount + ceil(uniqueNonMatchingCount/2f).toInt()
    }

}
