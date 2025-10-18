package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.list.GameCards
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.di.DieFactory

class DeckManager(
    private val supply: StackManager,
    private val hand: StackManager,
    private val discardPile: StackManager,
    private val dieFactory: DieFactory
) {

    val handSize: Int
        get() = hand.cardCount + hand.diceCount

    val isResupplyNeeded: Boolean
        get() = supply.cardCount == 0

    val pipTotal: Int
        get() = hand.pipTotal

    val allDice: Dice
        get() = Dice(supply.dice.dice + hand.dice.dice + discardPile.dice.dice)

    // Setup
    fun setup(seedlings: GameCards, startingDice: List<Die>) {
        supply.addAllCards(seedlings.cardIds).shuffle()
        supply.addAllDice(startingDice)
    }

    // Hand management
    fun hasCardInHand(cardId: CardID): Boolean = hand.hasCard(cardId)
    fun hasDieInHand(die: Die): Boolean = hand.hasDie(die)
    fun getItemsInHand(): List<HandItem> = hand.getItems()
    fun getItemsInDiscardPile(): List<HandItem> = discardPile.getItems()
    fun getItemsInSupply(): List<HandItem> = supply.getItems()

    fun discard(cardId: CardID): Boolean {
        if (!hand.hasCard(cardId)) return false
        if (hand.removeCard(cardId)) {
            discardPile.addCard(cardId)
            return true
        }
        return false
    }

    fun discard(die: Die): Boolean {
        if (!hand.hasDie(die)) return false
        if (hand.removeDie(die)) {
            discardPile.addDie(die)
            return true
        }
        return false
    }

    fun discard(die: DieValue): Boolean {
        if (!hand.hasDie(die)) return false
        if (hand.removeDie(die)) {
            discardPile.addDie(die.dieFrom(dieFactory))
            return true
        }
        return false
    }

    fun addCardToSupply(cardId: CardID): Boolean = supply.addCard(cardId)
    fun addDieToSupply(die: Die): Boolean = supply.addDie(die)
    fun addDieToSupply(die: DieValue): Boolean = supply.addDie(die.dieFrom(dieFactory))
    fun addCardToHand(cardId: CardID): Boolean = hand.addCard(cardId)
    fun addDieToHand(die: Die): Boolean = hand.addDie(die)
    fun addDieToHand(die: DieValue): Boolean = hand.addDie(die.dieFrom(dieFactory))
    fun addCardToDiscard(cardId: CardID): Boolean = discardPile.addCard(cardId)
    fun addDieToDiscard(die: Die): Boolean = discardPile.addDie(die)
    fun addDieToDiscard(die: DieValue): Boolean = discardPile.addDie(die.dieFrom(dieFactory))
    fun removeCardFromHand(cardId: CardID): Boolean = hand.removeCard(cardId)
    fun removeDieFromHand(die: Die): Boolean = hand.removeDie(die)
    fun removeCardFromDiscardPatch(cardId: CardID): Boolean = discardPile.removeCard(cardId)
    fun removeDieFromDiscard(die: Die): Boolean = discardPile.removeDie(die)

    // Drawing operations
    fun drawCard(): CardID? {
        return supply.drawCard()?.let { cardId ->
            if (hand.addCard(cardId)) cardId else null
        }
    }

    fun drawDie(): Die? {
        return supply.drawLowestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    fun drawBestDie(): Die? {
        return supply.drawHighestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    fun drawCardFromDiscard(): CardID? {
        return discardPile.drawCard()?.let { cardId ->
            if (hand.addCard(cardId)) cardId else null
        }
    }

    fun drawDieFromDiscard(): Die? {
        return discardPile.drawLowestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    fun drawBestDieFromDiscard(): Die? {
        return discardPile.drawHighestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    // Resource cycling
    fun resupply(): Boolean {
        supply.addAllCards(discardPile.getItems().mapNotNull {
            when (it) {
                is HandItem.aCard -> it.card.id
                is HandItem.aDie -> null
            }
        })
        supply.addAllDice(discardPile.getItems().mapNotNull {
            when (it) {
                is HandItem.aCard -> null
                is HandItem.aDie -> it.die
            }
        })
        discardPile.clear()
        supply.shuffle()
        return true
    }

    fun discardHand() {
        hand.getItems().forEach { item ->
            when (item) {
                is HandItem.aCard -> discardPile.addCard(item.card.id)
                is HandItem.aDie -> discardPile.addDie(item.die)
            }
        }
        hand.clear()
    }

    fun clear() {
        supply.clear()
        hand.clear()
        discardPile.clear()
    }

} 
