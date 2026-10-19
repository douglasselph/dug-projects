package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WispManagerTest {

    private lateinit var SUT: WispManager
    private lateinit var mockWispCard: GameCard
    private lateinit var mockNonWispCard: GameCard
    private lateinit var mockAnotherWispCard: GameCard

    companion object {
        private const val WISP_CARD_ID = 1
        private const val NON_WISP_CARD_ID = 2
        private const val ANOTHER_WISP_CARD_ID = 3
    }

    @BeforeEach
    fun setup() {
        SUT = WispManager()
        
        // Create mock wisp card
        mockWispCard = mockk<GameCard>(relaxed = true)
        every { mockWispCard.id } returns WISP_CARD_ID
        every { mockWispCard.type } returns FlourishType.WISP
        
        // Create mock non-wisp card
        mockNonWispCard = mockk<GameCard>(relaxed = true)
        every { mockNonWispCard.id } returns NON_WISP_CARD_ID
        every { mockNonWispCard.type } returns FlourishType.ROOT
        
        // Create another mock wisp card
        mockAnotherWispCard = mockk<GameCard>(relaxed = true)
        every { mockAnotherWispCard.id } returns ANOTHER_WISP_CARD_ID
        every { mockAnotherWispCard.type } returns FlourishType.WISP
    }

    @Test
    fun add_whenValidWispCard_addsSuccessfully() {
        // Act
        SUT.add(mockWispCard)
        
        // Assert
        assertTrue(SUT.has(mockWispCard))
    }

    @Test
    fun add_whenNonWispCard_throwsIllegalArgumentException() {
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockNonWispCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: ROOT", exception.message)
    }

    @Test
    fun add_whenVineCard_throwsIllegalArgumentException() {
        // Arrange
        val mockVineCard = mockk<GameCard>(relaxed = true)
        every { mockVineCard.type } returns FlourishType.VINE
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockVineCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: VINE", exception.message)
    }

    @Test
    fun add_whenFlowerCard_throwsIllegalArgumentException() {
        // Arrange
        val mockFlowerCard = mockk<GameCard>(relaxed = true)
        every { mockFlowerCard.type } returns FlourishType.FLOWER
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockFlowerCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: FLOWER", exception.message)
    }

    @Test
    fun add_whenResourceCard_throwsIllegalArgumentException() {
        // Arrange
        val mockResourceCard = mockk<GameCard>(relaxed = true)
        every { mockResourceCard.type } returns FlourishType.RESOURCE
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockResourceCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: RESOURCE", exception.message)
    }

    @Test
    fun remove_whenWispExists_removesSuccessfully() {
        // Arrange
        SUT.add(mockWispCard)
        assertTrue(SUT.has(mockWispCard))
        
        // Act
        val result = SUT.remove(mockWispCard)
        
        // Assert
        assertTrue(result)
        assertFalse(SUT.has(mockWispCard))
    }

    @Test
    fun remove_whenWispNotExists_returnsFalse() {
        // Act
        val result = SUT.remove(mockWispCard)
        
        // Assert
        assertFalse(result)
    }

    @Test
    fun has_whenWispExists_returnsTrue() {
        // Arrange
        SUT.add(mockWispCard)
        
        // Act
        val result = SUT.has(mockWispCard)
        
        // Assert
        assertTrue(result)
    }

    @Test
    fun has_whenWispNotExists_returnsFalse() {
        // Act
        val result = SUT.has(mockWispCard)
        
        // Assert
        assertFalse(result)
    }

    @Test
    fun add_multipleWispCards_allAddedSuccessfully() {
        // Act
        SUT.add(mockWispCard)
        SUT.add(mockAnotherWispCard)
        
        // Assert
        assertTrue(SUT.has(mockWispCard))
        assertTrue(SUT.has(mockAnotherWispCard))
    }

    @Test
    fun remove_whenMultipleWisps_removesCorrectOne() {
        // Arrange
        SUT.add(mockWispCard)
        SUT.add(mockAnotherWispCard)
        
        // Act
        val result = SUT.remove(mockWispCard)
        
        // Assert
        assertTrue(result)
        assertFalse(SUT.has(mockWispCard))
        assertTrue(SUT.has(mockAnotherWispCard))
    }

    @Test
    fun add_whenSameWispAddedTwice_bothInstancesAdded() {
        // Act
        SUT.add(mockWispCard)
        SUT.add(mockWispCard)
        
        // Assert
        assertTrue(SUT.has(mockWispCard))
        // Note: The list will contain the same card twice, which is valid behavior
    }

    @Test
    fun remove_whenSameWispAddedTwice_removesOneInstance() {
        // Arrange
        SUT.add(mockWispCard)
        SUT.add(mockWispCard)
        
        // Act
        val result = SUT.remove(mockWispCard)
        
        // Assert
        assertTrue(result)
        assertTrue(SUT.has(mockWispCard)) // Still has one instance
    }

    @Test
    fun add_whenCanopyCard_throwsIllegalArgumentException() {
        // Arrange
        val mockCanopyCard = mockk<GameCard>(relaxed = true)
        every { mockCanopyCard.type } returns FlourishType.CANOPY
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockCanopyCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: CANOPY", exception.message)
    }

    @Test
    fun add_whenNoneCard_throwsIllegalArgumentException() {
        // Arrange
        val mockNoneCard = mockk<GameCard>(relaxed = true)
        every { mockNoneCard.type } returns FlourishType.NONE
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SUT.add(mockNoneCard)
        }
        
        assertEquals("Only cards with FlourishType.WISP can be added to WispManager. Received: NONE", exception.message)
    }
}
