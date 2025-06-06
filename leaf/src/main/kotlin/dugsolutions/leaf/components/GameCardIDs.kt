package dugsolutions.leaf.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.tool.Randomizer

class GameCardIDs(
    private val cardManager: CardManager,
    initialCardIds: List<CardID>,
    private val randomizer: Randomizer
) {
    private val _cards = initialCardIds.toMutableList()
    val cardIds: List<CardID> get() = _cards.toList()

    // Utility functions to make GameCardIDs more list-like
    fun isEmpty(): Boolean = _cards.isEmpty()

    val size: Int
        get() = _cards.size

    operator fun get(index: Int): CardID = _cards[index]

    // TODO: Unit test
    fun getCard(index: Int): GameCard? {
        return cardManager.getCard(get(index))
    }

    operator fun plus(other: GameCardIDs): GameCardIDs = GameCardIDs(
        cardManager,
        _cards + other.cardIds,
        randomizer
    )

    // Draw and shuffle
    fun shuffle() {
        val newCards = randomizer.shuffled(_cards)
        _cards.clear()
        _cards.addAll(newCards)
    }

    fun draw(): CardID? {
        if (_cards.isEmpty()) return null
        return _cards.removeAt(0)
    }

    fun removeTop(): CardID? = draw()

    // Mutation
    fun reset(newCardIds: List<CardID>) {
        _cards.clear()
        _cards.addAll(newCardIds)
    }

    fun clear() {
        _cards.clear()
    }

    fun add(cardId: CardID) {
        _cards.add(cardId)
    }

    fun addAll(cardIds: List<CardID>) {
        _cards.addAll(cardIds)
    }

    fun add(cards: GameCardIDs) {
        addAll(cards.cardIds)
    }

    fun transfer(from: GameCardIDs) {
        addAll(from.cardIds)
        from._cards.clear()
    }

    fun remove(cardId: CardID): Boolean {
        val index = _cards.indexOfFirst { it == cardId }
        return if (index >= 0) {
            _cards.removeAt(index)
            true
        } else {
            false
        }
    }

    fun take(n: Int): GameCardIDs = GameCardIDs(
        cardManager,
        _cards.take(n),
        randomizer
    )

    fun <R> map(transform: (GameCard) -> R): List<R> =
        _cards.mapNotNull { cardManager.getCard(it) }.map(transform)
}
