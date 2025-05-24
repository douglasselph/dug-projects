package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.DecisionDirectorFactory
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.components.drawHand
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.player.effect.EffectsList

open class Player(
    private val deckManager: DeckManager,
    private val floralArray: FloralArray,
    private val cardManager: CardManager,
    private val retainedComponents: StackManager,
    private val dieFactory: DieFactory,
    private val costScore: CostScore,
    private val decisionDirectorFactory: DecisionDirectorFactory,
    private val chronicle: GameChronicle
) {
    companion object {
        private var NextID = 1

        fun resetID() {
            NextID = 1
        }
    }

    open val id = NextID++

    private val defaultName: String
        get() = "Player $id"

    var name: String = ""
        get() {
            return field.ifEmpty { defaultName }
        }

    var incomingDamage: Int = 0
    var deflectDamage: Int = 0
    var cardsReused: MutableList<GameCard> = mutableListOf()
    var pipModifier: Int = 0
    val effectsList: EffectsList = EffectsList()

    val handSize: Int
        get() = deckManager.handSize

    open val pipTotal: Int
        get() = deckManager.pipTotal + pipModifier

    val decisionDirector: DecisionDirector by lazy { decisionDirectorFactory(this) }

    open val diceInHand: Dice
        get() = getDiceFrom(deckManager.getItemsInHand())

    open val diceInCompost: Dice
        get() = getDiceFrom(deckManager.getItemsInCompost())

    open val diceInSupply: Dice
        get() = getDiceFrom(deckManager.getItemsInSupply())

    private fun getDiceFrom(items: List<HandItem>): Dice {
        return Dice(items.mapNotNull {
            when (it) {
                is HandItem.Card -> null
                is HandItem.Dice -> it.die
            }
        })
    }

    open val allDice: Dice
        get() = deckManager.allDice

    open val cardsInHand: List<GameCard>
        get() = deckManager.getItemsInHand().mapNotNull {
            when (it) {
                is HandItem.Card -> cardManager.getCard(it.card.id)
                is HandItem.Dice -> null
            }
        }

    open val cardsInSupply: List<GameCard>
        get() = deckManager.getItemsInSupply().mapNotNull {
            when (it) {
                is HandItem.Card -> cardManager.getCard(it.card.id)
                is HandItem.Dice -> null
            }
        }

    open val cardsInCompost: List<GameCard>
        get() = deckManager.getItemsInCompost().mapNotNull {
            when (it) {
                is HandItem.Card -> cardManager.getCard(it.card.id)
                is HandItem.Dice -> null
            }
        }

    val score: PlayerScore
        get() = PlayerScore(
            playerId = id,
            scoreDice = allDice.totalSides,
            scoreCards = allCardsInDeck.sumOf { costScore(it.cost) }
        )

    val cardsInSupplyCount: Int
        get() = cardsInSupply.size
    val cardsInCompostCount: Int
        get() = cardsInCompost.size
    val diceInSupplyCount: Int
        get() = diceInSupply.size
    val diceInCompostCount: Int
        get() = diceInCompost.size

    val totalDiceCount: Int
        get() = diceInSupplyCount + diceInCompost.size + diceInHand.size
    val totalCardCount: Int
        get() = cardsInSupplyCount + cardsInCompostCount + cardsInHand.size

    fun flowerCount(bloomCard: GameCard): Int {
        if (bloomCard.matchWith is MatchWith.Flower) {
            return floralArray.floralCount(bloomCard.matchWith.flowerCardId)
        }
        return 0
    }

    val allCardsInDeck: List<GameCard>
        get() = cardsInSupply + cardsInHand + cardsInCompost

    val floralCards: List<GameCard>
        get() = floralArray.cards

    // Game phase methods
    fun hasIncomingDamage(): Boolean = incomingDamage > 0

    // Hand management methods
    fun hasCardInHand(cardId: CardID): Boolean =
        deckManager.hasCardInHand(cardId)

    fun hasDieInHand(die: Die): Boolean =
        deckManager.hasDieInHand(die)

    fun getItemsInHand(): List<HandItem> =
        deckManager.getItemsInHand()

    fun getExtendedItems(): List<ExtendedHandItem> {
        val handItems = getItemsInHand()
        val floralItems = floralCards

        return handItems.map { item ->
            when (item) {
                is HandItem.Card -> ExtendedHandItem.Card(item.card)
                is HandItem.Dice -> ExtendedHandItem.Dice(item.die)
            }
        } + floralItems.map { card ->
            ExtendedHandItem.FloralArray(card)
        }
    }

    open fun discard(cardId: CardID): Boolean =
        deckManager.discard(cardId)

    open fun discard(die: Die): Boolean =
        deckManager.discard(die)

    open fun discard(die: DieValue): Boolean =
        deckManager.discard(die)

    open fun removeCardFromHand(cardId: CardID): Boolean =
        deckManager.removeCardFromHand(cardId)

    open fun removeDieFromHand(die: Die): Boolean =
        deckManager.removeDieFromHand(die)

    open fun removeCardFromFloralArray(cardId: CardID): Boolean =
        floralArray.remove(cardId)

    fun retainCard(cardId: CardID): Boolean =
        deckManager.hasCardInHand(cardId) && deckManager.discard(cardId) && retainedComponents.addCard(
            cardId
        )

    fun retainDie(die: Die): Boolean =
        deckManager.hasDieInHand(die) && deckManager.discard(die) && retainedComponents.addDie(die)

    open fun addCardToSupply(cardId: CardID) {
        deckManager.addCardToSupply(cardId)
    }

    open fun addDieToSupply(die: Die) {
        deckManager.addDieToSupply(die)
    }

    open fun addDieToSupply(die: DieValue) {
        deckManager.addDieToSupply(die)
    }

    open fun addCardToHand(cardId: CardID) {
        deckManager.addCardToHand(cardId)
    }

    open fun addDieToHand(die: Die) {
        deckManager.addDieToHand(die)
    }

    open fun addDieToHand(die: DieValue) {
        deckManager.addDieToHand(die)
    }

    open fun addCardToCompost(cardID: CardID) {
        deckManager.addCardToCompost(cardID)
    }

    open fun addDieToCompost(die: Die) {
        deckManager.addDieToCompost(die)
    }

    open fun addCardToFloralArray(cardID: CardID) {
        floralArray.add(cardID)
    }

    // Game flow methods
    fun setupInitialDeck(seedlings: GameCards) {
        deckManager.setup(seedlings, dieFactory.startingDice)
    }

    fun drawHand(preferredCardCount: Int) {
        drawHand(chronicle, preferredCardCount)
    }

    fun drawHand() {
        drawHand(decisionDirector.drawCountDecision())
    }

    fun drawCard(): CardID? {
        if (deckManager.isSupplyEmpty) {
            resupply()
        }
        return deckManager.drawCard()
    }

    fun drawDie(): Die? {
        if (deckManager.isSupplyEmpty) {
            resupply()
        }
        return deckManager.drawDie()?.roll()
    }

    fun drawBestDie(): Die? {
        if (deckManager.isSupplyEmpty) {
            resupply()
        }
        return deckManager.drawBestDie()?.roll()
    }

    fun drawCardFromCompost(): CardID? {
        return deckManager.drawCardFromCompost()
    }

    fun drawDieFromCompost(): Die? {
        return deckManager.drawDieFromCompost()
    }

    fun drawBestDieFromCompost(): Die? {
        return deckManager.drawBestDieFromCompost()
    }

    open fun discardHand() {
        deckManager.discardHand()
        clearEffects()
    }

    fun resupply() {
        deckManager.resupply()
    }

    fun reset() {
        discardHand()
        resupply()
    }

    private fun clearEffects() {
        incomingDamage = 0
        deflectDamage = 0
        pipModifier = 0
        cardsReused.clear()
    }

    fun trashSeedlingCards() {
        deckManager.trashSeedlingCards()
    }

    fun clearFloralCards() {
        floralArray.clear()
    }
}
