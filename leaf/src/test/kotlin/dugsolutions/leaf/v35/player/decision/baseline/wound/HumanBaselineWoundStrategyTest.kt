package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.*
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineWoundStrategyTest {
    @Test
    fun `all legal wound victims compete and lowest loss Plant is chosen`() {
        val weak = creature(1, "Vine_07_01", "Berry Important", GameEffect.RAISE_ANY_DIE_PLUS_1, 7, PlantType.VINE)
        val strong = creature(2, "Flower_17_04", "Queen's Blossom", GameEffect.DRAW_TWO_DICE, 17, PlantType.FLOWER)
        val context = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(creature = listOf(view(weak), view(strong)))
            )
        )

        val chosen = HumanBaselineWoundStrategy().choose(
            ChooseWoundRequest(
                legalChoices = listOf(WoundChoice.Flip(strong), WoundChoice.Flip(weak)),
                context = context
            )
        )

        assertEquals(weak.id, chosen.card.id)
    }

    private fun creature(id: Int, name: String, title: String, effect: GameEffect, cost: Int, type: PlantType) = CreatureCard(
        CreatureCardId(id),
        PlantCard(6, name, title, type, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        CreatureSide.LEFT,
        CreaturePosition(-id, 0),
        CreatureCard.Facing.FACE_UP
    )

    private fun view(card: CreatureCard) = CreatureCardView(
        card.id, card.card.name, card.card.title, card.card.type, card.card.cost,
        card.card.effect, card.card.scoringRule, card.side, card.position, card.facing, true
    )
}
