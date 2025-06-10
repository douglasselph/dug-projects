package dugsolutions.leaf.game.battle

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.core.DecisionDamageAbsorption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandleAbsorbDamageTest {

    private val mockGameChronicle: GameChronicle = mockk(relaxed = true)
    private val mockDecisionDirector: DecisionDirector = mockk(relaxed = true)
    private val mockDecisionDamageAbsorption: DecisionDamageAbsorption = mockk(relaxed = true)
    private val fakePlayer: PlayerTD = PlayerTD.create(1, mockDecisionDirector)
    private lateinit var fakeCard1: GameCard
    private lateinit var fakeCard2: GameCard
    private lateinit var sampleDie: SampleDie

    private val SUT: HandleAbsorbDamage = HandleAbsorbDamage(mockGameChronicle)

    @BeforeEach
    fun setup() {
        fakeCard1 = FakeCards.fakeRoot
        fakeCard2 = FakeCards.fakeCanopy
        sampleDie = SampleDie()

        fakePlayer.useDeckManager = false
        fakePlayer.addCardToHand(fakeCard1)
        fakePlayer.gotCardIds.clear()
        fakePlayer.gotDice.clear()

        every { mockDecisionDirector.damageAbsorptionDecision } returns mockDecisionDamageAbsorption
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result()
    }

    @Test
    fun invoke_whenNoIncomingDamage_doesNothing() = runBlocking {
        // Arrange
        fakePlayer.incomingDamage = 0

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify(exactly = 0) { fakePlayer.decisionDirector.damageAbsorptionDecision() }
        assertEquals(0, fakePlayer.gotCardIds.size)
        assertEquals(0, fakePlayer.gotDice.size)
    }

    @Test
    fun invoke_whenNoAbsorptionDecision_removesAllElements() = runBlocking {
        // Arrange
        val result = DecisionDamageAbsorption.Result()
        fakePlayer.addDieToHand(sampleDie.d6)
        fakePlayer.addCardToFloralArray(fakeCard2)
        fakePlayer.useDeckManager = false
        fakePlayer.incomingDamage = 2
        coEvery { mockDecisionDamageAbsorption() } returns result

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertEquals(0, fakePlayer.cardsInHand.size)
        assertEquals(0, fakePlayer.diceInHand.size)
        assertEquals(0, fakePlayer.floralCards.size)
    }

    @Test
    fun invoke_whenAbsorptionResultWithCards_removesCards() = runBlocking {
        // Arrange
        fakePlayer.incomingDamage = 7
        fakePlayer.addCardToHand(fakeCard2)
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1, fakeCard2),
            dice = emptyList(),
            floralCards = emptyList()
        )

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertTrue(fakePlayer.gotCardIds.contains(fakeCard1.id))
        assertTrue(fakePlayer.gotCardIds.contains(fakeCard2.id))
        assertEquals(0, fakePlayer.gotDice.size)
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
        fakePlayer.incomingDamage = 5
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = emptyList(),
            dice = listOf(d4, d6, d8, d10, d12, d20),
            floralCards = emptyList()
        )

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertEquals(0, fakePlayer.gotCardIds.size)
        assertTrue(fakePlayer.gotDice.contains(d4))
        assertTrue(fakePlayer.gotDice.contains(d6))
        assertTrue(fakePlayer.gotDice.contains(d8))
        assertTrue(fakePlayer.gotDice.contains(d10))
        assertTrue(fakePlayer.gotDice.contains(d12))
    }

    @Test
    fun invoke_whenAbsorptionResultWithCardsAndDice_removesBoth() = runBlocking {
        // Arrange
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        fakePlayer.incomingDamage = 2
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1),
            dice = listOf(d4, d6),
            floralCards = emptyList()
        )

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertTrue(fakePlayer.gotCardIds.contains(fakeCard1.id))
        assertTrue(fakePlayer.gotDice.contains(d4))
        assertTrue(fakePlayer.gotDice.contains(d6))
    }

    @Test
    fun invoke_whenAbsorptionResultWithFloralCards_removesFloralCards() = runBlocking {
        // Arrange
        val floralCard1 = FakeCards.fakeFlower
        val floralCard2 = FakeCards.fakeFlower2
        fakePlayer.incomingDamage = 1
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = emptyList(),
            dice = emptyList(),
            floralCards = listOf(floralCard1, floralCard2)
        )

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertTrue(fakePlayer.gotFloralArrayCards.contains(floralCard1.id))
        assertTrue(fakePlayer.gotFloralArrayCards.contains(floralCard2.id))
        assertEquals(0, fakePlayer.gotCardIds.size)
    }

    @Test
    fun invoke_whenHandEmptyYetHasFlowers_usesFloralArray() = runBlocking {
        // Arrange
        fakePlayer.discardHand()
        val floralCard1 = FakeCards.fakeFlower
        val floralCard2 = FakeCards.fakeFlower2
        fakePlayer.addCardToFloralArray(floralCard1)
        fakePlayer.addCardToFloralArray(floralCard2)
        fakePlayer.incomingDamage = 8
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = emptyList(),
            dice = emptyList(),
            floralCards = listOf(floralCard1)
        )

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption() }
        assertTrue(fakePlayer.gotFloralArrayCards.contains(floralCard1.id))
        verify { mockGameChronicle(Moment.TRASH_CARD(fakePlayer, floralCard1, floralArray = true)) }
        verify(exactly = 0) { mockGameChronicle(Moment.TRASH_CARD(fakePlayer, floralCard2, floralArray = true)) }
    }

    @Test
    fun invoke_whenHandNotEmptyAfterAbsorption_doesNotClearFloralArray() = runBlocking {
        // Arrange
        val remainingCard = mockk<GameCard>(relaxed = true)
        fakePlayer.incomingDamage = 6
        coEvery {mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = listOf(fakeCard1),
            dice = emptyList(),
            floralCards = emptyList()
        )
        fakePlayer.addCardToHand(remainingCard)

        // Act
        SUT(fakePlayer)

        // Assert
        coVerify { mockDecisionDamageAbsorption()}
        assertTrue(fakePlayer.gotCardIds.contains(fakeCard1.id))
        assertFalse(fakePlayer.gotClearFloralCards)
    }

    @Test
    fun invoke_whenNoExtendedItems_returnsZero() = runBlocking {
        // Arrange
        fakePlayer.incomingDamage = 5
        fakePlayer.discardHand()

        // Act
        val result = SUT(fakePlayer)

        // Assert
        assertEquals(0, result)
        coVerify(exactly = 0) { mockDecisionDamageAbsorption() }
    }

    @Test
    fun invoke_whenAllEmptyResult_usesAllAvailableItems() = runBlocking {
        // Arrange
        fakePlayer.incomingDamage = 5
        fakePlayer.addCardToHand(fakeCard1)
        fakePlayer.addDieToHand(sampleDie.d6)
        fakePlayer.addCardToFloralArray(FakeCards.fakeFlower)
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result()

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(0, fakePlayer.cardsInHand.size)
        assertEquals(0, fakePlayer.diceInHand.size)
        assertEquals(0, fakePlayer.floralCards.size)
    }

    @Test
    fun invoke_calculatesThornDamageCorrectly() = runBlocking {
        // Arrange
        val thornCard1 = mockk<GameCard> {
            every { id } returns 10
            every { resilience } returns 1
            every { thorn } returns 2
        }
        val thornCard2 = mockk<GameCard> {
            every { id } returns 11
            every { resilience } returns 1
            every { thorn } returns 3
        }
        fakePlayer.incomingDamage = 2
        coEvery {mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = listOf(thornCard1, thornCard2)
        )

        // Act
        val result = SUT(fakePlayer)

        // Assert
        assertEquals(5, result) // 2 + 3 thorn damage
        assertTrue(fakePlayer.gotCardIds.contains(10))
        assertTrue(fakePlayer.gotCardIds.contains(11))
    }

    @Test
    fun invoke_whenDamageBecomesNegative_setsToZero() = runBlocking {
        // Arrange
        val highResilienceCard = mockk<GameCard> {
            every { id } returns 10
            every { resilience } returns 5
            every { thorn } returns 0
        }
        fakePlayer.incomingDamage = 3
        coEvery { mockDecisionDamageAbsorption() } returns DecisionDamageAbsorption.Result(
            cards = listOf(highResilienceCard)
        )

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(0, fakePlayer.incomingDamage)
    }

    @Test
    fun invoke_whenRemainingDamageAndItems_recursivelyAbsorbs() = runBlocking {
        // Arrange
        val card1 = mockk<GameCard> {
            every { id } returns 10
            every { resilience } returns 2
            every { thorn } returns 1
        }
        val card2 = mockk<GameCard> {
            every { id } returns 11
            every { resilience } returns 2
            every { thorn } returns 1
        }
        fakePlayer.incomingDamage = 5
        coEvery { mockDecisionDamageAbsorption() }
            .returnsMany(
                DecisionDamageAbsorption.Result(cards = listOf(card1)),
                DecisionDamageAbsorption.Result(cards = listOf(card2))
            )

        // Act
        val result = SUT(fakePlayer)

        // Assert
        assertEquals(2, result) // Combined thorn damage from both cards
        coVerify(exactly = 2) { mockDecisionDamageAbsorption() }
        assertTrue(fakePlayer.gotCardIds.contains(10))
        assertTrue(fakePlayer.gotCardIds.contains(11))
    }
} 
