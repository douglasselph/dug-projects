package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.grove.Grove
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.random.di.DieFactory
import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.die.DieSides
import dugsolutions.leaf.v14.random.die.SampleDie
import dugsolutions.leaf.v14.game.turn.effect.EffectGainD20
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectGainD20Test {
    companion object {
        private const val PLAYER_ID = 1
    }

    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockDieFactory = mockk<DieFactory>(relaxed = true)
    private val mockChronicle = mockk<GameChronicle>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val sampleDie = SampleDie()
    private val d20: Die = sampleDie.d20

    private val SUT: EffectGainD20 = EffectGainD20(mockGrove, mockDieFactory, mockChronicle)

    @BeforeEach
    fun setup() {
        every { mockPlayer.id } returns PLAYER_ID
        every { mockDieFactory(DieSides.D20) } returns d20
    }

    @Test
    fun invoke_whenD20Available_addsDieToDiscardAndRecordsMoment() {
        // Arrange
        every { mockGrove.getDiceQuantity(20) } returns 1

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.addDieToDiscard(d20) }
        verify { mockChronicle(Moment.GAIN_D20(mockPlayer)) }
    }

    @Test
    fun invoke_whenD20NotAvailable_doesNothing() {
        // Arrange
        every { mockGrove.getDiceQuantity(20) } returns 0

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.addDieToDiscard(any()) }
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
