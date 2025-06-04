package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.di.factory.CardEffectBattleScoreFactory
import dugsolutions.leaf.di.factory.DecisionDirectorFactory
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.di.factory.GameCardIDsFactory
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.FloralCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.player.decisions.local.EffectBattleScore
import dugsolutions.leaf.player.decisions.local.GroveNearingTransition
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf.Effect

class PlayerTD private constructor(
    deckManager: DeckManager,
    floralArray: FloralArray,
    cardManager: CardManager,
    retainedComponents: StackManager,
    private val dieFactory: DieFactory,
    costScore: CostScore,
    decisionDirectorFactory: DecisionDirectorFactory
) : Player(
    deckManager,
    floralArray,
    cardManager,
    retainedComponents,
    dieFactory,
    costScore,
    decisionDirectorFactory
) {

    companion object {

        val randomizerTD = RandomizerTD()

        operator fun invoke(id: Int): PlayerTD {
            val name = "Player $id"
            return invoke(name, id)
        }

        operator fun invoke(name: String, id: Int): PlayerTD {
            val deckManager = mockk<DeckManager>(relaxed = true)
            val floralArray = mockk<FloralArray>(relaxed = true)
            val cardManager = mockk<CardManager>(relaxed = true)
            val retainedComponents = mockk<StackManager>(relaxed = true)
            val cardEffectBattleScoreFactory = mockk<CardEffectBattleScoreFactory>(relaxed = true)
            val cardEffectBattleScore = mockk<CardEffectBattleScore>(relaxed = true)
            val acquireCardEvaluator = mockk<AcquireCardEvaluator>(relaxed = true)
            val acquireDieEvaluator = mockk<AcquireDieEvaluator>(relaxed = true)
            val groveNearingTransition = mockk<GroveNearingTransition>(relaxed = true)
            val dieFactory = DieFactory(randomizerTD)
            val costScore = CostScore()
            val decisionDirectorFactory = DecisionDirectorFactory(
                cardManager, cardEffectBattleScoreFactory, acquireCardEvaluator,
                acquireDieEvaluator, groveNearingTransition
            )
            every { cardEffectBattleScoreFactory(any()) } returns cardEffectBattleScore

            return PlayerTD(
                deckManager,
                floralArray,
                cardManager,
                retainedComponents,
                dieFactory,
                costScore,
                decisionDirectorFactory
            ).apply {
                this.id = id
                this.name = name
            }
        }

        operator fun invoke(
            id: Int,
            cardManager: CardManager
        ): PlayerTD {
            val name = "Player $id"
            return invoke(name, id, cardManager)
        }

        /**
         *  randomizer = Randomizer.create()
         *  dieFactory = DieFactoryRandom(randomizer)
         *  costScore = CostScore()
         *  val gameCardsFactory = GameCardsFactory(randomizer, costScore)
         *  cardManager = CardManager(gameCardsFactory)
         *  cardManager.loadCards(FakeCards.ALL_CARDS)
         */
        operator fun invoke(
            name: String,
            id: Int,
            cardManager: CardManager
        ): PlayerTD {
            val gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizerTD)
            val supplyStack = StackManager(cardManager, gameCardIDsFactory)
            val handStack = StackManager(cardManager, gameCardIDsFactory)
            val compostStack = StackManager(cardManager, gameCardIDsFactory)
            val floralCount = FloralCount()
            val floralArray = FloralArray(cardManager, floralCount, gameCardIDsFactory)
            val dieFactory = DieFactory(randomizerTD)
            val deckManager = DeckManager(supplyStack, handStack, compostStack, dieFactory)
            val retainedComponents = mockk<StackManager>(relaxed = true)
            val groveNearingTransition = mockk<GroveNearingTransition>(relaxed = true)
            val effectBattleScore = EffectBattleScore()
            val cardEffectBattleScoreFactory = CardEffectBattleScoreFactory(effectBattleScore, floralCount)
            val costScore = CostScore()
            val bestCardEvaluator = BestCardEvaluator()
            val acquireCardEvaluator = AcquireCardEvaluator(bestCardEvaluator)
            val acquireDieEvaluator = AcquireDieEvaluator()
            val decisionDirectorFactory = DecisionDirectorFactory(
                cardManager, cardEffectBattleScoreFactory, acquireCardEvaluator,
                acquireDieEvaluator, groveNearingTransition
            )

            return PlayerTD(
                deckManager,
                floralArray,
                cardManager,
                retainedComponents,
                dieFactory,
                costScore,
                decisionDirectorFactory
            ).apply {
                this.id = id
                this.name = name
                this.useDeckManager = true
            }
        }
    }

    override var id = 0

    override fun toString(): String {
        return "PlayerImplTD('$name')"
    }

    var useDeckManager = false

    override val pipTotal: Int
        get() = if (useDeckManager) super.pipTotal else {
            diceInHand.dice.sumOf { it.value } + pipModifier
        }

    private val _diceInHand: Dice = Dice()
    private val _diceInSupply: Dice = Dice()
    private val _diceInCompost: Dice = Dice()

    override val diceInHand: Dice
        get() = if (useDeckManager) super.diceInHand else _diceInHand

    override val diceInCompost: Dice
        get() = if (useDeckManager) super.diceInCompost else _diceInCompost

    override val diceInSupply: Dice
        get() = if (useDeckManager) super.diceInSupply else _diceInSupply

    override fun addDieToSupply(die: Die) {
        super.addDieToSupply(die)
        diceInSupply.add(die)
    }

    override fun addDieToSupply(die: DieValue) {
        super.addDieToSupply(die)
        diceInSupply.add(die.dieFrom(dieFactory))
    }

    override fun addDieToHand(die: Die) {
        super.addDieToHand(die)
        diceInHand.add(die)
    }

    override fun addDieToHand(die: DieValue) {
        super.addDieToHand(die)
        diceInHand.add(die.dieFrom(dieFactory))
    }

    override fun addDieToCompost(die: Die) {
        super.addDieToCompost(die)
        diceInCompost.add(die)
    }

    override val allDice: Dice
        get() = Dice(diceInSupply.dice + diceInHand.dice + diceInCompost.dice)

    private val _cardsInHand = mutableListOf<GameCard>()
    private val _cardsInSupply = mutableListOf<GameCard>()
    private val _cardsInCompost = mutableListOf<GameCard>()
    private val _floralCards = mutableListOf<GameCard>()

    override val cardsInHand: List<GameCard>
        get() = if (useDeckManager) super.cardsInHand else _cardsInHand

    override val cardsInSupply: List<GameCard>
        get() = if (useDeckManager) super.cardsInSupply else _cardsInSupply

    override val cardsInCompost: List<GameCard>
        get() = if (useDeckManager) super.cardsInCompost else _cardsInCompost

    override val floralCards: List<GameCard>
        get() = if (useDeckManager) super.floralCards else _floralCards

    fun addCardToHand(card: GameCard) {
        super.addCardToHand(card.id)
        _cardsInHand.add(card)
    }

    fun removeCardFromHand(card: GameCard) {
        super.removeCardFromHand(card.id)
        _cardsInHand.remove(card)
    }

    fun addCardToSupply(card: GameCard) {
        super.addCardToSupply(card.id)
        _cardsInSupply.add(card)
    }

    fun addCardsToSupply(list: List<GameCard>) {
        list.forEach { addCardToSupply(it) }
    }

    fun addCardToFloralArray(card: GameCard) {
        super.addCardToFloralArray(card.id)
        _floralCards.add(card)
    }

    fun addCardToCompost(card: GameCard) {
        super.addCardToCompost(card.id)
        _cardsInCompost.add(card)
    }

    override fun removeCardFromHand(cardId: CardID): Boolean {
        _cardsInHand.find { it.id == cardId }?.let { _cardsInHand.remove(it) }
        return super.removeCardFromHand(cardId)
    }


    override fun removeDieFromHand(die: Die): Boolean {
        _diceInHand.remove(die)
        return super.removeDieFromHand(die)
    }

    var gotCardIds = mutableListOf<CardID>()

    override fun getItemsInHand(): List<HandItem> {
        return _cardsInHand.map { HandItem.Card(it) } + _diceInHand.dice.map { HandItem.Dice(it) }
    }

    override fun discard(cardId: CardID): Boolean {
        super.discard(cardId)
        gotCardIds.add(cardId)
        return true
    }

    var gotDice = mutableListOf<Die>()

    override fun discard(die: Die): Boolean {
        val flag = super.discard(die)
        _diceInHand.remove(die)
        _diceInCompost.add(die)
        gotDice.add(die)
        return flag
    }

    override fun discard(die: DieValue): Boolean {
        return discard(die.dieFrom(dieFactory))
    }

    override fun discardHand() {
        super.discardHand()
        _diceInHand.clear()
        _cardsInHand.clear()
    }

} 
