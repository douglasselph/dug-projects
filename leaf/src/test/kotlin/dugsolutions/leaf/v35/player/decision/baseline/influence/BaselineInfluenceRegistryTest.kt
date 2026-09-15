package dugsolutions.leaf.v35.player.decision.baseline.influence

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineInfluenceRegistryTest {
    private val registry = BaselineInfluenceRegistry()

    @Test
    fun `owned card influences are additive and preserve original score`() {
        val context = contextWith(
            owned(
                1,
                "Root_07_02",
                PlantType.ROOT,
                GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND
            ),
            owned(
                2,
                "Flower_14_01",
                PlantType.FLOWER,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            )
        )
        val candidate = DecisionCandidate(
            choice = "gain both",
            score = PriorityScore(50),
            tags = setOf(DecisionTag.ACQUIRE_WORM, DecisionTag.ACQUIRE_BEE)
        )

        val scored = registry.score(context, candidate)

        assertEquals(50, candidate.score.total)
        assertEquals(115, scored.score.total)
        assertEquals(2, scored.score.adjustments.size)
        assertTrue(scored.score.adjustments.any { "Root Appreciation" in it.reason })
        assertTrue(scored.score.adjustments.any { "Bee-loved Bloom" in it.reason })
    }

    @Test
    fun `unrelated tags receive no card adjustment`() {
        val context = contextWith(
            owned(
                1,
                "Root_07_02",
                PlantType.ROOT,
                GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND
            )
        )
        val candidate = DecisionCandidate(
            choice = "water",
            score = PriorityScore(40),
            tags = setOf(DecisionTag.ACQUIRE_WATER)
        )

        assertEquals(40, registry.score(context, candidate).score.total)
    }

    private fun contextWith(vararg cards: CreatureCardView): DecisionContext =
        DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(creature = cards.toList())
            )
        )

    private fun owned(
        id: Int,
        name: String,
        type: PlantType,
        effect: GameEffect
    ) = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = type,
        cost = 7,
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
