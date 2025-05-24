package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionRerollOneDieCoreStrategyTest {

    private lateinit var player: Player
    private lateinit var SUT: DecisionRerollOneDieCoreStrategy
    private lateinit var d4: Die
    private lateinit var d6: Die
    private lateinit var d8: Die

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        SUT = DecisionRerollOneDieCoreStrategy(player)
        
        d4 = mockk(relaxed = true) {
            every { sides } returns 4
            every { value } returns 2
        }
        d6 = mockk(relaxed = true) {
            every { sides } returns 6
            every { value } returns 3
        }
        d8 = mockk(relaxed = true) {
            every { sides } returns 8
            every { value } returns 4
        }
    }

    @Test
    fun invoke_whenNoDice_doesNotReroll() {
        // Arrange
        every { player.diceInHand } returns Dice()

        // Act
        val result = SUT()

        // Assert
        assertFalse(result)
    }

    @Test
    fun invoke_whenSingleDie_rerollsThatDie() {
        // Arrange
        every { player.diceInHand } returns Dice(listOf(d4))

        // Act
        val result = SUT()

        // Assert
        verify { d4.roll() }
        assertTrue(result)
    }

    @Test
    fun invoke_whenMultipleDice_rerollsDieWithGreatestPotential() {
        // Arrange
        // d4: potential = 4 - 2 = 2
        // d6: potential = 6 - 3 = 3
        // d8: potential = 8 - 4 = 4
        every { player.diceInHand } returns Dice(listOf(d4, d6, d8))

        // Act
        SUT()

        // Assert
        verify { d8.roll() }
        verify(exactly = 0) { d4.roll() }
        verify(exactly = 0) { d6.roll() }
    }

    @Test
    fun invoke_whenTiedPotential_rerollsFirstDieFound() {
        // Arrange
        val d6Low = mockk<Die>(relaxed = true) {
            every { sides } returns 6
            every { value } returns 2
        }
        val d6High = mockk<Die>(relaxed = true) {
            every { sides } returns 6
            every { value } returns 2
        }
        // Both dice have potential = 6 - 2 = 4
        every { player.diceInHand } returns Dice(listOf(d6Low, d6High))

        // Act
        SUT()

        // Assert
        verify { d6Low.roll() }
        verify(exactly = 0) { d6High.roll() }
    }

} 
