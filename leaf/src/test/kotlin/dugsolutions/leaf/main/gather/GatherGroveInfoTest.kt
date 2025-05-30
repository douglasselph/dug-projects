package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GatherGroveInfoTest {

    companion object {
        private val testCard = GameCard(
            id = 1,
            name = "Test Card",
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
        private const val NUM_CARDS = 5
    }

    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockGatherCardInfo = mockk<GatherCardInfo>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private lateinit var mockCards: GameCardIDs

    private lateinit var SUT: GatherGroveInfo

    @BeforeEach
    fun setup() {
        SUT = GatherGroveInfo(mockGrove, mockGatherCardInfo)

        mockCards = mockk<GameCardIDs>()
        every { mockCards.getCard(0) } returns testCard
        every { mockCards.size } returns NUM_CARDS

        every { mockGrove.getCardsFor(any()) } returns mockCards
        every { mockGatherCardInfo(testCard) } returns mockCardInfo
        every { mockCardInfo.name } returns testCard.name
    }

    @Test
    fun invoke_whenGroveHasCards_returnsCompleteGroveInfo() {
        // Arrange
        // Act
        val result = SUT()

        // Assert
        assertEquals(MarketStackID.entries.size, result.stacks.size)
        result.stacks.forEach { stack ->
            assertEquals(NUM_CARDS, stack.numCards)
            assertEquals(mockCardInfo, stack.topCard)
        }
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
    }
} 
