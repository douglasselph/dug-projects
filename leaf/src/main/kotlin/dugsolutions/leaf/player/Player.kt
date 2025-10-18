package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.domain.DrawCardResult
import dugsolutions.leaf.player.domain.DrawDieResult
import dugsolutions.leaf.player.domain.ExtendedHandItem
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue

open class Player(
    private val deckManager: DeckManager,
    private val floralArray: FloralArray,
    private val floralBonusCount: FloralBonusCount,
    private val cardManager: CardManager,
    private val dieFactory: DieFactory,
    private val costScore: CostScore,
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

    val isResupplyNeeded: Boolean
        get() = deckManager.isResupplyNeeded

    val handSize: Int
        get() = deckManager.handSize

    open val pipTotal: Int
        get() = deckManager.pipTotal + pipModifier

    val diceTotal: Int
        get() = deckManager.pipTotal

    open val diceInHand: Dice
        get() = getDiceFrom(deckManager.getItemsInHand())

    open val diceInDiscard: Dice
        get() = getDiceFrom(deckManager.getItemsInDiscardPatch())

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

    open val cardsInDiscard: List<GameCard>
        get() = deckManager.getItemsInDiscardPatch().mapNotNull {
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
    val cardsInDiscardCount: Int
        get() = cardsInDiscard.size
    val diceInSupplyCount: Int
        get() = diceInSupply.size
    val diceInDiscardCount: Int
        get() = diceInDiscard.size

    val totalDiceCount: Int
        get() = diceInSupplyCount + diceInDiscard.size + diceInHand.size
    val totalCardCount: Int
        get() = cardsInSupplyCount + cardsInDiscardCount + cardsInHand.size

    fun flowerCount(flowerCards: List<CardID>, bloomCard: GameCard): Int {
        if (bloomCard.matchWith is MatchWith.Flower) {
            return floralBonusCount(flowerCards, bloomCard.matchWith.flowerCardId)
        }
        return 0
    }

    val allCardsInDeck: List<GameCard>
        get() = cardsInSupply + cardsInHand + cardsInDiscard

    open val floralCards: List<GameCard>
        get() = floralArray.cards

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
    fun removeCardFromDiscardPatch(cardId: CardID): Boolean = deckManager.removeCardFromDiscardPatch(cardId)
    open fun removeDieFromHand(die: Die): Boolean = deckManager.removeDieFromHand(die)
    open fun removeCardFromFloralArray(cardId: CardID): Boolean = floralArray.remove(cardId)

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
    open fun addCardToDiscard(cardID: CardID) = deckManager.addCardToDiscardPatch(cardID)
    open fun addDieToDiscard(die: Die) = deckManager.addDieToDiscardPatch(die)
    fun removeDieFromDiscard(die: Die) = deckManager.removeDieFromDiscard(die)
    open fun addCardToFloralArray(cardId: CardID) = floralArray.add(cardId)

    fun addCardsToHand(cards: List<CardID>) = cards.forEach { addCardToHand(it) }
    fun addDiceToHand(dice: List<Die>) = dice.forEach { addDieToHand(it) }

    // Game flow methods
    fun setupInitialDeck(seedlings: GameCards) {
        deckManager.setup(seedlings, dieFactory.startingDice)
    }

    open fun drawCardWithoutResupply(): CardID? {
        return deckManager.drawCard()
    }

    open fun drawDieWithoutResupply(): Die? {
        return deckManager.drawDie()?.roll()
    }

    private fun reshuffleIfNeeded(): Boolean {
        return if (deckManager.isResupplyNeeded) {
            resupply()
        } else false
    }

    open fun drawCard(): DrawCardResult {
        val reshuffleDone = reshuffleIfNeeded()
        return DrawCardResult(
            cardId = deckManager.drawCard(),
            reshuffleDone = reshuffleDone
        )
    }

    open fun drawDie(): DrawDieResult {
        val reshuffleDone = reshuffleIfNeeded()
        return DrawDieResult(
            die = deckManager.drawDie()?.roll(),
            reshuffleDone = reshuffleDone
        )
    }

    fun drawBestDie(): DrawDieResult {
        val reshuffleDone = reshuffleIfNeeded()
        return DrawDieResult(
            die = deckManager.drawBestDie()?.roll(),
            reshuffleDone = reshuffleDone
        )
    }

    fun drawCardFromDiscard(): DrawCardResult = DrawCardResult(deckManager.drawCardFromDiscard())
    fun drawDieFromDiscard(): DrawDieResult = DrawDieResult(deckManager.drawDieFromDiscard())
    fun drawBestDieFromDiscard(): DrawDieResult = DrawDieResult(deckManager.drawBestDieFromDiscard())

    open fun discardHand() {
        deckManager.discardHand()
    }

    fun resupply(): Boolean {
        return deckManager.resupply()
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
        floralArray.clear()
    }
}
