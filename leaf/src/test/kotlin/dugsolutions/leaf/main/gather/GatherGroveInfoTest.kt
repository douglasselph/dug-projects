package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.GameCardIDs
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
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
        private const val PLAYER_NAME = "Test Player"
        private const val NUM_CARDS = 5
        private const val PIP_TOTAL = 10
    }

    private val testRootCard = FakeCards.rootCard
    private val testFlowerCard = FakeCards.flowerCard
    private val testBloomCard = FakeCards.bloomCard
    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockGatherCardInfo = mockk<GatherCardInfo>(relaxed = true)
    private val mockRootCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockFlowerCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockBloomCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockSelectAllDice = mockk<SelectAllDice>(relaxed = true)
    private val mockMatchingBloomCard = mockk<MatchingBloomCard>(relaxed = true)
    private val mockRootCards: GameCardIDs = mockk<GameCardIDs>(relaxed = true)
    private val mockFlowerCards: GameCardIDs = mockk<GameCardIDs>(relaxed = true)
    private val mockEmptyCards: GameCardIDs = mockk<GameCardIDs>(relaxed = true)
    private val mockPlayer: Player = mockk<Player>(relaxed = true)
    private val gameTime = GameTime()
    private val sampleDie = SampleDie()

    private val SUT = GatherGroveInfo(mockGrove, mockGatherCardInfo, mockSelectAllDice, mockMatchingBloomCard, gameTime)

    @BeforeEach
    fun setup() {
        every { mockRootCards.getCard(any()) } returns testRootCard
        every { mockRootCards.size } returns NUM_CARDS
        every { mockEmptyCards.getCard(any()) } returns null
        every { mockEmptyCards.size } returns 0
        every { mockGrove.getCardsFor(any()) } returns mockEmptyCards
        every { mockGrove.getCardsFor(MarketStackID.ROOT_1) } returns mockRootCards
        every { mockGrove.getCardsFor(MarketStackID.ROOT_2) } returns mockRootCards
        every { mockFlowerCards.getCard(any()) } returns testFlowerCard
        every { mockFlowerCards.size } returns NUM_CARDS
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_1) } returns mockFlowerCards
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_2) } returns mockFlowerCards
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_3) } returns mockFlowerCards
        every { mockPlayer.name } returns PLAYER_NAME
        every { mockPlayer.pipTotal } returns PIP_TOTAL
        every { mockGatherCardInfo(any(), testRootCard, any()) } returns mockRootCardInfo
        every { mockGatherCardInfo(any(), testBloomCard, any()) } returns mockBloomCardInfo
        every { mockGatherCardInfo(any(), testFlowerCard, any()) } returns mockFlowerCardInfo
        every { mockRootCardInfo.name } returns testRootCard.name
        every { mockFlowerCardInfo.name } returns testFlowerCard.name
        every { mockBloomCardInfo.name } returns testBloomCard.name
        every { mockSelectAllDice() } returns Dice()
        every { mockMatchingBloomCard(any()) } returns testBloomCard

        gameTime.phase = GamePhase.CULTIVATION
    }

    @Test
    fun invoke_whenGroveHasCards_returnsCompleteGroveInfo() {
        // Arrange
        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        result.stacks.forEachIndexed { index, stack ->
            if (stack.stack == MarketStackID.ROOT_1 || stack.stack == MarketStackID.ROOT_2) {
                assertEquals(NUM_CARDS, stack.numCards)
                assertEquals(mockRootCardInfo, stack.topCard)
                verify { mockGatherCardInfo(index = index, card = testRootCard, highlight = HighlightInfo.NONE) }
            } else if (stack.stack == MarketStackID.FLOWER_1 || stack.stack == MarketStackID.FLOWER_2 || stack.stack == MarketStackID.FLOWER_3) {
                assertEquals(NUM_CARDS, stack.numCards)
                assertEquals(mockFlowerCardInfo, stack.topCard)
                verify { mockGatherCardInfo(index = index, card = testFlowerCard, highlight = HighlightInfo.NONE) }
            }
        }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isNotEmpty()) // No flower stacks by default
    }

    @Test
    fun invoke_whenGroveHasEmptyStack_returnsEmptyStackInfo() {
        // Arrange
        every { mockGrove.getCardsFor(any()) } returns null

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        result.stacks.forEach { stack ->
            assertEquals(0, stack.numCards)
            assertNull(stack.topCard)
        }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isEmpty())
    }

    @Test
    fun invoke_whenGroveHasMixedStacks_returnsCorrectStackInfo() {
        // Arrange
        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(MarketStackID.entries.size, result.stacks.size)

        // Check populated stacks
        result.stacks.filter { it.stack == MarketStackID.ROOT_1 || it.stack == MarketStackID.ROOT_2 }
            .forEach { stack ->
                assertEquals(NUM_CARDS, stack.numCards)
                assertNotNull(stack.topCard)
                assertEquals(testRootCard.name, stack.topCard?.name)
            }

        // Check empty stacks
        result.stacks.filter {
            it.stack != MarketStackID.ROOT_1 && it.stack != MarketStackID.ROOT_2 &&
                    it.stack != MarketStackID.FLOWER_1 && it.stack != MarketStackID.FLOWER_2 && it.stack != MarketStackID.FLOWER_3
        }
            .forEach { stack ->
                assertEquals(0, stack.numCards)
                assertNull(stack.topCard)
            }
        assertNull(result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isNotEmpty())
    }

    @Test
    fun invoke_whenHighlightCardProvided_returnsHighlightedCards() {
        // Arrange
        val highlightCards = listOf(testRootCard)

        // Act
        val result = SUT(highlightCard = highlightCards)

        // Assert
        require(result != null)
        result.stacks.forEachIndexed { index, stack ->
            if (stack.topCard != null) {
                if (stack.stack == MarketStackID.ROOT_1 || stack.stack == MarketStackID.ROOT_2) {
                    verify { mockGatherCardInfo(index = index, card = testRootCard, highlight = HighlightInfo.SELECTABLE) }
                } else {
                    verify { mockGatherCardInfo(index = index, card = any(), highlight = HighlightInfo.NONE) }
                }
            }
        }
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenHighlightDieProvided_returnsHighlightedDice() {
        // Arrange
        val highlightDice = listOf(sampleDie.d6, sampleDie.d8)

        // Act
        val result = SUT(highlightDie = highlightDice)

        // Assert
        require(result != null)
        assertNotNull(result.dice)
        assertEquals(2, result.dice.values.size)
        result.dice.values.forEachIndexed { index, dieInfo ->
            assertEquals(index, dieInfo.index)
            assertEquals(highlightDice[index].sides.toString(), dieInfo.value)
            assertEquals(HighlightInfo.SELECTABLE, dieInfo.highlight)
            assertEquals(highlightDice[index], dieInfo.backingDie)
        }
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenSelectForPlayerProvided_returnsSelectText() {
        // Arrange
        // Act
        val result = SUT(selectForPlayer = mockPlayer)

        // Assert
        require(result != null)
        assertEquals("$PLAYER_NAME PIPS $PIP_TOTAL", result.instruction)
        assertNotNull(result.dice)
        assertTrue(result.dice.values.isEmpty())
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenAllParametersProvided_returnsCompleteInfo() {
        // Arrange
        val highlightCards = listOf(testRootCard)
        val highlightDice = listOf(sampleDie.d6, sampleDie.d8)

        // Act
        val result = SUT(
            highlightCard = highlightCards,
            highlightDie = highlightDice,
            selectForPlayer = mockPlayer
        )

        // Assert
        require(result != null)
        result.stacks.forEachIndexed { index, stack ->
            if (stack.topCard != null) {
                if (stack.stack == MarketStackID.ROOT_1 || stack.stack == MarketStackID.ROOT_2) {
                    verify { mockGatherCardInfo(index = index, card = testRootCard, highlight = HighlightInfo.SELECTABLE) }
                }
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
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsEmptyDice_setsQuantitiesToEmptyString() {
        // Arrange
        val emptyDice = Dice()
        every { mockSelectAllDice() } returns emptyDice

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(emptyDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsSingleDie_setsQuantitiesToDieString() {
        // Arrange
        val singleDie = Dice(listOf(sampleDie.d6))
        every { mockSelectAllDice() } returns singleDie

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(singleDie.toString(), result.quantities)
        verify { mockSelectAllDice() }
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsMultipleDice_setsQuantitiesToDiceString() {
        // Arrange
        val multipleDice = Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d8))
        every { mockSelectAllDice() } returns multipleDice

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(multipleDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_whenSelectAllDiceReturnsMixedDice_setsQuantitiesToFormattedString() {
        // Arrange
        val mixedDice = Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d6))
        every { mockSelectAllDice() } returns mixedDice

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertEquals(mixedDice.toString(), result.quantities)
        verify { mockSelectAllDice() }
        assertNotNull(result.blooms)
    }

    @Test
    fun invoke_phaseIsBattle_returnsNull() {
        // Arrange
        val mixedDice = Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d6))
        every { mockSelectAllDice() } returns mixedDice
        gameTime.phase = GamePhase.BATTLE

        // Act
        val result = SUT()

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenFlowerStackHasMatchingBloom_returnsBloomInfo() {
        // Arrange
        // Set up a flower stack with a matching bloom
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_1) } returns mockFlowerCards
        every { mockMatchingBloomCard(testFlowerCard) } returns testBloomCard

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertNotNull(result.blooms)
        assertEquals(1, result.blooms.size)

        val bloomInfo = result.blooms.first()
        assertEquals(mockBloomCardInfo, bloomInfo)
        verify { mockGatherCardInfo(index = 0, card = testBloomCard) }
        verify { mockMatchingBloomCard(testFlowerCard) }
    }

    @Test
    fun invoke_whenFlowerStackHasNoMatchingBloom_returnsEmptyBlooms() {
        // Arrange
        // Set up a flower stack with no matching bloom
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_1) } returns mockFlowerCards
        every { mockMatchingBloomCard(testFlowerCard) } returns null

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isEmpty())
        verify { mockMatchingBloomCard(testFlowerCard) }
    }

    @Test
    fun invoke_whenMultipleFlowerStacksHaveBlooms_returnsAllBloomInfo() {
        // Arrange
        // Set up multiple flower stacks with matching blooms
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_1) } returns mockFlowerCards
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_2) } returns mockFlowerCards
        every { mockMatchingBloomCard(testFlowerCard) } returns testBloomCard

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertNotNull(result.blooms)
        assertEquals(2, result.blooms.size)

        result.blooms.forEach { bloomInfo ->
            assertEquals(mockBloomCardInfo, bloomInfo)
        }
        verify(exactly = 2) { mockMatchingBloomCard(testFlowerCard) }
        verify(exactly = 2) { mockGatherCardInfo(any(), testBloomCard) }
    }

    @Test
    fun invoke_whenFlowerStackIsEmpty_returnsEmptyBlooms() {
        // Arrange
        // Set up an empty flower stack
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.FLOWER_1) } returns null

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isEmpty())
        verify(exactly = 0) { mockMatchingBloomCard(any()) }
    }

    @Test
    fun invoke_whenNonFlowerStackHasCards_doesNotIncludeInBlooms() {
        // Arrange
        // Set up a non-flower stack (should not be included in blooms)
        every { mockGrove.getCardsFor(any()) } returns null
        every { mockGrove.getCardsFor(MarketStackID.ROOT_1) } returns mockRootCards

        // Act
        val result = SUT()

        // Assert
        require(result != null)
        assertNotNull(result.blooms)
        assertTrue(result.blooms.isEmpty())
        verify(exactly = 0) { mockMatchingBloomCard(any()) }
    }
} 
