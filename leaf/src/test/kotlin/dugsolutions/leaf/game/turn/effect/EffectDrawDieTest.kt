package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
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
    fun invoke_fromCompost_drawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromCompost = true, drawHighest = true)
        every { mockPlayer.drawBestDieFromCompost() } returns d6

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_fromCompost_notDrawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromCompost = true, drawHighest = false)
        every { mockPlayer.drawDieFromCompost() } returns d6

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_notFromCompost_drawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromCompost = false, drawHighest = true)
        every { mockPlayer.drawBestDie() } returns d6

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_notFromCompost_notDrawHighest() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams(fromCompost = false, drawHighest = false)
        every { mockPlayer.drawDie() } returns d6

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify { mockChronicle(Moment.DRAW_DIE(mockPlayer, d6)) }
    }

    @Test
    fun invoke_whenNoDieDrawn_doesNotCallChronicle() {
        // Arrange
        val params = EffectDrawDie.DrawDieParams()
        every { mockPlayer.drawDie() } returns null
        every { mockPlayer.drawBestDie() } returns null
        every { mockPlayer.drawDieFromCompost() } returns null
        every { mockPlayer.drawBestDieFromCompost() } returns null

        // Act
        SUT(mockPlayer, params)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
