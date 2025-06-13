package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.game.acquire.cost.ApplyCostTD
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
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

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockEvaluateSimpleCost: EvaluateSimpleCost = mockk(relaxed = true)
    private val applyCost: ApplyCostTD = ApplyCostTD()
    private val sampleDie = SampleDie()
    private val d4: Die = sampleDie.d4
    private val d6: Die = sampleDie.d6
    private val d8: Die = sampleDie.d8
    private val d10: Die = sampleDie.d10
    private val d12: Die = sampleDie.d12
    private val d20: Die = sampleDie.d20
    private val mockDieFactory: DieFactory = mockk(relaxed = true)
    private val mockCombination: Combination = mockk(relaxed = true)
    private val mockGrove = mockk<Grove>(relaxed = true)
    private val chronicle: GameChronicle = mockk(relaxed = true)

    private val SUT: HandleDieUpgrade = HandleDieUpgrade(
        mockEvaluateSimpleCost,
        applyCost,
        mockDieFactory,
        mockGrove,
        chronicle
    )

    @BeforeEach
    fun setup() {
        every { mockDieFactory(DieSides.D4) } returns d4
        every { mockDieFactory(DieSides.D6) } returns d6
        every { mockDieFactory(DieSides.D8) } returns d8
        every { mockDieFactory(DieSides.D10) } returns d10
        every { mockDieFactory(DieSides.D12) } returns d12
        every { mockDieFactory(DieSides.D20) } returns d20

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
        verify { mockPlayer.addDieToHand(d6) }
        verify { mockGrove.addDie(d4) }
        verify { mockGrove.removeDie(d6) }
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
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockGrove.addDie(d6) }
        verify { mockGrove.removeDie(d8) }
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
        verify { mockPlayer.addDieToHand(d10) }
        verify { mockGrove.addDie(d8) }
        verify { mockGrove.removeDie(d10) }
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
        verify { mockPlayer.addDieToHand(d12) }
        verify { mockGrove.addDie(d10) }
        verify { mockGrove.removeDie(d12) }
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
        verify { mockPlayer.addDieToHand(d20) }
        verify { mockGrove.addDie(d12) }
        verify { mockGrove.removeDie(d20) }

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
        verify(exactly = 0) { mockGrove.addDie(any()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }
        
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
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockPlayer.discard(d8) }
        verify { mockGrove.addDie(d6) }
        verify { mockGrove.removeDie(d8) }

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
        verify { mockPlayer.addDieToHand(d10) }
        verify { mockGrove.addDie(d8) }
        verify { mockGrove.removeDie(d10) }
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
        verify { mockPlayer.addDieToHand(d10) }
        verify { mockGrove.addDie(d8) }
        verify { mockGrove.removeDie(d10) }

        // Check ApplyCost was not called for failed D12 upgrade
        assertFalse(applyCost.gotPlayers.contains(mockPlayer))
        assertFalse(applyCost.gotCombinations.contains(mockCombination))
        
        assertNotNull(result)
        assertEquals(10, result?.sides)
    }

    @Test
    fun invoke_whenMultipleDiceWithSameSides_selectsLowestValue() {
        // Arrange
        val d6High = sampleDie.d6.adjustTo(6)
        val d6Low = sampleDie.d6.adjustTo(1)
        every { mockPlayer.diceInHand.dice } returns listOf(d4, d6High, d6Low)
        every { mockPlayer.removeDieFromHand(d6Low) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d6Low) }
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockGrove.addDie(d6Low) }
        verify { mockGrove.removeDie(d8) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }

    @Test
    fun invoke_whenOnlyParameterSpecified_filtersEligibleDice() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4, d6, d8)
        every { mockPlayer.removeDieFromHand(d6) } returns true

        // Act
        val result = SUT(mockPlayer, false, only = listOf(DieSides.D4, DieSides.D6))

        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockGrove.addDie(d6) }
        verify { mockGrove.removeDie(d8) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }

    @Test
    fun invoke_whenOnlyParameterExcludesAllDice_returnsNull() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4, d6, d8)

        // Act
        val result = SUT(mockPlayer, false, only = listOf(DieSides.D20))

        // Assert
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify(exactly = 0) { mockPlayer.addDieToHand(any<Die>()) }
        verify(exactly = 0) { mockGrove.addDie(any()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }
        assertNull(result)
    }

    @Test
    fun invoke_whenOnlyParameterAndMultipleEligible_selectsLowestValue() {
        // Arrange
        val d4High = sampleDie.d4.adjustTo(4)
        val d4Low = sampleDie.d4.adjustTo(1)
        every { mockPlayer.diceInHand.dice } returns listOf(d4High, d4Low, d6)
        every { mockPlayer.removeDieFromHand(d4Low) } returns true

        // Act
        val result = SUT(mockPlayer, false, only = listOf(DieSides.D4))

        // Assert
        verify { mockPlayer.removeDieFromHand(d4Low) }
        verify { mockPlayer.addDieToHand(d6) }
        verify { mockGrove.addDie(d4Low) }
        verify { mockGrove.removeDie(d6) }
        assertNotNull(result)
        assertEquals(6, result?.sides)
    }

    @Test
    fun invoke_whenOnlyParameterIncludesD12AndEnoughPips_upgradesToD20() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6, d12)
        every { mockPlayer.pipTotal } returns 20
        every { mockPlayer.removeDieFromHand(d12) } returns true

        // Act
        val result = SUT(mockPlayer, false, only = listOf(DieSides.D6, DieSides.D12))

        // Assert
        verify { mockEvaluateSimpleCost(mockPlayer, 20) }
        verify { mockPlayer.removeDieFromHand(d12) }
        verify { mockPlayer.addDieToHand(d20) }
        verify { mockGrove.addDie(d12) }
        verify { mockGrove.removeDie(d20) }
        assertNotNull(result)
        assertEquals(20, result?.sides)
    }

    @Test
    fun invoke_whenUpgradeSuccessful_recordsInChronicle() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        every { mockPlayer.removeDieFromHand(d6) } returns true

        // Act
        val result = SUT(mockPlayer, false)

        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockGrove.addDie(d6) }
        verify { mockGrove.removeDie(d8) }


        verify { chronicle(Moment.UPGRADE_DIE(mockPlayer, d8)) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }
} 
