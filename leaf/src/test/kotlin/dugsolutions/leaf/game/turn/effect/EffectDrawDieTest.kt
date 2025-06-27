package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.DrawDieResult
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectDrawDieTest {

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val sampleDie = SampleDie()
    private val d6 = sampleDie.d6
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val SUT: EffectDrawDie = EffectDrawDie(mockChronicle)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_fromDiscard_drawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromDiscard = true, drawHighest = true)
        every { mockPlayer.drawBestDieFromDiscard() } returns DrawDieResult(d6)

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_fromDiscard_notDrawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromDiscard = true, drawHighest = false)
        every { mockPlayer.drawDieFromDiscard() } returns DrawDieResult(d6)

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_notFromDiscard_drawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromDiscard = false, drawHighest = true)
        every { mockPlayer.drawBestDie() } returns DrawDieResult(d6)

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_notFromDiscard_notDrawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromDiscard = false, drawHighest = false)
        every { mockPlayer.drawDie() } returns DrawDieResult(d6)

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_whenNoDieDrawn_doesNotCallChronicle() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams()
        every { mockPlayer.drawDie() } returns DrawDieResult()
        every { mockPlayer.drawBestDie() } returns DrawDieResult()
        every { mockPlayer.drawDieFromDiscard() } returns DrawDieResult()
        every { mockPlayer.drawBestDieFromDiscard() } returns DrawDieResult()

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
