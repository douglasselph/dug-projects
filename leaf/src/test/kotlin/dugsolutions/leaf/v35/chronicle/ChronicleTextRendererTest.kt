package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChronicleTextRendererTest {

    @Test
    fun `full Chronicle rendering resets the visible sequence for each round`() {
        val entries = listOf(
            GameEntry.RoundRevealed(
                sequence = 17,
                roundNumber = 1,
                cardName = "first",
                cardType = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.Marker(sequence = 18, message = "one"),
            GameEntry.Marker(sequence = 19, message = "two"),
            GameEntry.RoundRevealed(
                sequence = 20,
                roundNumber = 2,
                cardName = "second",
                cardType = RoundCardType.BATTLE,
                firstEffect = GameEffect.GAIN_ONE_VP,
                secondEffect = GameEffect.GAIN_ONE_VP
            ),
            GameEntry.Marker(sequence = 21, message = "three")
        )

        val lines = ChronicleTextRenderer.render(entries).lines()

        assertEquals("01.001  ROUND 1 REVEAL CULTIVATION: first [GAIN_ONE_VP | GAIN_ONE_VP]", lines[0])
        assertEquals("01.002  MARKER one", lines[1])
        assertEquals("01.003  MARKER two", lines[2])
        assertEquals("", lines[3])
        assertEquals("02.001  ROUND 2 REVEAL BATTLE: second [GAIN_ONE_VP | GAIN_ONE_VP]", lines[4])
        assertEquals("02.002  MARKER three", lines[5])
    }

    @Test
    fun `standalone entry rendering retains global sequence because round context is unavailable`() {
        assertEquals(
            "0042  MARKER standalone",
            ChronicleTextRenderer.render(GameEntry.Marker(sequence = 42, message = "standalone"))
        )
    }
}
