package dugsolutions.leaf.game.turn

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for PlayerOrder class
 */
class PlayerOrderTest {

    private lateinit var SUT: PlayerOrder
    private val randomizer = RandomizerTD()
    private val sampleDie = SampleDie(randomizer)
    private val tdPlayer1 = PlayerTD(1)
    private val tdPlayer2 = PlayerTD(2)
    private val tdPlayer3 = PlayerTD(3)
    private val tdPlayer4 = PlayerTD(4)
    private lateinit var mockGameChronicle: GameChronicle


    private val d6: Die
        get() = sampleDie.d6
    private val d8: Die
        get() = sampleDie.d8
    private val d10: Die
        get() = sampleDie.d10
    private val d12: Die
        get() = sampleDie.d12
    private val d20: Die
        get() = sampleDie.d20

    private fun setupDiceInHand(player: PlayerTD, dice: List<Die>) {
        player.diceInHand.clear()
        player.diceInHand.addAll(dice)
    }

    @BeforeEach
    fun setup() {
        // Initialize random components
        mockGameChronicle = mockk()
        SUT = PlayerOrder(mockGameChronicle)
        
        // Mock the chronicle
        every { mockGameChronicle(any()) } just Runs
    }

    @Test
    fun invoke_whenPlayersHaveDifferentPips_ordersByTotalPips() {
        // Arrange
        val player1Dice = listOf(d6.adjustTo(3), d8.adjustTo(5))   // 8
        val player2Dice = listOf(d10.adjustTo(9), d12.adjustTo(4), d20.adjustTo(6)) // 19
        val player3Dice = listOf(d6.adjustTo(6)) // 6

        setupDiceInHand(tdPlayer1, player1Dice)
        setupDiceInHand(tdPlayer2, player2Dice)
        setupDiceInHand(tdPlayer3, player3Dice)

        // Act
        val result = SUT(listOf(tdPlayer1, tdPlayer2, tdPlayer3))

        // Assert
        assertEquals(3, result.size)
        assertEquals(tdPlayer2, result[0]) // 19 pips (highest)
        assertEquals(tdPlayer1, result[1]) // 8 pips (middle)
        assertEquals(tdPlayer3, result[2]) // 6 pips (lowest)
        
        // Verify chronicle was called
        verify { mockGameChronicle(any()) }
    }

    @Test
    fun invoke_whenTiedPipsForHighestPlayer_rerollsUntilResolved() {
        // Arrange
        val player1Dice = listOf(d6.adjustTo(6)) // 6 pips
        val player2Dice = listOf(d6.adjustTo(6)) // 6 pips
        val player3Dice = listOf(d6.adjustTo(6)) // 6 pips
        randomizer.setValues(listOf(5, 4, 3))

        setupDiceInHand(tdPlayer1, player1Dice)
        setupDiceInHand(tdPlayer2, player2Dice)
        setupDiceInHand(tdPlayer3, player3Dice)

        // Act
        val result = SUT(listOf(tdPlayer1, tdPlayer2, tdPlayer3))

        // Assert - After rerolling, Player1 will have the highest pip value
        assertEquals(tdPlayer1, result[0])
        assertEquals(tdPlayer2, result[1])
        assertEquals(tdPlayer3, result[2])
    }

    @Test
    fun invoke_whenTiedAndHasHighestPlayerEstablished_ordersByClockwiseDistance() {
        // Arrange - Player3 has highest pips (6), Player1 and Player2 tied (5)
        val player1Dice = listOf(d6.adjustTo(5)) // 5 pips
        val player2Dice = listOf(d6.adjustTo(5)) // 5 pips
        val player3Dice = listOf(d6.adjustTo(6)) // 6 pips
        
        setupDiceInHand(tdPlayer1, player1Dice)
        setupDiceInHand(tdPlayer2, player2Dice)
        setupDiceInHand(tdPlayer3, player3Dice)

        // Act
        val result = SUT(listOf(tdPlayer1, tdPlayer2, tdPlayer3))
        
        // Assert - Player3 is highest, then Player1 (closest clockwise to Player3), then Player2
        assertEquals(tdPlayer3, result[0])
        assertEquals(tdPlayer1, result[1])
        assertEquals(tdPlayer2, result[2])
    }

    @Test
    fun invoke_withFourPlayers_correctlyOrdersBasedOnPipsAndToken() {
        // Arrange - two groups of tied players
        val player1Dice = listOf(d6.adjustTo(6)) // 6 pips - tied low group
        val player2Dice = listOf(d8.adjustTo(8)) // 8 pips - tied high group
        val player3Dice = listOf(d6.adjustTo(6)) // 6 pips - tied low group
        val player4Dice = listOf(d8.adjustTo(8)) // 8 pips - tied high group
        
        setupDiceInHand(tdPlayer1, player1Dice)
        setupDiceInHand(tdPlayer2, player2Dice)
        setupDiceInHand(tdPlayer3, player3Dice)
        setupDiceInHand(tdPlayer4, player4Dice)

        randomizer.setValues(listOf(4, 2, 5, 1))

        // Act
        val result = SUT(listOf(tdPlayer1, tdPlayer2, tdPlayer3, tdPlayer4))
        
        // Assert
        // High group (8 pips): Player2, Player4
        // Low group (6 pips): Player3 (closest to highest), Player1
        assertEquals(tdPlayer3, result[0]) // 8 pips, highest after reroll
        assertEquals(tdPlayer1, result[1]) // 8 pips
        assertEquals(tdPlayer2, result[2]) // 6 pips, closest to highest player
        assertEquals(tdPlayer4, result[3]) // 6 pips
    }

    @Test
    fun invoke_whenRelativeDistanceWrapsAround_ordersByWrappedDistance() {
        // Arrange
        val player1Dice = listOf(d8.adjustTo(8)) // 8 pips
        val player2Dice = listOf(d8.adjustTo(7)) // 7 pips
        val player3Dice = listOf(d8.adjustTo(7)) // 7 pips
        
        setupDiceInHand(tdPlayer1, player1Dice)
        setupDiceInHand(tdPlayer2, player2Dice)
        setupDiceInHand(tdPlayer3, player3Dice)
        
        // Act
        val result = SUT(listOf(tdPlayer1, tdPlayer2, tdPlayer3))
        
        // Assert - Player1 highest, then Player2 and Player3 tied but ordered by clockwise distance
        assertEquals(tdPlayer1, result[0]) // 8 pips (highest)
        assertEquals(tdPlayer2, result[1]) // 7 pips (tied but closest clockwise to Player1)
        assertEquals(tdPlayer3, result[2]) // 7 pips (tied but further from Player1)
    }
} 
