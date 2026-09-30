package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CardScoringHelpersSynergyActivationTest {
    @Test
    fun `Saplink activation credits only raises that can actually be realized`() {
        val context = context(
            hand = listOf(DieView(0, 6, 6), DieView(1, 8, 7)),
            plants = listOf(
                plant(1, PlantType.ROOT),
                plant(2, PlantType.ROOT),
                plant(3, PlantType.VINE)
            )
        )

        val score = CardScoringHelpers.playScore(
            context, CardPhase.CULTIVATION,
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE, "Vine_11_02", 0
        )

        assertEquals(5, score.total)
    }

    @Test
    fun `Bloom Backbone activation credits only raises that can actually be realized`() {
        val context = context(
            hand = listOf(DieView(0, 10, 10), DieView(1, 12, 10)),
            plants = listOf(
                plant(1, PlantType.VINE),
                plant(2, PlantType.FLOWER),
                plant(3, PlantType.FLOWER)
            )
        )

        val score = CardScoringHelpers.playScore(
            context, CardPhase.CULTIVATION,
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER, "Flower_11_02", 0
        )

        assertEquals(10, score.total)
    }

    private fun context(hand: List<DieView>, plants: List<CreatureCardView>) =
        DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(hand = hand, creature = plants)
            )
        )

    private fun plant(id: Int, type: PlantType) = CreatureCardView(
        id = CreatureCardId(id),
        name = "test_$id",
        title = "test_$id",
        type = type,
        cost = 7,
        effect = GameEffect.RAISE_ANY_DIE_PLUS_1,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )
}
