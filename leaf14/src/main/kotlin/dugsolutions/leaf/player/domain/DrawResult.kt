package dugsolutions.leaf.player.domain

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.random.die.Die


data class DrawCardResult(
    val cardId: CardID? = null,
    val reshuffleDone: Boolean = false
)

data class DrawDieResult(
    val die: Die? = null,
    val reshuffleDone: Boolean = false
)
