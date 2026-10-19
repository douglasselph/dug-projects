package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.domain.GameCards

class CheckGameCardNames(
    private val checkGameCardName: CheckGameCardName = CheckGameCardName()
) {
    operator fun invoke(cards: GameCards) {
        cards.forEach { card -> checkGameCardName(card) }
    }
}
