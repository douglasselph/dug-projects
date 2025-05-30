package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GatherCardInfoTest {

    private val SUT = GatherCardInfo()

    @Test
    fun invoke_whenCardHasAllEffects_returnsCompleteCardInfo() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = "Test Card",
            type = FlourishType.ROOT,
            resilience = 3,
            cost = Cost(listOf(CostElement.SingleDieMinimum(2))),
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
        val result = SUT(card)

        // Assert
        assertEquals("Test Card", result.name)
        assertEquals("R", result.type)
        assertEquals(3, result.resilience)
        assertEquals(1, result.thorn)
        assertEquals("DrawCard 2", result.primary)
        assertEquals("V DrawDie 1", result.match)
        assertEquals("Deflect 3", result.trash)
    }

    @Test
    fun invoke_whenCardHasNoEffects_returnsBasicCardInfo() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = "Basic Card",
            type = FlourishType.VINE,
            resilience = 2,
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
        val result = SUT(card)

        // Assert
        assertEquals("Basic Card", result.name)
        assertEquals("V", result.type)
        assertEquals(2, result.resilience)
        assertEquals(0, result.thorn)
        assertNull(result.primary)
        assertNull(result.match)
        assertNull(result.trash)
    }

    @Test
    fun invoke_whenCardHasOnRollMatch_returnsCorrectMatchString() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = "Roll Match Card",
            type = FlourishType.CANOPY,
            resilience = 4,
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
        val result = SUT(card)

        // Assert
        assertEquals("6 DrawDie 1", result.match)
    }

    @Test
    fun invoke_whenCardHasOnFlourishTypeMatch_returnsCorrectMatchString() {
        // Arrange
        val card = GameCard(
            id = 1,
            name = "Type Match Card",
            type = FlourishType.FLOWER,
            resilience = 3,
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
        val result = SUT(card)

        // Assert
        assertEquals("R DrawDie 1", result.match)
    }
} 
