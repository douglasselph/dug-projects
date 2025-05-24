package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.game.turn.cost.EvaluateSimpleCost
import dugsolutions.leaf.game.turn.cost.CoverCost
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandlePurchaseDieTest {
    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
        private val sampleCost = Cost(listOf(
            CostElement.FlourishTypePresent(FlourishType.VINE),
            CostElement.SingleDieMinimum(17),
            CostElement.TotalDiceMinimum(21)
        ))
    }

    private val sampleDie = SampleDie()
    private lateinit var handlePurchaseDie: HandlePurchaseDie
    private lateinit var coverCost: CoverCost
    private lateinit var evaluateSimpleCost: EvaluateSimpleCost
    private lateinit var dieCost: DieCost
    private lateinit var market: Market
    private lateinit var chronicle: GameChronicle
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        coverCost = mockk(relaxed = true)
        evaluateSimpleCost = mockk(relaxed = true)
        dieCost = mockk(relaxed = true)
        market = mockk(relaxed = true)
        chronicle = mockk(relaxed = true)

        // Create HandlePurchaseDie instance
        handlePurchaseDie = HandlePurchaseDie(market, chronicle, coverCost, evaluateSimpleCost, dieCost)

        // Create mock player
        player = mockk(relaxed = true)
        every { player.id } returns PLAYER_ID
        every { player.name } returns PLAYER_NAME
    }

    @Test
    fun invoke_whenPlayerHasEnoughDice_acquiresDieAndDiscardsDice() {
        // Arrange
        val die = sampleDie.d8
        val d4 = sampleDie.d4.adjustTo(2)
        val d6 = sampleDie.d6.adjustTo(6)
        val d8 = sampleDie.d8.adjustTo(4)
        val diceInHand = Dice(listOf(d4, d6, d8))
        val diceToDiscard = listOf(d4, d6)
        val diceToDiscard2 = Dice(diceToDiscard)

        every { player.diceInHand } returns diceInHand
        
        // Mock the cost calculation chain
        val dieCostValue = 3 // Example die cost
        every { dieCost(die) } returns dieCostValue
        every { coverCost(player, sampleCost) } returns diceToDiscard

        // Act
        val result = handlePurchaseDie(die, player)

        // Assert
        assertTrue(result)
        verify { evaluateSimpleCost(player, diceToDiscard) }
        verify { player.addDieToCompost(die) }
        verify { market.removeDie(die) }
        verify { chronicle(GameChronicle.Moment.ACQUIRE_DIE(player, die, diceToDiscard2)) }
    }

    @Test
    fun invoke_whenPlayerDoesNotHaveEnoughDice_returnsFalse() {
        // Arrange
        val die = sampleDie.d8
        val d4 = sampleDie.d4
        val diceInHand = Dice(listOf(d4))

        every { player.diceInHand } returns diceInHand
        
        // Mock the cost calculation chain to return empty list (can't afford)
        val dieCostValue = 3 // Example die cost
        every { dieCost(die) } returns dieCostValue
        every { coverCost(player, sampleCost) } returns emptyList()

        // Act
        val result = handlePurchaseDie(die, player)

        // Assert
        assertFalse(result)
        verify(exactly = 0) {
            evaluateSimpleCost(any(), any())
            player.addDieToCompost(any())
            market.removeDie(any())
            chronicle(any())
        }
    }

    @Test
    fun invoke_whenPlayerHasExactDiceNeeded_acquiresDieAndDiscardsAllDice() {
        // Arrange
        val die = sampleDie.d8
        val d4 = sampleDie.d4.adjustTo(1)
        val d6 = sampleDie.d6.adjustTo(5)
        val d8 = sampleDie.d8.adjustTo(2)
        val diceInHand = Dice(listOf(d4, d6, d8))
        val diceToDiscard = listOf(d4, d6, d8)
        val diceToDiscard2 = Dice(diceToDiscard)

        every { player.diceInHand } returns diceInHand
        
        // Mock the cost calculation chain
        val dieCostValue = 8 // Example die cost that requires all dice
        every { dieCost(die) } returns dieCostValue
        every { coverCost(player, sampleCost) } returns diceToDiscard

        // Act
        val result = handlePurchaseDie(die, player)

        // Assert
        assertTrue(result)
        verify { evaluateSimpleCost(player, diceToDiscard) }
        verify { player.addDieToCompost(die) }
        verify { market.removeDie(die) }
        verify { chronicle(GameChronicle.Moment.ACQUIRE_DIE(player, die, diceToDiscard2)) }
    }

    @Test
    fun invoke_whenDiePurchase_acquiresDieDiscardingDice() {
        // Arrange
        val die = sampleDie.d4
        val d6 = sampleDie.d6.adjustTo(3)
        val d8 = sampleDie.d8.adjustTo(4)
        val diceInHand = Dice(listOf(d6, d8))
        val diceToDiscard = listOf(d8)
        val diceToDiscard2 = Dice(diceToDiscard)

        every { player.diceInHand } returns diceInHand
        
        // Mock the cost calculation chain
        val dieCostValue = 4 // Example die cost that requires one d8
        every { dieCost(die) } returns dieCostValue
        every { coverCost(player, sampleCost) } returns diceToDiscard

        // Act
        val result = handlePurchaseDie(die, player)

        // Assert
        assertTrue(result)
        verify { evaluateSimpleCost(player, diceToDiscard) }
        verify { player.addDieToCompost(die) }
        verify { market.removeDie(die) }
        verify { chronicle(GameChronicle.Moment.ACQUIRE_DIE(player, die, diceToDiscard2)) }
    }

    @Test
    fun invoke_whenPlayerHasNoDice_returnsFalse() {
        // Arrange
        val die = sampleDie.d4
        val diceInHand = Dice()

        every { player.diceInHand } returns diceInHand
        
        // Mock the cost calculation chain to return empty list (can't afford)
        val dieCostValue = 2 // Example die cost
        every { dieCost(die) } returns dieCostValue
        every { coverCost(player, sampleCost) } returns emptyList()

        // Act
        val result = handlePurchaseDie(die, player)

        // Assert
        assertFalse(result)
        verify(exactly = 0) {
            evaluateSimpleCost(any(), any())
            player.addDieToCompost(any())
            market.removeDie(any())
            chronicle(any())
        }
    }
}
