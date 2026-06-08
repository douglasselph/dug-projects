package dugsolutions.leaf.v14.player.domain

import dugsolutions.leaf.v14.cards.domain.CardID
import dugsolutions.leaf.v14.random.die.Die


data class DrawCardResult(
    val cardId: CardID? = null,
    val reshuffleDone: Boolean = false
)

data class DrawDieResult(
    val die: Die? = null,
    val reshuffleDone: Boolean = false
)
