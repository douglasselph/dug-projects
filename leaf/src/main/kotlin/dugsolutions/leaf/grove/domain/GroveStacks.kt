package dugsolutions.leaf.grove.domain

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.di.factory.GameCardIDsFactory

class GroveStacks(
    private val cardManager: CardManager,
    private val gameCardIDsFactory: GameCardIDsFactory
) {

    private val stacks: MutableMap<MarketStackID, GameCardIDs> = mutableMapOf()
    private val diceSupply: DiceSupply = DiceSupply()

    init {
        for (stackId in MarketStackID.entries) {
            stacks[stackId] = gameCardIDsFactory(emptyList())
        }
    }

    // Building methods
    fun add(stack: MarketStackID, cards: GameCards) {
        get(stack)?.addAll(cards.cards.map { it.id })
    }

    fun add(stack: MarketStackID, cardIds: GameCardIDs) {
        get(stack)?.addAll(cardIds.cardIds)
    }

    fun set(stack: MarketStackID, cards: GameCards) {
        val cardIds = cards.cards.map { it.id }
        stacks[stack] = gameCardIDsFactory(cardIds)
    }

    fun set(stack: MarketStackID, cardIds: GameCardIDs) {
        stacks[stack] = gameCardIDsFactory(cardIds.cardIds)
    }

    fun repeat(stack: MarketStackID, card: CardID, count: Int) {
        val repeatedCardIds = gameCardIDsFactory(List(count) { card })
        set(stack, repeatedCardIds)
    }

    fun shuffle(stack: MarketStackID) {
        stacks[stack]?.shuffle()
    }

    fun clearAll() {
        diceSupply.clear()
        for (stackId in MarketStackID.entries) {
            stacks[stackId]?.clear()
        }
    }

    fun setDiceCount(sides: DieSides, count: Int) {
        diceSupply.addMany(sides, count)
    }

    // Card-related methods
    operator fun get(stack: MarketStackID): GameCardIDs? = stacks[stack]

    // Get all the cards of a particular stack type (e.g., ROOT, BLOOM, etc.)
    fun getStacksByType(type: MarketStackType): GameCardIDs {
        val combinedCardIds = stacks
            .filterKeys { it.type == type }
            .values
            .flatMap { it.cardIds }
        return gameCardIDsFactory(combinedCardIds)
    }

    private fun removeTopCardFromStack(stack: MarketStackID): Boolean {
        val currentStack = get(stack) ?: return false
        if (currentStack.isEmpty()) return false
        currentStack.removeTop()
        return true
    }

    // Dice-related methods
    fun getAvailableDiceSides(): List<Int> = diceSupply.getAvailableSides()

    fun getDiceQuantity(sides: Int): Int = diceSupply.getQuantity(sides)

    fun removeDie(sides: Int): Boolean = diceSupply.removeDie(sides)

    fun hasDie(sides: Int): Boolean = diceSupply.hasDie(sides)

    fun addDie(sides: Int, count: Int = 1): Boolean = diceSupply.addDie(sides, count)

    // Returns all stacks where the given card is the top-most (available) card
    fun findStacksWithTopShowingForCard(cardId: CardID): List<MarketStackID> =
        MarketStackID.entries
            .filter { stack -> 
                get(stack)?.cardIds?.firstOrNull() == cardId
            }

    // Returns the current cards showing that can be purchased (top card from each non-empty stack)
    fun getTopShowingCards(): List<GameCard> =
        MarketStackID.entries
            .mapNotNull { stack -> 
                get(stack)?.cardIds?.firstOrNull()?.let { cardId ->
                    cardManager.getCard(cardId)
                }
            }

    fun findGameCardByID(cardId: CardID): GameCard? =
        cardManager.getCard(cardId)

    fun isCardShowing(cardId: CardID): Boolean =
        findStacksWithTopShowingForCard(cardId).isNotEmpty()

    fun removeTopShowingCardOf(cardId: CardID): Boolean {
        val stacks = findStacksWithTopShowingForCard(cardId)
        if (stacks.isEmpty()) return false
        // Not normally expected to return multiple stacks
        return removeTopCardFromStack(stacks.first())
    }

    fun isDieAvailable(sides: Int): Boolean =
        diceSupply.getQuantity(sides) > 0

    fun getAffordableDice(availablePips: Int): List<Int> =
        diceSupply.getAffordableSides(availablePips)

    fun push(
        source: GameCards,
        index: Int, stack:
        MarketStackID,
        count: Int
    ) {
        source.getOrNull(index)?.let { card -> repeat(stack, card.id, count) }
    }
}
