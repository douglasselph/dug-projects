package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.DecisionDirectorFactory
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.effect.EffectsList
import kotlin.math.max
import kotlin.math.min

open class Player(
    private val deckManager: DeckManager,
    private val cardManager: CardManager,
    private val retainedComponents: StackManager,
    private val dieFactory: DieFactory,
    private val chronicle: GameChronicle,
    private val costScore: CostScore,
    private val decisionDirectorFactory: DecisionDirectorFactory
) {
    companion object {
        private const val HAND_SIZE = 4
        private var NextID = 1

        fun resetID() {
            NextID = 1
        }
    }

    open val id = NextID++
    var name: String = "Player $id"

    val bloomCount: Int
        get() = deckManager.bloomCount

    var incomingDamage: Int = 0
    var thornDamage: Int = 0
    var deflectDamage: Int = 0
    var cardsReused: MutableList<GameCard> = mutableListOf()
    var pipModifier: Int = 0

    open val pipTotal: Int
        get() = deckManager.pipTotal + pipModifier

    val effectsList: EffectsList = EffectsList()

    // Player state properties

    var hasPassed: Boolean = false
    var wasHit: Boolean = false
    var isDormant: Boolean = false
    var bonusDie: Die? = null

    private val handSize: Int
        get() = deckManager.handSize

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

    val cardsToPlay = mutableListOf<GameCard>()
    val score: PlayerScore
        get() = PlayerScore(
            playerId = id,
            scoreDice = allDice.totalSides,
            scoreCards = allCards.sumOf { costScore(it.cost) }
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

    val allCards: List<GameCard>
        get() = cardsInSupply + cardsInHand + cardsInCompost

    val canPlayCard: Boolean
        get() = !hasPassed && !isDormant && cardsToPlay.isNotEmpty()

    // Game phase methods
    fun hasIncomingDamage(): Boolean = incomingDamage > 0

    // Hand management methods
    fun hasCardInHand(cardId: CardID): Boolean =
        deckManager.hasCardInHand(cardId)

    fun hasDieInHand(die: Die): Boolean =
        deckManager.hasDieInHand(die)

    fun getItemsInHand(): List<HandItem> =
        deckManager.getItemsInHand()

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

    // Game flow methods
    fun setupInitialDeck(seedlings: GameCards) {
        deckManager.setup(seedlings, dieFactory.startingDice)
    }

    fun draw(preferredCardCount: Int) {
        val spaceLeft = HAND_SIZE - handSize
        if (spaceLeft <= 0) return

        // If supply is low, take everything from supply first
        if (cardsInSupplyCount + diceInSupplyCount < spaceLeft) {
            // Draw all remaining cards from supply
            repeat(cardsInSupplyCount) { drawCard() }
            // Draw all remaining dice from supply
            repeat(diceInSupplyCount) { drawDie() }
            
            // Calculate remaining space after taking all supply
            val remainingSpace = spaceLeft - (cardsInSupplyCount + diceInSupplyCount)
            if (remainingSpace <= 0) {
                chronicle(GameChronicle.Moment.DRAW_HAND(this))
                return
            }

            // If we haven't met preferredCardCount, try to draw more cards
            val cardsStillNeeded = preferredCardCount - cardsInHand.size
            if (cardsStillNeeded > 0) {
                repeat(minOf(cardsStillNeeded, remainingSpace)) { drawCard() }
            }
            
            // Fill remaining space with dice
            val finalSpaceLeft = HAND_SIZE - handSize
            repeat(finalSpaceLeft) { drawDie() }
        } else {
            // Normal case - supply is plentiful
            val cardsLeft = max(0, preferredCardCount - cardsInHand.size)
            val cardsLeftToDraw = min(spaceLeft, cardsLeft)
            
            // Draw cards first
            repeat(cardsLeftToDraw) { drawCard() }
            // Then draw dice to fill remaining space
            val diceLeft = max(0, HAND_SIZE - handSize)
            repeat(diceLeft) { drawDie() }
        }
        chronicle(GameChronicle.Moment.DRAW_HAND(this))
    }

    fun drawHand() {
        draw(decisionDirector.drawCountDecision())
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
        thornDamage = 0
        deflectDamage = 0
        pipModifier = 0
        cardsReused.clear()
    }

    fun trashSeedlingCards() {
        deckManager.trashSeedlingCards()
    }
}
