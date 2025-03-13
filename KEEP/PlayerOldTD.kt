package dugsolutions.leaf.player

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.domain.ScorePlayer
import dugsolutions.leaf.common.Commons.HAND_SIZE
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.HandItem
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.decisions.DecisionAcquireCard
import dugsolutions.leaf.player.decisions.DecisionAcquireSelect
import dugsolutions.leaf.player.decisions.DecisionAcquireSelectCoreStrategy
import dugsolutions.leaf.player.decisions.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.DecisionDamageAbsorptionCoreStrategy
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.DecisionDrawCount
import dugsolutions.leaf.player.decisions.DecisionDrawCountCoreStrategy
import dugsolutions.leaf.player.decisions.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.effect.EffectsList
import io.mockk.mockk

class PlayerOldTD private constructor(
    override var name: String,
    override val id: Int,
    defaultAcquireDecision: DecisionAcquireSelectCoreStrategy,
    defaultDamageAbsorptionDecision: DecisionDamageAbsorptionCoreStrategy,
    defaultDrawCountDecisionDefault: DecisionDrawCountCoreStrategy,
    defaultShouldProcessTrashEffect: DecisionShouldProcessTrashEffect
) : Player {

    companion object {
        // Factory method to create PlayerTD with default name from ID
        operator fun invoke(id: Int): PlayerTD {
            val name = "Player $id"
            return invoke(name, id)
        }

        // Factory method to create PlayerTD with custom name but auto-created mocks
        operator fun invoke(name: String, id: Int): PlayerTD {
            val acquireDecision = mockk<DecisionAcquireSelectCoreStrategy>(relaxed = true)
            val damageAbsorptionDecision =
                mockk<DecisionDamageAbsorptionCoreStrategy>(relaxed = true)
            val drawCountDecision = mockk<DecisionDrawCountCoreStrategy>(relaxed = true)
            val shouldProcessTrashEffect = mockk<DecisionShouldProcessTrashEffect>(relaxed = true)
            return PlayerTD(
                name,
                id,
                acquireDecision,
                damageAbsorptionDecision,
                drawCountDecision,
                shouldProcessTrashEffect
            )
        }
    }

    // Properties used during game phases
    var respondWithBloomCount: Int = 0
    var respondWithEffectsList: EffectsList = EffectsList()

    // Decisions'
    val respondWithDamageAbsorptionDecision = mutableListOf<DecisionDamageAbsorption.Result>()
    
    // Default is DecisionDrawCount.Result(0) when empty
    val respondWithDrawCountDecision = mutableListOf<DecisionDrawCount.Result>()
    
    // Default is Result(BuyItem.Card) when empty
    val respondWithAcquireDecision = mutableListOf<DecisionAcquireSelect.Result>()
    
    val respondWithAcquireCardDecision = mutableListOf<DecisionAcquireCard.Result>()

    // Draw method results
    var respondWithDrawCard: CardID? = null
    var respondWithDrawDie: Die? = null
    var respondWithDrawBestDie: Die? = null
    var respondWithDrawCardFromCompost: CardID? = null
    var respondWithDrawDieFromCompost: Die? = null
    var respondWithDrawBestDieFromCompost: Die? = null
    var respondWithShouldProcessTrashEffect: Boolean = false
    var respondWithScorePlayer: ScorePlayer = ScorePlayer(0, 0)

    // Argument capture variables
    val gotCardIds = mutableListOf<CardID>()
    var gotCards = mutableListOf<GameCard>()

    override val didNotTrash = mutableListOf<GameCard>()

    // Interface implementation
    override val bloomCount: Int
        get() = respondWithBloomCount

    override var incomingDamage: Int = 0
    override var thornDamage: Int = 0
    override var deflectDamage: Int = 0
    override var pipModifiers = mutableListOf<Int>()
    override val pipModifierTotal: Int
        get() = pipModifiers.sumOf { it }

    override fun pipModifierAdd(value: Int) {
        pipModifiers.add(value)
    }

    override fun pipModifierRemove(value: Int) {
        pipModifiers.remove(value)
    }

    override val cardsReused: MutableList<GameCard> = mutableListOf()

    private val _handCards = mutableListOf<GameCard>()
    private val _supplyCards = mutableListOf<GameCard>()
    private val _compostCards = mutableListOf<GameCard>()

    val retainedCards = mutableListOf<CardID>()
    val retainedDice = mutableListOf<Die>()

    // Player state properties
    override var hasPassed: Boolean = false
    override var wasHit: Boolean = false
    override var isDormant: Boolean = false
    override var bonusDie: Die? = null
    override val diceInHand: Dice = Dice(emptyList())
    override val diceInCompost: Dice = Dice(emptyList())
    override val diceInSupply: Dice = Dice(emptyList())
    override val score: ScorePlayer
        get() = respondWithScorePlayer

    override val effectsList: EffectsList
        get() = respondWithEffectsList

    override val decisionDirector: DecisionDirector = DecisionDirector(
        defaultAcquireDecision,
        defaultAcquireCardDecision,
        defaultDamageAbsorptionDecision,
        defaultDrawCountDecisionDefault,
        defaultShouldProcessTrashEffect
    )

    // Decisions
    override val damageAbsorptionDecision: DecisionDamageAbsorption.Result?
        get() = respondWithDamageAbsorptionDecision.removeFirstOrNull()

    override val drawCountDecision: DecisionDrawCount.Result
        get() = respondWithDrawCountDecision.removeFirstOrNull() ?: DecisionDrawCount.Result(0)

    override val acquireDecision: DecisionAcquireSelect.Result
        get() = respondWithAcquireDecision.removeFirstOrNull() ?: DecisionAcquireSelect.Result(DecisionAcquireSelect.BuyItem.Card)

    override fun acquireCardDecision(cards: List<GameCard>): DecisionAcquireCard.Result? {
        gotCards.addAll(cards)
        return respondWithAcquireCardDecision.removeFirstOrNull()
    }

    override fun shouldProcessTrashEffect(card: GameCard): Boolean {
        return respondWithShouldProcessTrashEffect
    }

    override val cardsInHand: MutableList<GameCard>
        get() = _handCards

    override val cardsToPlay = mutableListOf<GameCard>()

    override val cardsInSupply: MutableList<GameCard>
        get() = _supplyCards

    override val cardsInCompost: MutableList<GameCard>
        get() = _compostCards

    // Computed properties
    override val handSize: Int
        get() = cardsInHand.size + diceInHand.size

    override val pipTotal: Int
        get() = diceInHand.dice.sumOf { it.value } + pipModifiers.sumOf { it }

    override val cardsInSupplyCount: Int
        get() = _supplyCards.size

    override val diceInSupplyCount: Int
        get() = diceInSupply.size

    override val cardsInCompostCount: Int
        get() = _compostCards.size

    override val totalCardCount: Int
        get() = cardsInSupplyCount + cardsInHand.size + cardsInCompostCount

    override val totalDiceCount: Int
        get() = diceInSupplyCount + diceInHand.size + diceInCompost.size

    override val allDice: Dice
        get() = diceInSupply + diceInHand + diceInCompost

    // Game phase methods
    override fun hasIncomingDamage(): Boolean = incomingDamage > 0

    // Hand management methods
    override fun hasCardInHand(cardId: CardID): Boolean {
        return cardsInHand.any { it.id == cardId }
    }

    override fun hasDieInHand(die: Die): Boolean {
        return diceInHand.dice.any { it.sides == die.sides }
    }

    override fun getItemsInHand(): List<HandItem> {
        val items1 = cardsInHand.map { HandItem.Card(it) }
        val items2 = diceInHand.dice.map { HandItem.Dice(it) }
        return items1 + items2
    }

    override fun discard(cardId: CardID): Boolean {
        val removed = _handCards.firstOrNull { it.id == cardId }?.also { _handCards.remove(it) }
            ?: return false
        _compostCards.add(removed)
        gotCardIds.add(cardId)
        return true
    }

    override fun discard(die: Die): Boolean {
        val removed =
            diceInHand.dice.firstOrNull { it.sides == die.sides }?.also { diceInHand.remove(it) }
                ?: return false
        diceInCompost.add(removed)
        return true
    }

    override fun removeCardFromHand(cardId: CardID): Boolean {
        return _handCards.removeIf { it.id == cardId }
    }

    override fun removeDieFromHand(die: Die): Boolean {
        return diceInHand.remove(die)
    }

    override fun retainCard(cardId: CardID): Boolean {
        retainedCards.add(cardId)
        return true
    }

    override fun retainDie(die: Die): Boolean {
        retainedDice.add(die)
        return true
    }

    fun addCardToSupply(card: GameCard) {
        _supplyCards.add(card)
    }

    override fun addCardToHand(cardId: CardID) {
        gotCardIds.add(cardId)
    }

    fun addCardToHand(card: GameCard) {
        addCardToHand(card.id)
        _handCards.add(card)
    }

    override fun addDieToHand(die: Die) {
        diceInHand.add(die)
    }

    override fun addCardToCompost(cardID: CardID) {
        gotCardIds.add(cardID)
    }

    fun addCardToCompost(card: GameCard) {
        _compostCards.add(card)
        addCardToCompost(card.id)
    }

    override fun addDieToCompost(die: Die) {
        diceInCompost.add(die)
    }

    // Game flow methods
    override fun setupInitialDeck(seedlings: GameCards) {
        _supplyCards.addAll(seedlings)
    }

    override fun draw(preferredCardCount: Int) {
        val cards = _supplyCards.take(preferredCardCount)
        _supplyCards.removeAll(cards)
        _handCards.addAll(cards)
        val diceCount = HAND_SIZE - cards.size
        val dice = diceInSupply.dice.take(diceCount)
        dice.forEach {
            diceInSupply.remove(it)
            diceInHand.add(it)
        }
    }

    override fun drawHand() {
        draw(drawCountDecision.cardCount)
    }

    override fun drawCard(): CardID? = respondWithDrawCard
    override fun drawDie(): Die? = respondWithDrawDie
    override fun drawBestDie(): Die? = respondWithDrawBestDie
    override fun drawCardFromCompost(): CardID? = respondWithDrawCardFromCompost
    override fun drawDieFromCompost(): Die? = respondWithDrawDieFromCompost
    override fun drawBestDieFromCompost(): Die? = respondWithDrawBestDieFromCompost
    override fun discardHand() {
        _compostCards.addAll(cardsInHand)
        _handCards.clear()
        diceInCompost.addAll(diceInHand.dice)
        diceInHand.clear()
    }

    override fun reset() {}

    override fun resupply() {
        _supplyCards.addAll(_compostCards)
        diceInSupply.addAll(diceInCompost.dice)
        _compostCards.clear()
        diceInCompost.clear()
    }

    override fun toString(): String {
        return "Player('$name')"
    }

    override fun trashSeedlingCards() {
        cardsInHand.removeAll { it.type == FlourishType.SEEDLING }
        cardsInCompost.removeAll { it.type == FlourishType.SEEDLING }
        cardsInSupply.removeAll { it.type == FlourishType.SEEDLING }
    }

    fun clearCardsInCompost() {
        cardsInCompost.clear()
    }

    fun clearCardsInSupply() {
        cardsInSupply.clear()
    }

    fun clearDiceInHand() {
        diceInHand.clear()
    }

    fun clearDiceInCompost() {
        diceInCompost.clear()
    }

    fun clearDiceInSupply() {
        diceInSupply.clear()
    }


}
