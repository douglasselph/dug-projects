package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.list.GameCardIDs
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreatureManagerTest {

    private val mockCardManager = mockk<CardManager>(relaxed = true)
    private val SUT: CreatureManager = CreatureManager(mockCardManager)

    companion object {
        private const val ROOT_CARD_ID = 1
        private const val VINE_CARD_ID = 2
        private const val FLOWER_CARD_ID = 3
        private const val INVALID_CARD_ID = 66
    }

    @BeforeEach
    fun setUp() {
        // Setup mock cards
        every { mockCardManager.getCard(ROOT_CARD_ID) } returns GameCard(
            id = ROOT_CARD_ID,
            name = "Test Root",
            type = FlourishType.ROOT,
            resilience = 1,
            cost = mockk(),
            phase = mockk(),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = mockk(),
            matchEffect = null,
            matchValue = 0,
            count = 1
        )
        
        every { mockCardManager.getCard(VINE_CARD_ID) } returns GameCard(
            id = VINE_CARD_ID,
            name = "Test Vine",
            type = FlourishType.VINE,
            resilience = 1,
            cost = mockk(),
            phase = mockk(),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = mockk(),
            matchEffect = null,
            matchValue = 0,
            count = 1
        )
        
        every { mockCardManager.getCard(FLOWER_CARD_ID) } returns GameCard(
            id = FLOWER_CARD_ID,
            name = "Test Flower",
            type = FlourishType.FLOWER,
            resilience = 1,
            cost = mockk(),
            phase = mockk(),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = mockk(),
            matchEffect = null,
            matchValue = 0,
            count = 1
        )
        
        every { mockCardManager.getCard(INVALID_CARD_ID) } returns GameCard(
            id = INVALID_CARD_ID,
            name = "Invalid Card",
            type = FlourishType.CANOPY, // Not allowed type
            resilience = 1,
            cost = mockk(),
            phase = mockk(),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = mockk(),
            matchEffect = null,
            matchValue = 0,
            count = 1
        )
        
        every { mockCardManager.getCard("nonexistent") } returns null
    }

    @Test
    fun addCard_whenValidRootCard_returnsTrue() {
        // Act
        val result = SUT.addCard(ROOT_CARD_ID)
        
        // Assert
        assertTrue(result)
        assertTrue(SUT.isPositionOccupied(0, -1))
    }

    @Test
    fun addCard_whenValidVineCard_returnsTrue() {
        // Arrange
        val r1 = SUT.addCard(VINE_CARD_ID)
        val r2 = SUT.addCard(VINE_CARD_ID)
        val r3 = SUT.addCard(VINE_CARD_ID)

        // Act
        val result = SUT.addCard(VINE_CARD_ID)
        
        // Assert
        assertTrue(r1)
        assertTrue(r2)
        assertTrue(r3)
        assertTrue(result)
    }

    @Test
    fun addCard_whenValidFlowerCard_returnsTrue() {
        // Act
        val result = SUT.addCard(FLOWER_CARD_ID)
        
        // Assert
        assertTrue(result)
    }

    @Test
    fun addCard_whenInvalidCardType_returnsFalse() {
        // Act
        val result = SUT.addCard(INVALID_CARD_ID)
        
        // Assert
        assertFalse(result)
    }

    @Test
    fun addCard_afterAllFlowers_returnsFalse() {
        // Arrange
        SUT.addCard(FLOWER_CARD_ID)
        SUT.addCard(FLOWER_CARD_ID)
        SUT.addCard(FLOWER_CARD_ID)

        // Act - Try to add another card to the same position
        val result = SUT.addCard(VINE_CARD_ID)
        
        // Assert
        assertFalse(result)
    }

    @Test
    fun leafCards_whenFlowerCardAdded_returnsFlowerCard() {
        // Arrange
        SUT.addCard(FLOWER_CARD_ID)
        
        // Act
        val leafCards = SUT.leafCards
        
        // Assert
        assertEquals(1, leafCards.size)
        assertEquals(FLOWER_CARD_ID, leafCards[0].id)
    }

    @Test
    fun leafCards_whenRootCardAdded_returnsRootCard() {
        // Arrange
        SUT.addCard(ROOT_CARD_ID)
        
        // Act
        val leafCards = SUT.leafCards
        
        // Assert
        assertEquals(1, leafCards.size)
        assertEquals(ROOT_CARD_ID, leafCards[0].id)
    }

    @Test
    fun getAllCards_returnsAllAddedCards() {
        // Arrange
        SUT.addCard(ROOT_CARD_ID)
        SUT.addCard(VINE_CARD_ID)
        SUT.addCard(FLOWER_CARD_ID)
        
        // Act
        val allCards = SUT.allCards
        
        // Assert
        assertEquals(3, allCards.size)
        assertTrue(allCards.any { it.id == ROOT_CARD_ID })
        assertTrue(allCards.any { it.id == VINE_CARD_ID })
        assertTrue(allCards.any { it.id == FLOWER_CARD_ID })
    }

    @Test
    fun getCardAt_whenPositionHasCard_returnsCard() {
        // Arrange
        SUT.addCard(ROOT_CARD_ID)
        
        // Act
        val card = SUT.getCardAt(0, -1)
        
        // Assert
        assertNotNull(card)
        assertEquals(ROOT_CARD_ID, card?.id)
    }

    @Test
    fun getCardAt_whenPositionEmpty_returnsNull() {
        // Act
        val card = SUT.getCardAt(0, 0)
        
        // Assert
        assertNull(card)
    }

    @Test
    fun graftedDice_whenNoRoots_returnsEmptyList() {
        // Act
        val dice = SUT.graftedDice
        
        // Assert
        assertTrue(dice.isEmpty())
    }

    @Test
    fun graftedDice_whenOneRoot_returnsOneDie() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        SUT.addCard(ROOT_CARD_ID)
        SUT.addDie(mockDie)
        
        // Act
        val dice = SUT.graftedDice
        
        // Assert
        assertEquals(1, dice.size)
        assertEquals(mockDie, dice[0])
    }

    @Test
    fun graftedDice_whenMultipleRoots_returnsMultipleDice() {
        // Arrange
        val mockDie1 = mockk<Die>(relaxed = true)
        val mockDie2 = mockk<Die>(relaxed = true)
        val mockDie3 = mockk<Die>(relaxed = true)
        
        SUT.addCard(ROOT_CARD_ID)
        SUT.addCard(ROOT_CARD_ID)
        SUT.addDie(mockDie1)
        SUT.addDie(mockDie2)
        SUT.addDie(mockDie3)
        
        // Act
        val dice = SUT.graftedDice
        
        // Assert
        assertEquals(3, dice.size)
        assertTrue(dice.contains(mockDie1))
        assertTrue(dice.contains(mockDie2))
        assertTrue(dice.contains(mockDie3))
    }

    @Test
    fun pullDie_whenDieExists_returnsRolledDie() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        every { mockDie.sides } returns 6
        every { mockDie.roll() } returns mockDie

        SUT.addCard(ROOT_CARD_ID)
        SUT.addDie(mockDie)
        
        // Act
        val result = SUT.pullDie(DieSides.D6)
        
        // Assert
        assertNotNull(result)
        assertEquals(mockDie, result)
        assertEquals(0, SUT.graftedDice.size)
    }

    @Test
    fun pullDie_whenDieNotExists_returnsNull() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        every { mockDie.sides } returns 4
        
        SUT.addCard(ROOT_CARD_ID)
        SUT.addDie(mockDie)
        
        // Act
        val result = SUT.pullDie(DieSides.D6)
        
        // Assert
        assertNull(result)
        assertEquals(1, SUT.graftedDice.size)
    }

    @Test
    fun addDie_whenAtLimit_returnsFalse() {
        // Arrange
        val mockDie1 = mockk<Die>(relaxed = true)
        val mockDie2 = mockk<Die>(relaxed = true)
        
        SUT.addCard(ROOT_CARD_ID) // 1 root = max 2 dice
        SUT.addDie(mockDie1)
        SUT.addDie(mockDie2)
        
        // Act
        val result = SUT.addDie(mockk<Die>(relaxed = true))
        
        // Assert
        assertFalse(result)
        assertEquals(2, SUT.graftedDice.size)
    }

    @Test
    fun addDie_whenUnderLimit_returnsTrue() {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
        SUT.addCard(ROOT_CARD_ID) // 1 root = max 2 dice
        
        // Act
        val result = SUT.addDie(mockDie)
        
        // Assert
        assertTrue(result)
        assertEquals(1, SUT.graftedDice.size)
    }

    @Test
    fun addCard_whenOneVineThenFourFlowers_allReturnTrue() {
        // Act & Assert - 1 VINE card
        assertTrue(SUT.addCard(VINE_CARD_ID))
        
        // Act & Assert - 4 FLOWER cards
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        
        // Act & Assert - Another VINE should fail (no vine path)
        assertFalse(SUT.addCard(VINE_CARD_ID))
    }

    @Test
    fun addCard_whenTwoVinesThenFiveFlowers_allReturnTrue() {
        // Act & Assert - 2 VINE cards
        assertTrue(SUT.addCard(VINE_CARD_ID))
        assertTrue(SUT.addCard(VINE_CARD_ID))
        
        // Act & Assert - 5 FLOWER cards
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        
        // Act & Assert - Another VINE should fail (no vine path)
        assertFalse(SUT.addCard(VINE_CARD_ID))
    }

    @Test
    fun addCard_whenVineFlowerVineFlowerPattern_allReturnTrue() {
        // Act & Assert - 1 VINE card
        assertTrue(SUT.addCard(VINE_CARD_ID))
        
        // Act & Assert - 2 FLOWER cards
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        
        // Act & Assert - Another VINE card
        assertTrue(SUT.addCard(VINE_CARD_ID))
        
        // Act & Assert - 3 more FLOWER cards
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))
        assertTrue(SUT.addCard(FLOWER_CARD_ID))

        // Act & Assert - Another VINE should fail (no vine path)
        assertFalse(SUT.addCard(VINE_CARD_ID))
    }
}
