package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.list.GameCardIDs
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue

class StackManager(
    private val cardManager: CardManager,
    gameCardIDsFactory: GameCardIDsFactory
) {

    private val cards: GameCardIDs = gameCardIDsFactory(emptyList())

    val dice: Dice = Dice()

    val cardCount: Int
        get() = cards.size

    val diceCount: Int
        get() = dice.size

    val isEmpty: Boolean
        get() = cards.isEmpty() && dice.isEmpty()

    val pipTotal: Int
        get() = dice.dice.sumOf { it.value }

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
    fun addAllCards(cardIds: List<CardID>): StackManager {
        cards.addAll(cardIds)
        return this
    }

    fun addAllDice(diceList: List<Die>) {
        dice.addAll(diceList)
    }

    fun getItems(): List<HandItem> =
        cards.cardIds.mapNotNull { cardId ->
            cardManager.getCard(cardId)?.let { card -> HandItem.aCard(card) }
        } + dice.dice.map { HandItem.aDie(it) }

    fun clear() {
        cards.clear()
        dice.clear()
    }

    fun shuffle() {
        cards.shuffle()
    }

} 
