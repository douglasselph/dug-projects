package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.purchase.cost.ApplyCostTD
import dugsolutions.leaf.game.purchase.cost.ApplyEffects
import dugsolutions.leaf.game.purchase.credit.CombinationGenerator
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.game.purchase.domain.Combinations
import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionAcquireSelect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PurchaseItemTest {

    companion object {
        private const val TEST_ID = 123
        private val SAMPLE_TYPE = FlourishType.ROOT
    }
    // Test subject
    private lateinit var SUT: PurchaseItem

    // Dependencies
    private lateinit var mockCombinationGenerator: CombinationGenerator
    private lateinit var mockPurchaseCardEvaluator: PurchaseCardEvaluator
    private lateinit var mockPurchaseDieEvaluator: PurchaseDieEvaluator
    private lateinit var mockManagePurchasedFloralTypes: ManagePurchasedFloralTypes
    private lateinit var mockApplyEffects: ApplyEffects
    private lateinit var applyCostTD: ApplyCostTD
    private lateinit var mockChronicle: GameChronicle
    
    // Test data
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard
    private lateinit var mockCombination: Combination
    private lateinit var mockDie: Die
    private lateinit var mockCardChoice: PurchaseCardEvaluator.BestChoice
    private lateinit var mockDieChoice: DecisionAcquireSelect.BuyItem.Die
    private lateinit var mockDieBestChoice: PurchaseDieEvaluator.BestChoice
    private lateinit var mockCombinations: Combinations
    private lateinit var mockMarketCards: List<GameCard>
    
    @BeforeEach
    fun setup() {
        // Setup mocks
        mockCombinationGenerator = mockk(relaxed = true)
        mockPurchaseCardEvaluator = mockk(relaxed = true)
        mockPurchaseDieEvaluator = mockk(relaxed = true)
        mockManagePurchasedFloralTypes = mockk(relaxed = true)
        mockApplyEffects = mockk(relaxed = true)
        applyCostTD = ApplyCostTD()
        mockChronicle = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        
        // Create the test subject
        SUT = PurchaseItem(
            mockCombinationGenerator,
            mockPurchaseCardEvaluator,
            mockPurchaseDieEvaluator,
            mockManagePurchasedFloralTypes,
            applyCostTD,
            mockChronicle
        )
        
        // Setup common test data
        mockCard = mockk(relaxed = true) {
            every { id } returns TEST_ID // CardID is a type alias for Int
            every { type } returns SAMPLE_TYPE
        }
        mockCombination = mockk(relaxed = true)
        mockDie = mockk(relaxed = true)
        mockCombinations = mockk(relaxed = true)
        mockMarketCards = listOf(mockCard)
        
        // Setup card and die choices
        mockCardChoice = PurchaseCardEvaluator.BestChoice(mockCard, mockCombination)
        mockDieBestChoice = PurchaseDieEvaluator.BestChoice(mockDie, mockCombination)
        mockDieChoice = DecisionAcquireSelect.BuyItem.Die(mockDieBestChoice)
        
        // Default behavior - generate combinations
        every { mockCombinationGenerator(mockPlayer) } returns mockCombinations
    }

    @Test
    fun invoke_whenPlayerChoosesCard_addsCardToCompost() {
        // Arrange
        every { mockPurchaseCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockPurchaseDieEvaluator(mockCombinations) } returns mockDieBestChoice
        
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
        verify { mockManagePurchasedFloralTypes.add(SAMPLE_TYPE) }
        verify { mockChronicle(any<GameChronicle.Moment.ACQUIRE_CARD>()) }
    }

    @Test
    fun invoke_whenPlayerChoosesDie_addsDieToCompost() {
        // Arrange
        every { mockPurchaseCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockPurchaseDieEvaluator(mockCombinations) } returns mockDieBestChoice
        
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
        verify(exactly = 0) { mockManagePurchasedFloralTypes.add(any()) }
    }
    
    @Test
    fun invoke_whenPlayerChoosesNothing_discardsAllDice() {
        // Arrange
        every { mockPurchaseCardEvaluator(mockPlayer, mockCombinations, mockMarketCards) } returns mockCardChoice
        every { mockPurchaseDieEvaluator(mockCombinations) } returns mockDieBestChoice
        
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns DecisionAcquireSelect.BuyItem.None
        
        val die1 = mockk<Die>(relaxed = true)
        val die2 = mockk<Die>(relaxed = true)
        val playerDice = listOf(die1, die2)
        every { mockPlayer.diceInHand.dice } returns playerDice
        
        // Act
        SUT(mockPlayer, mockMarketCards)
        
        // Assert
        verifySequence {
            mockCombinationGenerator(mockPlayer)
            mockPurchaseCardEvaluator(mockPlayer, mockCombinations, mockMarketCards)
            mockPurchaseDieEvaluator(mockCombinations)
            mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice)
            mockPlayer.discardHand()
        }
        
        // Verify no interactions with Test Double and other mocks
        assert(applyCostTD.gotPlayers.isEmpty())
        assert(applyCostTD.gotCombinations.isEmpty())
        assert(!applyCostTD.callbackWasInvoked)
        verify(exactly = 0) { mockManagePurchasedFloralTypes.add(any()) }
        verify(exactly = 0) { mockChronicle(any()) }
    }
    
    @Test
    fun invoke_withEmptyMarketCards_stillEvaluatesOptions() {
        // Arrange
        val emptyMarketCards = emptyList<GameCard>()
        
        every { mockPurchaseCardEvaluator(mockPlayer, mockCombinations, emptyMarketCards) } returns mockCardChoice
        every { mockPurchaseDieEvaluator(mockCombinations) } returns mockDieBestChoice
        every { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) } returns DecisionAcquireSelect.BuyItem.None
        
        every { mockPlayer.diceInHand.dice } returns emptyList()
        
        // Act
        SUT(mockPlayer, emptyMarketCards)
        
        // Assert
        verify { mockCombinationGenerator(mockPlayer) }
        verify { mockPurchaseCardEvaluator(mockPlayer, mockCombinations, emptyMarketCards) }
        verify { mockPurchaseDieEvaluator(mockCombinations) }
        verify { mockPlayer.decisionDirector.acquireSelectDecision(mockCardChoice, mockDieBestChoice) }
    }

} 
