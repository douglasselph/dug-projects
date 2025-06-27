package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.NutrientReward
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleCompostRecoveryTest {

    private val mockNutrientReward: NutrientReward = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)

    private val SUT: HandleCompostRecovery = HandleCompostRecovery(mockNutrientReward)

    @BeforeEach
    fun setup() {
        // No special setup needed for this test
    }

    @Test
    fun invoke_whenNutrientsLessThan10_doesNotCallNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 9

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockNutrientReward(any()) }
    }

    @Test
    fun invoke_whenNutrientsEquals10_callsNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 10

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockNutrientReward(mockPlayer) }
    }

    @Test
    fun invoke_whenNutrientsGreaterThan10_callsNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 15

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockNutrientReward(mockPlayer) }
    }

    @Test
    fun invoke_whenNutrientsEquals0_doesNotCallNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 0

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockNutrientReward(any()) }
    }

    @Test
    fun invoke_whenNutrientsEqualsNegative_doesNotCallNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns -5

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockNutrientReward(any()) }
    }

    @Test
    fun invoke_whenNutrientsEquals9_doesNotCallNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 9

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockNutrientReward(any()) }
    }

    @Test
    fun invoke_whenNutrientsEquals11_callsNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 11

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockNutrientReward(mockPlayer) }
    }

    @Test
    fun invoke_whenNutrientsEquals20_callsNutrientReward() {
        // Arrange
        every { mockPlayer.nutrients } returns 20

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockNutrientReward(mockPlayer) }
    }

    @Test
    fun invoke_whenCalledMultipleTimesWithSamePlayer_callsNutrientRewardEachTime() {
        // Arrange
        every { mockPlayer.nutrients } returns 12

        // Act
        SUT(mockPlayer)
        SUT(mockPlayer)
        SUT(mockPlayer)

        // Assert
        verify(exactly = 3) { mockNutrientReward(mockPlayer) }
    }

    @Test
    fun invoke_whenCalledWithDifferentPlayers_callsNutrientRewardForEach() {
        // Arrange
        val mockPlayer2: Player = mockk(relaxed = true)
        every { mockPlayer.nutrients } returns 10
        every { mockPlayer2.nutrients } returns 15

        // Act
        SUT(mockPlayer)
        SUT(mockPlayer2)

        // Assert
        verify { mockNutrientReward(mockPlayer) }
        verify { mockNutrientReward(mockPlayer2) }
    }
} 