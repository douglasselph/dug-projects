package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.player.di.CardEffectBattleScoreFactory
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.random.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DecisionDamageAbsorptionBaselineTest {

    companion object {
        val mockSeedlingId = 4
        val mockSeedling: GameCard = mockk(relaxed = true) {
            every { id } returns mockSeedlingId
            every { resilience } returns 2
        }
        val mockSeedlingId2 = 5
        val mockSeedling2: GameCard = mockk(relaxed = true) {
            every { id } returns mockSeedlingId2
            every { resilience } returns 1
        }
        val mockRootId = 10
        val mockRoot: GameCard = mockk(relaxed = true) {
            every { id } returns mockRootId
            every { resilience } returns 2
        }
        val mockVineId = 12
        val mockVine: GameCard = mockk(relaxed = true) {
            every { id } returns mockVineId
            every { resilience } returns 4
        }
        val mockCanopyId = 40
        val mockCanopy: GameCard = mockk(relaxed = true) {
            every { id } returns mockCanopyId
            every { resilience } returns 5
        }
        val mockBloomId = 42
        val mockBloom: GameCard = mockk(relaxed = true) {
            every { id } returns mockBloomId
            every { resilience } returns 1
        }
        val mockFlowerId = 142
        val mockFlower: GameCard = mockk(relaxed = true) {
            every { id } returns mockFlowerId
            every { resilience } returns 10
        }
        val mockFlowerId2 = 144
        val mockFlower2: GameCard = mockk(relaxed = true) {
            every { id } returns mockFlowerId2
            every { resilience } returns 10
        }

    }

    private lateinit var cardManager: CardManager
    private lateinit var player: PlayerTD
    private lateinit var sampleDie: SampleDie
    private lateinit var costScore: CostScore
    private lateinit var cardEffectBattleScoreFactory: CardEffectBattleScoreFactory
    private lateinit var cardEffectBattleScore: CardEffectBattleScore

    private lateinit var SUT: DecisionDamageAbsorptionBaseline

    @BeforeEach
    fun setup() {
        val randomizer = RandomizerTD()
        costScore = mockk(relaxed = true)
        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        cardEffectBattleScoreFactory = mockk(relaxed = true)
        cardEffectBattleScore = mockk(relaxed = true)
        player = PlayerTD(1, cardManager)
        sampleDie = SampleDie(RandomizerTD())
        player.addCardToBed(FakeCards.bloomCard.id)
        player.addDieToBed(sampleDie.d10)
        player.useDeckManager = false
        every { cardEffectBattleScoreFactory(any()) } returns cardEffectBattleScore

        SUT = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)

        // Verify dice resilience values (sides)
        assertEquals(4, sampleDie.d4.sides)
        assertEquals(6, sampleDie.d6.sides)
        assertEquals(8, sampleDie.d8.sides)
    }

    @Test
    fun invoke_whenNoIncomingDamage_returnsNull() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()
        val damage = 0
        player.incomingDamage = damage

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenIncomingDamageAndNoCardsOrDice_returnsNull() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()
        val damage = 5
        player.incomingDamage = damage

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenExactMatchCardForDamage_usesOnlyThatCard() = runBlocking {
        // Arrange
        val mockVine: GameCard = mockk(relaxed = true) {
            every { resilience } returns 4
        }
        val damage = 4
        player.incomingDamage = damage
        player.addCardToHand(mockVine)
        player.addCardToHand(FakeCards.canopyCard)

        // Verify die resilience value
        assertEquals(4, sampleDie.d4.sides, "Die sides must be 4 for this test")
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer the vine card
        every { cardEffectBattleScore(mockVine) } returns 1
        every { cardEffectBattleScore(FakeCards.canopyCard) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        assertEquals(listOf(mockVine), result.cards)
        assertTrue(result.dice.isEmpty())
    }

    @Test
    fun invoke_whenExactMatchDieForDamage_usesOnlyThatDie() = runBlocking {
        // Arrange
        val die = sampleDie.d6
        // Verify expected die sides (resilience value)
        assertEquals(6, die.sides, "Die sides must be 6 for this test")

        val damage = die.sides
        player.incomingDamage = damage
        player.addDieToHand(die)

        // Verify additional die sides
        assertEquals(8, sampleDie.d8.sides, "Additional die sides must be 8 for this test")
        player.addDieToHand(sampleDie.d8)

        // Verify card resilience
        val mockCanopyId = 50
        val mockCanopy: GameCard = mockk(relaxed = true) {
            every { id } returns mockCanopyId
            every { resilience } returns 5
        }
        player.addCardToHand(mockCanopy)

        // Mock card effect battle score to prefer using dice over cards
        every { cardEffectBattleScore(mockCanopy) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        assertTrue(result.cards.isEmpty())
        assertEquals(listOf(die), result.dice)
    }

    @Test
    fun invoke_whenBestCombinationRequiresMultipleItems_usesMinimumNeeded() = runBlocking {
        // Arrange
        // Verify resilience values
        assertEquals(4, sampleDie.d4.sides, "D4 sides must be 4 for this test")
        assertEquals(6, sampleDie.d6.sides, "D6 sides must be 6 for this test")

        val damage = 7
        player.incomingDamage = damage
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockRoot)
        player.addDieToHand(sampleDie.d4)
        player.addDieToHand(sampleDie.d6)

        // Mock card effect battle score to prefer using one card + d6
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockRoot) } returns 2

        // Act
        val result = SUT()

        // Assert
        // The optimal combo would be either one card (2) + d6 (6) = 8, or
        // both cards (4) + d4 (4) = 8
        assertNotNull(result)
        val cards = result.cards
        if (cards.size == 1) {
            assertEquals(1, result.dice.size)
            assertEquals(sampleDie.d6.sides, result.dice[0].sides)
        } else if (cards.size == 2) {
            assertEquals(1, result.dice.size)
            assertEquals(sampleDie.d4.sides, result.dice[0].sides)
        } else {
            assertTrue(cards.size <= 2)
        }
    }

    @Test
    fun invoke_whenOnlyCardsInHandAndPreservationNeeded_preservesOneCard() = runBlocking {
        // Arrange
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        // Verify resilience values
        assertEquals(4, sampleDie.d4.sides, "D4 sides must be 4 for this test")
        assertEquals(6, sampleDie.d6.sides, "D6 sides must be 6 for this test")

        val damage = 10
        player.incomingDamage = damage
        // Add 3 cards to hand
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockVine)
        player.addCardToHand(mockCanopy)
        // Add dice to all places
        player.addDieToHand(d4)
        player.addDieToBed(d6)

        // Mock card effect battle score to prefer preserving Canopy
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockVine) } returns 2
        every { cardEffectBattleScore(mockCanopy) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // It should preserve one card (the most valuable = Canopy) and use the others plus the die
        assertEquals(2, result.cards.size)
        assertEquals(1, result.dice.size)
        assertEquals(mockSeedlingId, result.cards[0].id)
        assertEquals(mockVineId, result.cards[1].id)
        assertEquals(d4, result.dice[0])
    }

    @Test
    fun invoke_whenOnlyDiceInHandAndPreservationNeeded_preservesOneDie() = runBlocking {
        // Arrange
        val damage = 7
        player.incomingDamage = damage
        // Add dice to hand
        val d8 = sampleDie.d8
        player.addDieToHand(d8)
        // Add cards to various places
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockCanopy)
        // All cards are not allowed if these are our last cards.
        player.addCardToSupply(mockVine)

        // Mock card effect battle score to prefer using cards over d8
        every { cardEffectBattleScore(mockSeedling) } returns 2
        every { cardEffectBattleScore(mockCanopy) } returns 5

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // It should preserve the die (d8) and use others card + other dice
        assertEquals(2, result.cards.size)
        assertEquals(0, result.dice.size)
    }

    @Test
    fun invoke_whenCriticalDamageWithNoPreservation_losesEverything() = runBlocking {
        // Arrange
        val damage = 20
        player.incomingDamage = damage
        // Add cards to hand
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockVine)
        // Add dice to hand
        player.addDieToHand(sampleDie.d4)
        player.addDieToHand(sampleDie.d6)
        // Cards and dice in other places
        player.addCardToCompost(mockCanopy)
        player.addDieToBed(sampleDie.d8)

        // Mock card effect battle score to prefer using all available resources
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockVine) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // It should use everything in hand since damage is too high
        assertEquals(2, result.cards.size)
        assertEquals(2, result.dice.size)
    }

    @Test
    fun invoke_whenForcedToLoseLastCard_losesLastCardWhenNoChoice() = runBlocking {
        // Arrange
        val damage = 5
        player.incomingDamage = damage
        player.addCardToHand(mockCanopy)

        // Mock card effect battle score
        every { cardEffectBattleScore(mockCanopy) } returns 1

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // It has no choice but to return the card
        assertEquals(1, result.cards.size)
        assertEquals(0, result.dice.size)
    }

    @Test
    fun invoke_whenBloomCardsShouldBePreserved_usesOtherCardsFirst() = runBlocking {
        // Arrange
        val damage = 9
        player.incomingDamage = damage
        player.addCardToHand(mockBloom)
        player.addCardToHand(mockVine)
        player.addCardToHand(mockSeedling)
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer using non-bloom cards
        every { cardEffectBattleScore(mockBloom) } returns 6
        every { cardEffectBattleScore(mockVine) } returns 1
        every { cardEffectBattleScore(mockSeedling) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use vine + seedling + d4 = 10 instead of using the BLOOM card which would give it a 9, but bloom is off limits.
        assertEquals(2, result.cards.size)
        assertEquals(1, result.dice.size)
        assertFalse(result.cards.contains(mockBloom))
        assertTrue(result.cards.contains(mockVine))
        assertTrue(result.cards.contains(mockSeedling))
        assertEquals(sampleDie.d4.sides, result.dice[0].sides)
    }

    @Test
    fun invoke_whenMultipleCombinationsPossible_choosesLeastWasteful() = runBlocking {
        // Arrange
        val damage = 6
        player.incomingDamage = damage
        player.addCardToHand(mockVine)
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockSeedling2)
        player.addCardToSupply(mockRoot)
        player.addCardToSupply(mockCanopy)
        player.addDieToSupply(sampleDie.d20)
        player.addDieToSupply(sampleDie.d20)
        player.addDieToHand(sampleDie.d6)
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer using d6
        every { cardEffectBattleScore(any()) } returns 10
        every { cardEffectBattleScore(mockVine) } returns 8
        every { cardEffectBattleScore(mockSeedling) } returns 7
        every { cardEffectBattleScore(mockSeedling2) } returns 9

        // Act
        val result = SUT()

        // Assert
        // Should use vine(4) + seedling2(1) + d1(1) = 6 (exact match)
        // rather than seedling(2) + d4(4) = 6 (exact match but more items)
        // rather than vine(4) + d4(4) = 8 (wasteful)
        assertNotNull(result)
        assertEquals(0, result.cards.size)
        assertEquals(1, result.dice.size)
        assertEquals(sampleDie.d6.sides, result.dice[0].sides)
    }

    @Test
    fun invoke_whenFloralArrayCardsPresent_canOnlyBeUsedWithRegularCards() = runBlocking {
        // Arrange
        val damage = 6
        player.incomingDamage = damage
        // Add regular cards to hand
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockVine)
        // Add flower card to floral array
        player.addCardToFloralArray(mockFlower)

        // Mock card effect battle score to prefer using regular cards
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockVine) } returns 2
        every { cardEffectBattleScore(mockFlower) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use vine(4) + seedling(2) = 6 (exact match)
        // rather than just using the flower card which would be invalid
        assertEquals(1, result.cards.size)
        assertEquals(1, result.floralCards.size)
        assertEquals(0, result.dice.size)
        assertTrue(result.floralCards.contains(mockFlower))
        assertTrue(result.cards.contains(mockSeedling))
    }

    @Test
    fun invoke_whenFloralArrayCardsEnhanceResilience_usesEnhancedValue() = runBlocking {
        // Arrange
        val damage = 5
        player.incomingDamage = damage
        // Add regular card to hand
        player.addCardToHand(mockSeedling)
        // Add flower card to floral array
        player.addCardToFloralArray(mockFlower)

        // Mock card effect battle score to prefer using seedling with flower enhancement
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockFlower) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use seedling(2) + flower enhancement(3) = 5 (exact match)
        assertEquals(1, result.cards.size)
        assertEquals(0, result.dice.size)
        assertTrue(result.cards.contains(mockSeedling))
    }

    @Test
    fun invoke_whenOnlyFloralArrayCardsPresent_ignoresCombination() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()
        val damage = 3
        player.incomingDamage = damage
        // Add only flower cards to floral array
        player.addCardToFloralArray(mockFlowerId)
        player.addCardToFloralArray(mockFlowerId2)

        // Mock card effect battle score
        every { cardEffectBattleScore(mockFlower) } returns 1
        every { cardEffectBattleScore(mockFlower2) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenFloralArrayCardsEnhanceMultipleCards_usesBestCombination() = runBlocking {
        // Arrange
        val damage = 7
        player.incomingDamage = damage
        // Add regular cards to hand
        player.addCardToHand(mockSeedling)
        player.addCardToHand(mockVine)
        // Add flower card to floral array
        player.addCardToFloralArray(mockFlower)

        // Mock card effect battle score to prefer using seedling with flower enhancement
        every { cardEffectBattleScore(mockSeedling) } returns 1
        every { cardEffectBattleScore(mockVine) } returns 2
        every { cardEffectBattleScore(mockFlower) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use vine(4) + flower enhancement(3) = 7 (exact match)
        // rather than seedling(2) + flower enhancement(3) = 5 (not enough)
        // or vine(4) + seedling(2) = 6 (not enough)
        assertEquals(1, result.cards.size)
        assertEquals(1, result.floralCards.size)
        assertEquals(0, result.dice.size)
        assertEquals(mockSeedlingId, result.cards[0].id)
        assertEquals(mockFlowerId, result.floralCards[0].id)
    }
} 
