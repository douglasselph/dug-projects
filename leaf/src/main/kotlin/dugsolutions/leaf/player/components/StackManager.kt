package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.GameCardIDsFactory

class StackManager(
    private val cardManager: CardManager,
    gameCardIDsFactory: GameCardIDsFactory
) {

    private val cards: GameCardIDs = gameCardIDsFactory(emptyList())

    val dice: Dice = Dice()

    // Properties
    val cardCount: Int
        get() = cards.size

    val diceCount: Int
        get() = dice.size

    val isEmpty: Boolean
        get() = cards.isEmpty() && dice.isEmpty()

    val pipTotal: Int
        get() = dice.dice.sumOf { it.value }

    val bloomCount: Int
        get() = cards.cardIds.count { cardId ->
            cardManager.getCard(cardId)?.type == FlourishType.BLOOM
        }

    // Card operations
    fun hasCard(cardId: CardID): Boolean =
        cards.cardIds.contains(cardId)

    fun addCard(cardId: CardID): Boolean {
        cards.add(cardId)
        return true
    }

    fun removeCard(cardId: CardID): Boolean {
        if (!hasCard(cardId)) return false
        return cards.remove(cardId)
    }

    fun drawCard(): CardID? =
        cards.draw()

    fun drawLowestDie(): Die? =
        dice.drawLowest()

    fun drawHighestDie(): Die? =
        dice.drawHighest()

    // Dice operations
    fun hasDie(die: Die): Boolean =
        dice.hasDie(die)

    fun hasDie(die: DieValue): Boolean =
        dice.hasDie(die)

    fun addDie(die: Die): Boolean {
        dice.add(die)
        return true
    }

    fun removeDie(die: Die): Boolean {
        if (!hasDie(die)) return false
        return dice.remove(die)
    }

    fun removeDie(die: DieValue): Boolean {
        if (!hasDie(die)) return false
        return dice.remove(die)
    }

    // Bulk operations
    fun addAllCards(cardIds: List<CardID>) {
        cards.addAll(cardIds)
    }

    fun addAllDice(diceList: List<Die>) {
        dice.addAll(diceList)
    }

    fun getItems(): List<HandItem> =
        cards.cardIds.mapNotNull { cardId ->
            cardManager.getCard(cardId)?.let { card -> HandItem.Card(card) }
        } + dice.dice.map { HandItem.Dice(it) }

    fun clear() {
        cards.clear()
        dice.clear()
    }

    fun trashSeedlingCards() {
        val seedlingCards = cards.cardIds.filter { cardId ->
            cardManager.getCard(cardId)?.type == FlourishType.SEEDLING
        }
        seedlingCards.forEach { cardId ->
            cards.remove(cardId)
        }
    }

} 
