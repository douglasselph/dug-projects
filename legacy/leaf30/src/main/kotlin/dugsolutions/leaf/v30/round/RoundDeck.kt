package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.round.domain.RoundCards

class RoundDeck(
    private val roundCardManager: RoundCardManager,
    private val randomizer: Randomizer
) {
    private var drawPile = RoundCards(emptyList())
    private var topCard: RoundCard? = null

    val cards: RoundCards
        get() = drawPile

    val top: RoundCard?
        get() = topCard

    val remaining: Int
        get() = drawPile.size

    val isEmpty: Boolean
        get() = remaining == 0

    fun setup(numBattle: Int, numCultivation: Int) {
        require(numBattle >= 0) { "Battle card count cannot be negative: $numBattle" }
        require(numCultivation >= 0) { "Cultivation card count cannot be negative: $numCultivation" }

        val grouped = roundCardManager.getAllCards().cards.groupBy { it.cardType }
        val battleCards = randomizer.shuffled(grouped[RoundCardType.BATTLE].orEmpty())
        val cultivationCards = randomizer.shuffled(grouped[RoundCardType.CULTIVATION].orEmpty())

        require(battleCards.size >= numBattle) {
            "Not enough battle cards: requested=$numBattle available=${battleCards.size}"
        }
        require(cultivationCards.size >= numCultivation) {
            "Not enough cultivation cards: requested=$numCultivation available=${cultivationCards.size}"
        }

        // Draw pile is top-first: cultivation cards are drawn before battle cards.
        drawPile = RoundCards(
            cultivationCards.take(numCultivation) + battleCards.take(numBattle)
        )
        topCard = null
    }

    fun next(): RoundCard? {
        if (topCard != null) {
            drawPile = RoundCards(drawPile.cards.drop(1))
        }
        topCard = drawPile.getOrNull(0)
        return topCard
    }
}
