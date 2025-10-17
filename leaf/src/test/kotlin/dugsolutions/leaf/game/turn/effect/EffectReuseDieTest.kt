package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.domain.HandItem
import dugsolutions.leaf.random.RandomizerTD
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EffectReuseDieTest {

    companion object {
        private const val D6_VALUE = 3
        private const val D8_VALUE = 4
        private const val D10_VALUE = 2
        private const val ALT_VALUE = 6
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val randomizer = RandomizerTD().setValues(listOf(ALT_VALUE))
    private val sampleDie = SampleDie(randomizer)
    private val d6: Die = sampleDie.d6.adjustTo(D6_VALUE)
    private val d8: Die = sampleDie.d8.adjustTo(D8_VALUE)
    private  val d10: Die = sampleDie.d10.adjustTo(D10_VALUE)

    private val SUT = EffectReuseDie(mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakePlayer.diceInHand.clear()
        fakePlayer.reused.clear()
    }

    @Test
    fun invoke_whenNoDiceInHand_noReroll_doesNothing() {
        // Act
        SUT(fakePlayer, false)

        // Assert
        assertTrue(fakePlayer.reused.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenNoDiceInHand_reroll_doesNothing() {
        // Act
        SUT(fakePlayer, true)

        // Assert
        assertTrue(fakePlayer.reused.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withSingleDie_noReroll_reusesThatDie() {
        // Arrange
        fakePlayer.addDieToHand(d6)

        // Act
        SUT(fakePlayer, false)

        // Assert
        assertEquals(1, fakePlayer.reused.size)
        assertEquals(HandItem.aDie(d6), fakePlayer.reused[0])
        verify { mockChronicle(Moment.REUSE_DIE(fakePlayer, d6)) }
    }

    @Test
    fun invoke_withSingleDie_reroll_reusesThatDieWithReroll() {
        // Arrange
        fakePlayer.addDieToHand(d6)

        // Act
        SUT(fakePlayer, true)

        // Assert
        assertEquals(1, fakePlayer.reused.size)
        assertEquals(HandItem.aDie(d6), fakePlayer.reused[0])
        verify { mockChronicle(Moment.REUSE_DIE(fakePlayer, d6)) }
        assertEquals(ALT_VALUE, d6.value)
    }

    @Test
    fun invoke_withMultipleDice_reroll_reusesDieWithMostSides() {
        // Arrange
        fakePlayer.addDieToHand(d6)
        fakePlayer.addDieToHand(d8)
        fakePlayer.addDieToHand(d10)

        // Act
        SUT(fakePlayer, true)

        // Assert
        assertEquals(1, fakePlayer.reused.size)
        assertEquals(HandItem.aDie(d10), fakePlayer.reused[0])
        verify { mockChronicle(Moment.REUSE_DIE(fakePlayer, d10)) }
    }

    @Test
    fun invoke_withMultipleDice_noReroll_reusesDieWithTheValuesValue() {
        // Arrange
        fakePlayer.addDieToHand(d6)
        fakePlayer.addDieToHand(d8)
        fakePlayer.addDieToHand(d10)

        // Act
        SUT(fakePlayer, false)

        // Assert
        assertEquals(1, fakePlayer.reused.size)
        assertEquals(HandItem.aDie(d8), fakePlayer.reused[0])
        verify { mockChronicle(Moment.REUSE_DIE(fakePlayer, d8)) }
    }

    @Test
    fun invoke_withTiedSides_noReroll_reusesDieWithLargerValue() {
        // Arrange
        val d10_2 = sampleDie.d10.adjustTo(6)
        fakePlayer.addDieToHand(d10.adjustTo(4))
        fakePlayer.addDieToHand(d10_2)

        // Act
        SUT(fakePlayer, false)

        // Assert
        assertEquals(1, fakePlayer.reused.size)
        assertEquals(HandItem.aDie(d10_2), fakePlayer.reused[0])
        verify { mockChronicle(Moment.REUSE_DIE(fakePlayer, d10_2)) }
    }

} 
