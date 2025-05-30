package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class GatherPlayerInfoTest {

    private val gatherCardInfo = GatherCardInfo()
    private val gatherDiceInfo = GatherDiceInfo()
    private lateinit var sampleDie: SampleDie
    private lateinit var SUT: GatherPlayerInfo

    @BeforeEach
    fun setup() {
        sampleDie = SampleDie()
        SUT = GatherPlayerInfo(gatherCardInfo, gatherDiceInfo)
    }

    @Test
    fun invoke_whenPlayerHasAllComponents_returnsCompletePlayerInfo() {
        // Arrange
        val mockPlayer = mockk<Player>()
        
        // Setup hand cards
        val handCard = GameCard(
            id = 1,
            name = "Hand Card",
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

        // Setup hand dice
        val handDice = Dice(listOf(sampleDie.d6.adjustTo(4)))
        every { mockPlayer.diceInHand } returns handDice

        // Setup supply dice
        val supplyDice = Dice(listOf(
            sampleDie.d4.adjustTo(1),
            sampleDie.d4.adjustTo(2),
            sampleDie.d6.adjustTo(3)
        ))
        every { mockPlayer.diceInSupply } returns supplyDice

        // Setup compost dice
        val compostDice = Dice(listOf(sampleDie.d8.adjustTo(5)))
        every { mockPlayer.diceInCompost } returns compostDice

        // Setup floral cards
        val floralCard = GameCard(
            id = 2,
            name = "Floral Card",
            type = FlourishType.FLOWER,
            resilience = 2,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
        every { mockPlayer.floralCards } returns listOf(floralCard)

        // Setup card counts
        every { mockPlayer.cardsInSupplyCount } returns 3
        every { mockPlayer.cardsInCompostCount } returns 2
        every { mockPlayer.name } returns "Test Player"

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals("Test Player", result.name)
        assertEquals(1, result.handCards.size)
        assertEquals("Hand Card", result.handCards[0].name)
        assertEquals(listOf("D6=4"), result.handDice.values)
        assertEquals(listOf("2D4", "1D6"), result.supplyDice.values)
        assertEquals(listOf("1D8"), result.compostDice.values)
        assertEquals(1, result.floralArray.size)
        assertEquals("Floral Card", result.floralArray[0].name)
        assertEquals(3, result.supplyCardCount)
        assertEquals(2, result.compostCardCount)
    }

    @Test
    fun invoke_whenPlayerHasEmptyComponents_returnsEmptyPlayerInfo() {
        // Arrange
        val mockPlayer = mockk<Player>()
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockPlayer.diceInHand } returns Dice(emptyList())
        every { mockPlayer.diceInSupply } returns Dice(emptyList())
        every { mockPlayer.diceInCompost } returns Dice(emptyList())
        every { mockPlayer.floralCards } returns emptyList()
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.cardsInCompostCount } returns 0
        every { mockPlayer.name } returns "Empty Player"

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals("Empty Player", result.name)
        assertTrue(result.handCards.isEmpty())
        assertTrue(result.handDice.values.isEmpty())
        assertTrue(result.supplyDice.values.isEmpty())
        assertTrue(result.compostDice.values.isEmpty())
        assertTrue(result.floralArray.isEmpty())
        assertEquals(0, result.supplyCardCount)
        assertEquals(0, result.compostCardCount)
    }
} 
