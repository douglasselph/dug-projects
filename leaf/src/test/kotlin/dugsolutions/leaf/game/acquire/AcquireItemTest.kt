package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.acquire.cost.ApplyCostTD
import dugsolutions.leaf.game.acquire.credit.CombinationGenerator
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.game.acquire.evaluator.PossibleCards
import dugsolutions.leaf.game.acquire.evaluator.PossibleBestDice
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AcquireItemTest {

    private val mockCombinationGenerator = mockk<CombinationGenerator>(relaxed = true)
    private val mockPossibleCards = mockk<PossibleCards>(relaxed = true)
    private val mockPossibleBestDice = mockk<PossibleBestDice>(relaxed = true)
    private val mockManageAcquiredFloralTypes = mockk<ManageAcquiredFloralTypes>(relaxed = true)
    private val applyCostTD = ApplyCostTD()
    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockChronicle = mockk<GameChronicle>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val sampleDie = SampleDie()

    private val SUT = AcquireItem(
        mockCombinationGenerator,
        mockPossibleCards,
        mockPossibleBestDice,
        mockManageAcquiredFloralTypes,
        applyCostTD,
        mockGrove,
        mockChronicle
    )

    @BeforeEach
    fun setup() {
        val combinations = Combinations(listOf(FakeCombination.combinationD4D6))
        every { mockCombinationGenerator(mockPlayer) } returns combinations
    }

    @Test
    fun invoke_whenCardSelected_acquiresCard() = runBlocking {
        // Arrange
        val marketCards = listOf(FakeCards.fakeRoot)
        val possibleCards = listOf(ChoiceCard(FakeCards.fakeRoot, FakeCombination.combinationD6))
        val possibleDice = emptyList<ChoiceDie>()
        every { mockPossibleCards(mockPlayer, any(), marketCards) } returns possibleCards
        every { mockPossibleBestDice(any()) } returns possibleDice
        coEvery { mockPlayer.decisionDirector.acquireSelectDecision(possibleCards, possibleDice) } returns
                DecisionAcquireSelect.BuyItem.Card(possibleCards[0])

        // Act
        val result = SUT(mockPlayer, marketCards)

        // Assert
        assertTrue(result)
        assertTrue(applyCostTD.callbackWasInvoked)
        coVerify {
            mockGrove.removeCard(FakeCards.fakeRoot.id)
            mockChronicle(Moment.ACQUIRE_CARD(mockPlayer, FakeCards.fakeRoot, FakeCombination.combinationD6))
            mockManageAcquiredFloralTypes.add(FakeCards.fakeRoot.type)
        }
    }

    @Test
    fun invoke_whenDieSelected_acquiresDie() = runBlocking {
        // Arrange
        val marketCards = emptyList<GameCard>()
        val possibleCards = emptyList<ChoiceCard>()
        val expectedDie = sampleDie.d6
        val possibleDice = listOf(ChoiceDie(expectedDie, FakeCombination.combinationD6))
        every { mockPossibleCards(mockPlayer, any(), marketCards) } returns possibleCards
        every { mockPossibleBestDice(any()) } returns possibleDice
        val decisionDirector = mockPlayer.decisionDirector
        coEvery { decisionDirector.acquireSelectDecision(possibleCards, possibleDice) } returns
                DecisionAcquireSelect.BuyItem.Die(possibleDice[0])

        // Act
        val result = SUT(mockPlayer, marketCards)

        // Assert
        assertTrue(result)
        assertTrue(applyCostTD.callbackWasInvoked)

        coVerify {
            mockChronicle(Moment.ACQUIRE_DIE(mockPlayer, expectedDie, FakeCombination.combinationD6))
            mockGrove.removeDie(expectedDie)
        }
    }

    @Test
    fun invoke_whenNoSelection_returnsFalse() = runBlocking {
        // Arrange
        val marketCards = listOf(FakeCards.fakeRoot)
        val possibleCards = listOf(ChoiceCard(FakeCards.fakeRoot, FakeCombination.combinationD8))
        val possibleDice = emptyList<ChoiceDie>()
        every { mockPossibleCards(mockPlayer, any(), marketCards) } returns possibleCards
        every { mockPossibleBestDice(any()) } returns possibleDice
        coEvery { mockPlayer.decisionDirector.acquireSelectDecision(possibleCards, possibleDice) } returns
                DecisionAcquireSelect.BuyItem.None

        // Act
        val result = SUT(mockPlayer, marketCards)

        // Assert
        assertFalse(result)
        assertFalse(applyCostTD.callbackWasInvoked)
        coVerify(exactly = 0) {
            mockGrove.removeCard(any())
            mockChronicle(any())
            mockManageAcquiredFloralTypes.add(any())
        }
    }

    @Test
    fun invoke_whenCostApplicationFails_returnsFalse() = runBlocking {
        // Arrange
        val marketCards = listOf(FakeCards.fakeRoot)
        val possibleCards = listOf(ChoiceCard(FakeCards.fakeRoot, FakeCombination.combinationD6))
        val possibleDice = emptyList<ChoiceDie>()
        every { mockPossibleCards(mockPlayer, any(), marketCards) } returns possibleCards
        every { mockPossibleBestDice(any()) } returns possibleDice
        coEvery { mockPlayer.decisionDirector.acquireSelectDecision(possibleCards, possibleDice) } returns
                DecisionAcquireSelect.BuyItem.Card(possibleCards[0])

        var gotException: Exception? = null
        applyCostTD.respondWithException = Exception("Some fake exception")
        // Act
        val result = try {
            SUT(mockPlayer, marketCards)
        } catch (ex: Exception) {
            gotException = ex
            false
        }

        // Assert
        assertFalse(result)
        assertTrue(gotException != null)
        coVerify(exactly = 0) {
            mockGrove.removeCard(any())
            mockChronicle(any())
            mockManageAcquiredFloralTypes.add(any())
        }
    }

    @Test
    fun invoke_whenFlowerCardSelected_addsToFloralArray() = runBlocking {
        // Arrange
        val marketCards = listOf(FakeCards.fakeFlower)
        val possibleCards = listOf(ChoiceCard(FakeCards.fakeFlower, FakeCombination.combinationD12))
        val possibleDice = emptyList<ChoiceDie>()
        every { mockPossibleCards(mockPlayer, any(), marketCards) } returns possibleCards
        every { mockPossibleBestDice(any()) } returns possibleDice
        coEvery { mockPlayer.decisionDirector.acquireSelectDecision(possibleCards, possibleDice) } returns
                DecisionAcquireSelect.BuyItem.Card(possibleCards[0])

        // Act
        val result = SUT(mockPlayer, marketCards)

        // Assert
        assertTrue(result)
        assertTrue(applyCostTD.callbackWasInvoked)
        coVerify {
            mockGrove.removeCard(FakeCards.fakeFlower.id)
            mockChronicle(Moment.ACQUIRE_CARD(mockPlayer, FakeCards.fakeFlower, FakeCombination.combinationD12))
            mockManageAcquiredFloralTypes.add(FlourishType.FLOWER)
        }
    }
} 
