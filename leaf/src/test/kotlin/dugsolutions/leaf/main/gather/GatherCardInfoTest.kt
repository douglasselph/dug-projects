package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.main.domain.HighlightInfo
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GatherCardInfoTest {

    companion object {
        private const val TEST_CARD_NAME = "Test Card"
        private const val BASIC_CARD_NAME = "Basic Card"
        private const val ROLL_MATCH_CARD_NAME = "Roll Match Card"
        private const val TYPE_MATCH_CARD_NAME = "Type Match Card"
    }

    private val mockMatchingBloomCard: MatchingBloomCard = mockk(relaxed = true)
    private val SUT = GatherCardInfo(mockMatchingBloomCard)

    @Test
    fun invoke_whenCardHasAllEffects_returnsCompleteCardInfo() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = TEST_CARD_NAME,
            type = FlourishType.ROOT,
            resilience = 13,
            nutrient = 1,
            cost = Cost.from(listOf(CostElement.SingleDieMinimum(2))),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 2,
            matchWith = MatchWith.OnFlourishType(FlourishType.VINE),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = CardEffect.DEFLECT,
            trashValue = 3,
            thorn = 1
        )

        // Act
        val result = SUT(index = 5, card = card)

        // Assert
        assertEquals(5, result.index)
        assertEquals(TEST_CARD_NAME, result.name)
        assertEquals("R", result.type)
        assertEquals(13, result.resilience)
        assertEquals(1, result.nutrient)
        assertEquals(1, result.thorn)
        assertEquals("DrawCard 2", result.primary)
        assertEquals("V DrawDie 1", result.match)
        assertEquals("Deflect 3", result.trash)
        assertEquals(HighlightInfo.NONE, result.highlight)
    }

    @Test
    fun invoke_whenCardHasNoEffects_returnsBasicCardInfo() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = BASIC_CARD_NAME,
            type = FlourishType.VINE,
            resilience = 6,
            nutrient = 1,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        // Act
        val result = SUT(index = 0, card = card)

        // Assert
        assertEquals(0, result.index)
        assertEquals(BASIC_CARD_NAME, result.name)
        assertEquals("V", result.type)
        assertEquals(6, result.resilience)
        assertEquals(1, result.nutrient)
        assertEquals(0, result.thorn)
        assertNull(result.primary)
        assertNull(result.match)
        assertNull(result.trash)
        assertEquals(HighlightInfo.NONE, result.highlight)
    }

    @Test
    fun invoke_whenCardHasOnRollMatch_returnsCorrectMatchString() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = ROLL_MATCH_CARD_NAME,
            type = FlourishType.CANOPY,
            resilience = 14,
            nutrient = 3,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.OnRoll(6),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        // Act
        val result = SUT(index = 2, card = card)

        // Assert
        assertEquals(2, result.index)
        assertEquals("6 DrawDie 1", result.match)
        assertEquals(HighlightInfo.NONE, result.highlight)
    }

    @Test
    fun invoke_whenCardHasOnFlourishTypeMatch_returnsCorrectMatchString() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = TYPE_MATCH_CARD_NAME,
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.OnFlourishType(FlourishType.ROOT),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        // Act
        val result = SUT(index = 3, card = card)

        // Assert
        assertEquals(3, result.index)
        assertEquals("R DrawDie 1", result.match)
        assertEquals(HighlightInfo.NONE, result.highlight)
    }

    @Test
    fun invoke_whenHighlightProvided_returnsCardInfoWithHighlight() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = TEST_CARD_NAME,
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
        val highlight = HighlightInfo.SELECTED

        // Act
        val result = SUT(index = 1, card = card, highlight = highlight)

        // Assert
        assertEquals(1, result.index)
        assertEquals(highlight, result.highlight)
    }
} 
