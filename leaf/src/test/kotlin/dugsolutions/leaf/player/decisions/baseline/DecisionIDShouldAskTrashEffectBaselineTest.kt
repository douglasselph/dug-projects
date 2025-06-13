package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecisionIDShouldAskTrashEffectBaselineTest {

    companion object {
        private val fakeSeedling = FakeCards.fakeSeedling
        private val fakeVineUnsupported = FakeCards.fakeVine
        private val fakeNoEffect = FakeCards.fakeRoot
    }

    private val mockPlayer: Player = mockk(relaxed = true)
    private val groveNearingTransition = mockk<GroveNearingTransition>(relaxed = true)
    private val gameTime = GameTime()

    private val SUT: DecisionShouldProcessTrashEffectBaseline = DecisionShouldProcessTrashEffectBaseline(mockPlayer, groveNearingTransition, gameTime)

    @BeforeEach
    fun setup() {
        assertTrue(fakeNoEffect.trashEffect == null)
        assertTrue(fakeSeedling.trashEffect != null)
        assertTrue(fakeVineUnsupported.trashEffect != null)
    }

    @Test
    fun invoke_whenCardHasNoEffects_returnsTrash() = runBlocking {
        // Arrange
        every { groveNearingTransition() } returns false

        // Act
        val result = SUT(fakeNoEffect)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.TRASH, result)
    }

    @Test
    fun invoke_whenCardIsNotSeedling_returnsDoNotTrash() = runBlocking {
        // Arrange
        every { groveNearingTransition() } returns true

        // Act
        val result = SUT(fakeVineUnsupported)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun invoke_whenCardIsSeedlingAndGroveNearingTransition_returnsTrash() = runBlocking {
        // Arrange
        every { groveNearingTransition() } returns true

        // Act
        val result = SUT(fakeSeedling)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.TRASH, result)
    }

    @Test
    fun invoke_whenCardIsSeedlingAndGroveNotNearingTransition_returnsDoNotTrash() = runBlocking {
        // Arrange
        every { groveNearingTransition() } returns false

        // Act
        val result = SUT(fakeSeedling)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun invoke_whenCardHasOnlyPrimaryEffect_checksTypeAndGroveStatus() = runBlocking {
        // Arrange
        val cardWithPrimaryOnly = FakeCards.fakeSeedling.copy(
            primaryEffect = CardEffect.DRAW_CARD,
            matchEffect = null
        )
        every { groveNearingTransition() } returns true

        // Act
        val result = SUT(cardWithPrimaryOnly)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.TRASH, result)
    }

    @Test
    fun invoke_whenCardHasOnlyMatchEffect_checksTypeAndGroveStatus() = runBlocking {
        // Arrange
        val cardWithMatchOnly = FakeCards.fakeSeedling.copy(
            primaryEffect = null,
            matchEffect = CardEffect.DRAW_CARD
        )
        every { groveNearingTransition() } returns false

        // Act
        val result = SUT(cardWithMatchOnly)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun reset_whenCalled_doesNotThrow() {
        // Arrange
        // Act & Assert
        SUT.reset()
    }

} 
