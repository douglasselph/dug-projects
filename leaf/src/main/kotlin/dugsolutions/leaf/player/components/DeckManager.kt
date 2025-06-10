package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.di.DieFactory

class DeckManager(
    private val supply: StackManager,
    private val hand: StackManager,
    private val compost: StackManager,
    private val dieFactory: DieFactory
) {

    val handSize: Int
        get() = hand.cardCount + hand.diceCount

    val isSupplyEmpty: Boolean
        get() = supply.isEmpty

    val pipTotal: Int
        get() = hand.pipTotal

    val allDice: Dice
        get() = Dice(supply.dice.dice + hand.dice.dice + compost.dice.dice)

    // Setup
    fun setup(seedlings: GameCards, startingDice: List<Die>) {
        supply.addAllCards(seedlings.cardIds).shuffle()
        supply.addAllDice(startingDice)
    }

    // Hand management
    fun hasCardInHand(cardId: CardID): Boolean = hand.hasCard(cardId)
    fun hasDieInHand(die: Die): Boolean = hand.hasDie(die)
    fun getItemsInHand(): List<HandItem> = hand.getItems()
    fun getItemsInCompost(): List<HandItem> = compost.getItems()
    fun getItemsInSupply(): List<HandItem> = supply.getItems()

    fun discard(cardId: CardID): Boolean {
        if (!hand.hasCard(cardId)) return false
        if (hand.removeCard(cardId)) {
            compost.addCard(cardId)
            return true
        }
        return false
    }

    fun discard(die: Die): Boolean {
        if (!hand.hasDie(die)) return false
        if (hand.removeDie(die)) {
            compost.addDie(die)
            return true
        }
        return false
    }

    fun discard(die: DieValue): Boolean {
        if (!hand.hasDie(die)) return false
        if (hand.removeDie(die)) {
            compost.addDie(die.dieFrom(dieFactory))
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
    fun addCardToCompost(cardId: CardID): Boolean = compost.addCard(cardId)
    fun addDieToCompost(die: Die): Boolean = compost.addDie(die)
    fun addDieToCompost(die: DieValue): Boolean = compost.addDie(die.dieFrom(dieFactory))
    fun removeCardFromHand(cardId: CardID): Boolean = hand.removeCard(cardId)
    fun removeDieFromHand(die: Die): Boolean = hand.removeDie(die)
    fun removeCardFromCompost(cardId: CardID): Boolean = compost.removeCard(cardId)
    fun removeDieFromCompost(die: Die): Boolean = compost.removeDie(die)

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

    fun drawCardFromCompost(): CardID? {
        return compost.drawCard()?.let { cardId ->
            if (hand.addCard(cardId)) cardId else null
        }
    }

    fun drawDieFromCompost(): Die? {
        return compost.drawLowestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    fun drawBestDieFromCompost(): Die? {
        return compost.drawHighestDie()?.let { die ->
            if (hand.addDie(die)) die else null
        }
    }

    // Resource cycling
    fun resupply() {
        supply.addAllCards(compost.getItems().mapNotNull {
            when (it) {
                is HandItem.aCard -> it.card.id
                is HandItem.aDie -> null
            }
        })
        supply.addAllDice(compost.getItems().mapNotNull {
            when (it) {
                is HandItem.aCard -> null
                is HandItem.aDie -> it.die
            }
        })
        compost.clear()
        supply.shuffle()
    }

    fun discardHand() {
        hand.getItems().forEach { item ->
            when (item) {
                is HandItem.aCard -> compost.addCard(item.card.id)
                is HandItem.aDie -> compost.addDie(item.die)
            }
        }
        hand.clear()
    }

    fun clear() {
        supply.clear()
        hand.clear()
        compost.clear()
    }

    fun trashSeedlingCards(): List<CardID> {
        val trashed = mutableListOf<CardID>()
        trashed.addAll(supply.trashSeedlingCards())
        trashed.addAll(hand.trashSeedlingCards())
        trashed.addAll(compost.trashSeedlingCards())
        return trashed
    }
} 
