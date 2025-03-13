package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.game.purchase.cost.ApplyCostTD
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleDieUpgradeTest {

    private lateinit var SUT: HandleDieUpgrade
    private lateinit var mockPlayer: Player
    private lateinit var mockEvaluateSimpleCost: EvaluateSimpleCost
    private lateinit var applyCost: ApplyCostTD
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die
    private lateinit var d10: Die
    private lateinit var d12: Die
    private lateinit var d20: Die
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var mockCombination: Combination

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockEvaluateSimpleCost = mockk(relaxed = true)
        applyCost = ApplyCostTD()
        mockCombination = mockk(relaxed = true)

        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        // Create the test subject with the correct constructor parameters
        SUT = HandleDieUpgrade(
            mockEvaluateSimpleCost,
            applyCost,
            dieFactory
        )

        // Create test dice
        d4 = dieFactory(DieSides.D4)
        d6 = dieFactory(DieSides.D6)
        d8 = dieFactory(DieSides.D8)
        d10 = dieFactory(DieSides.D10)
        d12 = dieFactory(DieSides.D12)
        d20 = dieFactory(DieSides.D20)

        // Default behavior
        every { mockPlayer.pipTotal } returns 10
        every { mockPlayer.removeDieFromHand(any()) } returns true
        
        // Default behavior for EvaluateSimpleCost
        every { mockEvaluateSimpleCost(any(), any()) } returns mockCombination
        
        // Clear test double tracking for each test
        applyCost.gotPlayers.clear()
        applyCost.gotCombinations.clear()
        applyCost.gotCallbacks.clear()
        applyCost.callbackWasInvoked = false
    }

    @Test
    fun invoke_whenPlayerHasD4_upgradesToD6() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4)
        every { mockPlayer.removeDieFromHand(d4) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        assertNotNull(result)
        assertEquals(6, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasD6_upgradesToD8() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        every { mockPlayer.removeDieFromHand(d6) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasD8_upgradesToD10() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d8)
        every { mockPlayer.removeDieFromHand(d8) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d8) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        assertNotNull(result)
        assertEquals(10, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasD10_upgradesToD12() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d10)
        every { mockPlayer.removeDieFromHand(d10) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d10) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        assertNotNull(result)
        assertEquals(12, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasD12AndEnoughPips_upgradesToD20() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d12)
        every { mockPlayer.pipTotal } returns 20

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockEvaluateSimpleCost(mockPlayer, 20) }
        verify { mockPlayer.removeDieFromHand(d12) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        
        // Check if ApplyCost was called with correct parameters
        assertEquals(1, applyCost.gotPlayers.size)
        assertEquals(mockPlayer, applyCost.gotPlayers[0])
        assertEquals(1, applyCost.gotCombinations.size)
        assertEquals(mockCombination, applyCost.gotCombinations[0])
        assertTrue(applyCost.callbackWasInvoked)
        
        assertNotNull(result)
        assertEquals(20, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasD12ButNotEnoughPips_fallsBackToSimpleUpgrade() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d12)
        every { mockPlayer.pipTotal } returns 7
        every { mockEvaluateSimpleCost(mockPlayer, 20) } returns null

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockEvaluateSimpleCost(mockPlayer, 20) }
        
        // Verify ApplyCost was not called
        assertTrue(applyCost.gotPlayers.isEmpty())
        assertTrue(applyCost.gotCombinations.isEmpty())
        assertFalse(applyCost.callbackWasInvoked)
        
        verify(exactly = 0) { mockPlayer.addDieToHand(any<Die>()) }
        assertNull(result)
    }

    @Test
    fun invoke_whenPlayerHasD20_doesNotUpgrade() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d20)

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify(exactly = 0) { mockPlayer.addDieToHand(any<Die>()) }
        
        // Verify ApplyCost was not called
        assertTrue(applyCost.gotPlayers.isEmpty())
        assertTrue(applyCost.gotCombinations.isEmpty())
        assertFalse(applyCost.callbackWasInvoked)
        
        assertNull(result)
    }

    @Test
    fun invoke_whenDiscardAfterUse_discardsNewDie() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        every { mockPlayer.removeDieFromHand(d6) } returns true
        every { mockPlayer.discard(any<Die>()) } returns true

        // Act
        val result = SUT(mockPlayer, true)

        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        verify { mockPlayer.discard(any<Die>()) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }

    @Test
    fun invoke_whenPlayerHasMultipleDice_upgradesHighestEligible() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4, d6, d8)
        every { mockPlayer.removeDieFromHand(d8) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d8) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        assertNotNull(result)
        assertEquals(10, result?.sides)
    }
    
    @Test
    fun invoke_whenFailsToUpgradeD12_fallsBackToLowerDie() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d8, d12)
        every { mockEvaluateSimpleCost(mockPlayer, 20) } returns null
        
        // Act
        val result = SUT(mockPlayer, false)
        
        // Assert
        verify { mockEvaluateSimpleCost(mockPlayer, 20) }
        verify { mockPlayer.removeDieFromHand(d8) }
        verify { mockPlayer.addDieToHand(any<Die>()) }
        
        // Check ApplyCost was not called for failed D12 upgrade
        assertFalse(applyCost.gotPlayers.contains(mockPlayer))
        assertFalse(applyCost.gotCombinations.contains(mockCombination))
        
        assertNotNull(result)
        assertEquals(10, result?.sides)
    }
} 
