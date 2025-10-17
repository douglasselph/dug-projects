package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.CostScore
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.list.GameCards
import dugsolutions.leaf.common.domain.Butterfly
import dugsolutions.leaf.common.domain.Insect
import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.CreatureManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.domain.DrawCardResult
import dugsolutions.leaf.player.domain.DrawDieResult
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.player.domain.PlayerScore
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.die.Dice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    private lateinit var mockDeckManager: DeckManager
    private lateinit var mockCardManager: CardManager
    private lateinit var mockCreatureManager: CreatureManager
    private lateinit var mockInsectManager: InsectManager
    private lateinit var mockButterflyManager: ButterflyManager
    private lateinit var mockDieFactory: DieFactory
    private lateinit var mockCostScore: CostScore
    private lateinit var mockDecisionDirector: DecisionDirector
    private lateinit var SUT: Player

    private lateinit var randomizer: Randomizer
    private lateinit var dieFactory: DieFactory
    private lateinit var sampleDie: SampleDie
    private lateinit var sampleD4: Die
    private lateinit var sampleD6: Die
    private lateinit var sampleD8: Die
    private lateinit var mockCard: GameCard

    companion object {
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
        private const val DAMAGE_AMOUNT = 5
        private const val PIP_MODIFIER = 3
    }

    @BeforeEach
    fun setup() {
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactory(randomizer)
        sampleDie = SampleDie(randomizer)

        // Create sample dice
        sampleD4 = dieFactory(DieSides.D4)
        sampleD6 = dieFactory(DieSides.D6)
        sampleD8 = dieFactory(DieSides.D8)

        mockDeckManager = mockk(relaxed = true)
        mockCardManager = mockk(relaxed = true)
        mockCreatureManager = mockk(relaxed = true)
        mockInsectManager = mockk(relaxed = true)
        mockButterflyManager = mockk(relaxed = true)
        mockDieFactory = mockk(relaxed = true)
        mockCostScore = mockk(relaxed = true)
        mockDecisionDirector = mockk(relaxed = true)
        mockCard = mockk(relaxed = true)

        SUT = Player(
            mockDeckManager,
            mockCardManager,
            mockCreatureManager,
            mockInsectManager,
            mockButterflyManager,
            mockDieFactory,
            mockCostScore,
            mockDecisionDirector
        )
        every { mockCardManager.getCard(any<CardID>()) } returns mockCard
    }

    @Test
    fun initialize_setsNameAndInitializesDecisionDirector() {
        // Arrange
        val id = "Player " + SUT.id

        // Act
        val result = SUT.initialize()

        // Assert
        assertEquals(id, SUT.name)
        verify { mockDecisionDirector.initialize(SUT) }
        assertEquals(SUT, result)
    }

    @Test
    fun id_isIncrementedForEachPlayer() {
        // Arrange
        val player2 = Player(
            mockDeckManager, mockCardManager, mockCreatureManager,
            mockInsectManager, mockButterflyManager, mockDieFactory,
            mockCostScore, mockDecisionDirector
        )
        val expectedId = SUT.id + 1

        // Assert
        assertEquals(expectedId, player2.id)
    }

    @Test
    fun isResupplyNeeded_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns true

        // Act
        val result = SUT.isResupplyNeeded

        // Assert
        assertTrue(result)
    }

    @Test
    fun handSize_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.handSize } returns 5

        // Act
        val result = SUT.handSize

        // Assert
        assertEquals(5, result)
    }

    @Test
    fun pipTotal_includesPipModifier() {
        // Arrange
        every { mockDeckManager.pipTotal } returns 10
        SUT.pipModifier = PIP_MODIFIER

        // Act
        val result = SUT.pipTotal

        // Assert
        assertEquals(13, result)
    }

    @Test
    fun diceTotal_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.pipTotal } returns 15

        // Act
        val result = SUT.diceTotal

        // Assert
        assertEquals(15, result)
    }

    @Test
    fun cardsInHand_delegatesToDeckManager() {
        // Arrange
        val expectedCards = listOf(CARD_ID_1, CARD_ID_2)
        every { mockDeckManager.cardsInHand } returns expectedCards

        // Act
        val result = SUT.cardsInHand

        // Assert
        assertEquals(expectedCards, result)
    }

    @Test
    fun cardsInHand_withGameCards_returnsGameCards() {
        // Arrange
        val cardIds = listOf(CARD_ID_1, CARD_ID_2)
        val gameCard1 = mockk<GameCard>(relaxed = true)
        val gameCard2 = mockk<GameCard>(relaxed = true)
        every { mockDeckManager.cardsInHand } returns cardIds
        every { mockCardManager.getCard(CARD_ID_1) } returns gameCard1
        every { mockCardManager.getCard(CARD_ID_2) } returns gameCard2

        // Act
        val result = SUT.cardsInHand()

        // Assert
        assertEquals(listOf(gameCard1, gameCard2), result)
    }

    @Test
    fun cardsInSupplyCount_returnsCorrectCount() {
        // Arrange
        val supplyItems = listOf(
            HandItem.aCard(mockk<GameCard>().apply { every { id } returns CARD_ID_1 }),
            HandItem.aCard(mockk<GameCard>().apply { every { id } returns CARD_ID_2 }),
            HandItem.aDie(mockk<Die>())
        )
        every { mockDeckManager.getItemsInSupply() } returns supplyItems

        // Act
        val result = SUT.cardsInSupplyCount

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun diceInSupplyCount_returnsCorrectCount() {
        // Arrange
        val supplyItems = listOf(
            HandItem.aCard(mockCardgit ),
            HandItem.aDie(sampleD6),
            HandItem.aDie(sampleD4)
        )
        every { mockDeckManager.getItemsInSupply() } returns supplyItems

        // Act
        val result = SUT.diceInSupplyCount

        // Assert
        assertEquals(2, result)
    }

    @Test
    fun totalDiceCount_calculatesCorrectly() {
        // Arrange
        every { mockDeckManager.getItemsInSupply() } returns listOf(HandItem.aDie(sampleD6), HandItem.aDie(sampleD6), HandItem.aDie(sampleD6))
        every { mockDeckManager.getItemsInDiscardPile() } returns listOf(HandItem.aDie(sampleD6), HandItem.aDie(sampleD6))
        every { mockDeckManager.getItemsInHand() } returns listOf(HandItem.aDie(sampleD6))

        // Act
        val result = SUT.totalDiceCount

        // Assert
        assertEquals(6, result)
    }

    @Test
    fun totalCardCount_calculatesCorrectly() {
        // Arrange
        every { mockDeckManager.getItemsInSupply() } returns listOf(HandItem.aCard(mockCard), HandItem.aCard(mockCard), HandItem.aCard(mockCard))
        every { mockDeckManager.getItemsInDiscardPile() } returns listOf(HandItem.aCard(mockCard), HandItem.aCard(mockCard), HandItem.aCard(mockCard))
        every { mockDeckManager.cardsInHand } returns listOf(1, 2, 3, 4, 5, 6, 7)
        val expectedCount = 7 + 3 + 3

        // Act
        val result = SUT.totalCardCount

        // Assert
        assertEquals(expectedCount, result)
    }

    @Test
    fun score_calculatesCorrectly() {
        // Arrange
        val mockDice = mockk<Dice>(relaxed = true)
        every { mockDice.totalSides } returns 20

        every { mockDeckManager.getItemsInSupply() } returns listOf(HandItem.aCard(mockCard), HandItem.aCard(mockCard))
        every { mockDeckManager.getItemsInDiscardPile() } returns listOf(HandItem.aCard(mockCard), HandItem.aCard(mockCard), HandItem.aCard(mockCard))
        every { mockDeckManager.cardsInHand } returns listOf(1, 2, 3)
        every { mockDeckManager.allDice } returns mockDice

        every { mockCostScore(mockCard.cost) } returns 5
        val expectedCardScore = 8 * 5 // 8 cards total * 5 points per card

        // Act
        val result = SUT.score

        // Assert
        assertEquals(SUT.id, result.playerId)
        assertEquals(20, result.scoreDice)
        assertEquals(expectedCardScore, result.scoreCards)
    }

    @Test
    fun hasCardInHand_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.hasCardInHand(CARD_ID_1) } returns true

        // Act
        val result = SUT.hasCardInHand(CARD_ID_1)

        // Assert
        assertTrue(result)
    }

    @Test
    fun hasDieInHand_delegatesToDeckManager() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        every { mockDeckManager.hasDieInHand(mockDie) } returns true

        // Act
        val result = SUT.hasDieInHand(mockDie)

        // Assert
        assertTrue(result)
    }

    @Test
    fun discard_withCardId_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.discard(CARD_ID_1) } returns true

        // Act
        val result = SUT.discard(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.discard(CARD_ID_1) }
    }

    @Test
    fun discard_withDie_delegatesToDeckManager() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        every { mockDeckManager.discard(mockDie) } returns true

        // Act
        val result = SUT.discard(mockDie)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.discard(mockDie) }
    }

    @Test
    fun discard_withDieValue_delegatesToDeckManager() {
        // Arrange
        val dieValue = DieValue(4, 4) // D4 with value 4
        every { mockDeckManager.discard(dieValue) } returns true

        // Act
        val result = SUT.discard(dieValue)

        // Assert
        assertTrue(result)
        verify { mockDeckManager.discard(dieValue) }
    }

    @Test
    fun retainCard_whenCardInHand_retainsSuccessfully() {
        // Arrange
        val mockCard = mockk<GameCard>(relaxed = true)
        every { mockCard.id } returns CARD_ID_1
        every { SUT.hasCardInHand(CARD_ID_1) } returns true
        every { SUT.removeCardFromHand(CARD_ID_1) } returns true

        // Act
        val result = SUT.retainCard(mockCard)

        // Assert
        assertTrue(result)
        assertTrue(SUT.retained.contains(HandItem.aCard(mockCard)))
    }

    @Test
    fun retainCard_whenCardNotInHand_returnsFalse() {
        // Arrange
        val mockCard = mockk<GameCard>(relaxed = true)
        every { mockCard.id } returns CARD_ID_1
        every { SUT.hasCardInHand(CARD_ID_1) } returns false

        // Act
        val result = SUT.retainCard(mockCard)

        // Assert
        assertFalse(result)
    }

    @Test
    fun retainDie_whenDieInHand_retainsSuccessfully() {
        // Arrange
        every { SUT.hasDieInHand(sampleD4) } returns true
        every { SUT.removeDieFromHand(sampleD4) } returns true

        // Act
        val result = SUT.retainDie(sampleD4)

        // Assert
        assertTrue(result)
        assertTrue(SUT.retained.contains(HandItem.aDie(sampleD4)))
    }

    @Test
    fun addCardToCreature_delegatesToCreatureManager() {
        // Arrange
        every { mockCreatureManager.addCard(CARD_ID_1) } returns true

        // Act
        val result = SUT.addCardToCreature(CARD_ID_1)

        // Assert
        assertTrue(result)
        verify { mockCreatureManager.addCard(CARD_ID_1) }
    }

    @Test
    fun addDieToCreature_delegatesToCreatureManager() {
        // Arrange
        every { mockCreatureManager.addDie(sampleD6) } returns true

        // Act
        val result = SUT.addDieToCreature(sampleD6)

        // Assert
        assertTrue(result)
        verify { mockCreatureManager.addDie(sampleD6) }
    }

    @Test
    fun creatureLeafCards_delegatesToCreatureManager() {
        // Arrange
        val mockCards = listOf(mockk<GameCard>(relaxed = true), mockk<GameCard>(relaxed = true))
        every { mockCreatureManager.leafCards } returns mockCards

        // Act
        val result = SUT.creatureLeafCards

        // Assert
        assertEquals(mockCards, result)
    }

    @Test
    fun addInsect_delegatesToInsectManager() {
        // Arrange
        val mockInsect = mockk<Insect>(relaxed = true)
        every { mockInsectManager.add(mockInsect) } returns true

        // Act
        val result = SUT.addInsect(mockInsect)

        // Assert
        assertTrue(result)
        verify { mockInsectManager.add(mockInsect) }
    }

    @Test
    fun removeInsect_delegatesToInsectManager() {
        // Arrange
        val mockInsect = mockk<Insect>(relaxed = true)
        every { mockInsectManager.remove(mockInsect) } returns true

        // Act
        val result = SUT.removeInsect(mockInsect)

        // Assert
        assertTrue(result)
        verify { mockInsectManager.remove(mockInsect) }
    }

    @Test
    fun countInsect_delegatesToInsectManager() {
        // Arrange
        val mockInsect = mockk<Insect>(relaxed = true)
        every { mockInsectManager.countOf(mockInsect) } returns 3

        // Act
        val result = SUT.count(mockInsect)

        // Assert
        assertEquals(3, result)
        verify { mockInsectManager.countOf(mockInsect) }
    }

    @Test
    fun addButterfly_delegatesToButterflyManager() {
        // Arrange
        val mockButterfly = mockk<Butterfly>(relaxed = true)
        every { mockButterflyManager.add(mockButterfly) } returns true

        // Act
        val result = SUT.addButterfly(mockButterfly)

        // Assert
        assertTrue(result)
        verify { mockButterflyManager.add(mockButterfly) }
    }

    @Test
    fun removeButterfly_delegatesToButterflyManager() {
        // Arrange
        val mockButterfly = mockk<Butterfly>(relaxed = true)
        every { mockButterflyManager.remove(mockButterfly) } returns true

        // Act
        val result = SUT.removeButterfly(mockButterfly)

        // Assert
        assertTrue(result)
        verify { mockButterflyManager.remove(mockButterfly) }
    }

    @Test
    fun hasButterfly_delegatesToButterflyManager() {
        // Arrange
        val mockButterfly = mockk<Butterfly>(relaxed = true)
        every { mockButterflyManager.has(mockButterfly) } returns true

        // Act
        val result = SUT.has(mockButterfly)

        // Assert
        assertTrue(result)
        verify { mockButterflyManager.has(mockButterfly) }
    }

    @Test
    fun setupInitialDeck_delegatesToDeckManager() {
        // Arrange
        val mockSeedlings = mockk<GameCards>(relaxed = true)
        val mockStartingDice = listOf(mockk<Die>(relaxed = true))
        every { mockDieFactory.startingDice } returns mockStartingDice

        // Act
        SUT.setupInitialDeck(mockSeedlings)

        // Assert
        verify { mockDeckManager.setup(mockSeedlings, mockStartingDice) }
    }

    @Test
    fun drawCard_whenNoResupplyNeeded_returnsCardWithoutReshuffle() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns false
        every { mockDeckManager.drawCard() } returns CARD_ID_1

        // Act
        val result = SUT.drawCard()

        // Assert
        assertEquals(CARD_ID_1, result.cardId)
        assertFalse(result.reshuffleDone)
    }

    @Test
    fun drawCard_whenResupplyNeeded_returnsCardWithReshuffle() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns true
        every { mockDeckManager.drawCard() } returns CARD_ID_1
        every { mockDeckManager.resupply() } returns true

        // Act
        val result = SUT.drawCard()

        // Assert
        assertEquals(CARD_ID_1, result.cardId)
        assertTrue(result.reshuffleDone)
    }

    @Test
    fun drawDie_whenNoResupplyNeeded_returnsDieWithoutReshuffle() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns false
        every { mockDeckManager.drawDie() } returns sampleD6

        // Act
        val result = SUT.drawDie()

        // Assert
        assertEquals(sampleD6, result.die)
        assertFalse(result.reshuffleDone)
    }

    @Test
    fun drawDie_whenResupplyNeeded_returnsDieWithReshuffle() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns true
        every { mockDeckManager.drawDie() } returns sampleD8
        every { mockDeckManager.resupply() } returns true

        // Act
        val result = SUT.drawDie()

        // Assert
        assertEquals(sampleD8, result.die)
        assertTrue(result.reshuffleDone)
    }

    @Test
    fun drawBestDie_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.isResupplyNeeded } returns false
        every { mockDeckManager.drawBestDie() } returns sampleD6

        // Act
        val result = SUT.drawBestDie()

        // Assert
        assertEquals(sampleD6, result.die)
        verify { mockDeckManager.drawBestDie() }
    }

    @Test
    fun drawCardFromDiscard_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.drawCardFromDiscard() } returns CARD_ID_1

        // Act
        val result = SUT.drawCardFromDiscard()

        // Assert
        assertEquals(CARD_ID_1, result.cardId)
        verify { mockDeckManager.drawCardFromDiscard() }
    }

    @Test
    fun drawDieFromDiscard_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.drawDieFromDiscard() } returns sampleD4

        // Act
        val result = SUT.drawDieFromDiscard()

        // Assert
        assertEquals(sampleD4, result.die)
        verify { mockDeckManager.drawDieFromDiscard() }
    }

    @Test
    fun drawBestDieFromDiscard_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.drawBestDieFromDiscard() } returns sampleD8

        // Act
        val result = SUT.drawBestDieFromDiscard()

        // Assert
        assertEquals(sampleD8, result.die)
        verify { mockDeckManager.drawBestDieFromDiscard() }
    }

    @Test
    fun discardHand_delegatesToDeckManager() {
        // Act
        SUT.discardHand()

        // Assert
        verify { mockDeckManager.discardHand() }
    }

    @Test
    fun resupply_delegatesToDeckManager() {
        // Arrange
        every { mockDeckManager.resupply() } returns true

        // Act
        val result = SUT.resupply()

        // Assert
        assertTrue(result)
        verify { mockDeckManager.resupply() }
    }

    @Test
    fun reset_discardsHandAndResuppliesAndClearsEffects() {
        // Arrange
        every { mockDeckManager.discardHand() } returns Unit
        every { mockDeckManager.resupply() } returns true

        // Act
        SUT.reset()

        // Assert
        verify { mockDeckManager.discardHand() }
        verify { mockDeckManager.resupply() }
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
        assertTrue(SUT.retained.isEmpty())
        assertTrue(SUT.reused.isEmpty())
        assertTrue(SUT.cardsToPlay.isEmpty())
    }

    @Test
    fun clearEffects_resetsAllEffectValues() {
        // Arrange
        SUT.incomingDamage = DAMAGE_AMOUNT
        SUT.deflectDamage = 3
        SUT.pipModifier = PIP_MODIFIER
        SUT.retained.add(HandItem.aCard(mockk<GameCard>(relaxed = true)))
        SUT.reused.add(HandItem.aDie(mockk<Die>(relaxed = true)))
        SUT.cardsToPlay.add(mockk<GameCard>(relaxed = true))

        // Act
        SUT.clearEffects()

        // Assert
        assertEquals(0, SUT.incomingDamage)
        assertEquals(0, SUT.deflectDamage)
        assertEquals(0, SUT.pipModifier)
        assertTrue(SUT.retained.isEmpty())
        assertTrue(SUT.reused.isEmpty())
        assertTrue(SUT.cardsToPlay.isEmpty())
    }

    @Test
    fun addCardsToHand_addsMultipleCards() {
        // Arrange
        val cardIds = listOf(CARD_ID_1, CARD_ID_2, CARD_ID_3)
        every { SUT.addCardToHand(any()) } returns true

        // Act
        SUT.addCardsToHand(cardIds)

        // Assert
        verify { SUT.addCardToHand(CARD_ID_1) }
        verify { SUT.addCardToHand(CARD_ID_2) }
        verify { SUT.addCardToHand(CARD_ID_3) }
    }

    @Test
    fun addDiceToHand_addsMultipleDice() {
        // Arrange
        val dice = listOf(sampleD4, sampleD6)
        every { SUT.addDieToHand(any<Die>()) } returns true

        // Act
        SUT.addDiceToHand(dice)

        // Assert
        verify { SUT.addDieToHand(sampleD4) }
        verify { SUT.addDieToHand(sampleD6) }
    }
}
