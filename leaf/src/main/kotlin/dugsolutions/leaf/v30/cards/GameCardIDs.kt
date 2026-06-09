package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCardID
import dugsolutions.leaf.v30.random.Randomizer

class GameCardIDs(
    private val cardManager: GameCardManager,
    initialCardIds: List<GameCardID>,
    private val randomizer: Randomizer
) {
    private val _cards = initialCardIds.toMutableList()
    private val lock = Any()

    val cardIds: List<GameCardID>
        get() = synchronized(lock) {
            _cards.toList()
        }

    // Utility functions to make GameCardIDs more list-like
    fun isEmpty(): Boolean = synchronized(lock) {
        _cards.isEmpty()
    }

    val size: Int
        get() = synchronized(lock) {
            _cards.size
        }

    operator fun get(index: Int): GameCardID = synchronized(lock) {
        _cards[index]
    }

    fun getCard(index: Int): GameCard? = synchronized(lock) {
        if (index < 0 || index >= _cards.size) {
            return null
        }
        return cardManager.getCard(get(index))
    }

    operator fun plus(other: GameCardIDs): GameCardIDs = synchronized(lock) {
        GameCardIDs(
            cardManager,
            _cards + other.cardIds,
            randomizer
        )
    }

    // Draw and shuffle
    fun shuffle() {
        synchronized(lock) {
            val newCards = randomizer.shuffled(_cards)
            _cards.clear()
            _cards.addAll(newCards)
        }
    }

    fun draw(): GameCardID? {
        synchronized(lock) {
            if (_cards.isEmpty()) return null
            return _cards.removeAt(0)
        }
    }

    fun removeTop(): GameCardID? = draw()

    // Mutation
    fun reset(newCardIds: List<GameCardID>) {
        synchronized(lock) {
            _cards.clear()
            _cards.addAll(newCardIds)
        }
    }

    fun clear() {
        synchronized(lock) {
            _cards.clear()
        }
    }

    fun add(cardId: GameCardID) {
        synchronized(lock) {
            _cards.add(cardId)
        }
    }

    fun addAll(cardIds: List<GameCardID>) {
        synchronized(lock) {
            _cards.addAll(cardIds)
        }
    }

    fun add(cards: GameCardIDs) {
        addAll(cards.cardIds)
    }

    fun transfer(from: GameCardIDs) {
        synchronized(lock) {
            synchronized(from.lock) {
                addAll(from.cardIds)
                from._cards.clear()
            }
        }
    }

    fun remove(cardId: GameCardID): Boolean {
        synchronized(lock) {
            val index = _cards.indexOfFirst { it == cardId }
            return if (index >= 0) {
                _cards.removeAt(index)
                true
            } else {
                false
            }
        }
    }

    fun take(n: Int): GameCardIDs = synchronized(lock) {
        GameCardIDs(
            cardManager,
            _cards.take(n),
            randomizer
        )
    }

    fun <R> map(transform: (GameCard) -> R): List<R> =
        synchronized(lock) {
            _cards.mapNotNull { cardManager.getCard(it) }.map(transform)
        }

    fun sort() {
        synchronized(lock) {
            _cards.sort()
        }
    }
}