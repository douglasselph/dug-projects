package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.random.die.DieSides

data class ItemsToBuy(
    val dice: List<DieSides> = emptyList(),
    val cards: GameCards = GameCards(emptyList()),
    val crittersUsed: Critters = Critters()
)
