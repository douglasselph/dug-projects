package dugsolutions.leaf.main.gather

import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
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
        private const val HAND_CARD_NAME = "Hand Card"
        private const val FLORAL_CARD_NAME = "Floral Card"
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

        every { mockCardInfo.name } returns HAND_CARD_NAME
        every { mockDiceInfo.values } returns listOf(D6_VALUE)
    }

    @Test
    fun invoke_whenPlayerHasAllComponents_returnsCompletePlayerInfo() {
        // Arrange
        val mockPlayer = mockk<Player>(relaxed = true)

        // Setup hand cards
        val handCard = GameCard(
            id = 1,
            name = HAND_CARD_NAME,
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
        every { mockPlayer.diceInCompost } returns compostDice
        every { mockGatherDiceInfo(compostDice, false) } returns DiceInfo(listOf(D8_COUNT))

        // Setup floral cards
        val floralCard = GameCard(
            id = 2,
            name = FLORAL_CARD_NAME,
            type = FlourishType.FLOWER,
            resilience = 2,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.ADORN,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
        val mockFlowerCardInfo = mockk<CardInfo>(relaxed = true)
        every { mockFlowerCardInfo.name } returns FLORAL_CARD_NAME

        every { mockPlayer.floralCards } returns listOf(floralCard)
        every { mockGatherCardInfo(any(), floralCard, any()) } returns mockFlowerCardInfo

        // Setup card counts
        every { mockPlayer.cardsInSupplyCount } returns 3
        every { mockPlayer.cardsInCompostCount } returns 2
        every { mockPlayer.name } returns PLAYER_NAME

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(PLAYER_NAME, result.name)
        assertEquals(1, result.handCards.size)
        assertEquals(HAND_CARD_NAME, result.handCards[0].name)
        assertEquals(listOf(D6_VALUE), result.handDice.values)
        assertEquals(listOf(D4_COUNT, D6_COUNT), result.supplyDice.values)
        assertEquals(listOf(D8_COUNT), result.compostDice.values)
        assertEquals(1, result.floralArray.size)
        assertEquals(FLORAL_CARD_NAME, result.floralArray[0].name)
        assertEquals(3, result.supplyCardCount)
        assertEquals(2, result.compostCardCount)

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
        every { mockPlayer.diceInCompost } returns Dice(emptyList())
        every { mockPlayer.floralCards } returns emptyList()
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.cardsInCompostCount } returns 0
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
        assertTrue(result.compostDice.values.isEmpty())
        assertTrue(result.floralArray.isEmpty())
        assertEquals(0, result.supplyCardCount)
        assertEquals(0, result.compostCardCount)

        // Verify gatherDiceInfo calls
        verify { mockGatherDiceInfo(Dice(emptyList()), true) }
        verify { mockGatherDiceInfo(Dice(emptyList()), false) }
    }
} 
