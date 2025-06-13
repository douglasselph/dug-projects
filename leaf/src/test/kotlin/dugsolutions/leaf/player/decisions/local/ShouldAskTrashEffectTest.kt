package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.ui.support.DecisionMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ShouldAskTrashEffectTest {

    private val monitor = DecisionMonitor()
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val SUT = ShouldAskTrashEffect()
    private val testCard = FakeCards.fakeCanopy

    @BeforeEach
    fun setup() {
        SUT.askTrashOkay = true
    }

    @Test
    fun invoke_whenAskTrashOkayIsTrue_asksPlayerForDecision() = runBlocking {
        // Arrange
        val expectedResult = DecisionShouldProcessTrashEffect.Result.TRASH
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(testCard) } returns expectedResult

        // Act
        val result = SUT(mockPlayer, testCard)

        // Assert
        assertEquals(expectedResult, result)
    }

    @Test
    fun invoke_whenAskTrashOkayIsFalse_returnsDoNotTrash() = runBlocking {
        // Arrange
        SUT.askTrashOkay = false

        // Act
        val result = SUT(mockPlayer, testCard)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun invoke_whenPlayerReturnsTrash_returnsTrash() = runBlocking {
        // Arrange
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(testCard) } returns 
            DecisionShouldProcessTrashEffect.Result.TRASH

        // Act
        val result = SUT(mockPlayer, testCard)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.TRASH, result)
    }

    @Test
    fun invoke_whenPlayerReturnsDoNotTrash_returnsDoNotTrash() = runBlocking {
        // Arrange
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(testCard) } returns 
            DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH

        // Act
        val result = SUT(mockPlayer, testCard)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }
} 
