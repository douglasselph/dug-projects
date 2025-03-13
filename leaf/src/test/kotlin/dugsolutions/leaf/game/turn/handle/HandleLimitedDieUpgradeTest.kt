package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.MissingDieException
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.RandomizerTD
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

    // Test subject
    private lateinit var SUT: HandleLimitedDieUpgrade
    
    // Dependencies
    private lateinit var dieFactory: DieFactory
    private lateinit var sampleDie: SampleDie
    
    // Test data
    private lateinit var mockPlayer: Player
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die
    private lateinit var d10: Die
    private lateinit var randomizer: RandomizerTD

    @BeforeEach
    fun setup() {
        // Setup dependencies
        mockPlayer = mockk(relaxed = true)
        randomizer = RandomizerTD()
        dieFactory = DieFactoryRandom(randomizer)
        sampleDie = SampleDie(randomizer)
        
        // Create the test subject
        SUT = HandleLimitedDieUpgrade(dieFactory)
        
        // Create test dice
        d4 = sampleDie.d4
        d6 = sampleDie.d6
        d8 = sampleDie.d8
        d10 = sampleDie.d10
        
        // Default player behavior
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
        verify { mockPlayer.addDieToHand(any<Die>()) }
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
        verify { mockPlayer.addDieToHand(any<Die>()) }
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
        verify { mockPlayer.addDieToCompost(any<Die>()) }
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
        verify { mockPlayer.addDieToHand(any<Die>()) }
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
