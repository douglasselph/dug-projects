package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import net.bytebuddy.matcher.DeclaringAnnotationMatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleAbsorbDamageTest {

    private lateinit var mockPlayer: Player
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var fakeCard1: GameCard
    private lateinit var fakeCard2: GameCard
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: HandleAbsorbDamage

    @BeforeEach
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        mockGameChronicle = mockk(relaxed = true)
        fakeCard1 = FakeCards.fakeRoot
        fakeCard2 = FakeCards.fakeCanopy

        sampleDie = SampleDie()

        SUT = HandleAbsorbDamage(mockGameChronicle)

        every { mockPlayer.removeDieFromHand(any()) } returns true
        every { mockPlayer.removeCardFromHand(any()) } returns true
    }

    @Test
    fun invoke_whenNoIncomingDamage_doesNothing() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns 0

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify(exactly = 0) { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
    }

    @Test
    fun invoke_whenNoAbsorptionDecision_doesNothing() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns 2
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result()

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithCards_removesCards() = runBlocking {
        // Arrange
        every { mockPlayer.incomingDamage } returns 7
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1, fakeCard2),
            dice = emptyList(),
            floralCards = emptyList()
        )

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(fakeCard1.id) }
        verify { mockPlayer.removeCardFromHand(fakeCard2.id) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeCardFromFloralArray(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithDice_removesDice() = runBlocking {
        // Arrange
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        val d10 = sampleDie.d10
        val d12 = sampleDie.d12
        val d20 = sampleDie.d20
        every { mockPlayer.incomingDamage } returns 5
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = emptyList(),
            dice = listOf(d4, d6, d8, d10, d12, d20),
            floralCards = emptyList()
        )

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.removeDieFromHand(d8) }
        verify { mockPlayer.removeDieFromHand(d10) }
        verify { mockPlayer.removeDieFromHand(d12) }
        verify(exactly = 0) { mockPlayer.removeCardFromFloralArray(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithCardsAndDice_removesBoth() = runBlocking {
        // Arrange
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        every { mockPlayer.incomingDamage } returns 2
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1),
            dice = listOf(d4, d6),
            floralCards = emptyList()
        )

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(fakeCard1.id) }
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.removeDieFromHand(d6) }
        verify(exactly = 0) { mockPlayer.removeCardFromFloralArray(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithFloralCards_removesFloralCards() = runBlocking {
        // Arrange
        val floralCard1 = FakeCards.fakeFlower
        val floralCard2 = FakeCards.fakeFlower2
        every { mockPlayer.incomingDamage } returns 1
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = emptyList(),
            dice = emptyList(),
            floralCards = listOf(floralCard1, floralCard2)
        )

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
        verify { mockPlayer.removeCardFromFloralArray(floralCard1.id) }
        verify { mockPlayer.removeCardFromFloralArray(floralCard2.id) }
    }

    @Test
    fun invoke_whenHandEmptyAfterAbsorption_clearsFloralArray() = runBlocking {
        // Arrange
        val floralCard1 = FakeCards.fakeFlower
        val floralCard2 = FakeCards.fakeFlower2
        every { mockPlayer.incomingDamage } returns 2
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1),
            dice = emptyList(),
            floralCards = emptyList()
        )
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockPlayer.floralCards } returns listOf(floralCard1, floralCard2)

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(fakeCard1.id) }
        verify { mockPlayer.clearFloralCards() }
        verify { mockGameChronicle(Moment.TRASH_CARD(mockPlayer, floralCard1, floralArray = true)) }
        verify { mockGameChronicle(Moment.TRASH_CARD(mockPlayer, floralCard2, floralArray = true)) }
    }

    @Test
    fun invoke_whenHandNotEmptyAfterAbsorption_doesNotClearFloralArray() = runBlocking {
        // Arrange
        val remainingCard = mockk<GameCard>(relaxed = true)
        every { mockPlayer.incomingDamage } returns 6
        coEvery { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1),
            dice = emptyList(),
            floralCards = emptyList()
        )
        every { mockPlayer.cardsInHand } returns listOf(remainingCard)

        // Act
        SUT(mockPlayer)

        // Assert
        coVerify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(fakeCard1.id) }
        verify(exactly = 0) { mockPlayer.clearFloralCards() }
    }
} 
