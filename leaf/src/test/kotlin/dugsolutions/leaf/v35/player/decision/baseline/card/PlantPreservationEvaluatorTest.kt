package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.*
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlantPreservationEvaluatorTest {
    private val evaluator = PlantPreservationEvaluator()

    @Test
    fun `permanent preservation combines intrinsic usefulness printed cost and projected VP`() {
        val root = view(1, "Root_05_01", 5, PlantScoringRule.Fixed(1), PlantType.ROOT, GameEffect.DOUBLE_ONE_DIE)
        val flower = view(2, "Flower_17_04", 17, PlantScoringRule.Fixed(1), PlantType.FLOWER, GameEffect.DRAW_TWO_DICE)
        val context = context(root, flower)

        assertTrue(evaluator(context, flower) > evaluator(context, root))
    }

    @Test
    fun `projected VP remains part of permanent preservation`() {
        val lowVp = view(1, "Root_05_01", 5, PlantScoringRule.Fixed(0), PlantType.ROOT, GameEffect.DOUBLE_ONE_DIE)
        val highVp = lowVp.copy(id = CreatureCardId(2), scoringRule = PlantScoringRule.Fixed(4))
        val lowContext = context(lowVp)
        val highContext = context(highVp)

        assertEquals(40, evaluator(highContext, highVp) - evaluator(lowContext, lowVp))
    }

    private fun context(vararg cards: CreatureCardView) = DecisionContext.EMPTY.copy(
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(creature = cards.toList())
        )
    )

    private fun view(
        id: Int,
        name: String,
        cost: Int,
        scoringRule: PlantScoringRule,
        type: PlantType,
        effect: GameEffect
    ) = CreatureCardView(
        CreatureCardId(id), name, name, type, cost, effect, scoringRule,
        CreatureSide.LEFT, CreaturePosition(-id, 0), CreatureCard.Facing.FACE_DOWN, true
    )
}
