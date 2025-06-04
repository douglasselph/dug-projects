package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DecisionShouldTargetPlayerBaselineTest {

    private lateinit var player: Player
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: DecisionShouldTargetPlayerBaseline

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        sampleDie = SampleDie()

        SUT = DecisionShouldTargetPlayerBaseline(player)
    }

    @Test
    fun constructor_whenGivenPlayer_setsPlayer() {
        // Arrange
        val testPlayer = mockk<Player>(relaxed = true)

        // Act
        val testStrategy = DecisionShouldTargetPlayerBaseline(testPlayer)

        // Assert
        // No direct way to verify private field, but we can verify the strategy was created
        // This test will be expanded when implementation is added
    }

    @Test
    fun invoke_whenNoDiceInHand_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        every { player.diceInHand } returns Dice()

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenAllDiceWouldExceedMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(5) // 5 + 5 > 6
        val d8 = sampleDie.d8.adjustTo(7) // 7 + 5 > 8
        val dice = Dice(listOf(d6, d8))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenOneDieCanBeAdjusted_returnsFalse() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(5) // 5 + 5 > 6
        val d8 = sampleDie.d8.adjustTo(2) // 2 + 5 <= 8
        val dice = Dice(listOf(d6, d8))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun invoke_whenAllDiceCanBeAdjusted_returnsFalse() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(1) // 1 + 5 <= 6
        val d8 = sampleDie.d8.adjustTo(2) // 2 + 5 <= 8
        val dice = Dice(listOf(d6, d8))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun invoke_whenDieExactlyAtMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(6) // 6 + 5 > 6
        val dice = Dice(listOf(d6))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenDieOneBelowMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(5) // 5 + 5 > 6
        val dice = Dice(listOf(d6))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenDieTwoBelowMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(4) // 4 + 5 > 6
        val dice = Dice(listOf(d6))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenDieThreeBelowMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 5
        val d6 = sampleDie.d6.adjustTo(3) // 3 + 5 > 6
        val dice = Dice(listOf(d6))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun invoke_whenDieMuchLowerThanMaxYetStillTheAmountIsTooLargeForMax_returnsTrue() {
        // Arrange
        val targetPlayer = mockk<Player>(relaxed = true)
        val amount = 6
        val d6 = sampleDie.d6.adjustTo(2) // 2 + 5 <= 6
        val dice = Dice(listOf(d6))
        every { player.diceInHand } returns dice

        // Act
        val result = SUT.invoke(targetPlayer, amount)

        // Assert
        assertEquals(true, result)
    }

} 
