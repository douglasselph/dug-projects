package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.CostScore
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.list.GameCards
import dugsolutions.leaf.common.domain.Butterfly
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.CreatureManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.components.VPManager
import dugsolutions.leaf.player.components.WispManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.DrawCardResult
import dugsolutions.leaf.player.domain.DrawDieResult
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.player.domain.PlayerScore
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue

class Player(
    private val deckManager: DeckManager,
    private val cardManager: CardManager,
    private val creatureManager: CreatureManager,
    private val insectManager: InsectManager,
    private val butterflyManager: ButterflyManager,
    private val wispManager: WispManager,
    private val vpManager: VPManager,
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

    val id = NextID++

    fun initialize(): Player {
        name = "Player $id"
        decisionDirector.initialize(this)
        return this
    }

    var name: String = "Player Unset"
    var incomingDamage: Int = 0
    var deflectDamage: Int = 0

    val reused: MutableList<HandItem> = mutableListOf()
    val retained: MutableList<HandItem> = mutableListOf()
    val cardsToPlay: MutableList<GameCard> = mutableListOf()

    val isResupplyNeeded: Boolean
        get() = deckManager.isResupplyNeeded

    val handSize: Int
        get() = deckManager.handSize

    val pipTotal: Int
        get() = deckManager.pipTotal

    val diceTotal: Int
        get() = deckManager.pipTotal

    val diceInHand: Dice
        get() = getDiceFrom(deckManager.getItemsInHand())

    val diceInDiscard: Dice
        get() = getDiceFrom(deckManager.getItemsInDiscardPile())

    val diceInSupply: Dice
        get() = getDiceFrom(deckManager.getItemsInSupply())

    private fun getDiceFrom(items: List<HandItem>): Dice {
        return Dice(items.mapNotNull {
            when (it) {
                is HandItem.aCard -> null
                is HandItem.aDie -> it.die
            }
        })
    }

    val allDice: Dice
        get() = deckManager.allDice

    val cardsInHand: List<CardID>
        get() = deckManager.cardsInHand

    fun cardsInHand(): List<GameCard> = cardsInHand.mapNotNull { cardManager.getCard(it) }

    val cardsInSupply: List<CardID>
        get() = deckManager.getItemsInSupply().mapNotNull {
            when (it) {
                is HandItem.aCard -> it.card.id
                is HandItem.aDie -> null
            }
        }

    fun cardsInSupply(): List<GameCard> = cardsInSupply.mapNotNull { cardManager.getCard(it) }

    val cardsInDiscard: List<CardID>
        get() = deckManager.getItemsInDiscardPile().mapNotNull {
            when (it) {
                is HandItem.aCard -> it.card.id
                is HandItem.aDie -> null
            }
        }

    fun cardsInDiscard(): List<GameCard> = cardsInDiscard.mapNotNull { cardManager.getCard(it) }

    val score: PlayerScore
        get() = PlayerScore(
            playerId = id,
            scoreDice = allDice.totalSides,
            scoreCards = allCardsInDeck().sumOf { costScore(it.cost) }
        )

    val cardsInSupplyCount: Int
        get() = cardsInSupply.size
    val cardsInDiscardCount: Int
        get() = cardsInDiscard.size
    val cardsInHandCount: Int
        get() = cardsInHand.size
    val diceInSupplyCount: Int
        get() = diceInSupply.size
    val diceInDiscardCount: Int
        get() = diceInDiscard.size

    val totalDiceCount: Int
        get() = diceInSupplyCount + diceInDiscard.size + diceInHand.size
    val totalCardCount: Int
        get() = cardsInSupplyCount + cardsInDiscardCount + cardsInHandCount

    val allCardsInDeck: List<CardID>
        get() = cardsInSupply + cardsInHand + cardsInDiscard

    fun allCardsInDeck(): List<GameCard> {
        return cardsInSupply() + cardsInHand() + cardsInDiscard()
    }

    // Hand management methods
    fun hasCardInHand(cardId: CardID): Boolean = deckManager.hasCardInHand(cardId)
    fun hasDieInHand(die: Die): Boolean = deckManager.hasDieInHand(die)

    fun discard(cardId: CardID): Boolean = deckManager.discard(cardId)
    fun discard(die: Die): Boolean = deckManager.discard(die)
    fun discard(die: DieValue): Boolean = deckManager.discard(die)
    fun discard(values: List<DieValue>): Boolean = deckManager.discard(values)
    fun removeCardFromHand(cardId: CardID): Boolean = deckManager.removeCardFromHand(cardId)
    fun removeCardFromDiscardPatch(cardId: CardID): Boolean = deckManager.removeCardFromDiscardPatch(cardId)
    fun removeDieFromHand(die: Die): Boolean = deckManager.removeDieFromHand(die)

    fun retainCard(card: GameCard): Boolean =
        hasCardInHand(card.id) && removeCardFromHand(card.id) && retained.add(HandItem.aCard(card))

    fun retainDie(die: Die): Boolean =
        hasDieInHand(die) && removeDieFromHand(die) && retained.add(HandItem.aDie(die))

    fun addCardToSupply(cardId: CardID) = deckManager.addCardToSupply(cardId)
    fun addDieToSupply(die: Die) = deckManager.addDieToSupply(die)
    fun addDieToSupply(die: DieValue) = deckManager.addDieToSupply(die)

    fun addCardToHand(cardId: CardID) = deckManager.addCardToHand(cardId)
    fun addDieToHand(die: Die) = deckManager.addDieToHand(die)
    fun addDieToHand(die: DieValue) = deckManager.addDieToHand(die)
    fun addCardsToHand(cards: List<CardID>) = cards.forEach { addCardToHand(it) }
    fun addDiceToHand(dice: List<Die>) = dice.forEach { addDieToHand(it) }

    fun addCardToDiscard(cardID: CardID) = deckManager.addCardToDiscard(cardID)
    fun addDieToDiscard(die: Die) = deckManager.addDieToDiscard(die)
    fun removeDieFromDiscard(die: Die) = deckManager.removeDieFromDiscard(die)

    fun addCardToCreature(cardId: CardID) = creatureManager.addCard(cardId)
    fun addDieToCreature(die: Die) = creatureManager.addDie(die)
    fun removeCardFromCreature(cardID: CardID) = creatureManager.removeCard(cardID)
    val creatureLeafCards: List<GameCard>
        get() = creatureManager.leafCards

    fun addBug(insect: Token) = insectManager.add(insect)
    fun removeBug(insect: Token) = insectManager.remove(insect)
    fun count(insect: Token) = insectManager.countOf(insect)
    fun hasBug(insect: Token) = insectManager.countOf(insect) > 0
    fun insects(): List<Token> = insectManager.all

    fun addButterfly(butterfly: Butterfly) = butterflyManager.add(butterfly)
    fun removeButterfly(butterfly: Butterfly) = butterflyManager.remove(butterfly)
    fun has(butterfly: Butterfly) = butterflyManager.has(butterfly)

    fun addWisp(wisp: GameCard) = wispManager.add(wisp)
    fun removeWisp(wisp: GameCard) = wispManager.remove(wisp)

    fun addVP() {
        vpManager.count++
    }

    val sapLeft: Int
        get() = creatureManager.sapLeft

    val hasSap: Boolean
        get() = creatureManager.hasSap

    fun useSap() = creatureManager.useSap()

    val hasGraftedDice: Boolean
        get() = creatureManager.hasGraftedDice

    val graftedDice: List<Die>
        get() = creatureManager.graftedDice

    val cardsOnCreature: List<GameCard>
        get() = creatureManager.allCards

    val cardsExecuted: List<GameCard>
        get() = creatureManager.cardsExecuted

    fun cardExecuted(card: GameCard) {
        creatureManager.cardExecuted(card)
    }

    // Game flow methods
    fun setupInitialDeck(seedlings: GameCards) {
        deckManager.setup(seedlings, dieFactory.startingDice)
    }

    fun drawCardWithoutResupply(): CardID? {
        return deckManager.drawCard()
    }

    fun drawDieWithoutResupply(): Die? {
        return deckManager.drawDie()?.roll()
    }

    private fun reshuffleIfNeeded(): Boolean {
        return if (deckManager.isResupplyNeeded) {
            resupply()
        } else false
    }

    fun drawCard(): DrawCardResult {
        val reshuffleDone = reshuffleIfNeeded()
        return DrawCardResult(
            cardId = deckManager.drawCard(),
            reshuffleDone = reshuffleDone
        )
    }

    fun drawDie(): DrawDieResult {
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

    fun discardHand() {
        deckManager.discardHand()
    }

    fun resupply(): Boolean {
        return deckManager.resupply()
    }

    fun reset() {
        discardHand()
        resupply()
        clearEffects()
        creatureManager.resetCardsExecuted()
        creatureManager.resetSap()
    }

    fun clearEffects() {
        incomingDamage = 0
        deflectDamage = 0
        retained.clear()
        reused.clear()
        cardsToPlay.clear()
    }

}
