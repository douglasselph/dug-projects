package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EffectUseOpponentDieTest {

    companion object {
        private const val D6_VALUE = 3
        private const val D8_VALUE = 6
        private const val D10_VALUE = 8
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val fakeTarget = PlayerTD.create2(2)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val sampleDie = SampleDie()
    private val d6 = sampleDie.d6.adjustTo(D6_VALUE)
    private val d8 = sampleDie.d8.adjustTo(D8_VALUE)
    private val d10 = sampleDie.d10.adjustTo(D10_VALUE)

    private val SUT = EffectUseOpponentDie(mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakeTarget.useDeckManager = false
        fakePlayer.pipModifier = 0
        fakeTarget.diceInHand.clear()
    }

    @Test
    fun invoke_whenTargetHandEmpty_doesNothing() {
        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(0, fakePlayer.pipModifier)
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withSingleDie_addsValueToPipModifier() {
        // Arrange
        fakeTarget.addDieToHand(d6)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(D6_VALUE, fakePlayer.pipModifier)
        verify { mockChronicle(Moment.USE_OPPONENT_DIE(fakePlayer, d6)) }
    }

    @Test
    fun invoke_withMultipleDice_selectsHighestValue() {
        // Arrange
        fakeTarget.addDieToHand(d6)
        fakeTarget.addDieToHand(d8)
        fakeTarget.addDieToHand(d10)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(D10_VALUE, fakePlayer.pipModifier)
        verify { mockChronicle(Moment.USE_OPPONENT_DIE(fakePlayer, d10)) }
    }

    @Test
    fun invoke_withTiedHighestValues_selectsFirstHighest() {
        // Arrange
        val d10Tie = sampleDie.d10.adjustTo(D10_VALUE)
        fakeTarget.addDieToHand(d10)
        fakeTarget.addDieToHand(d10Tie)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(D10_VALUE, fakePlayer.pipModifier)
        verify { mockChronicle(Moment.USE_OPPONENT_DIE(fakePlayer, d10)) }
    }

    @Test
    fun invoke_withExistingPipModifier_addsToCurrentValue() {
        // Arrange
        fakePlayer.pipModifier = 5
        fakeTarget.addDieToHand(d8)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(5 + D8_VALUE, fakePlayer.pipModifier)
        verify { mockChronicle(Moment.USE_OPPONENT_DIE(fakePlayer, d8)) }
    }
} 
