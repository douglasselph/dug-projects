package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraftTopologyEvaluatorTest {

    @Test
    fun emptyCreature_hasTwoInitialVineGrowthSlotsAndNoFlowerSlots() {
        assertEquals(2, GraftTopologyEvaluator.openGrowthSlots(emptyList()))
        assertEquals(0, GraftTopologyEvaluator.openFlowerSlots(emptyList()))
        assertEquals(
            setOf(CreaturePosition(-1, 0), CreaturePosition(1, 0)),
            GraftTopologyEvaluator.openGrowthPositions(emptyList())
        )
    }

    @Test
    fun vineCreatesAdditionalFutureGrowthWhileFlowerOnlyConsumes() {
        val creature = listOf(
            card(1, PlantType.VINE, CreatureSide.LEFT, -1, 0)
        )
        val flowerPlacement = GraftPlacement(
            CreatureSide.LEFT,
            CreaturePosition(-2, 0)
        )
        val vinePlacement = flowerPlacement

        assertEquals(3, GraftTopologyEvaluator.openGrowthSlots(creature))
        assertEquals(
            2,
            GraftTopologyEvaluator.futureGrowthSlotsAfter(
                creature,
                PlantType.FLOWER,
                flowerPlacement
            )
        )
        assertTrue(
            GraftTopologyEvaluator.futureGrowthSlotsAfter(
                creature,
                PlantType.VINE,
                vinePlacement
            ) > 2
        )
    }

    @Test
    fun wouldFlowerConsumeLastGrowthSlot_detectsBlockingEndpoint() {
        val creature = listOf(
            card(1, PlantType.VINE, CreatureSide.LEFT, -1, 0),
            card(2, PlantType.VINE, CreatureSide.RIGHT, 1, 0),
            card(3, PlantType.FLOWER, CreatureSide.LEFT, -1, 1),
            card(4, PlantType.FLOWER, CreatureSide.RIGHT, 1, 1),
            card(5, PlantType.FLOWER, CreatureSide.RIGHT, 2, 0)
        )
        val finalSlot = GraftPlacement(
            CreatureSide.LEFT,
            CreaturePosition(-2, 0)
        )

        assertEquals(1, GraftTopologyEvaluator.openGrowthSlots(creature))
        assertTrue(GraftTopologyEvaluator.wouldFlowerConsumeLastGrowthSlot(creature))
        assertTrue(GraftTopologyEvaluator.wouldFlowerConsumeLastGrowthSlot(creature, finalSlot))
        assertEquals(
            0,
            GraftTopologyEvaluator.futureGrowthSlotsAfter(
                creature,
                PlantType.FLOWER,
                finalSlot
            )
        )
        assertFalse(
            GraftTopologyEvaluator.futureGrowthSlotsAfter(
                creature,
                PlantType.VINE,
                finalSlot
            ) == 0
        )
    }

    private fun card(
        id: Int,
        type: PlantType,
        side: CreatureSide,
        x: Int,
        y: Int
    ): CreatureCardView =
        CreatureCardView(
            id = CreatureCardId(id),
            name = "Card_$id",
            title = "Card $id",
            type = type,
            cost = 7,
            effect = GameEffect.UNKNOWN,
            scoringRule = PlantScoringRule.Fixed(0),
            side = side,
            position = CreaturePosition(x, y),
            facing = CreatureCard.Facing.FACE_UP,
            isSnippable = true
        )
}
