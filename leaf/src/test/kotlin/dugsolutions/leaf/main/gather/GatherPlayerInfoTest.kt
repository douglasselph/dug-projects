package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GatherPlayerInfoTest {

    companion object {
        private const val PLAYER_NAME = "Test Player"
        private const val EMPTY_PLAYER_NAME = "Empty Player"
        private val D6_VALUE = DieInfo(value = "D6=4")
        private val D4_COUNT = DieInfo(value = "2D4")
        private val D6_COUNT = DieInfo(value = "1D6")
        private val D8_COUNT = DieInfo(value = "1D8")
    }

    private val mockGatherCardInfo = mockk<GatherCardInfo>(relaxed = true)
    private val mockGatherDiceInfo = mockk<GatherDiceInfo>(relaxed = true)
    private val mockCardInfo = mockk<CardInfo>(relaxed = true)
    private val mockDiceInfo = mockk<DiceInfo>(relaxed = true)
    private lateinit var sampleDie: SampleDie
    private lateinit var SUT: GatherPlayerInfo

    @BeforeEach
    fun setup() {
        sampleDie = SampleDie()
        SUT = GatherPlayerInfo(mockGatherCardInfo, mockGatherDiceInfo)

        every { mockCardInfo.name } returns FakeCards.rootCard.name
        every { mockDiceInfo.values } returns listOf(D6_VALUE)
    }

    @Test
    fun invoke_whenPlayerHasAllComponents_returnsCompletePlayerInfo() {
        // Arrange
        val mockPlayer = mockk<Player>(relaxed = true)

        // Setup hand cards
        val handCard = FakeCards.rootCard
        val handName = handCard.name
        every { mockPlayer.cardsInHand } returns listOf(handCard)
        every { mockGatherCardInfo(any(), handCard, any()) } returns mockCardInfo
        every { mockPlayer.score } returns PlayerScore(1, 3, 3)

        // Setup hand dice
        val handDice = Dice(listOf(sampleDie.d6.adjustTo(4)))
        every { mockPlayer.diceInHand } returns handDice
        every { mockGatherDiceInfo(handDice, true) } returns mockDiceInfo

        // Setup supply dice
        val supplyDice = Dice(
            listOf(
                sampleDie.d4.adjustTo(1),
                sampleDie.d4.adjustTo(2),
                sampleDie.d6.adjustTo(3)
            )
        )
        every { mockPlayer.diceInSupply } returns supplyDice
        every { mockGatherDiceInfo(supplyDice, false) } returns DiceInfo(listOf(D4_COUNT, D6_COUNT))

        // Setup compost dice
        val compostDice = Dice(listOf(sampleDie.d8.adjustTo(5)))
        every { mockPlayer.diceInBed } returns compostDice
        every { mockGatherDiceInfo(compostDice, false) } returns DiceInfo(listOf(D8_COUNT))

        // Setup floral cards
        val floralCard = FakeCards.flowerCard
        val floralName = floralCard.name
        val mockFlowerCardInfo = mockk<CardInfo>(relaxed = true)
        every { mockFlowerCardInfo.name } returns floralName

        every { mockPlayer.floralCards } returns listOf(floralCard)
        every { mockGatherCardInfo(any(), floralCard, any()) } returns mockFlowerCardInfo

        // Setup card counts
        every { mockPlayer.cardsInSupplyCount } returns 3
        every { mockPlayer.cardsInBedCount } returns 2
        every { mockPlayer.name } returns PLAYER_NAME

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(PLAYER_NAME, result.name)
        assertEquals(1, result.handCards.size)
        assertEquals(handName, result.handCards[0].name)
        assertEquals(listOf(D6_VALUE), result.handDice.values)
        assertEquals(listOf(D4_COUNT, D6_COUNT), result.supplyDice.values)
        assertEquals(listOf(D8_COUNT), result.bedDice.values)
        assertEquals(1, result.floralArray.size)
        assertEquals(floralName, result.floralArray[0].name)
        assertEquals(3, result.supplyCardCount)
        assertEquals(2, result.bedCardCount)

        // Verify gatherCardInfo calls
        verify { mockGatherCardInfo(index = 0, card = handCard, highlight = any()) }
        verify { mockGatherCardInfo(index = 0, card = floralCard, highlight = any()) }

        // Verify gatherDiceInfo calls
        verify { mockGatherDiceInfo(handDice, true) }
        verify { mockGatherDiceInfo(supplyDice, false) }
        verify { mockGatherDiceInfo(compostDice, false) }
    }

    @Test
    fun invoke_whenPlayerHasEmptyComponents_returnsEmptyPlayerInfo() {
        // Arrange
        val mockPlayer = mockk<Player>(relaxed = true)
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockPlayer.diceInHand } returns Dice(emptyList())
        every { mockPlayer.diceInSupply } returns Dice(emptyList())
        every { mockPlayer.diceInBed } returns Dice(emptyList())
        every { mockPlayer.floralCards } returns emptyList()
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.cardsInBedCount } returns 0
        every { mockPlayer.name } returns EMPTY_PLAYER_NAME
        every { mockPlayer.score } returns PlayerScore(1, 0, 0)

        every { mockGatherDiceInfo(any(), any()) } returns DiceInfo(emptyList())

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(EMPTY_PLAYER_NAME, result.name)
        assertTrue(result.handCards.isEmpty())
        assertTrue(result.handDice.values.isEmpty())
        assertTrue(result.supplyDice.values.isEmpty())
        assertTrue(result.bedDice.values.isEmpty())
        assertTrue(result.floralArray.isEmpty())
        assertEquals(0, result.supplyCardCount)
        assertEquals(0, result.bedCardCount)

        // Verify gatherDiceInfo calls
        verify { mockGatherDiceInfo(Dice(emptyList()), true) }
        verify { mockGatherDiceInfo(Dice(emptyList()), false) }
    }

    @Test
    fun invoke_whenPlayerHasMultipleFloralCards_returnsSortedFloralArray() {
        // Arrange
        val mockPlayer = mockk<Player>(relaxed = true)
        val flowerNameA = "AFlower"
        val flowerNameB = "BFlower"
        val flowerNameC = "CFlower"
        
        // Create floral cards in unsorted order
        val floralCardA = FakeCards.flowerCard.copy(name = flowerNameA)
        val floralCardB = FakeCards.flowerCard2.copy(name = flowerNameB)
        val floralCardC = FakeCards.flowerCard3.copy(name = flowerNameC)
        
        // Set up cards in unsorted order
        every { mockPlayer.floralCards } returns listOf(floralCardB, floralCardA, floralCardC)
        
        // Create mock card info objects
        val mockFlowerCardAInfo = mockk<CardInfo>(relaxed = true)
        val mockFlowerCardBInfo = mockk<CardInfo>(relaxed = true)
        val mockFlowerCardCInfo = mockk<CardInfo>(relaxed = true)
        
        every { mockFlowerCardAInfo.name } returns floralCardA.name
        every { mockFlowerCardBInfo.name } returns floralCardB.name
        every { mockFlowerCardCInfo.name } returns floralCardC.name
        
        // Set up gatherCardInfo to return different mocks for each card
        every { mockGatherCardInfo(0, floralCardA, any()) } returns mockFlowerCardAInfo
        every { mockGatherCardInfo(1, floralCardB, any()) } returns mockFlowerCardBInfo
        every { mockGatherCardInfo(2, floralCardC, any()) } returns mockFlowerCardCInfo
        
        // Set up other player properties
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockPlayer.diceInHand } returns Dice(emptyList())
        every { mockPlayer.diceInSupply } returns Dice(emptyList())
        every { mockPlayer.diceInBed } returns Dice(emptyList())
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.cardsInBedCount } returns 0
        every { mockPlayer.name } returns PLAYER_NAME
        every { mockPlayer.score } returns PlayerScore(1, 0, 0)
        every { mockGatherDiceInfo(any(), any()) } returns DiceInfo(emptyList())

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(3, result.floralArray.size)
        
        // Verify cards are sorted by name (alphabetical order)
        assertEquals(floralCardA.name, result.floralArray[0].name) // flowerCardA should be first
        assertEquals(floralCardB.name, result.floralArray[1].name) // flowerCardB should be second
        assertEquals(floralCardC.name, result.floralArray[2].name) // flowerCardC should be third
        
        // Verify gatherCardInfo was called with correct indices for sorted order
        verify { mockGatherCardInfo(index = 0, card = floralCardA, highlight = any()) }
        verify { mockGatherCardInfo(index = 1, card = floralCardB, highlight = any()) }
        verify { mockGatherCardInfo(index = 2, card = floralCardC, highlight = any()) }
    }
} 
