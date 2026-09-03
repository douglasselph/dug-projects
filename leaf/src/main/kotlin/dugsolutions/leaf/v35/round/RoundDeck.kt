package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCards
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Mutable per-game Round draw deck.
 *
 * RoundCardManager stores one definition per CSV row. setup() expands each
 * definition according to quantity before choosing and shuffling the physical
 * cards used in this game.
 */
class RoundDeck(
    private val roundCardManager: RoundCardManager,
    private val randomizer: Randomizer
) {

    private var drawPile = RoundCards(emptyList())
    private var topCard: RoundCard? = null

    /**
     * Cards not yet revealed.
     */
    val cards: RoundCards
        get() = drawPile

    /**
     * The currently revealed Round card, or null before the first next().
     */
    val top: RoundCard?
        get() = topCard

    /**
     * Number of Round cards not yet revealed.
     */
    val remaining: Int
        get() = drawPile.size

    val isEmpty: Boolean
        get() = remaining == 0

    /**
     * Creates the standard ordered Round deck:
     * selected Cultivation rounds first, selected Battle rounds second.
     *
     * The definitions are expanded by quantity before selection. This matters
     * because the current CSV has multiple physical copies of Cultivation
     * Round card definitions.
     */
    fun setup(
        numBattle: Int,
        numCultivation: Int
    ) {
        require(numBattle >= 0) {
            "Battle card count cannot be negative: $numBattle"
        }
        require(numCultivation >= 0) {
            "Cultivation card count cannot be negative: $numCultivation"
        }

        val battleCards = expandedCards(RoundCardType.BATTLE)
        val cultivationCards = expandedCards(RoundCardType.CULTIVATION)

        require(battleCards.size >= numBattle) {
            "Not enough battle cards: requested=$numBattle " +
                "available=${battleCards.size}"
        }
        require(cultivationCards.size >= numCultivation) {
            "Not enough cultivation cards: requested=$numCultivation " +
                "available=${cultivationCards.size}"
        }

        val selectedCultivation = randomizer
            .shuffled(cultivationCards)
            .take(numCultivation)

        val selectedBattle = randomizer
            .shuffled(battleCards)
            .take(numBattle)

        drawPile = RoundCards(
            selectedCultivation + selectedBattle
        )
        topCard = null
    }

    /**
     * Reveals and removes the next card from the draw pile.
     */
    fun next(): RoundCard? {
        val nextCard = drawPile.getOrNull(0)
        topCard = nextCard

        if (nextCard != null) {
            drawPile = RoundCards(drawPile.cards.drop(1))
        }

        return nextCard
    }

    private fun expandedCards(
        type: RoundCardType
    ): List<RoundCard> =
        roundCardManager.getCardsByType(type)
            .flatMap { card ->
                List(card.quantity) { card }
            }
}
