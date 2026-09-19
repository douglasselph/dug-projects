package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDieToMax
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.die.SampleDie
import dugsolutions.leaf.v14.game.turn.effect.EffectDieToMax
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectDieToMaxTest {

    companion object {
        private const val DIE_VALUE = 2
        private const val MAX_VALUE = 6
    }

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockSelectDieToMax: SelectDieToMax = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)

    private val sampleDie = SampleDie()
    private val d6: Die = sampleDie.d6.adjustTo(DIE_VALUE)

    private val SUT: EffectDieToMax = EffectDieToMax(mockSelectDieToMax, mockChronicle)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_whenDieSelected_adjustsDieToMax_andCallsChronicle() {
        // Arrange
        every { mockSelectDieToMax(mockPlayer.diceInHand) } returns d6

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockChronicle(Moment.ADJUST_DIE(mockPlayer, d6, MAX_VALUE - DIE_VALUE)) }
    }

    @Test
    fun invoke_whenNoDieSelected_doesNothing() {
        // Arrange
        every { mockSelectDieToMax(mockPlayer.diceInHand) } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
