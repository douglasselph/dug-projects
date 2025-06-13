package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.BuddingStack
import dugsolutions.leaf.player.components.DrawNewHand
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.player.decisions.local.EffectBattleScore
import dugsolutions.leaf.player.di.CardEffectBattleScoreFactory
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.RandomizerTD
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import io.mockk.every
import io.mockk.mockk

class PlayerTD private constructor(
    deckManager: DeckManager,
    buddingStack: BuddingStack,
    floralBonusCount: FloralBonusCount,
    cardManager: CardManager,
    private val dieFactory: DieFactory,
    costScore: CostScore,
    drawNewHand: DrawNewHand,
    decisionDirector: DecisionDirector
) : Player(
    deckManager,
    buddingStack,
    floralBonusCount,
    cardManager,
    dieFactory,
    costScore,
    drawNewHand,
    decisionDirector,
) {

    companion object {

        val randomizerTD = RandomizerTD()

        operator fun invoke(id: Int): PlayerTD {
            val name = "Player $id"
            return invoke(name, id)
        }

        operator fun invoke(name: String, id: Int): PlayerTD {
            val deckManager = mockk<DeckManager>(relaxed = true)
            val buddingStack = mockk<BuddingStack>(relaxed = true)
            val floralBonusCount = mockk<FloralBonusCount>(relaxed = true)
            val cardManager = mockk<CardManager>(relaxed = true)
            val cardEffectBattleScoreFactory = mockk<CardEffectBattleScoreFactory>(relaxed = true)
            val cardEffectBattleScore = mockk<CardEffectBattleScore>(relaxed = true)
            val dieFactory = DieFactory(randomizerTD)
            val costScore = CostScore()
            val decisionDirector = mockk<DecisionDirector>(relaxed = true)
            val drawNewHand: DrawNewHand = mockk(relaxed = true)

            every { cardEffectBattleScoreFactory(any()) } returns cardEffectBattleScore

            return PlayerTD(
                deckManager,
                buddingStack,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                drawNewHand,
                decisionDirector
            ).apply {
                this.id = id
                initialize()
                this.name = name
            }
        }

        fun create(id: Int, decisionDirector: DecisionDirector): PlayerTD {
            val deckManager = mockk<DeckManager>(relaxed = true)
            val buddingStack = mockk<BuddingStack>(relaxed = true)
            val floralBonusCount = mockk<FloralBonusCount>(relaxed = true)
            val cardManager = mockk<CardManager>(relaxed = true)
            val cardEffectBattleScoreFactory = mockk<CardEffectBattleScoreFactory>(relaxed = true)
            val cardEffectBattleScore = mockk<CardEffectBattleScore>(relaxed = true)
            val dieFactory = DieFactory(randomizerTD)
            val costScore = CostScore()
            val drawNewHand: DrawNewHand = mockk(relaxed = true)
            every { cardEffectBattleScoreFactory(any()) } returns cardEffectBattleScore

            return PlayerTD(
                deckManager,
                buddingStack,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                drawNewHand,
                decisionDirector
            ).apply {
                this.id = id
                initialize()
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
            val floralBonusCount = FloralBonusCount()
            val buddingStack = BuddingStack(cardManager, gameCardIDsFactory)
            val dieFactory = DieFactory(randomizerTD)
            val deckManager = DeckManager(supplyStack, handStack, compostStack, dieFactory)
            val costScore = CostScore()
            val drawNewHand = DrawNewHand()
            val decisionDirector = mockk<DecisionDirector>(relaxed = true)

            return PlayerTD(
                deckManager,
                buddingStack,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                drawNewHand,
                decisionDirector
            ).apply {
                this.id = id
                initialize()
                this.name = name
                this.useDeckManager = true
            }
        }

        fun create2(id: Int): PlayerTD {
            val deckManager = mockk<DeckManager>(relaxed = true)
            val buddingStack = mockk<BuddingStack>(relaxed = true)
            val floralBonusCount = mockk<FloralBonusCount>(relaxed = true)
            val cardManager = mockk<CardManager>(relaxed = true)
            val dieFactory = DieFactory(randomizerTD)
            val costScore = CostScore()
            val floralCount = FloralBonusCount()
            val groveNearingTransition = mockk<GroveNearingTransition>(relaxed = true)
            val effectBattleScore = EffectBattleScore()
            val cardEffectBattleScoreFactory = CardEffectBattleScoreFactory(effectBattleScore, floralCount)
            val bestCardEvaluator = BestCardEvaluator()
            val acquireCardEvaluator = AcquireCardEvaluator(bestCardEvaluator)
            val acquireDieEvaluator = AcquireDieEvaluator()
            val gameTime = GameTime()
            val drawNewHand = DrawNewHand()
            val decisionDirector = DecisionDirector(
                cardEffectBattleScoreFactory, cardManager, acquireCardEvaluator,
                acquireDieEvaluator, groveNearingTransition, gameTime
            )
            return PlayerTD(
                deckManager,
                buddingStack,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                drawNewHand,
                decisionDirector
            ).apply {
                this.id = id
                initialize()
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
    private val _diceInBed: Dice = Dice()

    override val diceInHand: Dice
        get() = if (useDeckManager) super.diceInHand else _diceInHand

    override val diceInBed: Dice
        get() = if (useDeckManager) super.diceInBed else _diceInBed

    override val diceInSupply: Dice
        get() = if (useDeckManager) super.diceInSupply else _diceInSupply

    override fun addDieToSupply(die: Die): Boolean {
        diceInSupply.add(die)
        return super.addDieToSupply(die)
    }

    override fun addDieToSupply(die: DieValue): Boolean {
        diceInSupply.add(die.dieFrom(dieFactory))
        return super.addDieToSupply(die)
    }

    override fun addDieToHand(die: Die): Boolean {
        diceInHand.add(die)
        return super.addDieToHand(die)
    }

    override fun addDieToHand(die: DieValue): Boolean {
        diceInHand.add(die.dieFrom(dieFactory))
        return super.addDieToHand(die)
    }

    override fun addDieToBed(die: Die): Boolean {
        diceInBed.add(die)
        return super.addDieToBed(die)
    }

    override val allDice: Dice
        get() = Dice(diceInSupply.dice + diceInHand.dice + diceInBed.dice)

    private val _cardsInHand = mutableListOf<GameCard>()
    private val _cardsInSupply = mutableListOf<GameCard>()
    private val _cardsInCompost = mutableListOf<GameCard>()
    private val _floralCards = mutableListOf<GameCard>()

    override val cardsInHand: List<GameCard>
        get() = if (useDeckManager) super.cardsInHand else _cardsInHand

    override val cardsInSupply: List<GameCard>
        get() = if (useDeckManager) super.cardsInSupply else _cardsInSupply

    override val cardsInBed: List<GameCard>
        get() = if (useDeckManager) super.cardsInBed else _cardsInCompost

    override val floralCards: List<GameCard>
        get() = if (useDeckManager) super.floralCards else _floralCards

    fun addCardToHand(card: GameCard) {
        super.addCardToHand(card.id)
        _cardsInHand.add(card)
    }

    fun removeCardFromHand(card: GameCard) {
        _cardsInHand.remove(card)
        super.removeCardFromHand(card.id)
    }

    fun addCardToSupply(card: GameCard) {
        _cardsInSupply.add(card)
        super.addCardToSupply(card.id)
    }

    fun addCardsToSupply(list: List<GameCard>) {
        list.forEach { addCardToSupply(it) }
    }

    override fun addCardToBuddingStack(cardId: CardID) {
        gotFloralArrayCards.add(cardId)
        super.addCardToBuddingStack(cardId)
    }

    fun addCardToFloralArray(card: GameCard) {
        _floralCards.add(card)
        super.addCardToBuddingStack(card.id)
    }

    fun addCardToCompost(card: GameCard) {
        _cardsInCompost.add(card)
        super.addCardToBed(card.id)
    }

    override fun removeCardFromHand(cardId: CardID): Boolean {
        gotCardIds.add(cardId)
        _cardsInHand.find { it.id == cardId }?.let { _cardsInHand.remove(it) }
        return super.removeCardFromHand(cardId)
    }

    override fun removeDieFromHand(die: Die): Boolean {
        _diceInHand.remove(die)
        gotDice.add(die)
        return super.removeDieFromHand(die)
    }

    override fun removeCardFromBuddingStack(cardId: CardID): Boolean {
        gotFloralArrayCards.add(cardId)
        _floralCards.removeIf { it.id == cardId }
        return super.removeCardFromBuddingStack(cardId)
    }

    override fun clearFloralCards() {
        gotClearFloralCards = true
        super.clearFloralCards()
    }

    var gotClearFloralCards = false
    val gotCardIds = mutableListOf<CardID>()
    val gotFloralArrayCards = mutableListOf<CardID>()

    override fun getItemsInHand(): List<HandItem> {
        return _cardsInHand.map { HandItem.aCard(it) } + _diceInHand.dice.map { HandItem.aDie(it) }
    }

    override fun discard(cardId: CardID): Boolean {
        super.discard(cardId)
        gotCardIds.add(cardId)
        return true
    }

    val gotDice = mutableListOf<Die>()

    override fun discard(die: Die): Boolean {
        val flag = super.discard(die)
        _diceInHand.remove(die)
        _diceInBed.add(die)
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

    override fun drawCard(): CardID? {
        if (_cardsInSupply.isNotEmpty()) {
            val card = _cardsInSupply.removeAt(0)
            _cardsInHand.add(card)
            return card.id
        }
        return super.drawCard()
    }

} 
