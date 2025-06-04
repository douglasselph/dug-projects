package dugsolutions.leaf.player.components

import dugsolutions.leaf.components.CardID

class FloralCount {

    operator fun invoke(cardIds: List<CardID>, flowerCardID: CardID): Int {
        val matchingCount = cardIds.count { it == flowerCardID }
        val nonMatchingCount = cardIds.count { it != flowerCardID }
        val bonusCount = nonMatchingCount / 2  // Integer division automatically truncates odd numbers
        return matchingCount + bonusCount
    }

}
