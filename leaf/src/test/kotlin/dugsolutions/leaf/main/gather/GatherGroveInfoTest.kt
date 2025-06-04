package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.turn.select.SelectAllDice
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GatherGroveInfoTest {

    companion object {
        private const val TEST_CARD_NAME = "Test Card"
        private const val PLAYER_NAME = "Test Player"
        private const val NUM_CARDS = 5
        private const val PIP_TOTAL = 10
    }

    private val testCard = GameCard(
        id = 1,
        name = TEST_CARD_NAME,
        type = FlourishType.ROOT,
        resilience = 3,
        cost = Cost(emptyList()),
        primaryEffect = CardEffect.DRAW_CARD,
        primaryValue = 1,
        matchWith = MatchWith.None,
        matchEffect = null,
        matchValue = 0,
        trashEffect = null,
        trashValue = 0,
        thorn = 0
    )

    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockGatherCardInfo = mockk<GatherCardInfo>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockSelectAllDice = mockk<SelectAllDice>(relaxed = true)
    private lateinit var mockCards: GameCardIDs
    private lateinit var mockPlayer: Player
    private val sampleDie = SampleDie()

    private val SUT = GatherGroveInfo(mockGrove, mockGatherCardInfo, mockSelectAllDice)

    @BeforeEach
    fun setup() {
        mockCards = mockk<GameCardIDs>()
        every { mockCards.getCard(0) } returns testCard
        every { mockCards.size } returns NUM_CARDS

        mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns PLAYER_NAME
        every { mockPlayer.pipTotal } returns PIP_TOTAL

        every { mockGrove.getCardsFor(any()) } returns mockCards
        every { mockGatherCardInfo(any(), any(), any()) } returns mockCardInfo
        every { mockCardInfo.name } returns testCard.name
        
        // Default mock for selectAllDice
        every { mockSelectAllDice() } returns Dice()
    }

    @Test
    fun invoke_whenGroveHasCards_returnsCompleteGroveInfo() {
        // Arrange
        // Act
        val result = SUT()

        // Assert
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        result.stacks.forEachIndexed { index, stack ->
            assertEquals(NUM_CARDS, stack.numCards)
            assertEquals(mockCardInfo, stack.topCard)
            verify { mockGatherCardInfo(index = index, incoming = testCard, highlight = HighlightInfo.NONE) }
        }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
    }

    @Test
    fun invoke_whenGroveHasEmptyStack_returnsEmptyStackInfo() {
        // Arrange
        every { mockGrove.getCardsFor(any()) } returns null

        // Act
        val result = SUT()

        // Assert
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        result.stacks.forEach { stack ->
            assertEquals(0, stack.numCards)
            assertNull(stack.topCard)
        }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
    }

    @Test
    fun invoke_whenGroveHasMixedStacks_returnsCorrectStackInfo() {
        // Arrange
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.ROOT_1) } returns mockCards
        every { mockGrove.getCardsFor(MarketStackID.ROOT_2) } returns mockCards

        // Act
        val result = SUT()

        // Assert
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        
        // Check populated stacks
        result.stacks.filter { it.stack == MarketStackID.ROOT_1 || it.stack == MarketStackID.ROOT_2 }
            .forEach { stack ->
                assertEquals(NUM_CARDS, stack.numCards)
                assertNotNull(stack.topCard)
                assertEquals(testCard.name, stack.topCard?.name)
            }

        // Check empty stacks
        result.stacks.filter { it.stack != MarketStackID.ROOT_1 && it.stack != MarketStackID.ROOT_2 }
            .forEach { stack ->
                assertEquals(0, stack.numCards)
                assertNull(stack.topCard)
            }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
    }

    @Test
    fun invoke_whenHighlightCardProvided_returnsHighlightedCards() {
        // Arrange
        val highlightCards = listOf(testCard)

        // Act
        val result = SUT(highlightCard = highlightCards)

        // Assert
        result.stacks.forEachIndexed { index, stack ->
            if (stack.topCard != null) {
                verify { mockGatherCardInfo(index = index, incoming = testCard, highlight = HighlightInfo.SELECTABLE) }
            }
        }
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
    }

    @Test
    fun invoke_whenHighlightDieProvided_returnsHighlightedDice() {
        // Arrange
        val highlightDice = listOf(sampleDie.d6, sampleDie.d8)

        // Act
        val result = SUT(highlightDie = highlightDice)

        // Assert
        assertNotNull(result.dice)
        assertEquals(2, result.dice.values.size)
        result.dice.values.forEachIndexed { index, dieInfo ->
            assertEquals(index, dieInfo.index)
            assertEquals(highlightDice[index].sides.toString(), dieInfo.value)
            assertEquals(HighlightInfo.SELECTABLE, dieInfo.highlight)
            assertEquals(highlightDice[index], dieInfo.backingDie)
        }
    }

    @Test
    fun invoke_whenSelectForPlayerProvided_returnsSelectText() {
        // Arrange
        // Act
        val result = SUT(selectForPlayer = mockPlayer)

        // Assert
        assertEquals("$PLAYER_NAME PIPS $PIP_TOTAL", result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
    }

    @Test
    fun invoke_whenAllParametersProvided_returnsCompleteInfo() {
        // Arrange
        val highlightCards = listOf(testCard)
        val highlightDice = listOf(sampleDie.d6, sampleDie.d8)

        // Act
        val result = SUT(
            highlightCard = highlightCards,
            highlightDie = highlightDice,
            selectForPlayer = mockPlayer
        )

        // Assert
        // Check stacks
        result.stacks.forEachIndexed { index, stack ->
            if (stack.topCard != null) {
                verify { mockGatherCardInfo(index = index, incoming = testCard, highlight = HighlightInfo.SELECTABLE) }
            }
        }

        // Check dice
        assertNotNull(result.dice)
        assertEquals(2, result.dice.values.size)
        result.dice.values.forEachIndexed { index, dieInfo ->
            assertEquals(index, dieInfo.index)
            assertEquals(highlightDice[index].sides.toString(), dieInfo.value)
            assertEquals(HighlightInfo.SELECTABLE, dieInfo.highlight)
            assertEquals(highlightDice[index], dieInfo.backingDie)
        }

        // Check instruction
        assertEquals("$PLAYER_NAME PIPS $PIP_TOTAL", result.instruction)
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsEmptyDice_setsQuantitiesToEmptyString() {
        // Arrange
        val emptyDice = Dice()
        every { mockSelectAllDice() } returns emptyDice

        // Act
        val result = SUT()

        // Assert
        assertEquals(emptyDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsSingleDie_setsQuantitiesToDieString() {
        // Arrange
        val singleDie = Dice(listOf(sampleDie.d6))
        every { mockSelectAllDice() } returns singleDie

        // Act
        val result = SUT()

        // Assert
        assertEquals(singleDie.toString(), result.quantities)
        verify { mockSelectAllDice() }
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsMultipleDice_setsQuantitiesToDiceString() {
        // Arrange
        val multipleDice = Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8))
        every { mockSelectAllDice() } returns multipleDice

        // Act
        val result = SUT()

        // Assert
        assertEquals(multipleDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsMixedDice_setsQuantitiesToFormattedString() {
        // Arrange
        val mixedDice = Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d6))
        every { mockSelectAllDice() } returns mixedDice

        // Act
        val result = SUT()

        // Assert
        assertEquals(mixedDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
    }
} 
