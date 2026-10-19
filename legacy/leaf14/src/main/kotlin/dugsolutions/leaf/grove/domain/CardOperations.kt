package dugsolutions.leaf.grove.domain

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.list.GameCards

interface CardOperations {

    fun setup()
    fun getGameCards(type: FlourishType): GameCards

}
