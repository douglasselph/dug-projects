package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDieAnyToReroll
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EffectDieRerollAnyTest {

    companion object {
        private const val PLAYER_DIE_VALUE = 3
        private const val OPPONENT_DIE_VALUE = 5
    }

    private val mockSelectDieAnyToReroll: SelectDieAnyToReroll = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockTarget: Player = mockk(relaxed = true)
    private val sampleDie = SampleDie()
    private val playerDie: Die = sampleDie.d6
    private val opponentDie: Die = sampleDie.d8

    private val SUT: EffectDieRerollAny = EffectDieRerollAny(mockSelectDieAnyToReroll, mockChronicle)

    @BeforeEach
    fun setup() {
        playerDie.adjustTo(PLAYER_DIE_VALUE)
        opponentDie.adjustTo(OPPONENT_DIE_VALUE)
    }

    @Test
    fun invoke_whenPlayerDieSelected_chroniclesPlayerReroll() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = null)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, PLAYER_DIE_VALUE)) }
    }

    @Test
    fun invoke_whenOpponentDieSelected_chroniclesOpponentReroll() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockTarget, opponentDie, OPPONENT_DIE_VALUE)) }
    }

    @Test
    fun invoke_whenBothDiceAvailable_playerDieTakesPrecedence() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, PLAYER_DIE_VALUE)) }
        verify(exactly = 0) { mockChronicle(Moment.REROLL(mockTarget, opponentDie, OPPONENT_DIE_VALUE)) }
    }

    @Test
    fun invoke_whenNoDiceSelected_doesNothing() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = null)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenPlayerDieHasDifferentValue_chroniclesCorrectBeforeValue() {
        // Arrange
        val differentValue = 4
        playerDie.adjustTo(differentValue)
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = null)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, differentValue)) }
    }

    @Test
    fun invoke_whenOpponentDieHasDifferentValue_chroniclesCorrectBeforeValue() {
        // Arrange
        val differentValue = 6
        opponentDie.adjustTo(differentValue)
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockTarget, opponentDie, differentValue)) }
    }

    @Test
    fun invoke_whenPlayerDieIsMaxValue_stillChroniclesReroll() {
        // Arrange
        playerDie.adjustTo(playerDie.sides) // Set to maximum value
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = null)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, playerDie.sides)) }
    }

    @Test
    fun invoke_whenOpponentDieIsMinValue_stillChroniclesReroll() {
        // Arrange
        opponentDie.adjustTo(1) // Set to minimum value
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockTarget, opponentDie, 1)) }
    }

    @Test
    fun invoke_whenMultipleCalls_chroniclesEachReroll() {
        // Arrange
        val bestDie1 = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = null)
        val bestDie2 = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) }.returnsMany(bestDie1, bestDie2)

        // Act
        SUT(mockPlayer, mockTarget) // First call - player die
        SUT(mockPlayer, mockTarget) // Second call - opponent die

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, PLAYER_DIE_VALUE)) }
        verify { mockChronicle(Moment.REROLL(mockTarget, opponentDie, OPPONENT_DIE_VALUE)) }
    }

    @Test
    fun invoke_whenSelectDieAnyToRerollReturnsNull_doesNothing() {
        // Arrange
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns SelectDieAnyToReroll.BestDie()

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenPlayerDieIsNullButOpponentDieExists_chroniclesOpponentReroll() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = null, opponentDie = opponentDie)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockTarget, opponentDie, OPPONENT_DIE_VALUE)) }
    }

    @Test
    fun invoke_whenOpponentDieIsNullButPlayerDieExists_chroniclesPlayerReroll() {
        // Arrange
        val bestDie = SelectDieAnyToReroll.BestDie(playerDie = playerDie, opponentDie = null)
        every { mockSelectDieAnyToReroll(mockPlayer, mockTarget) } returns bestDie

        // Act
        SUT(mockPlayer, mockTarget)

        // Assert
        verify { mockChronicle(Moment.REROLL(mockPlayer, playerDie, PLAYER_DIE_VALUE)) }
    }

} 
