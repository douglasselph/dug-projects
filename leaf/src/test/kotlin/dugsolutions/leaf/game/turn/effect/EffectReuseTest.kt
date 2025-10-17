package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EffectReuseTest {

    companion object {
        private const val D6_VALUE = 3
        private const val D8_VALUE = 4
        private const val D10_VALUE = 5
        private const val D12_VALUE = 6
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val mockEffectReuseCard: EffectReuseCard = mockk(relaxed = true)
    private val mockEffectReuseDie: EffectReuseDie = mockk(relaxed = true)
    private val sampleDie = SampleDie()
    private val d6: Die = sampleDie.d6.adjustTo(D6_VALUE)
    private val d8: Die = sampleDie.d8.adjustTo(D8_VALUE)
    private val d10: Die = sampleDie.d10.adjustTo(D10_VALUE)
    private val d12: Die = sampleDie.d12.adjustTo(D12_VALUE)

    private val SUT = EffectReuse(mockEffectReuseCard, mockEffectReuseDie)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakePlayer.diceInHand.clear()
    }

    @Test
    fun invoke_whenNoDiceInHand_callsReuseCard() {
        // Act
        SUT(fakePlayer)

        // Assert
        verify { mockEffectReuseCard(fakePlayer) }
        verify(exactly = 0) { mockEffectReuseDie(any(), any()) }
    }

    @Test
    fun invoke_whenHighestDieLessThan10_callsReuseCard() {
        // Arrange
        fakePlayer.addDieToHand(d6)
        fakePlayer.addDieToHand(d8)

        // Act
        SUT(fakePlayer)

        // Assert
        verify { mockEffectReuseCard(fakePlayer) }
        verify(exactly = 0) { mockEffectReuseDie(any(), any()) }
    }

    @Test
    fun invoke_whenHighestDieIs10_callsReuseDie() {
        // Arrange
        fakePlayer.addDieToHand(d6)
        fakePlayer.addDieToHand(d8)
        fakePlayer.addDieToHand(d10)

        // Act
        SUT(fakePlayer)

        // Assert
        verify { mockEffectReuseDie(fakePlayer, rerollOkay = false) }
        verify(exactly = 0) { mockEffectReuseCard(any()) }
    }

    @Test
    fun invoke_whenHighestDieGreaterThan10_callsReuseDie() {
        // Arrange
        fakePlayer.addDieToHand(d6)
        fakePlayer.addDieToHand(d8)
        fakePlayer.addDieToHand(d12)

        // Act
        SUT(fakePlayer)

        // Assert
        verify { mockEffectReuseDie(fakePlayer, rerollOkay = false) }
        verify(exactly = 0) { mockEffectReuseCard(any()) }
    }

    @Test
    fun invoke_whenMultipleDiceWithSameHighestSides_usesFirstOne() {
        // Arrange
        val d10_2 = sampleDie.d10.adjustTo(6)
        fakePlayer.addDieToHand(d10)
        fakePlayer.addDieToHand(d10_2)

        // Act
        SUT(fakePlayer)

        // Assert
        verify { mockEffectReuseDie(fakePlayer, rerollOkay = false) }
        verify(exactly = 0) { mockEffectReuseCard(any()) }
    }
} 
