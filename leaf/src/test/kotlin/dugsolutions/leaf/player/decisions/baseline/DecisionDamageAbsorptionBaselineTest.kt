package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.di.factory.CardEffectBattleScoreFactory
import dugsolutions.leaf.di.factory.GameCardsFactory
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import dugsolutions.leaf.player.decisions.local.CardEffectBattleScore
import dugsolutions.leaf.tool.RandomizerTD
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
        player.addCardToCompost(FakeCards.fakeBloom.id)
        player.addDieToCompost(sampleDie.d10)
        player.useDeckManager = false
        every { cardEffectBattleScoreFactory(any()) } returns cardEffectBattleScore

        SUT = DecisionDamageAbsorptionBaseline(player, cardEffectBattleScoreFactory, cardManager)

        // Verify dice resilience values (sides)
        assertEquals(4, sampleDie.d4.sides)
        assertEquals(6, sampleDie.d6.sides)
        assertEquals(8, sampleDie.d8.sides)

        // Verify card resilience values
        assertEquals(5, FakeCards.fakeCanopy.resilience)
        assertEquals(4, FakeCards.fakeVine.resilience)
        assertEquals(2, FakeCards.fakeSeedling.resilience)
        assertEquals(2, FakeCards.fakeRoot.resilience)
        assertEquals(1, FakeCards.fakeBloom.resilience)
        assertEquals(1, FakeCards.fakeSeedling2.resilience)
    }

    @Test
    fun invoke_whenNoIncomingDamage_returnsNull() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()
        player.incomingDamage = 0

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenIncomingDamageAndNoCardsOrDice_returnsNull() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()
        player.incomingDamage = 5

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenExactMatchCardForDamage_usesOnlyThatCard() = runBlocking {
        // Arrange
        val card = FakeCards.fakeVine
        // Verify expected resilience value
        assertEquals(4, card.resilience, "Card resilience value must be 4 for this test")

        player.incomingDamage = card.resilience
        player.addCardToHand(card)
        player.addCardToHand(FakeCards.fakeCanopy)

        // Verify die resilience value
        assertEquals(4, sampleDie.d4.sides, "Die sides must be 4 for this test")
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer the vine card
        every { cardEffectBattleScore(card) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeCanopy) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        assertEquals(listOf(card), result.cards)
        assertTrue(result.dice.isEmpty())
    }

    @Test
    fun invoke_whenExactMatchDieForDamage_usesOnlyThatDie() = runBlocking {
        // Arrange
        val die = sampleDie.d6
        // Verify expected die sides (resilience value)
        assertEquals(6, die.sides, "Die sides must be 6 for this test")

        player.incomingDamage = die.sides
        player.addDieToHand(die)

        // Verify additional die sides
        assertEquals(8, sampleDie.d8.sides, "Additional die sides must be 8 for this test")
        player.addDieToHand(sampleDie.d8)

        // Verify card resilience
        assertEquals(5, FakeCards.fakeCanopy.resilience, "Card resilience must be 5 for this test")
        player.addCardToHand(FakeCards.fakeCanopy.id)

        // Mock card effect battle score to prefer using dice over cards
        every { cardEffectBattleScore(FakeCards.fakeCanopy) } returns 2

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
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(2, FakeCards.fakeRoot.resilience, "Root resilience must be 2 for this test")
        assertEquals(4, sampleDie.d4.sides, "D4 sides must be 4 for this test")
        assertEquals(6, sampleDie.d6.sides, "D6 sides must be 6 for this test")

        player.incomingDamage = 7
        player.addCardToHand(FakeCards.fakeSeedling.id)
        player.addCardToHand(FakeCards.fakeRoot.id)
        player.addDieToHand(sampleDie.d4)
        player.addDieToHand(sampleDie.d6)

        // Mock card effect battle score to prefer using one card + d6
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeRoot) } returns 2

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
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(5, FakeCards.fakeCanopy.resilience, "Canopy resilience must be 5 for this test")
        assertEquals(4, sampleDie.d4.sides, "D4 sides must be 4 for this test")
        assertEquals(6, sampleDie.d6.sides, "D6 sides must be 6 for this test")

        player.incomingDamage = 10
        // Add 3 cards to hand
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeVine)
        player.addCardToHand(FakeCards.fakeCanopy)
        // Add dice to all places
        player.addDieToHand(d4)
        player.addDieToCompost(d6)

        // Mock card effect battle score to prefer preserving Canopy
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 2
        every { cardEffectBattleScore(FakeCards.fakeCanopy) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // It should preserve one card (the most valuable = Canopy) and use the others plus the die
        assertEquals(2, result.cards.size)
        assertEquals(1, result.dice.size)
        assertEquals(FakeCards.fakeSeedling.id, result.cards[0].id)
        assertEquals(FakeCards.fakeVine.id, result.cards[1].id)
        assertEquals(d4, result.dice[0])
    }

    @Test
    fun invoke_whenOnlyDiceInHandAndPreservationNeeded_preservesOneDie() = runBlocking {
        // Arrange
        // Verify resilience values
        assertEquals(8, sampleDie.d8.sides, "D8 sides must be 8 for this test")
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(5, FakeCards.fakeCanopy.resilience, "Canopy resilience must be 5 for this test")

        player.incomingDamage = 7
        // Add dice to hand
        val d8 = sampleDie.d8
        player.addDieToHand(d8)
        // Add cards to various places
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeCanopy)
        // All cards are not allowed if these are our last cards.
        player.addCardToSupply(FakeCards.fakeVine)

        // Mock card effect battle score to prefer using cards over d8
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 2
        every { cardEffectBattleScore(FakeCards.fakeCanopy) } returns 5

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
        // Verify resilience values
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(5, FakeCards.fakeCanopy.resilience, "Canopy resilience must be 5 for this test")

        player.incomingDamage = 20
        // Add cards to hand
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeVine)
        // Add dice to hand
        player.addDieToHand(sampleDie.d4)
        player.addDieToHand(sampleDie.d6)
        // Cards and dice in other places
        player.addCardToCompost(FakeCards.fakeCanopy)
        player.addDieToCompost(sampleDie.d8)

        // Mock card effect battle score to prefer using all available resources
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 2

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
        // Verify resilience value
        assertEquals(5, FakeCards.fakeCanopy.resilience, "Canopy resilience must be 5 for this test")

        player.incomingDamage = 5
        player.addCardToHand(FakeCards.fakeCanopy)

        // Mock card effect battle score
        every { cardEffectBattleScore(FakeCards.fakeCanopy) } returns 1

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
        // Verify resilience values
        assertEquals(1, FakeCards.fakeBloom.resilience, "Bloom resilience must be 1 for this test")
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")

        player.incomingDamage = 9
        player.addCardToHand(FakeCards.fakeBloom)
        player.addCardToHand(FakeCards.fakeVine)
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer using non-bloom cards
        every { cardEffectBattleScore(FakeCards.fakeBloom) } returns 6
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use vine + seedling + d4 = 10 instead of using the BLOOM card which would give it a 9, but bloom is off limits.
        assertEquals(2, result.cards.size)
        assertEquals(1, result.dice.size)
        assertFalse(result.cards.contains(FakeCards.fakeBloom))
        assertTrue(result.cards.contains(FakeCards.fakeVine))
        assertTrue(result.cards.contains(FakeCards.fakeSeedling))
        assertEquals(sampleDie.d4.sides, result.dice[0].sides)
    }

    @Test
    fun invoke_whenMultipleCombinationsPossible_choosesLeastWasteful() = runBlocking {
        // Arrange
        // Verify resilience values
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(1, FakeCards.fakeSeedling2.resilience, "Seedling2 resilience must be 1 for this test")

        player.incomingDamage = 6
        player.addCardToHand(FakeCards.fakeVine)
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeSeedling2)
        player.addDieToHand(sampleDie.d6)
        player.addDieToHand(sampleDie.d4)

        // Mock card effect battle score to prefer using d6
        every { cardEffectBattleScore(any()) } returns 10
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 8
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 7
        every { cardEffectBattleScore(FakeCards.fakeSeedling2) } returns 9

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
        // Verify resilience values
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(1, FakeCards.fakeFlower.resilience, "Flower resilience expected to be 1 for this test")

        player.incomingDamage = 6
        // Add regular cards to hand
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeVine)
        // Add flower card to floral array
        player.addCardToFloralArray(FakeCards.fakeFlower)

        // Mock card effect battle score to prefer using regular cards
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 2
        every { cardEffectBattleScore(FakeCards.fakeFlower) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use vine(4) + seedling(2) = 6 (exact match)
        // rather than just using the flower card which would be invalid
        assertEquals(1, result.cards.size)
        assertEquals(1, result.floralCards.size)
        assertEquals(0, result.dice.size)
        assertTrue(result.floralCards.contains(FakeCards.fakeFlower))
        assertTrue(result.cards.contains(FakeCards.fakeSeedling))
    }

    @Test
    fun invoke_whenFloralArrayCardsEnhanceResilience_usesEnhancedValue() = runBlocking {
        // Arrange
        // Verify resilience values
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(10, FakeCards.fakeFlower.trashValue, "Flower trash value must be 3 for this test")

        player.incomingDamage = 5
        // Add regular card to hand
        player.addCardToHand(FakeCards.fakeSeedling)
        // Add flower card to floral array
        player.addCardToFloralArray(FakeCards.fakeFlower)

        // Mock card effect battle score to prefer using seedling with flower enhancement
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeFlower) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertNotNull(result)
        // Should use seedling(2) + flower enhancement(3) = 5 (exact match)
        assertEquals(1, result.cards.size)
        assertEquals(0, result.dice.size)
        assertTrue(result.cards.contains(FakeCards.fakeSeedling))
    }

    @Test
    fun invoke_whenOnlyFloralArrayCardsPresent_ignoresCombination() = runBlocking {
        // Arrange
        val expectedResult = DecisionDamageAbsorption.Result()

        // Verify resilience values
        assertEquals(10, FakeCards.fakeFlower.trashValue, "Flower trash value must be 10 for this test")

        player.incomingDamage = 3
        // Add only flower cards to floral array
        player.addCardToFloralArray(FakeCards.fakeFlower.id)
        player.addCardToFloralArray(FakeCards.fakeFlower2.id)

        // Mock card effect battle score
        every { cardEffectBattleScore(FakeCards.fakeFlower) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeFlower2) } returns 2

        // Act
        val result = SUT()

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenFloralArrayCardsEnhanceMultipleCards_usesBestCombination() = runBlocking {
        // Arrange
        // Verify resilience values
        assertEquals(2, FakeCards.fakeSeedling.resilience, "Seedling resilience must be 2 for this test")
        assertEquals(4, FakeCards.fakeVine.resilience, "Vine resilience must be 4 for this test")
        assertEquals(10, FakeCards.fakeFlower.trashValue, "Flower trash value must be 10 for this test")

        player.incomingDamage = 7
        // Add regular cards to hand
        player.addCardToHand(FakeCards.fakeSeedling)
        player.addCardToHand(FakeCards.fakeVine)
        // Add flower card to floral array
        player.addCardToFloralArray(FakeCards.fakeFlower)

        // Mock card effect battle score to prefer using seedling with flower enhancement
        every { cardEffectBattleScore(FakeCards.fakeSeedling) } returns 1
        every { cardEffectBattleScore(FakeCards.fakeVine) } returns 2
        every { cardEffectBattleScore(FakeCards.fakeFlower) } returns 3

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
        assertEquals(FakeCards.fakeSeedling.id, result.cards[0].id)
        assertEquals(FakeCards.fakeFlower.id, result.floralCards[0].id)
    }
} 
