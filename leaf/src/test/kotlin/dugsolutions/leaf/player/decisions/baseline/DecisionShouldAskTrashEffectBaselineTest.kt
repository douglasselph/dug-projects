package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.grove.local.GroveNearingTransition
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecisionShouldAskTrashEffectBaselineTest {

    companion object {
        private val fakeSeedling = FakeCards.seedlingCard
        private val fakeVineUnsupported = FakeCards.vineCard
        private val fakeNoTrashEffect = FakeCards.rootCard.copy(trashEffect = null)
    }

    private val groveNearingTransition = mockk<GroveNearingTransition>(relaxed = true)
    private val SUT: DecisionShouldProcessTrashEffectBaseline = DecisionShouldProcessTrashEffectBaseline(groveNearingTransition)

    @BeforeEach
    fun setup() {
        assertTrue(fakeNoTrashEffect.trashEffect == null)
        assertTrue(fakeSeedling.trashEffect != null)
        assertTrue(fakeVineUnsupported.trashEffect != null)
        every { groveNearingTransition() } returns false
    }

    @Test
    fun invoke_whenCardHasPrimaryOrMatchYetDoesHaveTrash_returnsTrash() = runBlocking {
        // Arrange
        val testCard = FakeCards.canopyCard.copy(primaryEffect = null, matchEffect = null, trashEffect = CardEffect.DRAW_CARD)
        // Act
        val result = SUT(testCard)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.TRASH, result)
    }

    @Test
    fun invoke_whenNoEffects_returnsNoTrash() = runBlocking {
        // Arrange
        val testCard = FakeCards.canopyCard.copy(primaryEffect = null, matchEffect = null, trashEffect = null)

        // Act
        val result = SUT(testCard)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun invoke_whenCardIsNotSeedling_returnsDoNotTrash() = runBlocking {
        // Arrange
        every { groveNearingTransition() } returns true
        val testCard = FakeCards.canopyCard.copy(
            primaryEffect = CardEffect.DRAW_CARD,
            matchEffect = CardEffect.RETAIN_CARD,
            trashEffect = CardEffect.DRAW_CARD_DISCARD
        )

        // Act
        val result = SUT(testCard)

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
    fun invoke_whenCardHasOnlyPrimaryEffect_returnsDoNotTrash() = runBlocking {
        // Arrange
        val cardWithPrimaryOnly = FakeCards.canopyCard.copy(
            primaryEffect = CardEffect.DRAW_CARD,
            matchEffect = null
        )
        every { groveNearingTransition() } returns true

        // Act
        val result = SUT(cardWithPrimaryOnly)

        // Assert
        assertEquals(DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH, result)
    }

    @Test
    fun invoke_whenCardHasOnlyMatchEffect_returnsDoNotTrash() = runBlocking {
        // Arrange
        val cardWithMatchOnly = FakeCards.canopyCard.copy(
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
    fun invoke_whenCardHasBothEffects_returnsDoNotTrash() = runBlocking {
        // Arrange
        val cardWithBothEffects = FakeCards.canopyCard.copy(
            primaryEffect = CardEffect.DRAW_CARD,
            matchEffect = CardEffect.DRAW_DIE
        )
        every { groveNearingTransition() } returns true

        // Act
        val result = SUT(cardWithBothEffects)

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
