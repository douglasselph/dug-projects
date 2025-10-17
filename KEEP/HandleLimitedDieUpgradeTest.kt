package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.MissingDieException
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandleLimitedDieUpgradeTest {

    private val sampleDie = SampleDie()
    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockDieFactory: DieFactory = mockk(relaxed = true)
    private val d4: Die = sampleDie.d4
    private val d6: Die = sampleDie.d6
    private val d8: Die = sampleDie.d8
    private val d10: Die = sampleDie.d10

    // Test subject
    private val SUT: HandleLimitedDieUpgrade = HandleLimitedDieUpgrade(mockDieFactory, mockGrove)

    @BeforeEach
    fun setup() {

        every { mockDieFactory(DieSides.D4) } returns d4
        every { mockDieFactory(DieSides.D6) } returns d6
        every { mockDieFactory(DieSides.D8) } returns d8
        every { mockDieFactory(DieSides.D10) } returns d10
        
        every { mockPlayer.removeDieFromHand(any()) } returns true
    }
    
    @Test
    fun invoke_whenPlayerHasD4AndD4Allowed_upgradesToD6() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4)
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D4, DieSides.D6), false)
        
        // Assert
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.addDieToHand(d6) }
        verify { mockGrove.removeDie(d6) }
        verify { mockGrove.addDie(d4) }
        assertNotNull(result)
        assertEquals(6, result?.sides)
    }
    
    @Test
    fun invoke_whenPlayerHasD6AndD6Allowed_upgradesToD8() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D6, DieSides.D8), false)
        
        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(d8) }
        verify { mockGrove.removeDie(d8) }
        verify { mockGrove.addDie(d6) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }
    
    @Test
    fun invoke_whenPlayerHasD4ButD4NotAllowed_returnsNull() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4)
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D6, DieSides.D8), false)
        
        // Assert
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify(exactly = 0) { mockPlayer.addDieToHand(any<Die>()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }
        verify(exactly = 0) { mockGrove.addDie(any()) }
        assertNull(result)
    }
    
    @Test
    fun invoke_whenPlayerHasEmptyHand_returnsNull() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns emptyList()
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D4, DieSides.D6), false)
        
        // Assert
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify(exactly = 0) { mockPlayer.addDieToHand(any<Die>()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }
        verify(exactly = 0) { mockGrove.addDie(any()) }
        assertNull(result)
    }
    
    @Test
    fun invoke_whenDiscardAfterUse_discardsNewDie() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        every { mockPlayer.discard(any<Die>()) } returns true
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D6, DieSides.D8), true)
        
        // Assert
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToBed(d8) }
        verify { mockGrove.addDie(d6) }
        verify { mockGrove.removeDie(d8) }
        assertNotNull(result)
        assertEquals(8, result?.sides)
    }
    
    @Test
    fun invoke_whenPlayerHasMultipleAllowedDice_upgradesHighestSided() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d4, d6, d8)
        
        // Act
        val result = SUT(mockPlayer, listOf(DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10), false)
        
        // Assert
        verify { mockPlayer.removeDieFromHand(d8) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(d4) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.addDieToHand(d10) }
        assertNotNull(result)
        assertEquals(10, result?.sides)
    }

    @Test
    fun invoke_whenRemoveDieFromHandFails_throwsMissingDieException() {
        // Arrange
        every { mockPlayer.diceInHand.dice } returns listOf(d6)
        every { mockPlayer.removeDieFromHand(d6) } returns false
        
        // Act & Assert
        val exception = assertThrows<MissingDieException> {
            SUT(mockPlayer, listOf(DieSides.D6, DieSides.D8), false)
        }
        assertEquals("Could not locate the die we are about to upgrade: $d6", exception.message)
    }
} 
