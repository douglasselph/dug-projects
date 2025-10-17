package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.game.turn.cost.EvaluateSimpleCost
import dugsolutions.leaf.game.turn.cost.CoverCost
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.components.SimpleCost
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HandlePurchaseCardTest {
    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var handlePurchaseCard: HandlePurchaseCard
    private lateinit var cardIsFree: CardIsFree
    private lateinit var coverCost: CoverCost
    private lateinit var evaluateSimpleCost: EvaluateSimpleCost
    private lateinit var market: Market
    private lateinit var chronicle: GameChronicle
    private lateinit var player: Player
    private lateinit var dice: Dice
    private val sampleDie = SampleDie()

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        cardIsFree = mockk(relaxed = true)
        coverCost = mockk(relaxed = true)
        evaluateSimpleCost = mockk(relaxed = true)
        market = mockk(relaxed = true)
        chronicle = mockk(relaxed = true)
        dice = Dice(listOf(sampleDie.d4, sampleDie.d4, sampleDie.d6, sampleDie.d6))

        // Create HandlePurchaseCard instance
        handlePurchaseCard = HandlePurchaseCard(cardIsFree, coverCost, evaluateSimpleCost, market, chronicle)

        // Create mock player
        player = mockk(relaxed = true)
        every { player.id } returns PLAYER_ID
        every { player.name } returns PLAYER_NAME
    }

    @Test
    fun invoke_whenCardIsFree_acquiresCardWithoutDiscardingDice() {
        // Arrange
        val card = FakeCards.fakeRoot
        every { cardIsFree(card, player) } returns false
        every { player.diceInHand } returns dice

        // Act
        val result = handlePurchaseCard(card, player)

        // Assert
        assertTrue(result)
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(card.type))
        verify {
            player.addCardToCompost(card.id)
            market.removeCard(card.id)
            chronicle(GameChronicle.Moment.ACQUIRE_CARD(player, card, dice))
        }
        verify(exactly = 0) { player.discard(any<Die>()) }
    }

    @Test
    fun invoke_whenPlayerHasEnoughDice_acquiresCardAndDiscardsDice() {
        // Arrange
        val card = FakeCards.fakeRoot
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        val diceInHand = Dice(listOf(d4, d6, d8))
        val diceToDiscard = listOf(d4, d6)
        val combination = Dice(diceToDiscard)

        every { cardIsFree(card, player) } returns true
        every { player.diceInHand } returns diceInHand
        every { coverCost(player, SimpleCost(2)) } returns diceToDiscard

        // Act
        val result = handlePurchaseCard(card, player)

        // Assert
        assertTrue(result)
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(card.type))
        verify { evaluateSimpleCost(player, diceToDiscard) }
        verify { player.addCardToCompost(card.id) }
        verify { market.removeCard(card.id) }
        verify { chronicle(GameChronicle.Moment.ACQUIRE_CARD(player, card, combination)) }
    }

    @Test
    fun invoke_whenPlayerDoesNotHaveEnoughDice_returnsFalse() {
        // Arrange
        val card = FakeCards.fakeRoot
        val d4 = sampleDie.d4
        val diceInHand = Dice(listOf(d4))

        every { cardIsFree(card, player) } returns false
        every { player.diceInHand } returns diceInHand
        every { coverCost(player, any<Cost>()) } returns emptyList()

        // Act
        val result = handlePurchaseCard(card, player)

        // Assert
        assertFalse(result)
        assertFalse(handlePurchaseCard.purchasedFlourishTypes.contains(card.type))
        verify(exactly = 0) {
            player.discard(any<Die>())
            player.addCardToCompost(any())
            market.removeCard(any())
            chronicle(any())
        }
    }

    @Test
    fun invoke_whenCardIsFreeAndPlayerHasNoDice_acquiresCard() {
        // Arrange
        val card = FakeCards.fakeRoot
        every { cardIsFree(card, player) } returns true
        every { player.diceInHand } returns Dice()

        // Act
        val result = handlePurchaseCard(card, player)

        // Assert
        assertTrue(result)
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(card.type))
        verify {
            player.addCardToCompost(card.id)
            market.removeCard(card.id)
            chronicle(GameChronicle.Moment.ACQUIRE_CARD(player, card, Dice(emptyList())))
        }
        verify(exactly = 0) { player.discard(any<Die>()) }
    }

    @Test
    fun invoke_whenCardIsFreeAndPlayerHasDice_acquiresCardWithoutDiscardingDice() {
        // Arrange
        val card = FakeCards.fakeRoot
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        val diceInHand = Dice(listOf(d4, d6))

        every { cardIsFree(card, player) } returns true
        every { player.diceInHand } returns diceInHand

        // Act
        val result = handlePurchaseCard(card, player)

        // Assert
        assertTrue(result)
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(card.type))
        verify {
            player.addCardToCompost(card.id)
            market.removeCard(card.id)
            chronicle(GameChronicle.Moment.ACQUIRE_CARD(player, card, Dice(emptyList())))
        }
        verify(exactly = 0) { player.discard(any<Die>()) }
    }

    @Test
    fun invoke_whenMultipleCardsPurchased_tracksAllFlourishTypes() {
        // Arrange
        val rootCard = FakeCards.fakeRoot
        val bloomCard = FakeCards.fakeBloom
        val vineCard = FakeCards.fakeVine

        every { cardIsFree(rootCard, player) } returns true
        every { cardIsFree(bloomCard, player) } returns true
        every { cardIsFree(vineCard, player) } returns true
        every { player.diceInHand } returns Dice()

        // Act
        handlePurchaseCard(rootCard, player)
        handlePurchaseCard(bloomCard, player)
        handlePurchaseCard(vineCard, player)

        // Assert
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(FlourishType.ROOT))
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(FlourishType.BLOOM))
        assertTrue(handlePurchaseCard.purchasedFlourishTypes.contains(FlourishType.VINE))
        assertEquals(3, handlePurchaseCard.purchasedFlourishTypes.size)
    }
} 
