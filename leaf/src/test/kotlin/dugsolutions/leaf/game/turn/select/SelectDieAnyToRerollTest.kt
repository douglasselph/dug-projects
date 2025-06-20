package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectDieAnyToRerollTest {

    private val mockSelectDieToReroll: SelectDieToReroll = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockTarget: Player = mockk(relaxed = true)
    private val sampleDie = SampleDie()
    private val playerD6: Die = sampleDie.d6
    private val opponentD8: Die = sampleDie.d8

    private val SUT: SelectDieAnyToReroll = SelectDieAnyToReroll(mockSelectDieToReroll)

    @BeforeEach
    fun setup() {
        // Reset dice to default values
        playerD6.adjustTo(1)
        opponentD8.adjustTo(1)
    }

    @Test
    fun invoke_whenPlayerDieBelowAverageAndOpponentDieAboveAverage_selectsPlayerDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0, difference = 3.0-2 = 1.0)
        // Opponent die: d8 value 5 (above average 4.0, difference = 5-4.0 = 1.0)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(5)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenPlayerDieBelowAverageAndOpponentDieAboveAverage_selectsOpponentDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0, difference = 3.0-2 = 1.0)
        // Opponent die: d8 value 6 (above average 4.0, difference = 6-4.0 = 2.0)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(6)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentD8, result.opponentDie)
    }

    @Test
    fun invoke_whenPlayerDieAtOrAboveAverage_returnsNullForPlayerDie() {
        // Arrange
        // Player die: d6 value 3 (at average 3.0, should not be considered)
        playerD6.adjustTo(3)
        opponentD8.adjustTo(5) // above average
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentD8, result.opponentDie)
    }

    @Test
    fun invoke_whenOpponentDieAtOrBelowAverage_returnsNullForOpponentDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0)
        // Opponent die: d8 value 4 (at average 4.0, should not be considered)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(4)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenPlayerDieAboveAverageAndOpponentDieBelowAverage_returnsEmptyBestDie() {
        // Arrange
        // Player die: d6 value 4 (above average 3.0, should not be considered)
        // Opponent die: d8 value 3 (below average 4.0, should not be considered)
        playerD6.adjustTo(4)
        opponentD8.adjustTo(3)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenNoPlayerDieSelected_returnsOpponentDie() {
        // Arrange
        opponentD8.adjustTo(5) // above average 4.0
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns null

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentD8, result.opponentDie)
    }

    @Test
    fun invoke_whenNoOpponentDieAboveAverage_returnsPlayerDie() {
        // Arrange
        playerD6.adjustTo(2) // below average 3.0
        opponentD8.adjustTo(3) // below average 4.0 for d8
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenMultipleOpponentDiceAboveAverage_selectsHighestValue() {
        // Arrange
        val opponentDie1 = sampleDie.d6.adjustTo(4) // d6 value 4 (above average 3.0)
        val opponentDie2 = sampleDie.d8.adjustTo(5) // d8 value 5 (above average 4.0)
        val opponentDie3 = sampleDie.d6.adjustTo(5) // d6 value 5 (above average 3.0, highest value)
        
        playerD6.adjustTo(2) // below average 3.0
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentDie1, opponentDie2, opponentDie3))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentDie3, result.opponentDie) // d6 value 5 should be selected (highest value)
    }

    @Test
    fun invoke_whenOpponentDiceHaveSameValueAboveAverage_selectsSmallestSides() {
        // Arrange
        val opponentDie1 = sampleDie.d6.adjustTo(4) // d6 value 4 (above average 3.0)
        val opponentDie2 = sampleDie.d8.adjustTo(4) // d8 value 4 (above average 4.0, but sides > 6)
        
        playerD6.adjustTo(4) // should ignore
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentDie1, opponentDie2))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentDie1, result.opponentDie) // d6 should be selected (smaller sides)
    }

    @Test
    fun invoke_whenPlayerDieHasLargerDifferenceFromAverage_selectsPlayerDie() {
        // Arrange
        // Player die: d6 value 1 (below average 3.0, difference = 3.0-1 = 2.0)
        // Opponent die: d8 value 5 (above average 4.0, difference = 5-4.0 = 1.0)
        playerD6.adjustTo(1)
        opponentD8.adjustTo(5)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenOpponentDieHasLargerDifferenceFromAverage_selectsOpponentDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0, difference = 3.0-2 = 1.0)
        // Opponent die: d8 value 7 (above average 4.0, difference = 7-4.0 = 3.0)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(7)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentD8, result.opponentDie)
    }

    @Test
    fun invoke_whenDifferencesAreEqual_selectsPlayerDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0, difference = 3.0-2 = 1.0)
        // Opponent die: d8 value 5 (above average 4.0, difference = 5-4.0 = 1.0)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(5)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenNoDiceAvailable_returnsEmptyBestDie() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(emptyList())
        every { mockTarget.diceInHand } returns Dice(emptyList())
        every { mockSelectDieToReroll(any()) } returns null

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertNull(result.opponentDie)
    }

    @Test
    fun invoke_whenPlayerDieAtExactAverage_returnsNullForPlayerDie() {
        // Arrange
        // Player die: d6 value 3 (exactly at average 3.0, should not be considered)
        playerD6.adjustTo(3)
        opponentD8.adjustTo(5) // above average 4.0
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertNull(result.playerDie)
        assertEquals(opponentD8, result.opponentDie)
    }

    @Test
    fun invoke_whenOpponentDieAtExactAverage_returnsNullForOpponentDie() {
        // Arrange
        // Player die: d6 value 2 (below average 3.0)
        // Opponent die: d8 value 4 (exactly at average 4.0, should not be considered)
        playerD6.adjustTo(2)
        opponentD8.adjustTo(4)
        
        every { mockPlayer.diceInHand } returns Dice(listOf(playerD6))
        every { mockTarget.diceInHand } returns Dice(listOf(opponentD8))
        every { mockSelectDieToReroll(listOf(playerD6)) } returns playerD6

        // Act
        val result = SUT(mockPlayer, mockTarget)

        // Assert
        assertEquals(playerD6, result.playerDie)
        assertNull(result.opponentDie)
    }

} 
