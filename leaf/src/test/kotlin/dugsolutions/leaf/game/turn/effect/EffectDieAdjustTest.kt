package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EffectDieAdjustTest {
    companion object {
        private const val ADJUST_AMOUNT = 2
        private const val D6_VALUE = 3
        private const val D8_VALUE = 8
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val fakeTarget = PlayerTD.create2(2)
    private val mockSelectDieToAdjust: SelectDieToAdjust = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockShouldTargetPlayer: DecisionShouldTargetPlayer = mockk(relaxed = true)
    private val sampleDie = SampleDie()

    private val d6: Die = sampleDie.d6.adjustTo(D6_VALUE)
    private val d8: Die = sampleDie.d8.adjustTo(D8_VALUE)

    private val SUT: EffectDieAdjust = EffectDieAdjust(mockSelectDieToAdjust, mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakeTarget.useDeckManager = false
        fakePlayer.addDieToHand(d6)
        fakeTarget.addDieToHand(d8)
        fakePlayer.decisionDirector.shouldTargetPlayer = mockShouldTargetPlayer
    }

    @Test
    fun invoke_whenTargetIsNull_adjustsPlayerDie() {
        // Arrange
        val expectedValue = ADJUST_AMOUNT + D6_VALUE
        every { mockSelectDieToAdjust(fakePlayer.diceInHand, ADJUST_AMOUNT) } returns d6

        // Act
        SUT(fakePlayer, ADJUST_AMOUNT, null)

        // Assert
        assertEquals(expectedValue, d6.value)
        verify { mockChronicle(Moment.ADJUST_DIE(fakePlayer, d6, ADJUST_AMOUNT)) }
    }

    @Test
    fun invoke_whenTargetIsNotNull_andShouldTargetPlayer_adjustsTargetDie() {
        // Arrange
        val expectedValue = D8_VALUE - ADJUST_AMOUNT
        every { mockShouldTargetPlayer(fakeTarget, ADJUST_AMOUNT) } returns true
        every { mockSelectDieToAdjust(fakeTarget.diceInHand, -ADJUST_AMOUNT) } returns d8

        // Act
        SUT(fakePlayer, ADJUST_AMOUNT, fakeTarget)

        // Assert
        assertEquals(expectedValue, d8.value)
        verify { mockChronicle(Moment.ADJUST_DIE(fakeTarget, d8, -ADJUST_AMOUNT)) }
    }

    @Test
    fun invoke_whenTargetIsNotNull_andShouldNotTargetPlayer_adjustsPlayerDie() {
        // Arrange
        every { mockShouldTargetPlayer(fakeTarget, ADJUST_AMOUNT) } returns false
        every { mockSelectDieToAdjust(fakePlayer.diceInHand, -ADJUST_AMOUNT) } returns d8
        every { mockSelectDieToAdjust(fakePlayer.diceInHand, ADJUST_AMOUNT) } returns d6

        // Act
        SUT(fakePlayer, ADJUST_AMOUNT, fakeTarget)

        // Assert
        val expectedValue = ADJUST_AMOUNT + D6_VALUE
        assertEquals(expectedValue, d6.value)
        verify { mockChronicle(Moment.ADJUST_DIE(fakePlayer, d6, ADJUST_AMOUNT)) }
    }
} 
