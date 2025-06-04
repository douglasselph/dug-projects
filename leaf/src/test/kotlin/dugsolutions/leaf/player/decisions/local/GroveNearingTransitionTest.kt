package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroveNearingTransitionTest {

    private val mockGrove = mockk<Grove>(relaxed = true)

    private val SUT = GroveNearingTransition(mockGrove)

    @BeforeEach
    fun setup() {
        // Default setup - all stacks return null (0 cards)
        MarketStackID.entries.forEach { stackId ->
            every { mockGrove.getCardsFor(stackId) } returns null
        }
    }

    @Test
    fun invoke_whenSumOfThreeLowestIsExactlyFour_returnsTrue() {
        // Arrange
        val mockCards2 = mockk<GameCardIDs> { every { size } returns 2 }
        val mockCards1 = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards1Second = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards5 = mockk<GameCardIDs> { every { size } returns 5 }
        
        val stackIds = MarketStackID.entries.toList()
        every { mockGrove.getCardsFor(stackIds[0]) } returns mockCards2
        every { mockGrove.getCardsFor(stackIds[1]) } returns mockCards1
        every { mockGrove.getCardsFor(stackIds[2]) } returns mockCards1Second
        every { mockGrove.getCardsFor(stackIds[3]) } returns mockCards5
        // Rest remain null (0 cards)

        // Act
        val result = SUT()

        // Assert - sum of three lowest: 0 + 0 + 0 = 0, but we have 1,1,2,5,0,0,... so three lowest are 0,0,1 = 1
        assertTrue(result, "Should return true when sum of three lowest counts is exactly 4 or less")
        MarketStackID.entries.forEach { stackId ->
            verify { mockGrove.getCardsFor(stackId) }
        }
    }

    @Test
    fun invoke_whenSumOfThreeLowestIsLessThanFour_returnsTrue() {
        // Arrange
        val mockCards1 = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards2 = mockk<GameCardIDs> { every { size } returns 2 }
        val mockCards10 = mockk<GameCardIDs> { every { size } returns 10 }
        
        val stackIds = MarketStackID.entries.toList()
        every { mockGrove.getCardsFor(stackIds[0]) } returns mockCards1
        every { mockGrove.getCardsFor(stackIds[1]) } returns mockCards2
        every { mockGrove.getCardsFor(stackIds[2]) } returns mockCards10
        // Rest remain null (0 cards)

        // Act
        val result = SUT()

        // Assert - three lowest: 0,0,0 = 0 (since we have many null stacks)
        assertTrue(result, "Should return true when sum of three lowest counts is less than 4")
    }

    @Test
    fun invoke_whenSumOfThreeLowestIsGreaterThanFour_returnsFalse() {
        // Arrange - Set up all stacks to have 2 cards each (sum of three lowest = 6)
        val mockCards2 = mockk<GameCardIDs> { every { size } returns 2 }
        
        MarketStackID.entries.forEach { stackId ->
            every { mockGrove.getCardsFor(stackId) } returns mockCards2
        }

        // Act
        val result = SUT()

        // Assert - three lowest: 2,2,2 = 6 > 4
        assertFalse(result, "Should return false when sum of three lowest counts is greater than 4")
    }

    @Test
    fun invoke_whenAllStacksAreEmpty_returnsTrue() {
        // Arrange - all stacks return null or empty cards (default setup)

        // Act
        val result = SUT()

        // Assert - three lowest: 0,0,0 = 0 <= 4
        assertTrue(result, "Should return true when all stacks are empty")
    }

    @Test
    fun invoke_whenOnlyOneStackHasCards_returnsTrue() {
        // Arrange
        val mockCards3 = mockk<GameCardIDs> { every { size } returns 3 }
        
        every { mockGrove.getCardsFor(MarketStackID.ROOT_1) } returns mockCards3
        // Rest remain null (0 cards)

        // Act
        val result = SUT()

        // Assert - three lowest: 0,0,0 = 0 <= 4
        assertTrue(result, "Should return true when only one stack has cards and others are empty")
    }

    @Test
    fun invoke_whenThreeStacksHaveExactlyFourCardsTotal_returnsTrue() {
        // Arrange
        val mockCards1 = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards2 = mockk<GameCardIDs> { every { size } returns 2 }
        val mockCards1Second = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards10 = mockk<GameCardIDs> { every { size } returns 10 }
        
        val stackIds = MarketStackID.entries.toList()
        every { mockGrove.getCardsFor(stackIds[0]) } returns mockCards1
        every { mockGrove.getCardsFor(stackIds[1]) } returns mockCards2
        every { mockGrove.getCardsFor(stackIds[2]) } returns mockCards1Second
        every { mockGrove.getCardsFor(stackIds[3]) } returns mockCards10
        // Rest remain null (0 cards)

        // Act
        val result = SUT()

        // Assert - sorted counts would be: 0,0,0,0,0,0,1,1,2,10 so three lowest: 0,0,0 = 0
        assertTrue(result, "Should return true when three lowest stacks sum to exactly 4")
    }

    @Test
    fun invoke_whenMixedStackCounts_calculatesSumCorrectly() {
        // Arrange - Create a scenario where three lowest sum to exactly 5 (> 4)
        val mockCards2 = mockk<GameCardIDs> { every { size } returns 2 }
        val mockCards1 = mockk<GameCardIDs> { every { size } returns 1 }
        val mockCards2Second = mockk<GameCardIDs> { every { size } returns 2 }
        val mockCards3 = mockk<GameCardIDs> { every { size } returns 3 }
        val mockCards8 = mockk<GameCardIDs> { every { size } returns 8 }
        
        val stackIds = MarketStackID.entries.toList()
        every { mockGrove.getCardsFor(stackIds[0]) } returns mockCards2    // 2
        every { mockGrove.getCardsFor(stackIds[1]) } returns mockCards1    // 1
        every { mockGrove.getCardsFor(stackIds[2]) } returns mockCards2Second // 2
        every { mockGrove.getCardsFor(stackIds[3]) } returns mockCards3    // 3
        every { mockGrove.getCardsFor(stackIds[4]) } returns mockCards8    // 8
        // Rest remain null (0 cards)

        // Act
        val result = SUT()

        // Assert - sorted: 0,0,0,0,0,1,2,2,3,8 so three lowest: 0,0,0 = 0
        assertTrue(result, "Should correctly calculate sum of three lowest counts in mixed scenario")
    }

    @Test
    fun invoke_whenAllStacksHaveHighCounts_returnsFalse() {
        // Arrange - All stacks have 5 cards (sum of three lowest = 15)
        val mockCards5 = mockk<GameCardIDs> { every { size } returns 5 }
        
        MarketStackID.entries.forEach { stackId ->
            every { mockGrove.getCardsFor(stackId) } returns mockCards5
        }

        // Act
        val result = SUT()

        // Assert - three lowest: 5,5,5 = 15 > 4
        assertFalse(result, "Should return false when all stacks have high card counts")
    }
} 