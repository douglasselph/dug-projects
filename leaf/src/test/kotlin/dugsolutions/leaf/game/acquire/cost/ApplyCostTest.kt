package dugsolutions.leaf.game.acquire.cost

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.components.die.MissingDieException
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryUniform
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.game.acquire.domain.Adjusted
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplyCostTest {

    private lateinit var mockApplyEffects: ApplyEffects
    private lateinit var mockPlayer: Player
    private lateinit var fakePlayer: PlayerTD
    private lateinit var testDie1: DieValue
    private lateinit var testDie2: DieValue
    private lateinit var dieValues: DieValues
    private lateinit var combination: Combination
    private lateinit var dieFactory: DieFactory

    private lateinit var SUT: ApplyCost

    @BeforeEach
    fun setup() {
        mockApplyEffects = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        val randomizer = RandomizerTD()
        val costScore: CostScore = mockk(relaxed = true)
        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        val cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        fakePlayer = PlayerTD(2, cardManager)
        dieFactory = DieFactoryUniform(randomizer)

        // Create test dice
        testDie1 = DieValue(6, 3)
        testDie2 = DieValue(8, 5)

        // Create DieValues from the test dice
        dieValues = DieValues(listOf(testDie1, testDie2))

        // Create a combination with the DieValues, addToTotal, and adjusted list
        combination = Combination(
            values = dieValues,
            addToTotal = 0,
            adjusted = emptyList()
        )
        for (value in combination.values) {
            fakePlayer.addDieToHand(value.dieFrom(dieFactory))
        }

        SUT = ApplyCost(mockApplyEffects)

        every { mockPlayer.discard(any<Die>()) } returns true
        every { mockPlayer.discard(any<DieValue>()) } returns true
    }

    @Test
    fun invoke_appliesEffectsAndDiscardsAllDice() {
        // Arrange
        // Act
        SUT(mockPlayer, combination)

        // Assert
        verify { mockApplyEffects(mockPlayer, combination) }
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
    }

    @Test
    fun invoke_whenDiscardFails_throwsException() {
        // Arrange
        every { mockPlayer.discard(any<Die>()) } returns false
        every { mockPlayer.discard(any<DieValue>()) } returns false

        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(mockPlayer, combination)
        }

        // Verify the exception details
        assertEquals("Could not discard the die $testDie1", exception.message, "Exception should have the correct error message")

        // Verify that effects were still applied before the exception
        verify { mockApplyEffects(mockPlayer, combination) }
    }

    @Test
    fun invoke_callsAcquireItemLambda() {
        // Arrange
        var acquireItemCalled = false
        val acquireItem: (Player) -> Unit = {
            acquireItemCalled = true
        }

        // Act
        SUT(mockPlayer, combination, acquireItem)

        // Assert
        assertTrue(acquireItemCalled, "The acquireItem lambda should be called")
    }

    @Test
    fun invoke_ensuresCorrectOperationOrder() {
        // Arrange
        var gotPlayer: Player? = null

        // Act
        SUT(fakePlayer, combination) { player ->
            // Verify that effects were applied and dice were discarded before this lambda is called
            verify { mockApplyEffects(fakePlayer, combination) }
            gotPlayer = player
        }

        // Assert
        assertEquals(0, fakePlayer.diceInHand.size)
        assertEquals(fakePlayer, gotPlayer, "The player should be passed to the acquireItem lambda")
    }

    @Test
    fun invoke_withDefaultAcquireItem_completesSuccessfully() {
        // Act - using default value for acquireItem
        SUT(mockPlayer, combination)

        // Assert - verify core functionality still runs
        verify { mockApplyEffects(mockPlayer, combination) }
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
    }

    @Test
    fun invoke_withEmptyCombination_onlyCallsAcquireItem() {
        // Arrange
        val emptyDieValues = DieValues(emptyList())
        val emptyCombination = Combination(
            values = emptyDieValues,
            addToTotal = 0,
            adjusted = emptyList()
        )
        var acquireItemCalled = false
        val acquireItem: (Player) -> Unit = {
            acquireItemCalled = true
        }

        // Act
        SUT(mockPlayer, emptyCombination, acquireItem)

        // Assert
        verify { mockApplyEffects(mockPlayer, emptyCombination) }
        assertTrue(acquireItemCalled, "The acquireItem lambda should be called")
        // No dice to discard so verify no discard calls
        verify(exactly = 0) { mockPlayer.discard(any<Die>()) }
    }

    @Test
    fun invoke_withAdjustments_stillDiscardsAllDice() {
        // Arrange
        val adjustedDie = DieValue(10, 4)
        val adjustedDieValues = DieValues(listOf(testDie1, testDie2, adjustedDie))

        // Create adjusted settings
        val adjustment = Adjusted.ByAmount(adjustedDie, 2)

        // Create combination with added total and adjustments
        val combinationWithAdjustments = Combination(
            values = adjustedDieValues,
            addToTotal = 3,
            adjusted = listOf(adjustment)
        )

        // Act
        SUT(mockPlayer, combinationWithAdjustments)

        // Assert
        verify { mockApplyEffects(mockPlayer, combinationWithAdjustments) }
        verify { mockPlayer.discard(testDie1) }
        verify { mockPlayer.discard(testDie2) }
        verify { mockPlayer.discard(adjustedDie) }
    }

    @Test
    fun invoke_withFakePlayer_successfullyDiscardsDice() {
        // Arrange
        fakePlayer.discardHand()
        fakePlayer.addDieToHand(testDie1)
        fakePlayer.addDieToHand(testDie2)

        // Act
        SUT(fakePlayer, combination)

        // Assert
        assertTrue(fakePlayer.diceInHand.isEmpty(), "All dice should be discarded")
        verify { mockApplyEffects(fakePlayer, combination) }
    }

    @Test
    fun invoke_withFakePlayerAndAcquireItem_successfullyCompletesFullFlow() {
        // Arrange
        fakePlayer.discardHand()
        fakePlayer.addDieToHand(testDie1)
        fakePlayer.addDieToHand(testDie2)
        var acquireItemCalled = false
        val acquireItem: (Player) -> Unit = {
            acquireItemCalled = true
        }

        // Act
        SUT(fakePlayer, combination, acquireItem)

        // Assert
        assertTrue(fakePlayer.diceInHand.isEmpty(), "All dice should be discarded")
        assertTrue(acquireItemCalled, "Acquire item lambda should be called")
        verify { mockApplyEffects(fakePlayer, combination) }
    }

    @Test
    fun invoke_withFakePlayerAndAdjustments_correctlyHandlesAdjustedDice() {
        // Arrange
        val adjustedDie = DieValue(10, 4)
        fakePlayer.discardHand()
        fakePlayer.addDieToHand(testDie1)
        fakePlayer.addDieToHand(testDie2)
        fakePlayer.addDieToHand(adjustedDie)

        val adjustedDieValues = DieValues(listOf(testDie1, testDie2, adjustedDie))
        val adjustment = Adjusted.ByAmount(adjustedDie, 2)
        val combinationWithAdjustments = Combination(
            values = adjustedDieValues,
            addToTotal = 3,
            adjusted = listOf(adjustment)
        )

        // Act
        SUT(fakePlayer, combinationWithAdjustments)

        // Assert
        assertTrue(fakePlayer.diceInHand.isEmpty(), "All dice including adjusted die should be discarded")
        verify { mockApplyEffects(fakePlayer, combinationWithAdjustments) }
    }

    @Test
    fun invoke_withFakePlayerAndEmptyHand_throwsMissingDieException() {
        // Arrange
        fakePlayer.discardHand()

        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(fakePlayer, combination)
        }

        assertEquals("Could not discard the die $testDie1", exception.message, "Exception should have the correct error message")
        verify { mockApplyEffects(fakePlayer, combination) }
    }
} 
