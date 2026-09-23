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
     * Creates the legacy ordered Round deck: selected Cultivation rounds first,
     * then selected Battle rounds.
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

        setup(
            roundTypes =
                List(numCultivation) { RoundCardType.CULTIVATION } +
                    List(numBattle) { RoundCardType.BATTLE }
        )
    }

    /**
     * Selects shuffled physical Round cards and places them in the requested
     * phase pattern.
     *
     * Cultivation and Battle physical pools are shuffled independently before
     * selection, exactly as in the ordered setup. [roundTypes] controls only
     * where those randomly selected cards appear in the draw deck.
     */
    fun setup(roundTypes: List<RoundCardType>) {
        require(roundTypes.isNotEmpty()) {
            "Game must contain at least one Round card"
        }

        val numCultivation = roundTypes.count { it == RoundCardType.CULTIVATION }
        val numBattle = roundTypes.count { it == RoundCardType.BATTLE }
        val cultivationCards = expandedCards(RoundCardType.CULTIVATION)
        val battleCards = expandedCards(RoundCardType.BATTLE)

        require(cultivationCards.size >= numCultivation) {
            "Not enough cultivation cards: requested=$numCultivation " +
                "available=${cultivationCards.size}"
        }
        require(battleCards.size >= numBattle) {
            "Not enough battle cards: requested=$numBattle " +
                "available=${battleCards.size}"
        }

        // Keep the historical shuffle order stable: Cultivation first, Battle second.
        val selectedCultivation = randomizer
            .shuffled(cultivationCards)
            .take(numCultivation)
        val selectedBattle = randomizer
            .shuffled(battleCards)
            .take(numBattle)

        var cultivationIndex = 0
        var battleIndex = 0
        val selectedCards = roundTypes.map { type ->
            when (type) {
                RoundCardType.CULTIVATION ->
                    selectedCultivation[cultivationIndex++]
                RoundCardType.BATTLE ->
                    selectedBattle[battleIndex++]
            }
        }

        drawPile = RoundCards(selectedCards)
        topCard = null
    }

    /**
     * Installs an exact draw order without consulting [randomizer].
     *
     * The first item in [cards] is the next card revealed. A scenario may use
     * only part of the physical Round-card inventory, but it may not request
     * more copies of a definition than the CSV quantity provides.
     */
    fun setupExact(cards: List<RoundCard>) {
        validatePhysicalCopies(cards)
        drawPile = RoundCards(cards.toList())
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

    private fun validatePhysicalCopies(cards: List<RoundCard>) {
        cards.groupingBy { it.name.trim().lowercase() }
            .eachCount()
            .forEach { (name, requested) ->
                val definition = requireNotNull(roundCardManager.getCard(name)) {
                    "Exact Round deck contains unknown card: $name"
                }
                require(requested <= definition.quantity) {
                    "Exact Round deck requests too many copies of ${definition.name}: " +
                        "requested=$requested, available=${definition.quantity}"
                }
            }

        cards.forEach { card ->
            val definition = requireNotNull(roundCardManager.getCard(card.name)) {
                "Exact Round deck contains unknown card: ${card.name}"
            }
            require(card == definition) {
                "Exact Round deck card does not match catalog definition: ${card.name}"
            }
        }
    }
}
