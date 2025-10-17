package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NutrientRewardTest {

    companion object {
        private val sampleDie = SampleDie()
        private val D4: Die = sampleDie.d4
        private val D6: Die = sampleDie.d6
        private val D8: Die = sampleDie.d8
        private val D10: Die = sampleDie.d10
        private val D12: Die = sampleDie.d12
        private val D20: Die = sampleDie.d20
    }

    private val mockDieFactory: DieFactory = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)

    private val SUT: NutrientReward = NutrientReward(mockDieFactory, mockChronicle)

    @BeforeEach
    fun setup() {
        // Setup default die factory responses
        every { mockDieFactory(DieSides.D4) } returns D4
        every { mockDieFactory(DieSides.D6) } returns D6
        every { mockDieFactory(DieSides.D8) } returns D8
        every { mockDieFactory(DieSides.D10) } returns D10
        every { mockDieFactory(DieSides.D12) } returns D12
        every { mockDieFactory(DieSides.D20) } returns D20
    }

    @Test
    fun invoke_whenNutrientsLessThan2_doesNothing() {
        // Arrange
        every { mockPlayer.nutrients } returns 1

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.addDieToDiscard(any()) }
        verify(exactly = 0) { mockPlayer.nutrients = any() }
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenNutrientsEquals2_awardsD4AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 2

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D4) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 2, DieSides.D4)) }
    }

    @Test
    fun invoke_whenNutrientsEquals3_awardsD6AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 3

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D6) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 3, DieSides.D6)) }
    }

    @Test
    fun invoke_whenNutrientsEquals4_awardsD8AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 4

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D8) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 4, DieSides.D8)) }
    }

    @Test
    fun invoke_whenNutrientsEquals5_awardsD10AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 5

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D10) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 5, DieSides.D10)) }
    }

    @Test
    fun invoke_whenNutrientsEquals6_awardsD12AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 6

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D12) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 6, DieSides.D12)) }
    }

    @Test
    fun invoke_whenNutrientsEquals10_awardsD20AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 10

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D20) }
        verify { mockPlayer.nutrients = 0 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 10, DieSides.D20)) }
    }

    @Test
    fun invoke_whenNutrientsGreaterThan10_awardsD20AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 15

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D20) }
        verify { mockPlayer.nutrients = 5 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 15, DieSides.D20)) }
    }

    @Test
    fun invoke_whenNutrientsEquals7_awardsD12AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 7

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D12) }
        verify { mockPlayer.nutrients = 1 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 7, DieSides.D12)) }
    }

    @Test
    fun invoke_whenNutrientsEquals8_awardsD12AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 8

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D12) }
        verify { mockPlayer.nutrients = 2 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 8, DieSides.D12)) }
    }

    @Test
    fun invoke_whenNutrientsEquals9_awardsD12AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 9

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D12) }
        verify { mockPlayer.nutrients = 3 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 9, DieSides.D12)) }
    }

    @Test
    fun invoke_whenNutrientsEquals11_awardsD20AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 11

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D20) }
        verify { mockPlayer.nutrients = 1 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 11, DieSides.D20)) }
    }

    @Test
    fun invoke_whenNutrientsEquals20_awardsD20AndReducesNutrients() {
        // Arrange
        every { mockPlayer.nutrients } returns 20

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(D20) }
        verify { mockPlayer.nutrients = 10 }
        verify { mockChronicle(Moment.NUTRIENT_REWARD(mockPlayer, 20, DieSides.D20)) }
    }

    @Test
    fun invoke_whenNutrientsEquals0_doesNothing() {
        // Arrange
        every { mockPlayer.nutrients } returns 0

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.addDieToDiscard(any()) }
        verify(exactly = 0) { mockPlayer.nutrients = any() }
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenNutrientsEqualsNegative_doesNothing() {
        // Arrange
        every { mockPlayer.nutrients } returns -5

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.addDieToDiscard(any()) }
        verify(exactly = 0) { mockPlayer.nutrients = any() }
        verify(exactly = 0) { mockChronicle(any()) }
    }

} 
