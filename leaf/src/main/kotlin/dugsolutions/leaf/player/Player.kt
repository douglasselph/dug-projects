package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.player.components.BuddingStack
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.DrawNewHand
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue

open class Player(
    private val deckManager: DeckManager,
    private val buddingStack: BuddingStack,
    private val floralBonusCount: FloralBonusCount,
    private val cardManager: CardManager,
    private val dieFactory: DieFactory,
    private val costScore: CostScore,
    private val drawNewHand: DrawNewHand,
    val decisionDirector: DecisionDirector
) {
    companion object {
        private var NextID = 1

        fun resetID() {
            NextID = 1
        }
    }

    open val id = NextID++

    fun initialize(): Player {
        name = "Player $id"
        decisionDirector.initialize(this)
        return this
    }

    var name: String = "Player Unset"
    var incomingDamage: Int = 0
    var deflectDamage: Int = 0
    var pipModifier: Int = 0
    var nutrients: Int = 0

    val reused: MutableList<HandItem> = mutableListOf()
    val retained: MutableList<HandItem> = mutableListOf()
    val delayedEffectList: MutableList<AppliedEffect> = mutableListOf()
    val cardsToPlay: MutableList<GameCard> = mutableListOf()

    val handSize: Int
        get() = deckManager.handSize

    open val pipTotal: Int
        get() = deckManager.pipTotal + pipModifier

    val diceTotal: Int
        get() = deckManager.pipTotal

    open val diceInHand: Dice
        get() = getDiceFrom(deckManager.getItemsInHand())

    open val diceInBed: Dice
        get() = getDiceFrom(deckManager.getItemsInBed())

    open val diceInSupply: Dice
        get() = getDiceFrom(deckManager.getItemsInSupply())

    private fun getDiceFrom(items: List<HandItem>): Dice {
        return Dice(items.mapNotNull {
            when (it) {
                is HandItem.aCard -> null
                is HandItem.aDie -> it.die
            }
        })
    }

    open val allDice: Dice
        get() = deckManager.allDice

    open val cardsInHand: List<GameCard>
        get() = deckManager.getItemsInHand().mapNotNull {
            when (it) {
                is HandItem.aCard -> cardManager.getCard(it.card.id)
                is HandItem.aDie -> null
            }
        }

    open val cardsInSupply: List<GameCard>
        get() = deckManager.getItemsInSupply().mapNotNull {
            when (it) {
                is HandItem.aCard -> cardManager.getCard(it.card.id)
                is HandItem.aDie -> null
            }
        }

    open val cardsInBed: List<GameCard>
        get() = deckManager.getItemsInBed().mapNotNull {
            when (it) {
                is HandItem.aCard -> cardManager.getCard(it.card.id)
                is HandItem.aDie -> null
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
    val cardsInBedCount: Int
        get() = cardsInBed.size
    val diceInSupplyCount: Int
        get() = diceInSupply.size
    val diceInBedCount: Int
        get() = diceInBed.size

    val totalDiceCount: Int
        get() = diceInSupplyCount + diceInBed.size + diceInHand.size
    val totalCardCount: Int
        get() = cardsInSupplyCount + cardsInBedCount + cardsInHand.size

    fun flowerCount(flowerCards: List<CardID>, bloomCard: GameCard): Int {
        if (bloomCard.matchWith is MatchWith.Flower) {
            return floralBonusCount(flowerCards, bloomCard.matchWith.flowerCardId)
        }
        return 0
    }

    val allCardsInDeck: List<GameCard>
        get() = cardsInSupply + cardsInHand + cardsInBed

    open val floralCards: List<GameCard>
        get() = buddingStack.cards

    // Hand management methods
    fun hasCardInHand(cardId: CardID): Boolean = deckManager.hasCardInHand(cardId)
    fun hasDieInHand(die: Die): Boolean = deckManager.hasDieInHand(die)
    open fun getItemsInHand(): List<HandItem> = deckManager.getItemsInHand()

    /** Normal Hand Items as well as the Floral Array **/
    fun getExtendedHandItems(): List<ExtendedHandItem> {
        val handItems = getItemsInHand()
        val floralItems = floralCards

        return handItems.map { item ->
            when (item) {
                is HandItem.aCard -> ExtendedHandItem.Card(item.card)
                is HandItem.aDie -> ExtendedHandItem.Dice(item.die)
            }
        } + floralItems.map { card ->
            ExtendedHandItem.FloralArray(card)
        }
    }

    open fun discard(cardId: CardID): Boolean = deckManager.discard(cardId)
    open fun discard(die: Die): Boolean = deckManager.discard(die)
    open fun discard(die: DieValue): Boolean = deckManager.discard(die)
    open fun removeCardFromHand(cardId: CardID): Boolean = deckManager.removeCardFromHand(cardId)
    fun removeCardFromBed(cardId: CardID): Boolean = deckManager.removeCardFromBed(cardId)
    open fun removeDieFromHand(die: Die): Boolean = deckManager.removeDieFromHand(die)
    open fun removeCardFromBuddingStack(cardId: CardID): Boolean = buddingStack.remove(cardId)

    fun retainCard(card: GameCard): Boolean =
        hasCardInHand(card.id) && removeCardFromHand(card.id) && retained.add(HandItem.aCard(card))

    fun retainDie(die: Die): Boolean =
        hasDieInHand(die) && removeDieFromHand(die) && retained.add(HandItem.aDie(die))

    open fun addCardToSupply(cardId: CardID) = deckManager.addCardToSupply(cardId)
    open fun addDieToSupply(die: Die) = deckManager.addDieToSupply(die)
    open fun addDieToSupply(die: DieValue) = deckManager.addDieToSupply(die)
    open fun addCardToHand(cardId: CardID) = deckManager.addCardToHand(cardId)
    open fun addDieToHand(die: Die) = deckManager.addDieToHand(die)
    open fun addDieToHand(die: DieValue) = deckManager.addDieToHand(die)
    open fun addCardToBed(cardID: CardID) = deckManager.addCardToBed(cardID)
    open fun addDieToBed(die: Die) = deckManager.addDieToBed(die)
    fun removeDieFromBed(die: Die) = deckManager.removeDieFromBed(die)
    open fun addCardToBuddingStack(cardId: CardID) = buddingStack.add(cardId)

    fun addCardsToHand(cards: List<CardID>) = cards.forEach { addCardToHand(it) }
    fun addDiceToHand(dice: List<Die>) = dice.forEach { addDieToHand(it) }

    // Game flow methods
    fun setupInitialDeck(seedlings: GameCards) {
        deckManager.setup(seedlings, dieFactory.startingDice)
    }

    fun drawHand(preferredCardCount: Int) {
        drawNewHand(this, preferredCardCount)
    }

    suspend fun drawHand() {
        drawHand(decisionDirector.drawCountDecision().count)
    }

    open fun drawCardWithoutResupply(): CardID? {
        return deckManager.drawCard()
    }

    open fun drawDieWithoutResupply(): Die? {
        return deckManager.drawDie()?.roll()
    }

    open fun drawCard(): CardID? {
        if (deckManager.isSupplyEmpty) {
            resupply()
        }
        return deckManager.drawCard()
    }

    open fun drawDie(): Die? {
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

    fun drawCardFromBed(): CardID? = deckManager.drawCardFromBed()
    fun drawDieFromBed(): Die? = deckManager.drawDieFromBed()
    fun drawBestDieFromBed(): Die? = deckManager.drawBestDieFromBed()

    open fun discardHand() {
        deckManager.discardHand()
    }

    fun resupply() {
        deckManager.resupply()
    }

    fun reset() {
        discardHand()
        resupply()
        clearEffects()
    }

    fun clearEffects() {
        incomingDamage = 0
        deflectDamage = 0
        pipModifier = 0
        retained.clear()
        reused.clear()
        cardsToPlay.clear()
        delayedEffectList.clear()
    }

    fun trashSeedlingCards(): List<CardID> {
        return deckManager.trashSeedlingCards()
    }

    open fun clearFloralCards() {
        buddingStack.clear()
    }
}
