package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDiceNotActivatingMatches
import dugsolutions.leaf.v14.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.random.RandomizerTD
import dugsolutions.leaf.v14.random.die.Dice
import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.die.SampleDie
import dugsolutions.leaf.v14.game.turn.effect.EffectDieReroll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectDieRerollTest {

    companion object {
        private const val DIE_VALUE = 3
        private const val DIE_VALUE2 = 5
    }

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockSelectDieToReroll: SelectDieToReroll = mockk(relaxed = true)
    private val mockSelectDiceNotActivatingMatches: SelectDiceNotActivatingMatches = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val randomizer = RandomizerTD().setValues(listOf(DIE_VALUE2))
    private val sampleDie = SampleDie(randomizer)
    private val d6: Die = sampleDie.d6
    private val d8: Die = sampleDie.d8
    private val sampleDice = Dice(listOf(d6, d8))

    private val SUT: EffectDieReroll =
        EffectDieReroll(mockSelectDieToReroll, mockSelectDiceNotActivatingMatches, mockChronicle)

    @BeforeEach
    fun setup() {
        d6.adjustTo(DIE_VALUE)
        every { mockPlayer.diceInHand } returns sampleDice
        every { mockSelectDieToReroll(sampleDice.dice) } returns d6
        every { mockSelectDiceNotActivatingMatches(mockPlayer) } returns sampleDice.dice
    }

    @Test
    fun invoke_takeBetterTrue_keepsBetterValue_andCallsChronicle() {
        // Arrange
        // Act
        SUT(mockPlayer, true)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, d6, DIE_VALUE)) }
    }

    @Test
    fun invoke_takeBetterFalse_alwaysUsesNewValue_andCallsChronicle() {
        // Arrange

        // Act
        SUT(mockPlayer, false)

        // Assert
        assertEquals(DIE_VALUE2, d6.value)
        verify { mockChronicle(Moment.REROLL(mockPlayer, d6, DIE_VALUE)) }
    }

    @Test
    fun invoke_whenNoDieSelected_doesNothing() {
        // Arrange
        every { mockSelectDieToReroll(sampleDice.dice) } returns null

        // Act
        SUT(mockPlayer, true)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
