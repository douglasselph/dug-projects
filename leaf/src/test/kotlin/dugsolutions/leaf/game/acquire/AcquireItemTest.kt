package dugsolutions.leaf.game.acquire

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.acquire.cost.ApplyCostTD
import dugsolutions.leaf.game.acquire.cost.ApplyEffects
import dugsolutions.leaf.game.acquire.credit.CombinationGenerator
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AcquireItemTest {

    companion object {
        private const val TEST_ID = 123
        private val SAMPLE_TYPE = FlourishType.ROOT
        private const val FLOWER_ID = 456
    }

    // Dependencies
    private lateinit var mockCombinationGenerator: CombinationGenerator
    private lateinit var mockAcquireCardEvaluator: AcquireCardEvaluator
    private lateinit var mockAcquireDieEvaluator: AcquireDieEvaluator
    private lateinit var mockManageAcquiredFloralTypes: ManageAcquiredFloralTypes
    private lateinit var mockApplyEffects: ApplyEffects
    private lateinit var applyCostTD: ApplyCostTD
    private lateinit var mockChronicle: GameChronicle
    private lateinit var mockGrove: Grove

    // Test data
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard
    private lateinit var mockFlowerCard: GameCard
    private lateinit var mockCombination: Combination
    private lateinit var mockDie: Die
    private lateinit var mockCardChoice: AcquireCardEvaluator.Choice
    private lateinit var mockFlowerCardChoice: AcquireCardEvaluator.Choice
    private lateinit var mockDieChoice: DecisionAcquireSelect.BuyItem.Die
    private lateinit var mockDieBestChoice: AcquireDieEvaluator.BestChoice
    private lateinit var mockCombinations: Combinations
    private lateinit var mockMarketCards: List<GameCard>

    // Test subject
    private lateinit var SUT: AcquireItem

    @BeforeEach
    fun setup() {
        // Setup mocks
        mockCombinationGenerator = mockk(relaxed = true)
        mockAcquireCardEvaluator = mockk(relaxed = true)
        mockAcquireDieEvaluator = mockk(relaxed = true)
        mockManageAcquiredFloralTypes = mockk(relaxed = true)
        mockApplyEffects = mockk(relaxed = true)
        applyCostTD = ApplyCostTD()
        mockChronicle = mockk(relaxed = true)
        mockGrove = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)

        // Create the test subject
        SUT = AcquireItem(
            mockCombinationGenerator,
            mockAcquireCardEvaluator,
            mockAcquireDieEvaluator,
            mockManageAcquiredFloralTypes,
            applyCostTD,
            mockGrove,
            mockChronicle
        )

        // Setup common test data
        mockCard = mockk(relaxed = true) {
            every { id } returns TEST_ID
            every { type } returns SAMPLE_TYPE
        }
        mockFlowerCard = mockk(relaxed = true) {
            every { id } returns FLOWER_ID
            every { type } returns FlourishType.FLOWER
        }
        mockCombination = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)
        mockCombinations = mockk(relaxed = true)
        mockMarketCards = listOf(mockCard, mockFlowerCard)

        // Setup card and die choices
        mockCardChoice = AcquireCardEvaluator.Choice(mockCard, mockCombination)
        mockFlowerCardChoice = AcquireCardEvaluator.Choice(mockFlowerCard, mockCombination)
        mockDieBestChoice = AcquireDieEvaluator.BestChoice(mockDie, mockCombination)
        mockDieChoice = DecisionAcquireSelect.BuyItem.Die(mockDieBestChoice)

        // Default behavior - generate combinations
        every { mockCombinationGenerator(mockPlayer) } returns mockCombinations
    }

    @Test
    fun invoke_whenPlayerChoosesNonFlowerCard_addsCardToCompost() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        val cardSelection = DecisionAcquireSelect.BuyItem.Card(mockCardChoice)
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns cardSelection

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        // Verify through Test Double
        assert(applyCostTD.gotPlayers.contains(mockPlayer))
        assert(applyCostTD.gotCombinations.contains(mockCombination))
        assert(applyCostTD.callbackWasInvoked)

        // Verify other interactions through mocks
        verify { mockPlayer.addCardToCompost(TEST_ID) }
        verify(exactly = 0) { mockPlayer.addCardToFloralArray(any()) }
        verify { mockManageAcquiredFloralTypes.add(SAMPLE_TYPE) }
        verify { mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>()) }
    }

    @Test
    fun invoke_whenPlayerChoosesFlowerCard_addsCardToFloralArray() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockFlowerCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        val cardSelection = DecisionAcquireSelect.BuyItem.Card(mockFlowerCardChoice)
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockFlowerCardChoice, mockDieBestChoice) } returns cardSelection

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        // Verify through Test Double
        assert(applyCostTD.gotPlayers.contains(mockPlayer))
        assert(applyCostTD.gotCombinations.contains(mockCombination))
        assert(applyCostTD.callbackWasInvoked)

        // Verify other interactions through mocks
        verify { mockPlayer.addCardToFloralArray(FLOWER_ID) }
        verify(exactly = 0) { mockPlayer.addCardToCompost(any()) }
        verify { mockManageAcquiredFloralTypes.add(FlourishType.FLOWER) }
        verify { mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>()) }
    }

    @Test
    fun invoke_whenPlayerChoosesDie_addsDieToCompost() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns mockDieChoice

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert

        // Verify through Test Double
        assert(applyCostTD.gotPlayers.contains(mockPlayer))
        assert(applyCostTD.gotCombinations.contains(mockCombination))
        assert(applyCostTD.callbackWasInvoked)

        // Verify other interactions through mocks
        verify { mockPlayer.addDieToCompost(mockDie) }
        verify { mockChronicle(any<GameChronicle.Moment.ACQUIRE_DIE>()) }
        verify(exactly = 0) { mockManageAcquiredFloralTypes.add(any()) }
    }

    @Test
    fun invoke_whenPlayerChoosesNothing_doesNothing() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns DecisionAcquireSelect.BuyItem.None

        val die1 = mockk<Die>(relaxed = true)
        val die2 = mockk<Die>(relaxed = true)
        val playerDice = listOf(die1, die2)
        every { mockPlayer.diceInHand.dice } returns playerDice

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
        }

        // Verify no interactions with Test Double and other mocks
        assert(applyCostTD.gotPlayers.isEmpty())
        assert(applyCostTD.gotCombinations.isEmpty())
        assert(!applyCostTD.callbackWasInvoked)
        verify(exactly = 0) { mockManageAcquiredFloralTypes.add(any()) }
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withEmptyMarketCards_stillEvaluatesOptions() = runBlocking {
        // Arrange
        val emptyMarketCards = emptyList<GameCard>()

        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, emptyMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns DecisionAcquireSelect.BuyItem.None

        every { mockPlayer.diceInHand.dice } returns emptyList()

        // Act
        SUT(mockPlayer, emptyMarketCards)

        // Assert
        verify { mockCombinationGenerator(mockPlayer) }
        coVerify { mockAcquireCardEvaluator(mockPlayer, mockCombinations, emptyMarketCards) }
        verify { mockAcquireDieEvaluator(mockCombinations) }
        verify { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) }
    }

    @Test
    fun invoke_whenPlayerChoosesCard_followsCorrectSequence() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        val cardSelection = DecisionAcquireSelect.BuyItem.Card(mockCardChoice)
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns cardSelection

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
            mockPlayer.addCardToCompost(TEST_ID)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>())
            mockManageAcquiredFloralTypes.add(SAMPLE_TYPE)
        }
    }

    @Test
    fun invoke_whenPlayerChoosesDie_followsCorrectSequence() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns mockDieChoice

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
            mockPlayer.addDieToCompost(mockDie)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_DIE>())
        }
    }

    @Test
    fun invoke_whenNoCombinationsAvailable_stillEvaluatesOptions() = runBlocking {
        // Arrange
        val emptyCombinations = mockk<Combinations>(relaxed = true)
        every { mockCombinationGenerator(mockPlayer) } returns emptyCombinations
        coEvery { mockAcquireCardEvaluator(mockPlayer, emptyCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(emptyCombinations) } returns mockDieBestChoice
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns DecisionAcquireSelect.BuyItem.None

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, emptyCombinations, mockMarketCards)
            mockAcquireDieEvaluator(emptyCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
        }
    }

    @Test
    fun invoke_whenNoCardChoiceAvailable_stillEvaluatesDieOptions() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns null
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice
        every { mockPlayer.decisionDirector.acquireSelectDecision(null, mockDieBestChoice) } returns mockDieChoice

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(null, mockDieBestChoice)
            mockPlayer.addDieToCompost(mockDie)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_DIE>())
        }
    }

    @Test
    fun invoke_whenNoDieChoiceAvailable_stillEvaluatesCardOptions() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns null
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, null) } returns DecisionAcquireSelect.BuyItem.Card(mockCardChoice)

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, null)
            mockPlayer.addCardToCompost(TEST_ID)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>())
            mockManageAcquiredFloralTypes.add(SAMPLE_TYPE)
        }
    }

    @Test
    fun invoke_whenPlayerChoosesCard_removesCardFromGrove() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        val cardSelection = DecisionAcquireSelect.BuyItem.Card(mockCardChoice)
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns cardSelection

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
            mockPlayer.addCardToCompost(TEST_ID)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>())
            mockGrove.removeCard(TEST_ID)
            mockManageAcquiredFloralTypes.add(SAMPLE_TYPE)
        }
    }

    @Test
    fun invoke_whenPlayerChoosesFlowerCard_removesCardFromGrove() = runBlocking {
        // Arrange
        coEvery { mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockFlowerCardChoice
        every { mockAcquireDieEvaluator(mockCombinations) } returns mockDieBestChoice

        val cardSelection = DecisionAcquireSelect.BuyItem.Card(mockFlowerCardChoice)
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockFlowerCardChoice, mockDieBestChoice) } returns cardSelection

        // Act
        SUT(mockPlayer, mockMarketCards)

        // Assert
        coVerifySequence {
            mockCombinationGenerator(mockPlayer)
            mockAcquireCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockAcquireDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockFlowerCardChoice, mockDieBestChoice)
            mockPlayer.addCardToFloralArray(FLOWER_ID)
            mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>())
            mockGrove.removeCard(FLOWER_ID)
            mockManageAcquiredFloralTypes.add(FlourishType.FLOWER)
        }
    }

} 
